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
package l9g.webapp.signaturepaddemo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
public class SignaturePadService
{
  private final ObjectMapper objectMapper = new ObjectMapper()
    .enable(SerializationFeature.INDENT_OUTPUT);

  @Value("${app.signature-pad.store-dir:${user.home}/.signaturepads}")
  private String storeDir;

  @PostConstruct
  public void init()
    throws IOException
  {
    Path dir = Paths.get(storeDir);
    if(Files.notExists(dir))
    {
      Files.createDirectories(dir);
      log.info("Created signature pad store directory: {}", dir);
    }
  }

  public SignaturePad createNewSignaturePad(String name)
    throws IOException
  {
    SignaturePad signaturePad = new SignaturePad(name);
    storeSignaturePad(signaturePad);
    return signaturePad;
  }

  public SignaturePad getSignaturePadByUUID(String uuid)
    throws IOException
  {
    return loadSignaturePad(uuid);
  }

  /**
   * Speichert das gegebene SignaturePad-Objekt als JSON-Datei unter
   * {storeDir}/{uuid}.json.
   */
  public void storeSignaturePad(SignaturePad pad)
    throws IOException
  {
    String filename = pad.getUuid() + ".json";
    Path file = Paths.get(storeDir, filename);
    objectMapper.writeValue(file.toFile(), pad);
    log.info("SignaturePad stored: {}", file.toAbsolutePath());
  }

  /**
   * Lädt ein SignaturePad-Objekt von der JSON-Datei
   * {storeDir}/{uuid}.json. Gibt null zurück, wenn die Datei nicht existiert.
   */
  public SignaturePad loadSignaturePad(String uuid)
    throws IOException
  {
    Path file = Paths.get(storeDir, uuid + ".json");
    if(Files.exists(file))
    {
      SignaturePad pad = objectMapper.readValue(file.toFile(), SignaturePad.class);
      log.info("SignaturePad loaded: {}", file.toAbsolutePath());
      return pad;
    }
    log.warn("SignaturePad file not found: {}", file.toAbsolutePath());
    return null;
  }

}
