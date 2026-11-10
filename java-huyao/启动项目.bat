@echo off
chcp 936 >nul
cd /d "%~dp0"
echo ============================================================
echo   狐缘阁 · 一键启动
echo   MySQL 3306   后端 8081   前端 5173   库 huyao
echo ============================================================
echo.
call "%~dp0_环境自检.bat"
if errorlevel 1 goto FAILED

echo.
echo [0/3] 启动 MySQL ...
call "%~dp0启动数据库.bat"
if errorlevel 1 goto FAILED

echo.
echo [1/3] 检查 / 初始化数据库 ...
call "%~dp0初始化数据库.bat"
if errorlevel 1 goto FAILED

echo.
echo [2/3] 启动后端 Spring Boot ...
call "%~dp0启动后端.bat"
if errorlevel 1 goto FAILED

echo.
echo [3/3] 启动前端 Vite ...
call "%~dp0启动前端.bat"
if errorlevel 1 goto FAILED

echo.
echo [等待] 正在等待 8081 与 5173 就绪（约 1 分钟）...
set /a TRIES=0
:WAIT_ALL
set /a TRIES+=1
netstat -ano | findstr /r /c:":8081 .*LISTENING" >nul 2>&1
if errorlevel 1 goto WAIT_TICK
netstat -ano | findstr /r /c:":5173 .*LISTENING" >nul 2>&1
if not errorlevel 1 goto READY
:WAIT_TICK
if %TRIES% GEQ 60 goto NOT_READY
ping -n 2 127.0.0.1 >nul
goto WAIT_ALL

:READY
echo.
echo ============================================================
echo   启动完成！请打开 http://localhost:5173
echo   测试账号：admin / 123456
echo   停止服务：停止项目.bat
echo   三个服务在独立窗口运行，请勿关闭它们
echo ============================================================
echo.
pause
exit /b 0

:NOT_READY
echo.
echo [警告] 等待超时：8081 或 5173 仍未就绪。
echo        请查看 Huyao-Backend-8081 与 Huyao-Frontend-5173 窗口里的报错。
echo.
pause
exit /b 1

:FAILED
echo.
echo [错误] 启动中止，请看上方 [错误] 提示定位原因。
echo        常见原因：端口被占用、D:\dev 便携工具链缺失、my.ini 配置错误。
echo.
pause
exit /b 1
