@echo off
title NutriWorks Desktop App
echo Starting NutriWorks Desktop App...
start "" "msedge.exe" --app="file:///%~dp0desktop_app.html" --window-size=420,860
exit
