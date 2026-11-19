@echo off
chcp 65001 >nul
call "%~dp0env.bat"

if exist "%MYSQL_DATA%\ibdata1" (
  echo 数据目录已存在，跳过初始化。如需重建请先备份并删除：
  echo %MYSQL_DATA%
  exit /b 0
)

echo [1/3] 初始化 MySQL 数据目录（root 空密码）...
"%MYSQL_HOME%\bin\mysqld.exe" --defaults-file="%RUNTIME_HOME%\my.ini" --initialize-insecure --console
if errorlevel 1 goto :error

echo [2/3] 启动 MySQL...
start "MySQL %MYSQL_PORT%" "%MYSQL_HOME%\bin\mysqld.exe" --defaults-file="%RUNTIME_HOME%\my.ini" --console
timeout /t 6 /nobreak >nul

echo [3/3] 设置 root 密码为 %MYSQL_PWD% ...
"%MYSQL_HOME%\bin\mysql.exe" -h127.0.0.1 -P%MYSQL_PORT% -uroot -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '%MYSQL_PWD%'; FLUSH PRIVILEGES;"
if errorlevel 1 goto :error

echo MySQL 初始化完成，端口 %MYSQL_PORT%。
exit /b 0

:error
echo 初始化失败，请查看控制台输出或 MySQL 错误日志。
exit /b 1
