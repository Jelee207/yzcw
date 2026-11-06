@echo off
chcp 936 >nul
setlocal enabledelayedexpansion
echo 正在停止 狐缘阁 服务（后端 8081 / 前端 5173）...

for %%P in (8081 5173) do (
  for /f "tokens=5" %%a in ('netstat -ano ^| findstr /r /c:":%%P .*LISTENING"') do (
    echo   结束端口 %%P 上的进程 PID=%%a
    taskkill /f /pid %%a >nul 2>&1
  )
)

taskkill /f /fi "WINDOWTITLE eq Huyao-Backend-8081*" >nul 2>&1
taskkill /f /fi "WINDOWTITLE eq Huyao-Frontend-5173*" >nul 2>&1
echo 已停止。重新启动请运行 启动项目.bat
exit /b 0
