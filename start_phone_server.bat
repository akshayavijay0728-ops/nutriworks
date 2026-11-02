@echo off
title NutriWorks Mobile Server
cd /d "%~dp0"

echo ===================================================
echo           NutriWorks Local Mobile Server
echo ===================================================
echo.
echo Local IP Address: 192.168.1.5
echo Server URL:       http://192.168.1.5:8080/
echo.
echo 1. Keep this command window open.
echo 2. Make sure your phone is connected to the same Wi-Fi.
echo 3. Scan the QR code on your screen or open http://192.168.1.5:8080/ in Chrome/Safari.
echo.
echo Starting server on port 8080...
echo.

"C:\Users\Lenovo\AppData\Local\Programs\Python\Python313\python.exe" -m http.server 8080 --bind 0.0.0.0

if %ERRORLEVEL% neq 0 (
    echo.
    echo Python server exited, falling back to PowerShell HTTP listener...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$port=8080; $htmlPath=Join-Path $PSScriptRoot 'index.html'; $listener=New-Object System.Net.HttpListener; $listener.Prefixes.Add(\"http://*:$port/\"); try { $listener.Start() } catch { Write-Host 'Binding to * failed, trying IP binding...'; $listener=New-Object System.Net.HttpListener; $listener.Prefixes.Add(\"http://localhost:$port/\"); $listener.Prefixes.Add(\"http://192.168.1.5:$port/\"); $listener.Start() }; Write-Host \"Server listening at http://192.168.1.5:$port/\"; while($listener.IsListening){ $ctx=$listener.GetContext(); $buf=[System.IO.File]::ReadAllBytes($htmlPath); $ctx.Response.ContentType='text/html; charset=utf-8'; $ctx.Response.ContentLength64=$buf.Length; $ctx.Response.OutputStream.Write($buf, 0, $buf.Length); $ctx.Response.Close() }"
)

pause
