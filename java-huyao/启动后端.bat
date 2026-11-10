@echo off
chcp 936 >nul
cd /d "%~dp0"
call "%~dp0_环境自检.bat"
if errorlevel 1 exit /b 1

netstat -ano | findstr /r /c:":8081 .*LISTENING" >nul 2>&1
if not errorlevel 1 (
  echo [后端] 8081 已在监听，跳过启动。
  exit /b 0
)

echo [后端] 现场编译并启动 spring-boot:run（日志留在独立窗口）。
echo [后端] 首次启动需下载 Maven 依赖，请耐心等待。
rem MVN_CMD 由自检保证不含空格；使用 call 避开引号嵌套写法
start "Huyao-Backend-8081" /d "%BACKEND_DIR%" cmd /k call %MVN_CMD% -f pom.xml -B -DskipTests spring-boot:run
echo [后端] 已在独立窗口启动，端口 8081，接口前缀 /api。
exit /b 0
