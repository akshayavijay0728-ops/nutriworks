package com.nutriworks.server;

/**
 * Generates the responsive, mobile-first web interface served to the phone
 * via the local embedded web server when scanning the QR code.
 */
public final class MobileWebHandler {

    private MobileWebHandler() {
    }

    public static String getMobileHtml(String token) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">\n" +
                "  <title>NutriWorks Camera Bridge</title>\n" +
                "  <style>\n" +
                "    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }\n" +
                "    body { background-color: #FFFFFF; color: #1E293B; min-height: 100vh; display: flex; flex-direction: column; }\n" +
                "    .header { background-color: #2E7D32; color: #FFFFFF; padding: 14px 18px; display: flex; align-items: center; justify-content: space-between; box-shadow: 0 2px 4px rgba(0,0,0,0.06); }\n" +
                "    .brand { font-size: 17px; font-weight: 700; display: flex; align-items: center; gap: 8px; }\n" +
                "    .badge { background-color: rgba(255,255,255,0.2); font-size: 11px; padding: 3px 8px; border-radius: 12px; font-weight: 600; }\n" +
                "    .content { flex: 1; padding: 20px; max-width: 440px; margin: 0 auto; width: 100%; display: flex; flex-direction: column; gap: 16px; }\n" +
                "    .card { background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 18px; padding: 18px; box-shadow: 0 1px 3px rgba(0,0,0,0.03); }\n" +
                "    .btn { display: flex; align-items: center; justify-content: center; gap: 8px; width: 100%; padding: 14px; font-size: 15px; font-weight: 600; border-radius: 14px; border: none; cursor: pointer; transition: all 0.2s; text-decoration: none; }\n" +
                "    .btn-primary { background: #2E7D32; color: #FFFFFF; }\n" +
                "    .btn-primary:active { background: #1B5E20; transform: scale(0.98); }\n" +
                "    .btn-secondary { background: #F1F5F9; color: #334155; border: 1px solid #CBD5E1; }\n" +
                "    .btn-secondary:active { background: #E2E8F0; }\n" +
                "    .preview-box { width: 100%; height: 260px; border-radius: 14px; overflow: hidden; background: #0F172A; display: none; position: relative; }\n" +
                "    .preview-box img { width: 100%; height: 100%; object-fit: cover; }\n" +
                "    .progress-bar-container { width: 100%; height: 8px; background: #E2E8F0; border-radius: 4px; overflow: hidden; display: none; margin-top: 10px; }\n" +
                "    .progress-bar { height: 100%; width: 0%; background: #2E7D32; transition: width 0.2s; }\n" +
                "    .status-msg { font-size: 12px; color: #64748B; text-align: center; margin-top: 8px; }\n" +
                "    .result-card { display: none; background: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 16px; padding: 16px; }\n" +
                "    .result-title { font-size: 18px; font-weight: 700; color: #0F172A; }\n" +
                "    .result-kcal { font-size: 22px; font-weight: 800; color: #2E7D32; margin-top: 4px; }\n" +
                "    .macro-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-top: 12px; text-align: center; }\n" +
                "    .macro-box { background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 10px; padding: 8px; font-size: 12px; }\n" +
                "    .error-box { background: #FEF2F2; border: 1px solid #FCA5A5; color: #991B1B; padding: 12px; border-radius: 12px; font-size: 13px; display: none; line-height: 1.4; }\n" +
                "    .footer { text-align: center; font-size: 11px; color: #94A3B8; padding: 16px; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"header\">\n" +
                "    <div class=\"brand\">NutriWorks Phone Bridge</div>\n" +
                "    <div class=\"badge\">Connected to Laptop</div>\n" +
                "  </div>\n" +
                "  <div class=\"content\">\n" +
                "    <div id=\"errorBox\" class=\"error-box\"></div>\n" +
                "    <div class=\"card\" id=\"captureSection\">\n" +
                "      <h2 style=\"font-size: 16px; font-weight: 700; margin-bottom: 4px;\">Send Food Photo to Laptop</h2>\n" +
                "      <p style=\"font-size: 12px; color: #64748B; margin-bottom: 16px;\">Take a clear photo of your meal. It will instantly appear on your laptop for calorie analysis.</p>\n" +
                "      <input type=\"file\" id=\"cameraInput\" accept=\"image/*\" capture=\"environment\" style=\"display:none;\">\n" +
                "      <input type=\"file\" id=\"galleryInput\" accept=\"image/*\" style=\"display:none;\">\n" +
                "      <div style=\"display: flex; flex-direction: column; gap: 10px;\">\n" +
                "        <button class=\"btn btn-primary\" onclick=\"document.getElementById('cameraInput').click()\">Open Phone Camera</button>\n" +
                "        <button class=\"btn btn-secondary\" onclick=\"document.getElementById('galleryInput').click()\">Choose from Gallery</button>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "    <div class=\"card\" id=\"previewSection\" style=\"display: none;\">\n" +
                "      <div class=\"preview-box\" id=\"previewBox\" style=\"display: block;\">\n" +
                "        <img id=\"previewImg\" src=\"\" alt=\"Preview\">\n" +
                "      </div>\n" +
                "      <div class=\"progress-bar-container\" id=\"progressContainer\">\n" +
                "        <div class=\"progress-bar\" id=\"progressBar\"></div>\n" +
                "      </div>\n" +
                "      <div class=\"status-msg\" id=\"uploadStatus\">Ready to send</div>\n" +
                "      <div style=\"display: flex; gap: 10px; margin-top: 14px;\">\n" +
                "        <button class=\"btn btn-secondary\" id=\"retakeBtn\" onclick=\"resetCapture()\" style=\"flex:1;\">Retake</button>\n" +
                "        <button class=\"btn btn-primary\" id=\"sendBtn\" onclick=\"uploadPhoto()\" style=\"flex:2;\">Send to Laptop</button>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "    <div class=\"result-card\" id=\"resultSection\">\n" +
                "      <div style=\"font-size: 11px; text-transform: uppercase; color: #64748B; font-weight: 700;\">Recognized on Laptop</div>\n" +
                "      <div class=\"result-title\" id=\"resultTitle\">Chicken Biryani</div>\n" +
                "      <div class=\"result-kcal\" id=\"resultKcal\">435 kcal</div>\n" +
                "      <div class=\"macro-grid\">\n" +
                "        <div class=\"macro-box\"><span style=\"color:#64748B; display:block;\">Protein</span><strong id=\"resProtein\">16.5g</strong></div>\n" +
                "        <div class=\"macro-box\"><span style=\"color:#64748B; display:block;\">Carbs</span><strong id=\"resCarbs\">48.0g</strong></div>\n" +
                "        <div class=\"macro-box\"><span style=\"color:#64748B; display:block;\">Fat</span><strong id=\"resFat\">19.5g</strong></div>\n" +
                "      </div>\n" +
                "      <button class=\"btn btn-primary\" onclick=\"resetCapture()\" style=\"margin-top: 14px;\">Scan Another Food</button>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "  <div class=\"footer\">NutriWorks Local Wi-Fi Bridge &bull; Offline & Secure</div>\n" +
                "  <script>\n" +
                "    const TOKEN = '" + token + "';\n" +
                "    let selectedBlob = null;\n" +
                "    document.getElementById('cameraInput').addEventListener('change', handleFileSelect);\n" +
                "    document.getElementById('galleryInput').addEventListener('change', handleFileSelect);\n" +
                "    function handleFileSelect(e) {\n" +
                "      const file = e.target.files[0];\n" +
                "      if (!file) return;\n" +
                "      downscaleImage(file, 1280, function(blob) {\n" +
                "        selectedBlob = blob;\n" +
                "        const reader = new FileReader();\n" +
                "        reader.onload = function(evt) {\n" +
                "          document.getElementById('previewImg').src = evt.target.result;\n" +
                "          document.getElementById('captureSection').style.display = 'none';\n" +
                "          document.getElementById('resultSection').style.display = 'none';\n" +
                "          document.getElementById('previewSection').style.display = 'block';\n" +
                "          document.getElementById('uploadStatus').textContent = 'Image optimized (1280px). Ready to send.';\n" +
                "        };\n" +
                "        reader.readAsDataURL(blob);\n" +
                "      });\n" +
                "    }\n" +
                "    function downscaleImage(file, maxDimension, callback) {\n" +
                "      const img = new Image();\n" +
                "      const reader = new FileReader();\n" +
                "      reader.onload = function(e) {\n" +
                "        img.onload = function() {\n" +
                "          let w = img.width, h = img.height;\n" +
                "          if (w > maxDimension || h > maxDimension) {\n" +
                "            if (w > h) {\n" +
                "              h = Math.round((h * maxDimension) / w);\n" +
                "              w = maxDimension;\n" +
                "            } else {\n" +
                "              w = Math.round((w * maxDimension) / h);\n" +
                "              h = maxDimension;\n" +
                "            }\n" +
                "          }\n" +
                "          const canvas = document.createElement('canvas');\n" +
                "          canvas.width = w;\n" +
                "          canvas.height = h;\n" +
                "          const ctx = canvas.getContext('2d');\n" +
                "          ctx.drawImage(img, 0, 0, w, h);\n" +
                "          canvas.toBlob(function(blob) {\n" +
                "            callback(blob || file);\n" +
                "          }, 'image/jpeg', 0.88);\n" +
                "        };\n" +
                "        img.src = e.target.result;\n" +
                "      };\n" +
                "      reader.readAsDataURL(file);\n" +
                "    }\n" +
                "    function uploadPhoto() {\n" +
                "      if (!selectedBlob) return;\n" +
                "      const sendBtn = document.getElementById('sendBtn');\n" +
                "      const retakeBtn = document.getElementById('retakeBtn');\n" +
                "      const pContainer = document.getElementById('progressContainer');\n" +
                "      const pBar = document.getElementById('progressBar');\n" +
                "      const status = document.getElementById('uploadStatus');\n" +
                "      sendBtn.disabled = true;\n" +
                "      retakeBtn.disabled = true;\n" +
                "      pContainer.style.display = 'block';\n" +
                "      pBar.style.width = '10%';\n" +
                "      status.textContent = 'Uploading to laptop...';\n" +
                "      const formData = new FormData();\n" +
                "      formData.append('photo', selectedBlob, 'phone_capture.jpg');\n" +
                "      const xhr = new XMLHttpRequest();\n" +
                "      xhr.open('POST', '/api/upload/' + TOKEN, true);\n" +
                "      xhr.upload.onprogress = function(e) {\n" +
                "        if (e.lengthComputable) {\n" +
                "          const pct = Math.round((e.loaded / e.total) * 90);\n" +
                "          pBar.style.width = pct + '%';\n" +
                "        }\n" +
                "      };\n" +
                "      xhr.onload = function() {\n" +
                "        sendBtn.disabled = false;\n" +
                "        retakeBtn.disabled = false;\n" +
                "        if (xhr.status === 200) {\n" +
                "          pBar.style.width = '100%';\n" +
                "          status.textContent = 'Sent! Analyzing on laptop...';\n" +
                "          try {\n" +
                "            const resp = JSON.parse(xhr.responseText);\n" +
                "            showResult(resp);\n" +
                "          } catch (ex) {\n" +
                "            resetCapture();\n" +
                "          }\n" +
                "        } else if (xhr.status === 403 || xhr.status === 401) {\n" +
                "          showError('Token expired or invalid. Please scan a fresh QR code from your laptop screen.');\n" +
                "        } else {\n" +
                "          showError('Upload failed (HTTP ' + xhr.status + '). Ensure phone and laptop are on the same Wi-Fi.');\n" +
                "        }\n" +
                "      };\n" +
                "      xhr.onerror = function() {\n" +
                "        sendBtn.disabled = false;\n" +
                "        retakeBtn.disabled = false;\n" +
                "        showError('Cannot connect to laptop server. Check if Windows Firewall allowed the connection or use phone hotspot.');\n" +
                "      };\n" +
                "      xhr.send(formData);\n" +
                "    }\n" +
                "    function showResult(data) {\n" +
                "      document.getElementById('previewSection').style.display = 'none';\n" +
                "      const resSec = document.getElementById('resultSection');\n" +
                "      resSec.style.display = 'block';\n" +
                "      document.getElementById('resultTitle').textContent = data.foodName || 'Recognized Food';\n" +
                "      document.getElementById('resultKcal').textContent = (data.calories || 350) + ' kcal';\n" +
                "      document.getElementById('resProtein').textContent = (data.protein || 10.0) + 'g';\n" +
                "      document.getElementById('resCarbs').textContent = (data.carbs || 30.0) + 'g';\n" +
                "      document.getElementById('resFat').textContent = (data.fat || 8.0) + 'g';\n" +
                "    }\n" +
                "    function showError(msg) {\n" +
                "      const box = document.getElementById('errorBox');\n" +
                "      box.textContent = msg;\n" +
                "      box.style.display = 'block';\n" +
                "      window.scrollTo({ top: 0, behavior: 'smooth' });\n" +
                "    }\n" +
                "    function resetCapture() {\n" +
                "      selectedBlob = null;\n" +
                "      document.getElementById('cameraInput').value = '';\n" +
                "      document.getElementById('galleryInput').value = '';\n" +
                "      document.getElementById('previewSection').style.display = 'none';\n" +
                "      document.getElementById('resultSection').style.display = 'none';\n" +
                "      document.getElementById('captureSection').style.display = 'block';\n" +
                "      document.getElementById('errorBox').style.display = 'none';\n" +
                "    }\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>";
    }
}
