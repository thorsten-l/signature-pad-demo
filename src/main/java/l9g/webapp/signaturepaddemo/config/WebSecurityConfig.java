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
 * Web security configuration for the signature pad demo application.
 * Configures CSRF protection, authorization rules, and Content Security Policy.
 * Allows unrestricted access to most endpoints while protecting sensitive API operations.
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Configuration
@EnableWebSecurity
@Slf4j
public class WebSecurityConfig
{
  /** Content Security Policy directives for enhanced security */
  private static final String CSP_POLICY =
    "default-src 'self'; "
    + "img-src 'self' data:;"
    + "style-src 'self' 'unsafe-inline';"
    + "script-src 'self' 'unsafe-inline';";

  /**
   * Configures the security filter chain with custom authorization and security policies.
   * 
   * @param http the HttpSecurity configuration object
   * @return the configured SecurityFilterChain
   * @throws Exception if configuration fails
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http)
    throws Exception
  {
    log.debug("securityFilterChain");
    http
      // Disable CSRF for signature pad API endpoints to allow device communication
      .csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/signature-pad/**"))
      
      // Configure authorization rules
      .authorizeHttpRequests(auth
        -> auth
        // Allow GET and POST requests to signature pad API for device communication
        .requestMatchers(HttpMethod.GET, "/api/v1/signature-pad/**").permitAll()
        .requestMatchers(HttpMethod.POST, "/api/v1/signature-pad/**").permitAll()
        // Deny all other HTTP methods for signature pad API
        .requestMatchers("/api/v1/signature-pad/**").denyAll()
        // Allow all other requests (admin interface, static resources, etc.)
        .anyRequest().permitAll())
      
      // Configure security headers including Content Security Policy
      .headers(headers
        -> headers
        .contentSecurityPolicy(csp -> csp.policyDirectives(CSP_POLICY)));
    return http.build();
  }
}
