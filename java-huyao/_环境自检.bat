@echo off
chcp 936 >nul
rem ============================================================
rem 公共环境自检，供其它脚本 call 使用
rem 输出变量：ROOT_DIR / BACKEND_DIR / FRONTEND_DIR / JAVA_EXE / MVN_CMD
rem           MYSQL_EXE / MYSQLD_EXE / MYSQL_HOME
rem 不启动任何服务；不依赖系统 PATH 是否已刷新（用户环境变量要重新登录才生效）
rem ============================================================

set "ROOT_DIR=%~dp0"
set "BACKEND_DIR=%~dp0springBootTest"
set "FRONTEND_DIR=%~dp0huyao-login-vue"

rem 固定后端端口，避免工具注入的 SERVER__PORT 覆盖 application-dev.properties
set "SERVER__PORT=8081"
set "SERVER_PORT=8081"

rem ---- 优先探测本机便携工具链，逐项独立判断，拷到别的机器也不受影响 ----
set "JAVA_EXE="
set "MVN_CMD="
set "MYSQL_EXE="
set "MYSQLD_EXE="
set "MYSQL_HOME="

if exist "D:\dev\jdk21\jdk-21.0.13+9\bin\java.exe" (
  set "JAVA_HOME=D:\dev\jdk21\jdk-21.0.13+9"
  set "JAVA_EXE=D:\dev\jdk21\jdk-21.0.13+9\bin\java.exe"
  set "PATH=D:\dev\jdk21\jdk-21.0.13+9\bin;%PATH%"
)
if exist "D:\dev\maven-3.9.9\bin\mvn.cmd" (
  set "MVN_CMD=D:\dev\maven-3.9.9\bin\mvn.cmd"
  set "PATH=D:\dev\maven-3.9.9\bin;%PATH%"
)
if exist "D:\dev\mysql-8.0.29-winx64\bin\mysql.exe" (
  set "MYSQL_HOME=D:\dev\mysql-8.0.29-winx64"
  set "MYSQL_EXE=D:\dev\mysql-8.0.29-winx64\bin\mysql.exe"
  set "MYSQLD_EXE=D:\dev\mysql-8.0.29-winx64\bin\mysqld.exe"
  set "PATH=D:\dev\mysql-8.0.29-winx64\bin;%PATH%"
)
if defined JAVA_HOME if not defined JAVA_EXE set "PATH=%JAVA_HOME%\bin;%PATH%"

echo [自检] 检查 Java ...
if defined JAVA_EXE (
  echo        Java: %JAVA_EXE%
) else (
  where java >nul 2>&1
  if errorlevel 1 (
    echo [错误] 未找到 java。请安装 JDK 17/21，或确认 D:\dev\jdk21 便携包完整。
    exit /b 1
  )
  echo        Java: 来自系统 PATH
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
if not defined MVN_CMD (
  where mvn >nul 2>&1
  if not errorlevel 1 set "MVN_CMD=mvn"
)
rem 用相对名，避免因项目路径含空格导致命令行引号嵌套
if not defined MVN_CMD if exist "%BACKEND_DIR%\mvnw.cmd" set "MVN_CMD=mvnw.cmd"
if not defined MVN_CMD (
  echo [错误] 未找到 Maven，mvnw.cmd 也不可用，无法构建后端。
  exit /b 1
)
echo        Maven: %MVN_CMD%

echo [自检] 定位 MySQL 客户端 ...
if not defined MYSQL_EXE (
  where mysql >nul 2>&1
  if not errorlevel 1 set "MYSQL_EXE=mysql"
)
if defined MYSQL_EXE (echo        MySQL: %MYSQL_EXE%) else (echo        [提示] 未找到 mysql.exe，将跳过自动建库检查)

exit /b 0
