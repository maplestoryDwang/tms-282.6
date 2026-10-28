@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\Check-Source.ps1"
exit /b %ERRORLEVEL%