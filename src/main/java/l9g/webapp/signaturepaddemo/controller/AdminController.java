/*
 * Copyright 2025 Thorsten Ludewig (t.ludewig@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package l9g.webapp.signaturepaddemo.controller;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import l9g.webapp.signaturepaddemo.service.SignaturePad;
import l9g.webapp.signaturepaddemo.service.SignaturePadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Controller
@Slf4j
@RequestMapping(path = "/admin")
@RequiredArgsConstructor
public class AdminController
{
  private final SignaturePadService signaturePadService;

  @GetMapping("/register-new-pad")
  public String registerNewPad(Model model)
  {
    log.debug("register-new-pad");
    Locale locale = LocaleContextHolder.getLocale();
    log.debug("locale={}", locale);
    model.addAttribute("locale", locale.toString());
    return "register-new-pad";
  }

  @PostMapping("/connect-new-pad")
  public String connectNewPad(@RequestParam("name") String padName, Model model)
    throws IOException
  {
    log.debug("connect-new-pad called");
    log.info("New pad name: {}", padName);
    Locale locale = LocaleContextHolder.getLocale();
    log.debug("locale={}", locale);

    SignaturePad signaturePad =
      signaturePadService.createNewSignaturePad(padName);

    model.addAttribute("locale", locale.toString());
    model.addAttribute("pad", signaturePad);
    return "connect-new-pad";
  }

  @GetMapping("/validate-new-pad")
  public String verifyNewPad(@RequestParam("uuid") String padUUID, Model model)
    throws NoSuchAlgorithmException, IOException
  {
    log.debug("validate-new-pad uuid={}", padUUID);
    Locale locale = LocaleContextHolder.getLocale();
    log.debug("locale={}", locale);

    SignaturePad signaturePad =
      signaturePadService.getSignaturePadByUUID(padUUID);

    if(signaturePad == null)
    {
      throw new ResponseStatusException(
        HttpStatus.NOT_FOUND,
        "Signature pad UUID not found!"
      );
    }

    if(signaturePad.isValidated())
    {
      log.error("Signature pad '{}/{}' already validated!", signaturePad.getUuid(), signaturePad.getName());
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Signature pad already validated!"
      );
    }

    String privateJwk = signaturePad.createPrivateJWK();
    signaturePadService.storeSignaturePad(signaturePad);
    model.addAttribute("locale", locale.toString());
    model.addAttribute("pad", signaturePad);
    model.addAttribute("padJwkJson", privateJwk);
    return "validate-new-pad";
  }

  @GetMapping("/wait-for-response")
  public String waitForResponse(
    @RequestParam("uuid") String padUUID, 
    @RequestParam("userId") String userId, 
    Model model)
    throws NoSuchAlgorithmException, IOException
  {
    log.debug("wait-for-response uuid={}", padUUID);
    Locale locale = LocaleContextHolder.getLocale();
    log.debug("locale={}", locale);

    SignaturePad signaturePad =
      signaturePadService.getSignaturePadByUUID(padUUID);

    if(signaturePad == null)
    {
      throw new ResponseStatusException(
        HttpStatus.NOT_FOUND,
        "Signature pad UUID not found!"
      );
    }

    model.addAttribute("locale", locale.toString());
    model.addAttribute("pad", signaturePad);
    model.addAttribute("userId", userId);
    return "wait-for-response";
  }

}
