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
package l9g.webapp.signaturepaddemo.controller.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import l9g.webapp.signaturepaddemo.dto.DtoAddress;
import l9g.webapp.signaturepaddemo.dto.DtoUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/v1/userinfo",
                produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiUserInfoController
{
  private final AuthService authService;

  @GetMapping(
    produces = MediaType.APPLICATION_JSON_VALUE)
  public DtoUserInfo userinfo(
    @RequestHeader("SIGNATURE_PAD_UUID") String padUuid,
    @RequestParam("userid") String userId
  )
    throws IOException
  {
    log.debug("userinfo called for {}", userId);
    authService.authCheck(padUuid, true);

    if (!userId.equalsIgnoreCase("user123"))
    {
      throw new ResponseStatusException(
        HttpStatus.NOT_FOUND,
        "ERROR: Unknown userid"
      );
    }
    
    DtoAddress semester = new DtoAddress(
      "c/o M. Maier", "Musterstr. Str 1701", "38302", "Wolfenbüttel", 
      "Niedersachsen", "Deutschland");
    DtoAddress home = new DtoAddress(
      null, "Neuer Weg 4711", "38302", "Wolfenbüttel", 
      "Niedersachsen", "Deutschland");

    String jpegPhoto;
    ClassPathResource imgFile = new ClassPathResource("demo/MarieMuster.jpg");

    try(InputStream is = imgFile.getInputStream())
    {
      byte[] imageBytes = is.readAllBytes();
      String base64 = Base64.getEncoder().encodeToString(imageBytes);
      jpegPhoto = "data:image/jpeg;base64," + base64;
    }

    DtoUserInfo userInfo = new DtoUserInfo(
      jpegPhoto, "Marie", "Muster", "user123", "m.muster@the.net", 
      "01.01.2005", semester, home);
    
    return userInfo;
  }

}
