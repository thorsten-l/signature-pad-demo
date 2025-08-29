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

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service class for managing signed JWT storage and retrieval.
 * Handles persistence of signed JWTs to the file system for later verification
 * and audit purposes.
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Service
@Slf4j
public class SignedJwtService
{
  /** Directory path for storing signed JWT files */
  @Value("${app.signed-jwt.store-dir:${user.home}/.signedjwt}")
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
      log.info("Created SignedJwt store directory: {}", dir);
    }
  }

  /**
   * Stores a signed JWT to the file system.
   * The JWT is saved as {storeDir}/{subject}.jwt using UTF-8 encoding.
   * 
   * @param subject the subject (user identifier) associated with the JWT
   * @param signedJWT the signed JWT string to store
   * @throws IOException if file writing fails
   */
  public void storeSignedJWT(String subject, String signedJWT)
    throws IOException
  {
    Path file = Paths.get(storeDir, subject + ".jwt");
    Files.writeString(file, signedJWT, StandardCharsets.UTF_8);
    log.info("SignedJwt stored: {}", file.toAbsolutePath());
  }

  /**
   * Loads a signed JWT from the file system by subject.
   * Returns null if the file does not exist.
   * 
   * @param subject the subject (user identifier) whose JWT to load
   * @return the signed JWT string or null if not found
   * @throws IOException if file reading fails
   */
  public String loadSignedJWT(String subject)
    throws IOException
  {
    Path file = Paths.get(storeDir, subject + ".jwt");
    
    if(Files.exists(file))
    {
      String jwt = Files.readString(file, StandardCharsets.UTF_8);
      log.info("SignedJWT loaded: {}", file.toAbsolutePath());
      return jwt;
    }
    
    log.warn("SignedJWT file not found: {}", file.toAbsolutePath());
    return null;
  }

}
