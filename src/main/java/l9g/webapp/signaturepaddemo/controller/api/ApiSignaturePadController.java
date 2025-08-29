
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
 * REST API controller for signature pad operations and management.
 * Provides endpoints for signature pad authentication, validation, signature capture,
 * QR code generation, and real-time communication with signature pad devices.
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
  /** Path template for signature pad validation endpoint */
  private static final String VALIDATE_NEW_PAD = "/admin/validate-new-pad";

  /** Service for managing signature pad operations and data persistence */
  private final SignaturePadService signaturePadService;

  /** WebSocket handler for real-time communication with signature pad devices */
  private final SignaturePadWebSocketHandler signaturePadWebSocketHandler;

  /** Service for managing signed JWT operations and storage */
  private final SignedJwtService signedJwtService;

  /** Service for authentication and authorization operations */
  private final AuthService authService;

  /** 
   * Map storing deferred results for asynchronous signature requests.
   * Key: signature pad UUID, Value: deferred result waiting for response
   */
  private final Map<String, DeferredResult<ResponsePayload>> waitingRequests =
    new ConcurrentHashMap<>();

  /** Base URL of the application for generating absolute URLs */
  @Value("${app.base-url}")
  private String appBaseUrl;

  /** Timeout in milliseconds for signature pad operations */
  @Value("${app.signature-pad.timeout:180000}")
  private long signaturePadTimeout;

  /**
   * Establishes a long-polling connection to wait for signature responses.
   * Creates a deferred result that will be completed when a signature is captured
   * or when the request times out.
   * 
   * @param padUuid the unique identifier of the signature pad
   * @return deferred result that will contain the signature response
   */
  @GetMapping("/wait-for-response")
  @ResponseBody
  public DeferredResult<ResponsePayload> waitForResponse(@RequestParam(name = "uuid") String padUuid)
  {
    log.debug("waitForResponse {}", padUuid);

    // Cancel any existing deferred request for this signature pad
    DeferredResult<ResponsePayload> oldDeferred = waitingRequests.get(padUuid);

    if(oldDeferred != null)
    {
      oldDeferred.setResult(new ResponsePayload("cancel", null));
      waitingRequests.remove(padUuid);
    }

    // Create new deferred result with configured timeout
    DeferredResult<ResponsePayload> deferred = new DeferredResult<>(signaturePadTimeout);

    // Configure timeout behavior
    deferred.onTimeout(() ->
    {
      log.warn("Timeout bei padUuid={}", padUuid);
      try
      {
        // Check if this deferred result is still the active one
        DeferredResult<ResponsePayload> checkDeferred = waitingRequests.get(padUuid);
        if(checkDeferred != null)
        {
          if(checkDeferred.equals(deferred))
          {
            log.debug("sending hide message");
            hide(padUuid); // Hide signature pad interface on timeout
          }
          else
          {
            log.debug("checkDeferred != deferred");
          }
        }
        else
        {
          log.debug("checkDeferred == null");
        }
      }
      catch(IOException ex)
      {
        log.error("hide signature pad", ex);
      }
      ResponsePayload payload = new ResponsePayload("timeout", null);
      deferred.setResult(payload);
    });

    // Clean up when request completes
    deferred.onCompletion(() -> waitingRequests.remove(padUuid));
    waitingRequests.put(padUuid, deferred);

    log.debug("waitForResponse - done");
    return deferred;
  }

  /**
   * Generates a QR code image for signature pad connection.
   * Creates a QR code containing the validation URL that signature pads can scan
   * to establish connection with the system.
   * 
   * @param uuid the unique identifier of the signature pad
   * @param response HTTP response to write the QR code image to
   * @throws IOException if QR code generation or response writing fails
   */
  @GetMapping(path = "/connect-qrcode", produces = MediaType.IMAGE_PNG_VALUE)
  public void connectQrcode(
    @RequestParam("uuid") String uuid,
    HttpServletResponse response
  )
    throws IOException
  {
    // Construct the validation URL for the QR code
    String targetUrl = appBaseUrl
      + VALIDATE_NEW_PAD
      + "?uuid="
      + uuid;

    log.debug("Generating QR code for URL: {}", targetUrl);

    int width = 300;
    int height = 300;

    // Configure QR code generation parameters
    Map<EncodeHintType, Object> hints = new HashMap<>();
    hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
    hints.put(EncodeHintType.MARGIN, 2);

    try
    {
      // Generate QR code and write to response
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

  /**
   * Validates a signature pad by processing its validation JWT.
   * Extracts the public key from the JWT and marks the signature pad as validated
   * in the system, enabling it for signature operations.
   * 
   * @param padUuid the unique identifier of the signature pad
   * @param signatureJwt the validation JWT containing public key and environment info
   * @throws IOException if signature pad data access fails
   * @throws ParseException if JWT parsing fails
   * @throws ResponseStatusException if validation fails or pad not found
   */
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

    // Authenticate signature pad and verify JWT
    SignaturePad signaturePad = authService.authCheck(padUuid, false);
    SignedJWT signedJWT = authService.verifyJwt(signaturePad, signatureJwt);

    try
    {
      // Extract public key from JWT claims
      Map<String, Object> publicJwkMap = signedJWT.getJWTClaimsSet().getJSONObjectClaim("publicJwk");

      log.trace("public jwk : {}", publicJwkMap);

      JWK jwk = JWK.parse(publicJwkMap);

      // Ensure the key is an RSA key as expected
      if( ! (jwk instanceof RSAKey))
      {
        throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "JWK is not an RSA key"
        );
      }

      // Store validation information and mark as validated
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

  /**
   * Processes a signature captured by a signature pad.
   * Verifies the signature JWT, extracts signature data, and notifies waiting
   * clients about the signature completion.
   * 
   * @param padUuid the unique identifier of the signature pad
   * @param signatureJwt the signature JWT containing captured signature data
   * @throws IOException if signature pad data access fails
   * @throws ParseException if JWT parsing fails
   * @throws ResponseStatusException if verification fails
   */
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

    // Authenticate signature pad and verify JWT
    SignaturePad signaturePad = authService.authCheck(padUuid, true);
    SignedJWT signedJWT = authService.verifyJwt(signaturePad, signatureJwt);

    // Get waiting deferred result for this signature pad
    DeferredResult<ResponsePayload> deferred = waitingRequests.get(padUuid);

    try
    {
      // Extract signature information from JWT
      String issuer = signedJWT.getJWTClaimsSet().getIssuer();
      String kid = signedJWT.getHeader().getKeyID();
      String sigpngBase64 = signedJWT.getJWTClaimsSet().getClaimAsString("sigpng");
      String sigsvgBase64 = signedJWT.getJWTClaimsSet().getClaimAsString("sigsvg");

      // Extract and format timestamp information
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

      // Store the signed JWT for later retrieval
      // Store the signed JWT for later retrieval
      signedJwtService.storeSignedJWT(signedJWT.getJWTClaimsSet().getSubject(), signatureJwt);

      // Notify waiting client with signature data
      if(deferred != null)
      {
        ResponsePayload payload = new ResponsePayload("ok", sigpngBase64);
        deferred.setResult(payload);
      }
    }
    catch(ParseException e)
    {
      log.error("Error parsing or verifying JWT", e);

      // Notify waiting client of error
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

  /**
   * Handles signature pad cancellation requests.
   * Notifies waiting clients that the signature operation was cancelled
   * by the user on the signature pad device.
   * 
   * @param padUuid the unique identifier of the signature pad
   * @param json the cancellation request data
   * @throws IOException if signature pad communication fails
   */
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

    // Authenticate signature pad
    authService.authCheck(padUuid, true);

    // Notify waiting client of cancellation
    DeferredResult<ResponsePayload> deferred = waitingRequests.get(padUuid);
    if(deferred != null)
    {
      ResponsePayload payload = new ResponsePayload("cancel", null);
      deferred.setResult(payload);
    }
  }

  /**
   * Shows a signature request on the specified signature pad.
   * Sends a show event to the signature pad device to display signature
   * interface for the specified user.
   * 
   * @param padUuid the unique identifier of the signature pad
   * @param cardNumber the identifier of the user requesting the signature
   * @throws IOException if WebSocket communication fails
   */
  @GetMapping(path = "/show",
              produces = MediaType.APPLICATION_JSON_VALUE)
  public void show(
    @RequestParam("uuid") String padUuid,
    @RequestParam("card") String cardNumber
  )
    throws IOException
  {
    log.debug("show padUuid = {}, card = {}", padUuid, cardNumber);
    signaturePadWebSocketHandler
      .fireEventToPad(new DtoEvent(DtoEvent.EVENT_SHOW, cardNumber), padUuid);
  }

  /**
   * Hides the signature interface on the specified signature pad.
   * Sends a hide event to the signature pad device to clear the display.
   * 
   * @param padUuid the unique identifier of the signature pad
   * @throws IOException if WebSocket communication fails
   */
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
