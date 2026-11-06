@echo off
chcp 936 >nul
cd /d "%~dp0"
call "%~dp0_环境自检.bat"
if errorlevel 1 exit /b 1

if not defined MYSQL_EXE (
  echo [警告] 未找到 mysql.exe，无法自动初始化。
  echo        请手动执行：mysql -uroot -p123456 ^< springBootTest\sql\huyao.sql
  exit /b 1
)

"%MYSQL_EXE%" -uroot -p123456 -e "USE huyao" >nul 2>&1
if not errorlevel 1 (
  echo [数据库] huyao 已存在，跳过初始化（不会清空已有数据）。
  exit /b 0
)

echo [数据库] huyao 不存在，执行 springBootTest\sql\huyao.sql ...
"%MYSQL_EXE%" -uroot -p123456 --default-character-set=utf8mb4 < "%~dp0springBootTest\sql\huyao.sql"
if errorlevel 1 (
  echo [错误] 初始化失败。确认 MySQL 服务已启动、账号密码为 root / 123456。
  exit /b 1
)
echo [数据库] 初始化完成，测试账号 admin / 123456。
exit /b 0
