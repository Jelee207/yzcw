@echo off
chcp 936 >nul
cd /d "%~dp0"
call "%~dp0_环境自检.bat"
if errorlevel 1 exit /b 1

netstat -ano | findstr /r /c:":5173 .*LISTENING" >nul 2>&1
if not errorlevel 1 (
  echo [前端] 5173 已在监听，跳过启动。
  exit /b 0
)

if not exist "%FRONTEND_DIR%\node_modules\.package-lock.json" (
  echo [前端] 依赖不存在或被清理，执行 npm ci 重新安装 ...
  pushd "%FRONTEND_DIR%"
  call npm ci --no-audit --no-fund
  if errorlevel 1 (
    echo [错误] npm ci 失败，检查网络或 package-lock.json 是否完整。
    popd
    exit /b 1
  )
  popd
) else (
  echo [前端] 依赖已就绪，跳过安装以加快启动。
)

echo [前端] 启动 Vite，端口 5173，/api 代理到 8081。
start "Huyao-Frontend-5173" /d "%FRONTEND_DIR%" cmd /k npm run dev
echo [前端] 已在独立窗口启动。
exit /b 0
