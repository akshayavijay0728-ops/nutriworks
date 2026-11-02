@echo off
setlocal
title NutriWorks Desktop Application Launcher (OpenNutriTracker Edition)
cd /d "%~dp0"

echo ========================================================
echo   NutriWorks Desktop - OpenNutriTracker Edition
echo ========================================================
echo.

set "JBR_JAVA=C:\Program Files\JetBrains\PyCharm 2025.1.1.1\jbr\bin\java.exe"
set "JBR_JAVAC=C:\Program Files\JetBrains\PyCharm 2025.1.1.1\jbr\bin\javac.exe"

if exist "%JBR_JAVAC%" (
    set "JAVAC_EXE=%JBR_JAVAC%"
    set "JAVA_EXE=%JBR_JAVA%"
    goto compileAndRun
)

where javac >nul 2>nul
if %ERRORLEVEL% equ 0 (
    set "JAVAC_EXE=javac"
    set "JAVA_EXE=java"
    goto compileAndRun
)

echo [ERROR] No Java installation found.
pause
exit /b 1

:compileAndRun
echo Using Java Runtime: "%JAVA_EXE%"
echo Compiling NutriWorksDesktop.java...
if not exist "bin" mkdir "bin"
"%JAVAC_EXE%" -d "bin" "NutriWorksDesktop.java"
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Compilation failed.
    pause
    exit /b 1
)

echo.
echo Launching NutriWorks Desktop on your laptop screen...
start "" "%JAVA_EXE%" -cp "bin" com.nutriworks.desktop.NutriWorksDesktop
echo Application launched!
exit /b 0
