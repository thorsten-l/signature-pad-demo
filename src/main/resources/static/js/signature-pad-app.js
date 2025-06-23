
/* global Swal, jose, wsBaseUrl, heartbeatEnabled */

// ----------------------------------------------------------------------------
// -- I18N --------------------------------------------------------------------
// ----------------------------------------------------------------------------

const defaultLang = 'de';
let dict = {};

async function loadDict(lang)
{
  const res = await fetch(`/i18n/${lang}.json`);
  if (!res.ok)
    throw new Error(`Can't load ${lang}.json`);
  return res.json();
}

function applyI18n()
{
  document.querySelectorAll('[data-i18n]').forEach(el => {
    const key = el.getAttribute('data-i18n');
    if (dict[key])
    {
      // title-Attribut (bei <title>) oder innerText
      if (el.tagName.toLowerCase() === 'title')
      {
        document.title = dict[key];
      }
      else
      {
        el.innerText = dict[key];
      }
    }
  });
}

async function switchLang(lang)
{
  try
  {
    dict = await loadDict(lang);
    localStorage.setItem('lang', lang);
    document.getElementById('lang-flag').src = `i18n/flags/${lang}.svg`;
    applyI18n();
  }
  catch (e)
  {
    console.error(e);
  }
}

document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('#lang-menu button').forEach(btn => {
    btn.addEventListener('click', () => {
      switchLang(btn.dataset.lang);
    });
  });
  const lang = localStorage.getItem('lang') || defaultLang;
  switchLang(lang);
});



// ----------------------------------------------------------------------------
// -- Signature Pad -----------------------------------------------------------
// ----------------------------------------------------------------------------

const wrapper = document.getElementById("signature-pad");
const clearButton = wrapper.querySelector("[data-action=clear]");
const cancelButton = wrapper.querySelector("[data-action=cancel]");
const okButton = wrapper.querySelector("[data-action=ok]");
const canvas = wrapper.querySelector("canvas");
const padName = localStorage.getItem('SIGNATURE_PAD_NAME') || '—';
const padUuid = localStorage.getItem('SIGNATURE_PAD_UUID') || '*undefined*';

var userId;
var userInfo;

const signaturePad = new SignaturePad(canvas, {
  backgroundColor: 'rgb(255, 255, 255)',
  penColor: 'rgb(0, 0, 200)'
});

function resizeCanvas()
{
  const ratio = Math.max(window.devicePixelRatio || 1, 1);
  canvas.width = canvas.offsetWidth * ratio;
  canvas.height = canvas.offsetHeight * ratio;
  canvas.getContext("2d").scale(ratio, ratio);
  signaturePad.fromData(signaturePad.toData());
}

function showAlert(keyTitle, keyText, icon, ...args) {
  const t = dict; 
  const title = t[keyTitle] || keyTitle;
  let text   = t[keyText]   || keyText;
  args.forEach((v,i) => {
    text = text.replace(`{${i}}`, v);
  });
  Swal.fire({
    title,
    text,
    icon,
    confirmButtonText: 'OK',
    timer: 3000,
    timerProgressBar: true,
    showConfirmButton: false
  });
}

function showUserinfo(userid)
{
  console.log(userid);

  fetch(`/api/v1/userinfo?userid=${encodeURIComponent(userid)}`, {
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

function activateSignaturePad(active)
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


// ----------------------------------------------------------------------------
// -- WebSocket ---------------------------------------------------------------
// ----------------------------------------------------------------------------

var ws;
var lastHeartbeatTimestamp = null;

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
    showAlert("alert.error.connectionLost", "alert.error.connectionLost", "error");
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
      userId = dtoEvent.message;
      showUserinfo(userId);
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
    showAlert("alert.websocket.open", "alert.websocket.open", "success");
    lastHeartbeatTimestamp = Date.now();
  };

  ws.onclose = function ()
  {
    console.log("WebSocket connection closed.");
    activateSignaturePad(false);
    setStatusLight(false);
    
    showAlert("alert.error.connectionLost", "alert.error.connectionLost", "error");
    reconnect();
  };

  ws.onerror = function (error)
  {
    setStatusLight(false);
    console.log("WebSocket error: ", error);
    switchLang(defaultLang);
    showAlert("alert.error.websocket", "alert.error.websocket", "error");
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

