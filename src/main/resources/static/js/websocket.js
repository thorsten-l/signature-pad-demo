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

/* global Swal */

// ----------------------------------------------------------------------------
// -- WebSocket ---------------------------------------------------------------
// ----------------------------------------------------------------------------

import { dict, switchLang, defaultLang } from './i18n.js';
import { activateSignaturePad, resizeCanvas, signaturePad, padUuid } from './signaturePad.js';
import { showAlert } from './alerts.js';
import { showUserinfo } from './userInfo.js';

var ws;
var lastHeartbeatTimestamp = null;

const bodyEl = document.body;
const wsBaseUrl = bodyEl.getAttribute('data-ws-base-url') || 'unknown-ws-base-url';
const heartbeatEnabled = bodyEl.getAttribute('data-heartbeat-enabled') === 'true';

function setStatusLight(status)
{
  if (status)
  {
    document.getElementById("green-light").style.display = "inline-flex";
    document.getElementById("red-light").style.display = "none";
  }
  else
  {
    document.getElementById("green-light").style.display = "none";
    document.getElementById("red-light").style.display = "inline-flex";
    activateSignaturePad(false);
  }
}

function checkHeartbeat()
{
  console.log("checkHeartbeat");
  if (lastHeartbeatTimestamp && (Date.now() - lastHeartbeatTimestamp > 30000))
  {
    activateSignaturePad(false);
    console.log("No heartbeat received for 30 seconds. Reconnecting...");
    showAlert("alert.error.connectionLost.title", "alert.error.connectionLost.text", "error");
    ws.close();
  }
}

function connect()
{
  ws = new WebSocket(
          wsBaseUrl + "/ws/signature-pad", ["SIGNATURE_PAD_UUID", padUuid]);

  ws.onmessage = function (event)
  {
    var dtoEvent = JSON.parse(event.data);
    console.log("Received event: ", dtoEvent);

    if (dtoEvent.event === "heartbeat")
    {
      document.getElementById("heartbeat").innerHTML
              = formatTimestamp(dtoEvent.timestamp);
      lastHeartbeatTimestamp = Date.now();
    }

    if (dtoEvent.event === "show")
    {
      console.log("show event received");
      showUserinfo(dtoEvent.message);
      activateSignaturePad(true);
      resizeCanvas();
      signaturePad.clear();
    }

    if (dtoEvent.event === "error")
    {
      setStatusLight(false);
      showAlert("ERROR", dtoEvent.message, "error");
    }
  };

  ws.onopen = function ()
  {
    setStatusLight(true);
    switchLang(defaultLang);
    console.log("WebSocket connection opened.");
    showAlert("alert.websocket.open.title", "alert.websocket.open.text", "success");
    lastHeartbeatTimestamp = Date.now();
  };

  ws.onclose = function ()
  {
    console.log("WebSocket connection closed.");
    activateSignaturePad(false);
    setStatusLight(false);
    
    showAlert("alert.error.connectionLost.title", "alert.error.connectionLost.text", "error");
    reconnect();
  };

  ws.onerror = function (error)
  {
    setStatusLight(false);
    console.log("WebSocket error: ", error);
    switchLang(defaultLang);
    showAlert("alert.error.websocket.title", "alert.error.websocket.text", "error");
  };
}

function reconnect()
{
  console.log("reconnect");
  setTimeout(connect, 10000);
}

// ----------------------------------------------------------------------------
// -- Start -------------------------------------------------------------------
// ----------------------------------------------------------------------------

window.onload = function ()
{
  console.log("startup signatur pad app");
  switchLang(defaultLang);
  if (padUuid === '*undefined*')
  {
    console.log("ERROR: signatur pad is unregistered");
    document.getElementById("signature-pad-container").style.display = "none";

    Swal.fire(
    {
      title: 'ERROR',
      html: 'This signature pad is unregistered!<br/><br/>Please contact your administrator.',
      icon: 'error',

      showConfirmButton: false,
      showCancelButton: false,

      allowOutsideClick: false,
      allowEscapeKey: false,

      timer: undefined
    });
  }
  else
  {
    console.log("INFO: signatur pad running");
    connect();
    if (heartbeatEnabled)
    {
      setInterval(checkHeartbeat, 15000);
    }
  }
};
