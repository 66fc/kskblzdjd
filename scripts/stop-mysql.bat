@echo off
chcp 65001 >nul
call "%~dp0env.bat"

echo 关闭占用 %MYSQL_PORT% 端口的 MySQL 实例...
"%MYSQL_HOME%\bin\mysqladmin.exe" -h127.0.0.1 -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PWD% shutdown
if errorlevel 1 (
  echo 正常关闭失败，可能服务未运行。
) else (
  echo MySQL 已关闭。
)
