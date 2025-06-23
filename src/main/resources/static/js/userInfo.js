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

import { padUuid } from './signaturePad.js';

export var userId;
export var userInfo;

export function showUserinfo(uid)
{
  console.log(uid);
  userId = uid;
  
  fetch(`/api/v1/userinfo?userid=${encodeURIComponent(uid)}`, {
    method: 'GET',
    headers: {
      'Accept': 'application/json',
      'SIGNATURE_PAD_UUID': padUuid
    }
  }).then(response => {
    if (!response.ok)
    {
      throw new Error(`Server-Fehler: ${response.status}`);
    }
    return response.json();
  }).then( dtoUserInfo => {
    userInfo = dtoUserInfo;
    console.log(userInfo);

    document.getElementById('userinfo-jpegpoto').src = userInfo.jpegPhoto;
    document.getElementById('userinfo-name').innerText =
            `${userInfo.firstname} ${userInfo.lastname}`;

    document.getElementById('userinfo-birthday').innerText =
            userInfo.birthday || '';

    document.getElementById('userinfo-userid').innerText =
            userInfo.uid || '';
    document.getElementById('userinfo-email').innerText =
            userInfo.mail || '';

    if (userInfo.semster)
    {
      const sem = userInfo.semster;
      const semText = [sem.co, sem.street, sem.zip, sem.city, sem.state, sem.country]
              .filter(Boolean)
              .join(', ');
      document.getElementById('userinfo-semester').innerText = semText;
      document.getElementById('userinfo-semester-container').style.display = 'block';
    }
    else
    {
      document.getElementById('userinfo-semester-container').style.display = 'none';
    }

    if (userInfo.home)
    {
      const home = userInfo.home;
      const homeText = [home.co, home.street, home.zip, home.city, home.state, home.country]
              .filter(Boolean)
              .join(', ');
      document.getElementById('userinfo-home').innerText = homeText;
      document.getElementById('userinfo-home-container').style.display = 'block';
    }
    else
    {
      document.getElementById('userinfo-home-container').style.display = 'none';
    }

  }).catch(err => {
    console.error('Fehler beim Laden der Userinfo:', err);
  });

}
