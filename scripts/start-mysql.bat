@echo off
chcp 65001 >nul
call "%~dp0env.bat"

if not exist "%MYSQL_DATA%" (
  echo [1/2] 初始化 MySQL 数据目录...
  "%MYSQL_HOME%\bin\mysqld.exe" --defaults-file="%RUNTIME_HOME%\my.ini" --initialize-insecure --console
)

echo [2/2] 启动 MySQL（端口 %MYSQL_PORT%）...
start "MySQL %MYSQL_PORT%" "%MYSQL_HOME%\bin\mysqld.exe" --defaults-file="%RUNTIME_HOME%\my.ini" --console
echo MySQL 启动窗口已打开，等待 5 秒...
timeout /t 5 /nobreak >nul
"%MYSQL_HOME%\bin\mysql.exe" -h127.0.0.1 -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PWD% -e "SELECT VERSION() AS mysql_version;" 2>nul
if errorlevel 1 (
  echo 连接失败：首次启动请先执行 scripts\init-db.bat 前设置 root 密码，
  echo 或参考 docs\部署启动说明.md 中的“全新环境初始化”章节。
) else (
  echo MySQL 已就绪。
)
