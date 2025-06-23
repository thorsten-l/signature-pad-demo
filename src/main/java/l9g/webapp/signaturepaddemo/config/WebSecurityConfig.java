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
package l9g.webapp.signaturepaddemo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Configuration
@EnableWebSecurity
@Slf4j
public class WebSecurityConfig
{
  private static final String CSP_POLICY =
    "default-src 'self'; "
    + "img-src 'self' data:;"
    + "style-src 'self' 'unsafe-inline';"
    + "script-src 'self' 'unsafe-inline';";

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http)
    throws Exception
  {
    log.debug("securityFilterChain");
    http
      .csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/signature-pad/**"))
      .authorizeHttpRequests(auth
        -> auth
        .requestMatchers(HttpMethod.GET, "/api/v1/signature-pad/**").permitAll()
        .requestMatchers(HttpMethod.POST, "/api/v1/signature-pad/**").permitAll()
        .requestMatchers("/api/v1/signature-pad/**").denyAll()
        .anyRequest().permitAll())
      .headers(headers
        -> headers
        .contentSecurityPolicy(csp -> csp.policyDirectives(CSP_POLICY)));
    return http.build();
  }
}
