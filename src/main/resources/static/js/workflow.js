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

import { fetchUserInfo, userInfo } from './userInfo.js';
import { activateSignaturePad, resizeCanvas, signaturePad, padUuid } from './signaturePad.js';

document.addEventListener('DOMContentLoaded', () => {
  let isScanning = false;
  let isProcessing = false;

  if (!('BarcodeDetector' in window)) {
    showMsg('BarcodeDetector nicht unterstützt', 'danger');
    return;
  }

  const padUuid = localStorage.getItem('SIGNATURE_PAD_UUID') || '*undefined*';
  let cardNumber = null;
  let uploadedFront = false;
  let uploadedBack = false;

  const btnNext = document.getElementById('btn-next');
  const video = document.getElementById('video');
  const message = document.getElementById('message');
  const scanner = document.getElementById('scanner');
  const photoSec = document.getElementById('photo-section');
  const photoMsg = document.getElementById('photo-msg');
  const btnFront = document.getElementById('btn-front');
  const btnBack = document.getElementById('btn-back');
  const inputF = document.getElementById('file-front');
  const inputB = document.getElementById('file-back');
  const previewF = document.getElementById('preview-front');
  const previewB = document.getElementById('preview-back');
  const imgFront = document.getElementById('img-front');
  const imgBack = document.getElementById('img-back');

  // Hilfsfunktionen
  function showScanner() {
    isScanning = false;
    isProcessing = false;

    clearPage();
    document.getElementById('scanner').classList.remove('d-none');
    document.getElementById('signature-pad-title').classList.remove('d-none');


    navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } })
      .then(stream => {
        video.srcObject = stream;
        video.play().catch(e => console.error("Play failed:", e));
        video.addEventListener('playing', () => {
          isScanning = true;
          scanLoop();
        });
      })
      .catch(err => showMsg('Kamera-Fehler: ' + err.message, 'danger'));
  }

  function showStartPage() {
    isScanning = false;
    if (video.srcObject) {
      video.srcObject.getTracks().forEach(track => track.stop());
      video.srcObject = null;
    }

    clearPage();
    document.getElementById('start-page').classList.remove('d-none');
    document.getElementById('signature-pad-title').classList.remove('d-none');
    document.getElementById('scanner-pages').classList.remove('d-none');

    cardNumber = null;
    uploadedFront = false;
    uploadedBack = false;
  }

  // BarcodeDetector (Code-39)
  const detector = new BarcodeDetector({ formats: ['code_39'] });

  // Scan-Loop
  async function scanLoop() {
    if (!isScanning) {
      return; // Stop the loop if scanning is disabled
    }

    // Only try to detect if not already processing
    if (!isProcessing && video.readyState >= 2) {
      try {
        const barcodes = await detector.detect(video);

        if (barcodes.length > 0) {
          isProcessing = true; // Stop further scans
          const detectedCardNumber = barcodes[0].rawValue;

          // Validate the barcode before sending
          const codePattern = /^\d{12}$/;
          if (codePattern.test(detectedCardNumber)) {
            showMsg(`Code ${detectedCardNumber} erkannt, prüfe...`, 'info');
            processCardNumber(detectedCardNumber)
              .catch(error => {
                // ERROR from server
                showMsg(`Fehler: ${error.message}. Erneut scannen...`, 'danger');
                setTimeout(() => {
                  isProcessing = false;
                }, 1000);
              });
          } else {
            // Invalid barcode format, show error and allow scanning again
            showMsg(`Ungültiger Barcode: ${detectedCardNumber}. Muss 12 Zahlen sein.`, 'danger');
            setTimeout(() => {
              isProcessing = false;
            }, 1000);
          }
        }
      } catch (e) {
        console.error('Detector error:', e);
        showMsg('Scan-Fehler: ' + e.message, 'danger');
      }
    }

    // Continue the loop if scanning is still active
    if (isScanning) {
      requestAnimationFrame(scanLoop);
    }
  }

  function processCardNumber(code) {
    return fetchUserInfo(code, padUuid)
      .then(_userInfo => {
        cardNumber = code;
        isScanning = false; // This will stop the loop
        showMsg('Karte akzeptiert', 'success');
        
        console.log("processCardNumber");
        console.log(userInfo);
        
        const userPhoto = document.getElementById('userinfo-jpegpoto-preview');
        userPhoto.src = userInfo.jpegPhoto;
        userPhoto.style.height = '120px';
        userPhoto.style.width = 'auto';

        document.getElementById('userinfo-name-preview').innerText = `${userInfo.firstname} ${userInfo.lastname}`;
        document.getElementById('userinfo-userid-preview').innerText = userInfo.uid || '';

        scanner.classList.add('d-none');
        document.getElementById('start-page').classList.add('d-none');
        photoSec.classList.remove('d-none');
      });
  }

  // Foto-Workflow
  btnFront.addEventListener('click', () => inputF.click());
  btnBack.addEventListener('click', () => inputB.click());

  inputF.addEventListener('change', () => handlePhoto('front', inputF.files[0]));
  inputB.addEventListener('change', () => handlePhoto('back', inputB.files[0]));

  function handlePhoto(side, file) {
    if (!cardNumber || !file) return;
    const reader = new FileReader();
    reader.onload = e => {
      if (side === 'front') {
        imgFront.src = e.target.result;
        previewF.style.display = 'block';
        uploadedFront = true;
      } else {
        imgBack.src = e.target.result;
        previewB.style.display = 'block';
        uploadedBack = true;
      }
      checkAllUploaded();
    };
    reader.readAsDataURL(file);

    uploadPhoto(side, file);
  }

  function checkAllUploaded() {
    if (uploadedFront && uploadedBack) {
      btnNext.classList.remove('d-none');
    }
  }

  function uploadPhoto(side, file) {
    console.log("upload photo");
    const fd = new FormData();
    fd.append('cardNumber', cardNumber);
    fd.append('side', side);
    fd.append('file', file, file.name);

    showPhotoMsg('Sende ' + side + '…', 'info');
    fetch('/api/v1/signature-pad/photo', { method: 'POST', body: fd })
      .then(r => r.json())
      .then(() => showPhotoMsg('Seite ' + side + ' hochgeladen', 'success'))
      .catch(e => showPhotoMsg('Upload-Fehler ' + side + ': ' + e, 'danger'));
  }

  function showMsg(text, type) {
    message.textContent = text;
    message.className = `mt-3 alert alert-${type}`;
    message.classList.remove('d-none');
  }

  function showPhotoMsg(text, type) {
    photoMsg.textContent = text;
    photoMsg.className = `mt-2 alert alert-${type}`;
  }

  function showSignaturePad() {
    console.log("showSignaturePad");
    clearPage();
    document.getElementById('scanner-pages').classList.add('d-none');
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
    
    activateSignaturePad(true);
    resizeCanvas();
  }

  function handleSendBarcode() {
    const barcodeInput = document.getElementById('library-barcode');
    const barcodeValue = barcodeInput.value;

    if (barcodeValue && barcodeValue.trim().length > 0) {
      const code = barcodeValue.trim();
      showMsg(`Code ${code} wird geprüft...`, 'info');
      processCardNumber(code)
        .catch(error => {
          showMsg(`Fehler: ${error.message}.`, 'danger');
          alert(`Fehler: ${error.message}.`);
        });
    } else {
      alert("Bitte geben Sie einen gültigen Barcode ein.");
    }
  }

  // Event Listeners
  document.getElementById('btn-show-scanner').addEventListener('click', showScanner);
  document.getElementById('btn-cancel-scan').addEventListener('click', showStartPage);
  document.getElementById('btn-cancel-photo').addEventListener('click', showStartPage);
  document.getElementById('btn-send-barcode').addEventListener('click', handleSendBarcode);
  document.getElementById('btn-next').addEventListener('click', showSignaturePad);
  document.addEventListener('signatureSubmitted', showStartPage);
});
