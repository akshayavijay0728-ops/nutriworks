@echo off
title Opening NutriWorks on Laptop Screen
cd /d "%~dp0"

echo ========================================================
echo   Launching NutriWorks Desktop (OpenNutriTracker)
echo ========================================================
echo.

call run_java_app.bat

exit /b 0
