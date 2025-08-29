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
 * Controller responsible for signature pad administration functionality.
 * Provides endpoints for registering, connecting, validating signature pads
 * and managing their lifecycle within the system.
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Controller
@Slf4j
@RequestMapping(path = "/admin")
@RequiredArgsConstructor
public class AdminController
{
  /** Service for managing signature pad operations and data persistence */
  private final SignaturePadService signaturePadService;

  /**
   * Displays the registration form for creating a new signature pad.
   * Provides the interface for administrators to initiate the signature pad setup process.
   * 
   * @param model Spring MVC model for passing data to the view
   * @return the name of the register-new-pad template to render
   */
  @GetMapping("/register-new-pad")
  public String registerNewPad(Model model)
  {
    log.debug("register-new-pad");
    Locale locale = LocaleContextHolder.getLocale();
    log.debug("locale={}", locale);
    model.addAttribute("locale", locale.toString());
    return "register-new-pad";
  }

  /**
   * Processes the creation of a new signature pad with the specified name.
   * Creates a new signature pad instance and displays connection instructions.
   * 
   * @param padName the display name for the new signature pad
   * @param model Spring MVC model for passing data to the view
   * @return the name of the connect-new-pad template to render
   * @throws IOException if signature pad creation fails
   */
  @PostMapping("/connect-new-pad")
  public String connectNewPad(@RequestParam("name") String padName, Model model)
    throws IOException
  {
    log.debug("connect-new-pad called");
    log.info("New pad name: {}", padName);
    Locale locale = LocaleContextHolder.getLocale();
    log.debug("locale={}", locale);

    // Create new signature pad with the provided name
    SignaturePad signaturePad =
      signaturePadService.createNewSignaturePad(padName);

    model.addAttribute("locale", locale.toString());
    model.addAttribute("pad", signaturePad);
    return "connect-new-pad";
  }

  /**
   * Validates and finalizes the setup of a newly created signature pad.
   * Generates cryptographic keys for the signature pad and marks it as validated.
   * 
   * @param padUUID the unique identifier of the signature pad to validate
   * @param model Spring MVC model for passing data to the view
   * @return the name of the validate-new-pad template to render
   * @throws NoSuchAlgorithmException if cryptographic algorithm is not available
   * @throws IOException if signature pad data access fails
   * @throws ResponseStatusException if signature pad not found or already validated
   */
  @GetMapping("/validate-new-pad")
  public String verifyNewPad(@RequestParam("uuid") String padUUID, Model model)
    throws NoSuchAlgorithmException, IOException
  {
    log.debug("validate-new-pad uuid={}", padUUID);
    Locale locale = LocaleContextHolder.getLocale();
    log.debug("locale={}", locale);

    // Retrieve signature pad by UUID
    SignaturePad signaturePad =
      signaturePadService.getSignaturePadByUUID(padUUID);

    // Validate signature pad exists
    if(signaturePad == null)
    {
      throw new ResponseStatusException(
        HttpStatus.NOT_FOUND,
        "Signature pad UUID not found!"
      );
    }

    // Ensure signature pad is not already validated
    if(signaturePad.isValidated())
    {
      log.error("Signature pad '{}/{}' already validated!", signaturePad.getUuid(), signaturePad.getName());
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Signature pad already validated!"
      );
    }

    // Generate private JWK for the signature pad and store it
    String privateJwk = signaturePad.createPrivateJWK();
    signaturePadService.storeSignaturePad(signaturePad);
    model.addAttribute("locale", locale.toString());
    model.addAttribute("pad", signaturePad);
    model.addAttribute("padJwkJson", privateJwk);
    return "validate-new-pad";
  }

  /**
   * Displays a waiting page for signature responses from signature pads.
   * Shows status information while waiting for user interaction with the signature pad.
   * 
   * @param padUUID the unique identifier of the signature pad
   * @param card the identifier of the user requesting the signature
   * @param model Spring MVC model for passing data to the view
   * @return the name of the wait-for-response template to render
   * @throws NoSuchAlgorithmException if cryptographic algorithm is not available
   * @throws IOException if signature pad data access fails
   * @throws ResponseStatusException if signature pad not found
   */
  @GetMapping("/wait-for-response")
  public String waitForResponse(
    @RequestParam("uuid") String padUUID, 
    @RequestParam("card") String cardNumber, 
    Model model)
    throws NoSuchAlgorithmException, IOException
  {
    log.debug("wait-for-response uuid={} card='{}'", padUUID, cardNumber );
    Locale locale = LocaleContextHolder.getLocale();
    log.debug("locale={}", locale);

    // Retrieve and validate signature pad exists
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
    model.addAttribute("card", cardNumber);
    return "wait-for-response";
  }

}
