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

/**
 * Service class for managing signature pad operations and data persistence.
 * Handles creation, storage, and retrieval of signature pad configurations
 * using JSON file-based storage system.
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Service
@Slf4j
public class SignaturePadService
{
  /** Object mapper for JSON serialization/deserialization with pretty printing */
  private final ObjectMapper objectMapper = new ObjectMapper()
    .enable(SerializationFeature.INDENT_OUTPUT);

  /** Directory path for storing signature pad configuration files */
  @Value("${app.signature-pad.store-dir:${user.home}/.signaturepads}")
  private String storeDir;

  /**
   * Initializes the service by creating the storage directory if it doesn't exist.
   * Called automatically after bean construction.
   * 
   * @throws IOException if directory creation fails
   */
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

  /**
   * Creates a new signature pad with the specified name.
   * Generates a unique UUID for the signature pad and stores it persistently.
   * 
   * @param name the display name for the new signature pad
   * @return the newly created signature pad instance
   * @throws IOException if storage operation fails
   */
  public SignaturePad createNewSignaturePad(String name)
    throws IOException
  {
    SignaturePad signaturePad = new SignaturePad(name);
    storeSignaturePad(signaturePad);
    return signaturePad;
  }

  /**
   * Retrieves a signature pad by its unique identifier.
   * 
   * @param uuid the unique identifier of the signature pad
   * @return the signature pad instance or null if not found
   * @throws IOException if file access fails
   */
  public SignaturePad getSignaturePadByUUID(String uuid)
    throws IOException
  {
    return loadSignaturePad(uuid);
  }

  /**
   * Stores the given signature pad object as a JSON file.
   * The file is saved as {storeDir}/{uuid}.json with pretty-printed formatting.
   * 
   * @param pad the signature pad to store
   * @throws IOException if file writing fails
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
   * Loads a signature pad object from the JSON file {storeDir}/{uuid}.json.
   * Returns null if the file does not exist.
   * 
   * @param uuid the unique identifier of the signature pad to load
   * @return the signature pad instance or null if file not found
   * @throws IOException if file reading or JSON parsing fails
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
