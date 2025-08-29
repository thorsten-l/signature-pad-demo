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
package l9g.webapp.signaturepaddemo.dto;

/**
 * Data Transfer Object representing user information for signature pad operations.
 * Contains personal details, contact information, and address data that can be
 * displayed on signature pad devices during the signing process.
 *
 * @param jpegPhoto Base64-encoded JPEG photo of the user (with data URI prefix)
 * @param firstname User's first name
 * @param lastname User's last name  
 * @param uid Unique user identifier
 * @param mail User's email address
 * @param birthday User's birth date as string
 * @param semster Semester/temporary address information
 * @param home Home address information
 * 
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
public record DtoUserInfo( 
  String jpegPhoto,
  String firstname,
  String lastname,
  String uid,
  String mail,
  String birthday,
  DtoAddress semster,
  DtoAddress home
  )
{
  
}
