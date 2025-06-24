
/*
 * Copyright 2024 Thorsten Ludewig (t.ludewig@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package l9g.webapp.signaturepaddemo.controller.api;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import l9g.webapp.signaturepaddemo.service.SignaturePad;
import l9g.webapp.signaturepaddemo.service.SignaturePadService;
import l9g.webapp.signaturepaddemo.service.SignedJwtService;
import l9g.webapp.signaturepaddemo.dto.DtoEvent;
import l9g.webapp.signaturepaddemo.ws.SignaturePadWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/v1/signature-pad",
                produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiSignaturePadController
{
  private static final String VALIDATE_NEW_PAD = "/admin/validate-new-pad";

  private final SignaturePadService signaturePadService;

  private final SignaturePadWebSocketHandler signaturePadWebSocketHandler;

  private final SignedJwtService signedJwtService;

  private final AuthService authService;

  private final Map<String, DeferredResult<ResponsePayload>> waitingRequests =
    new ConcurrentHashMap<>();

  @Value("${app.base-url}")
  private String appBaseUrl;

  @Value("${app.signature-pad.timeout:180000}")
  private long signaturePadTimeout;

  @GetMapping("/wait-for-response")
  @ResponseBody
  public DeferredResult<ResponsePayload> waitForResponse(@RequestParam(name = "uuid") String padUuid)
  {
    log.debug("waitForResponse {}", padUuid);

    DeferredResult<ResponsePayload> checkDeferred = waitingRequests.get(padUuid);

    if(checkDeferred != null)
    {
      checkDeferred.setResult(null);
      waitingRequests.remove(padUuid);
    }

    DeferredResult<ResponsePayload> deferred = new DeferredResult<>(signaturePadTimeout);

    deferred.onTimeout(() ->
    {
      log.warn("Timeout bei padUuid={}", padUuid);
      try
      {
        hide(padUuid);
      }
      catch(IOException ex)
      {
        log.error("hide signature pad", ex);
      }
      ResponsePayload payload = new ResponsePayload("timeout", null);
      deferred.setResult(payload);
    });

    deferred.onCompletion(() -> waitingRequests.remove(padUuid));
    waitingRequests.put(padUuid, deferred);

    log.debug("waitForResponse - done");
    return deferred;
  }

  @GetMapping(path = "/connect-qrcode", produces = MediaType.IMAGE_PNG_VALUE)
  public void connectQrcode(
    @RequestParam("uuid") String uuid,
    HttpServletResponse response
  )
    throws IOException
  {
    String targetUrl = appBaseUrl
      + VALIDATE_NEW_PAD
      + "?uuid="
      + uuid;

    log.debug("Generating QR code for URL: {}", targetUrl);

    int width = 300;
    int height = 300;

    Map<EncodeHintType, Object> hints = new HashMap<>();
    hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
    hints.put(EncodeHintType.MARGIN, 2);

    try
    {
      QRCodeWriter qrWriter = new QRCodeWriter();
      BitMatrix bitMatrix = qrWriter.encode(targetUrl, BarcodeFormat.QR_CODE, width, height, hints);

      response.setContentType(MediaType.IMAGE_PNG_VALUE);
      MatrixToImageWriter.writeToStream(bitMatrix, "PNG", response.getOutputStream());
      response.getOutputStream().flush();
    }
    catch(WriterException e)
    {
      log.error("Failed to generate QR code", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "QR code generation failed");
    }
  }

  @PostMapping(path = "/validate",
               consumes = MediaType.TEXT_PLAIN_VALUE,
               produces = MediaType.APPLICATION_JSON_VALUE)
  public void validate(
    @RequestHeader("SIGNATURE_PAD_UUID") String padUuid,
    @RequestBody String signatureJwt
  )
    throws IOException, ParseException
  {
    log.debug("validate called");
    log.debug("Received JWT length: {}", signatureJwt.length());

    SignaturePad signaturePad = authService.authCheck(padUuid, false);
    SignedJWT signedJWT = authService.verifyJwt(signaturePad, signatureJwt);

    try
    {
      Map<String, Object> publicJwkMap = signedJWT.getJWTClaimsSet().getJSONObjectClaim("publicJwk");

      log.trace("public jwk : {}", publicJwkMap);

      JWK jwk = JWK.parse(publicJwkMap);

      if( ! (jwk instanceof RSAKey))
      {
        throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "JWK is not an RSA key"
        );
      }

      signaturePad.setPublicJwk(publicJwkMap);
      signaturePad.setClientEnvironment(signedJWT.getJWTClaimsSet().getJSONObjectClaim("clientEnvironment"));
      signaturePad.setValidated(true);
      signaturePadService.storeSignaturePad(signaturePad);

      String issuer = signedJWT.getJWTClaimsSet().getIssuer();
      log.debug("issuer: {}", issuer);
    }
    catch(ParseException e)
    {
      log.error("Error parsing or verifying JWT", e);
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Invalid JWT payload or signature"
      );
    }
  }

  @PostMapping(path = "/signature",
               consumes = MediaType.TEXT_PLAIN_VALUE,
               produces = MediaType.APPLICATION_JSON_VALUE)
  public void signature(
    @RequestHeader("SIGNATURE_PAD_UUID") String padUuid,
    @RequestBody String signatureJwt
  )
    throws IOException, ParseException
  {
    log.debug("signature called");
    log.debug("Received JWT length: {}", signatureJwt.length());

    SignaturePad signaturePad = authService.authCheck(padUuid, true);
    SignedJWT signedJWT = authService.verifyJwt(signaturePad, signatureJwt);

    DeferredResult<ResponsePayload> deferred = waitingRequests.get(padUuid);

    try
    {
      String issuer = signedJWT.getJWTClaimsSet().getIssuer();
      String kid = signedJWT.getHeader().getKeyID();
      String sigpngBase64 = signedJWT.getJWTClaimsSet().getClaimAsString("sigpng");
      String sigsvgBase64 = signedJWT.getJWTClaimsSet().getClaimAsString("sigsvg");

      Instant iatInstant = signedJWT.getJWTClaimsSet().getIssueTime().toInstant();
      long iatEpoch = iatInstant.getEpochSecond();
      DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.of("Europe/Berlin"));
      String iatReadable = fmt.format(iatInstant);

      log.info("JWT verified. Issuer: {} / {}", issuer, kid);
      log.info("Issued At (iat): {} (epoch: {})", iatReadable, iatEpoch);
      log.debug("sigpng length: {}, sigsvg length: {}",
        sigpngBase64.length(), sigsvgBase64.length());
      log.debug("sigpad={}", signedJWT.getJWTClaimsSet().getClaimAsString("sigpad"));
      log.debug("name={}", signedJWT.getJWTClaimsSet().getClaimAsString("name"));
      log.debug("mail={}", signedJWT.getJWTClaimsSet().getClaimAsString("mail"));

      signedJwtService.storeSignedJWT(signedJWT.getJWTClaimsSet().getSubject(), signatureJwt);

      if(deferred != null)
      {
        ResponsePayload payload = new ResponsePayload("ok", sigpngBase64);
        deferred.setResult(payload);
      }
    }
    catch(ParseException e)
    {
      log.error("Error parsing or verifying JWT", e);

      if(deferred != null)
      {
        ResponsePayload payload = new ResponsePayload("error", null);
        deferred.setResult(payload);
      }

      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Invalid JWT payload or signature"
      );
    }
  }

  @PostMapping(path = "/cancel",
               consumes = MediaType.APPLICATION_JSON_VALUE,
               produces = MediaType.APPLICATION_JSON_VALUE)
  public void cancel(
    @RequestHeader("SIGNATURE_PAD_UUID") String padUuid,
    @RequestBody Map<String, Object> json
  )
    throws IOException
  {
    log.debug("cancel button pressed");
    log.info("json: {}", json);

    SignaturePad signaturePad = authService.authCheck(padUuid, true);

    DeferredResult<ResponsePayload> deferred = waitingRequests.get(padUuid);
    if(deferred != null)
    {
      ResponsePayload payload = new ResponsePayload("cancel", null);
      deferred.setResult(payload);
    }
  }

  @GetMapping(path = "/show",
              produces = MediaType.APPLICATION_JSON_VALUE)
  public void show(
    @RequestParam("uuid") String padUuid,
    @RequestParam("uid") String userId
  )
    throws IOException
  {
    log.debug("show padUuid = {}, uid = {}", padUuid, userId);
    signaturePadWebSocketHandler
      .fireEventToPad(new DtoEvent(DtoEvent.EVENT_SHOW, userId), padUuid);
  }

  @GetMapping(path = "/hide",
              produces = MediaType.APPLICATION_JSON_VALUE)
  public void hide(
    @RequestParam("uuid") String padUuid
  )
    throws IOException
  {
    log.debug("hide padUuid = {}", padUuid);
    signaturePadWebSocketHandler
      .fireEventToPad(new DtoEvent(DtoEvent.EVENT_HIDE, "hide"), padUuid);
  }

}
