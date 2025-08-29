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

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller responsible for handling signature pad related web requests.
 * Provides endpoints for displaying signature pad interfaces with proper
 * internationalization and WebSocket configuration.
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class SignaturePadController
{
  /** WebSocket base URL for real-time communication with signature pad devices */
  @Value("${app.ws-url}")
  private String wsBaseUrl;

  /** Flag indicating whether heartbeat functionality is enabled for connection monitoring */
  @Value("${scheduler.heartbeat.enabled}")
  private boolean heartbeatEnabled;

  /**
   * Displays the main signature pad interface.
   * Sets up the necessary model attributes for localization and WebSocket connectivity.
   * 
   * @param model Spring MVC model for passing data to the view
   * @return the name of the signature-pad template to render
   */
  @GetMapping("/signature-pad")
  public String signaturePad(Model model)
  {
    log.debug("signaturePad");
    Locale locale = LocaleContextHolder.getLocale();
    log.debug("locale={}", locale);
    model.addAttribute("locale", locale.toString());
    model.addAttribute("wsBaseUrl", wsBaseUrl);
    model.addAttribute("heartbeatEnabled", heartbeatEnabled);
    return "signpad2";
    // return "signature-pad";
  }
  
  /**
   * Displays an alternative signature pad interface (version 2).
   * Similar to the main signature pad but with different UI implementation.
   * 
   * @param model Spring MVC model for passing data to the view
   * @return the name of the signpad2 template to render
   */
  @GetMapping("/signpad2")
  public String signaturePad2(Model model)
  {
    log.debug("signpad2");
    Locale locale = LocaleContextHolder.getLocale();
    log.debug("locale={}", locale);
    model.addAttribute("locale", locale.toString());
    model.addAttribute("wsBaseUrl", wsBaseUrl);
    model.addAttribute("heartbeatEnabled", heartbeatEnabled);
    return "signpad2";
  }

}
