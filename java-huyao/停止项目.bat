@echo off
rem Stop backend 8081 and frontend 5173
rem All logic lives in start-huyao.ps1; this file is only a thin launcher.
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-huyao.ps1" -Mode stop
echo.
pause
