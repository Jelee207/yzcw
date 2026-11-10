@echo off
chcp 936 >nul
rem 3306 已在监听则直接跳过；否则拉起便携 MySQL
netstat -ano | findstr /r /c:":3306 .*LISTENING" >nul 2>&1
if not errorlevel 1 (
  echo [数据库] 3306 已在监听，跳过启动。
  exit /b 0
)

set "MYSQL_HOME=D:\dev\mysql-8.0.29-winx64"
if not exist "%MYSQL_HOME%\bin\mysqld.exe" (
  echo [错误] 3306 未监听，且找不到便携 MySQL：%MYSQL_HOME%\bin\mysqld.exe
  echo        请先启动系统 MySQL 服务，或把便携包放到该目录。
  exit /b 1
)
if not exist "%MYSQL_HOME%\my.ini" (
  echo [错误] 缺少配置文件 %MYSQL_HOME%\my.ini
  exit /b 1
)

echo [数据库] 正在启动 mysqld（独立最小化窗口）...
rem 使用 /k 保留窗口，启动失败时能看到报错；避开引号嵌套写法
start "Huyao-MySQL-3306" /min /d "%MYSQL_HOME%\bin" cmd /k mysqld.exe --defaults-file="%MYSQL_HOME%\my.ini" --console

set /a n=0
:WAIT
set /a n+=1
netstat -ano | findstr /r /c:":3306 .*LISTENING" >nul 2>&1
if not errorlevel 1 goto READY
if %n% GEQ 30 goto TIMEOUT
ping -n 2 127.0.0.1 >nul
goto WAIT
:TIMEOUT
echo [错误] MySQL 检测 30 次仍未在 3306 就绪。
echo        请查看最小化的 Huyao-MySQL-3306 窗口里的报错。
exit /b 1
:READY
echo [数据库] MySQL 已在 3306 就绪。
exit /b 0
