@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0launcher\build.ps1" -Publish
set "BUILD_EXIT=%ERRORLEVEL%"
pause
exit /b %BUILD_EXIT%
