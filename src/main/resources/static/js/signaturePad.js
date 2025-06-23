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

/* global Swal, jose, wsBaseUrl, heartbeatEnabled */

// ----------------------------------------------------------------------------
// -- Signature Pad -----------------------------------------------------------
// ----------------------------------------------------------------------------
import { switchLang, defaultLang } from './i18n.js';
import { showAlert } from './alerts.js';
import { userInfo, userId } from './userInfo.js';

const wrapper = document.getElementById("signature-pad");
const clearButton = wrapper.querySelector("[data-action=clear]");
const cancelButton = wrapper.querySelector("[data-action=cancel]");
const okButton = wrapper.querySelector("[data-action=ok]");
const canvas = wrapper.querySelector("canvas");

export const padName = localStorage.getItem('SIGNATURE_PAD_NAME') || '—';
export const padUuid = localStorage.getItem('SIGNATURE_PAD_UUID') || '*undefined*';

export const signaturePad = new SignaturePad(canvas, {
  backgroundColor: 'rgb(255, 255, 255)',
  penColor: 'rgb(0, 0, 200)'
});

export function resizeCanvas()
{
  const ratio = Math.max(window.devicePixelRatio || 1, 1);
  canvas.width = canvas.offsetWidth * ratio;
  canvas.height = canvas.offsetHeight * ratio;
  canvas.getContext("2d").scale(ratio, ratio);
  signaturePad.fromData(signaturePad.toData());
}

export function activateSignaturePad(active)
{
  if (active)
  {
    document.getElementById("signpad-active").style.display = "block";
    document.getElementById("signpad-standby").style.display = "none";
    resizeCanvas();
  }
  else
  {
    document.getElementById("signpad-active").style.display = "none";
    document.getElementById("signpad-standby").style.display = "block";
  }
}

function formatTimestamp(ts)
{
  const d = new Date(ts);
  const pad2 = n => String(n).padStart(2, '0');

  const yyyy = d.getFullYear();
  const mm = pad2(d.getMonth() + 1);
  const dd = pad2(d.getDate());

  const HH = pad2(d.getHours());
  const MM = pad2(d.getMinutes());
  const SS = pad2(d.getSeconds());

  return `${yyyy}-${mm}-${dd} ${HH}:${MM}:${SS}`;
}

window.onresize = resizeCanvas;
activateSignaturePad(false);

clearButton.addEventListener("click", () => {
  signaturePad.clear();
});

cancelButton.addEventListener("click", () => {
  console.log("Cancel button pressed");
  showAlert("alert.cancel.title", "alert.cancel.text", "error");
  activateSignaturePad(false);

  var payload = {};
  payload.userId = userId;
  payload.timestamp = Date.now();
  console.log("payload=" + JSON.stringify(payload));

  const url = '/api/v1/signature-pad/cancel';
  const response = fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'SIGNATURE_PAD_UUID': padUuid
    },
    body: JSON.stringify(payload)
  });
  
  switchLang(defaultLang);
});

okButton.addEventListener("click", async () => {
  if (signaturePad.isEmpty())
  {
    showAlert("alert.error.signature", "alert.error.signature", "error");
  }
  else
  {
    const {importJWK, CompactSign} = jose;
    const pngDataURL = signaturePad.toDataURL('image/png');
    const svgDataURL = signaturePad.toDataURL('image/svg+xml');
    const signaturePngBase64 = pngDataURL.split(',')[1];
    const signatureSvgBase64 = svgDataURL.split(',')[1];

    const payload = {
      iss: padUuid,
      sigpad: padName,
      sigpng: signaturePngBase64,
      sigsvg: signatureSvgBase64,
      sub: userInfo.uid,
      name: `${userInfo.firstname} ${userInfo.lastname}`,
      mail: userInfo.mail,
      iat: Math.floor(Date.now() / 1000)
    };

    const privateJwk = JSON.parse(localStorage.getItem('SIGNATURE_PAD_PRIVATE_JWK'));
    const privateKey = await importJWK(privateJwk, 'RS256');

    const jwt = await new CompactSign(
            new TextEncoder().encode(JSON.stringify(payload))
            )
            .setProtectedHeader({alg: 'RS256', kid: privateJwk.kid})
            .sign(privateKey);

    // console.log('🔐 Signiertes JWT:', jwt);

    const url = '/api/v1/signature-pad/signature';
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'text/plain',
        'SIGNATURE_PAD_UUID': padUuid
      },
      body: jwt
    });

    activateSignaturePad(false);

    if (response.ok)
    {
      showAlert("alert.success.signatureSent", "alert.success.signatureSent", "success");
    }
    else
    {
      const errorText = await response.text();
      showAlert("alert.error.sendingSignature", "alert.error.sendingSignature", "error", errorText);
    }
    switchLang(defaultLang);
  }
});


