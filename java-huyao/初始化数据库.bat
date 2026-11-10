@echo off
chcp 936 >nul
cd /d "%~dp0"
call "%~dp0_环境自检.bat"
if errorlevel 1 exit /b 1

if not defined MYSQL_EXE (
  echo [提示] 未找到 mysql.exe，跳过建库检查（若库不存在后端会连不上）。
  exit /b 0
)

"%MYSQL_EXE%" -uroot -p123456 -h127.0.0.1 -P3306 --protocol=tcp -e "USE huyao" >nul 2>&1
if not errorlevel 1 (
  echo [数据库] huyao 已存在，跳过初始化（不会清空已有数据）。
  rem 兼容机房旧库：缺 avatar 列则补上；列已存在时 --force 忽略报错
  "%MYSQL_EXE%" -uroot -p123456 -h127.0.0.1 -P3306 --protocol=tcp --force -e "ALTER TABLE huyao.tb_user ADD COLUMN avatar MEDIUMTEXT DEFAULT NULL" >nul 2>&1
  exit /b 0
)

echo [数据库] huyao 不存在，执行 springBootTest\sql\huyao.sql ...
"%MYSQL_EXE%" -uroot -p123456 -h127.0.0.1 -P3306 --protocol=tcp --default-character-set=utf8mb4 < "%~dp0springBootTest\sql\huyao.sql"
if errorlevel 1 (
  echo [错误] 初始化失败：确认 MySQL 已就绪、账号为 root / 123456。
  exit /b 1
)
echo [数据库] 初始化完成，测试账号 admin / 123456。
exit /b 0
