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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Slf4j
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SignaturePad
{
  private final String uuid;

  @Setter
  private String name;

  @Setter
  private boolean validated;

  @Setter
  private Map<String,Object> clientEnvironment;

  private int version;

  private long ttl;
  
  @Setter
  private Map<String,Object> publicJwk;
  
  public SignaturePad()
  {
    uuid = UUID.randomUUID().toString();
  }

  public SignaturePad(String name)
  {
    this();
    this.name = name;
  }

  @JsonIgnore
  public String getKeyId()
  {
    return uuid + "-" + version;
  }
  
  public String createPrivateJWK()
    throws NoSuchAlgorithmException
  {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(2048);
    KeyPair keyPair = keyGen.generateKeyPair();
    RSAPrivateKey privateKey = (RSAPrivateKey)keyPair.getPrivate();
    RSAPublicKey rsaPublicKey = (RSAPublicKey)keyPair.getPublic();
    version ++;

    String kid = uuid + "-" + version;
    log.debug("create new JWK with key id={}", kid);
    
    RSAKey fullJwk = new RSAKey.Builder(rsaPublicKey)
      .privateKey(privateKey)
      .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
      .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
      .keyID(kid)
      .build();
    
    publicJwk = fullJwk.toPublicJWK().toJSONObject();
    
    return fullJwk.toJSONString();
  }

}
