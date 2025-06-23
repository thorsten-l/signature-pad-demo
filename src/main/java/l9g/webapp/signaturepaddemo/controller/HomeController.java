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
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class HomeController
{
  private final SignaturePadService signaturePadService;
  private final SignaturePadWebSocketHandler signaturePadWebSocketHandler;

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

  public List<SignaturePadSession> getSignaturePadSessions()
  {
    final ArrayList<SignaturePadSession> list = new ArrayList<>();

    signaturePadWebSocketHandler.getSessionsBySessionId().forEach((id, session) ->
    {
      String padUuid = (String)session.getAttributes()
        .get(SignaturePadWebSocketConfig.SIGNATURE_PAD_UUID);

      if(padUuid != null &&  ! padUuid.isEmpty())
      {
        try
        {
          SignaturePad signaturePad = signaturePadService.getSignaturePadByUUID(padUuid);
          list.add(new SignaturePadSession(id, padUuid, signaturePad.getName()));
        }
        catch(IOException ex)
        {
          // do nothing
        }
      }
    });

    log.debug( "{} signature pad sessions running", list.size() );
    return list;
  }

}
