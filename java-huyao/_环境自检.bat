@echo off
chcp 936 >nul
rem ============================================================
rem 公共环境自检，供其它脚本 call 使用
rem 输出变量：ROOT_DIR / BACKEND_DIR / FRONTEND_DIR / MVN_CMD / MYSQL_EXE
rem 本脚本不启动任何服务，不读取 node_modules 与 target 的内容
rem ============================================================

set "ROOT_DIR=%~dp0"
set "BACKEND_DIR=%~dp0springBootTest"
set "FRONTEND_DIR=%~dp0huyao-login-vue"

rem 某些工具会往环境里注入 SERVER__PORT / SERVER_PORT，
rem 它会覆盖 application-dev.properties 的 server.port=8081，
rem 导致后端绑到已被占用的随机端口并启动失败。
rem 这里强制固定 8081，保证与 Vite 的 /api 代理一致。
set "SERVER__PORT=8081"
set "SERVER_PORT=8081"

echo [自检] 检查 Java ...
where java >nul 2>&1
if errorlevel 1 (
  echo [错误] 未找到 java，请安装 JDK 17 或 21，并配置 PATH 与 JAVA_HOME。
  exit /b 1
)

echo [自检] 检查 Node 与 npm ...
where node >nul 2>&1
if errorlevel 1 (
  echo [错误] 未找到 node，请安装 Node.js 18 以上。
  exit /b 1
)
where npm >nul 2>&1
if errorlevel 1 (
  echo [错误] 未找到 npm，请安装 Node.js 18 以上。
  exit /b 1
)

echo [自检] 定位 Maven ...
set "MVN_CMD="
where mvn >nul 2>&1
if not errorlevel 1 set "MVN_CMD=mvn"
if not defined MVN_CMD (
  for /f "delims=" %%i in ('where /r "%USERPROFILE%\.m2\wrapper\dists" mvn.cmd 2^>nul') do if not defined MVN_CMD set "MVN_CMD=%%i"
)
if not defined MVN_CMD (
  if exist "%BACKEND_DIR%\mvnw.cmd" set "MVN_CMD=%BACKEND_DIR%\mvnw.cmd"
)
if not defined MVN_CMD (
  echo [错误] 未找到 Maven，mvnw.cmd 也不可用，无法构建后端。
  echo        解决办法：安装 Maven 并加入 PATH，或保证 mvnw.cmd 可联网下载 Maven。
  exit /b 1
)
echo        Maven: %MVN_CMD%

echo [自检] 定位 MySQL 客户端（可选，仅初始化数据库需要）...
set "MYSQL_EXE="
where mysql >nul 2>&1
if not errorlevel 1 set "MYSQL_EXE=mysql"
if not defined MYSQL_EXE (
  for %%p in ("C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" "C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe" "C:\Program Files (x86)\MySQL\MySQL Server 8.0\bin\mysql.exe") do (
    if exist %%p if not defined MYSQL_EXE set "MYSQL_EXE=%%~p"
  )
)
if defined MYSQL_EXE (echo        MySQL: %MYSQL_EXE%) else (echo        未找到 mysql.exe，初始化数据库时会给出手动指引)

exit /b 0
