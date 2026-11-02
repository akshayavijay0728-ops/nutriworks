@echo off
title NutriWorks Desktop (OpenNutriTracker Edition)
cd /d "%~dp0"

echo ======================================================================
echo    NutriWorks - Open Source Nutrition Tracker (OpenNutriTracker)
echo ======================================================================
echo.

set "JAVA_BIN=C:\Program Files\JetBrains\PyCharm 2025.1.1.1\jbr\bin"

if exist "%JAVA_BIN%\javac.exe" (
    echo [1/2] Compiling NutriWorks Desktop and QR Engine...
    if not exist "bin" mkdir "bin"
    "%JAVA_BIN%\javac.exe" -d "bin" "src\com\nutriworks\desktop\PureQrEncoder.java" "NutriWorksDesktop.java"
    if %ERRORLEVEL% neq 0 (
        echo Compilation failed.
        pause
        exit /b 1
    )
    echo [2/2] Launching NutriWorks Desktop GUI Window...
    start "" "%JAVA_BIN%\javaw.exe" -cp "bin" com.nutriworks.desktop.NutriWorksDesktop
    echo NutriWorks Desktop window launched successfully!
    exit /b 0
)

where javac >nul 2>nul
if %ERRORLEVEL% equ 0 (
    echo [1/2] Compiling NutriWorks Desktop with system Java...
    if not exist "bin" mkdir "bin"
    javac -d "bin" "src\com\nutriworks\desktop\PureQrEncoder.java" "NutriWorksDesktop.java"
    echo [2/2] Launching NutriWorks Desktop Window...
    start "" javaw -cp "bin" com.nutriworks.desktop.NutriWorksDesktop
    exit /b 0
)

echo [ERROR] No Java SDK found.
pause
exit /b 1
