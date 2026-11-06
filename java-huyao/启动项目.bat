@echo off
chcp 936 >nul
cd /d "%~dp0"
echo ============================================================
echo   狐缘阁 登录注册系统  一键启动
echo   后端 8081   前端 5173   MySQL 库 huyao
echo ============================================================
call "%~dp0_环境自检.bat"
if errorlevel 1 (
  echo [错误] 环境自检未通过，启动中止。
  exit /b 1
)

echo.
echo [1/3] 检查数据库 ...
call "%~dp0初始化数据库.bat"

echo.
echo [2/3] 启动后端 Spring Boot ...
call "%~dp0启动后端.bat"
if errorlevel 1 (
  echo [错误] 后端启动失败。
  exit /b 1
)

echo.
echo [3/3] 启动前端 Vite ...
call "%~dp0启动前端.bat"
if errorlevel 1 (
  echo [错误] 前端启动失败。
  exit /b 1
)

echo.
echo ============================================================
echo   全部启动完成。稍等片刻后访问 http://localhost:5173
echo   停止服务请运行 停止项目.bat
echo   测试账号：admin / 123456
echo ============================================================
exit /b 0
