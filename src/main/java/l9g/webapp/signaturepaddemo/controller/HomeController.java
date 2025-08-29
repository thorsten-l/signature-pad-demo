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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import l9g.webapp.signaturepaddemo.service.SignaturePad;
import l9g.webapp.signaturepaddemo.service.SignaturePadService;
import l9g.webapp.signaturepaddemo.ws.SignaturePadSession;
import l9g.webapp.signaturepaddemo.ws.SignaturePadWebSocketConfig;
import l9g.webapp.signaturepaddemo.ws.SignaturePadWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller responsible for handling home page and dashboard functionality.
 * Displays overview of active signature pad sessions and provides navigation
 * to various signature pad features.
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class HomeController
{
  /** Service for managing signature pad operations and data persistence */
  private final SignaturePadService signaturePadService;
  
  /** WebSocket handler for managing real-time communication with signature pads */
  private final SignaturePadWebSocketHandler signaturePadWebSocketHandler;

  /**
   * Displays the main home page with an overview of active signature pad sessions.
   * Sets up localization and provides a list of currently connected signature pads.
   * 
   * @param model Spring MVC model for passing data to the view
   * @return the name of the home template to render
   */
  @GetMapping("/")
  public String home(Model model)
  {
    log.debug("home");
    Locale locale = LocaleContextHolder.getLocale();
    log.debug("locale={}", locale);
    model.addAttribute("locale", locale.toString());
    model.addAttribute("signaturePadSessions", getSignaturePadSessions());
    return "home";
  }

  /**
   * Retrieves a list of active signature pad sessions from WebSocket connections.
   * Filters sessions to only include those with valid signature pad UUIDs and
   * enriches them with signature pad name information.
   * 
   * @return list of active signature pad sessions with their details
   */
  public List<SignaturePadSession> getSignaturePadSessions()
  {
    final ArrayList<SignaturePadSession> list = new ArrayList<>();

    // Iterate through all active WebSocket sessions
    signaturePadWebSocketHandler.getSessionsBySessionId().forEach((id, session) ->
    {
      // Extract signature pad UUID from session attributes
      String padUuid = (String)session.getAttributes()
        .get(SignaturePadWebSocketConfig.SIGNATURE_PAD_UUID);

      // Only process sessions with valid signature pad UUIDs
      if(padUuid != null &&  ! padUuid.isEmpty())
      {
        try
        {
          // Fetch signature pad details and create session info
          SignaturePad signaturePad = signaturePadService.getSignaturePadByUUID(padUuid);
          list.add(new SignaturePadSession(id, padUuid, signaturePad.getName()));
        }
        catch(IOException ex)
        {
          // Silently ignore IO exceptions when fetching signature pad details
          // This could happen if the signature pad data is temporarily unavailable
        }
      }
    });

    log.debug( "{} signature pad sessions running", list.size() );
    return list;
  }

}
