@echo off
chcp 936 >nul
cd /d "%~dp0"
call "%~dp0_环境自检.bat"
if errorlevel 1 exit /b 1

echo [后端] 使用 spring-boot:run 现场编译并启动，不依赖已有 target。
echo [后端] 首次启动需下载 Maven 依赖，请耐心等待，日志留在本窗口。
start "Huyao-Backend-8081" /d "%BACKEND_DIR%" cmd /k ""%MVN_CMD%" -f pom.xml -B -DskipTests spring-boot:run"
echo [后端] 已在独立窗口启动，端口 8081，接口前缀 /api。
exit /b 0
