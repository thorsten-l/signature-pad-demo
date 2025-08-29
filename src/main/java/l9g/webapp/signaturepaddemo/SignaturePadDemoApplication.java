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
package l9g.webapp.signaturepaddemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Main Spring Boot application class for the Signature Pad Demo application.
 * This application demonstrates digital signature capture and validation functionality.
 * 
 * <p>The application excludes the default UserDetailsServiceAutoConfiguration
 * to allow for custom authentication and authorization configuration.</p>
 * 
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@SpringBootApplication(
  exclude = {
    UserDetailsServiceAutoConfiguration.class // Exclude default user details service to enable custom auth
  }
)
public class SignaturePadDemoApplication
{
  /**
   * Main entry point for the Spring Boot application.
   * Starts the embedded web server and initializes the application context.
   * 
   * @param args command line arguments passed to the application
   */
  public static void main(String[] args)
  {
    SpringApplication.run(SignaturePadDemoApplication.class, args);
  }
}
