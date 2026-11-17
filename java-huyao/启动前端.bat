@echo off
rem Start Vite frontend on 5173
rem All logic lives in start-huyao.ps1; this file is only a thin launcher.
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-huyao.ps1" -Mode frontend
echo.
pause
