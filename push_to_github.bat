@echo off
title Push NutriWorks to GitHub (akshayavijay0728-ops)
cd /d "%~dp0"

echo ======================================================================
echo    NutriWorks - Push to GitHub Repository
echo    Target: https://github.com/akshayavijay0728-ops/NutriWorks
echo ======================================================================
echo.

set "GIT_CMD=C:\Users\Lenovo\AppData\Local\Programs\MinGit\cmd\git.exe"
if not exist "%GIT_CMD%" (
    where git >nul 2>nul
    if %ERRORLEVEL% equ 0 (
        set "GIT_CMD=git"
    ) else (
        echo [ERROR] Git was not found!
        pause
        exit /b 1
    )
)

echo [1/3] Checking current Git status...
"%GIT_CMD%" status --short
echo.

echo [2/3] Ensure you have created the empty repository on GitHub:
echo       https://github.com/new  (Repository name: NutriWorks)
echo.
echo Please enter your GitHub Personal Access Token (or press Enter to try standard login):
set /p GH_TOKEN="GitHub Token (Classic or Fine-Grained): "

if not "%GH_TOKEN%"=="" (
    echo.
    echo Pushing to https://github.com/akshayavijay0728-ops/NutriWorks.git with provided token...
    "%GIT_CMD%" push https://akshayavijay0728-ops:%GH_TOKEN%@github.com/akshayavijay0728-ops/NutriWorks.git main -u
) else (
    echo.
    echo Pushing with standard Git credentials...
    "%GIT_CMD%" push origin main -u
)

if %ERRORLEVEL% equ 0 (
    echo.
    echo ======================================================================
    echo  SUCCESS! NutriWorks has been pushed to:
    echo  https://github.com/akshayavijay0728-ops/NutriWorks
    echo ======================================================================
) else (
    echo.
    echo [NOTICE] Push failed or authentication was required.
    echo Tip: GitHub requires a Personal Access Token instead of your account password.
    echo You can create one with 'repo' scope at: https://github.com/settings/tokens
)

echo.
pause
