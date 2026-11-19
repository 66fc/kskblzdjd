@echo off
chcp 65001 >nul
call "%~dp0env.bat"
cd /d "%PROJECT_ROOT%\database"

echo ============================================================
echo 重建数据库 badminton_db 并导入演示数据
echo 目标：127.0.0.1:%MYSQL_PORT%  用户：%MYSQL_USER%
echo ============================================================
for %%f in (01_create_database.sql 02_create_tables.sql 03_create_indexes.sql 04_create_triggers.sql 05_create_views.sql 06_create_procedures.sql 07_seed_data.sql) do (
  echo ^>^> 执行 %%f
  "%MYSQL_HOME%\bin\mysql.exe" -h127.0.0.1 -P%MYSQL_PORT% -u%MYSQL_USER% --default-character-set=utf8mb4 < "%%f"
  if errorlevel 1 (
    echo [失败] %%f，请检查 MySQL 是否已启动。
    exit /b 1
  )
)
echo.
echo 数据库初始化完成。
