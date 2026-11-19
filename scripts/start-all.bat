@echo off
chcp 65001 >nul
set "ROOT=%~dp0"

echo ============================================================
echo 星羽羽毛球场地管理系统 - 一键启动
echo MySQL : 127.0.0.1:3307
echo 后端   : http://127.0.0.1:8080/swagger-ui.html
echo 前端   : http://127.0.0.1:5173
echo ============================================================

start "MySQL" cmd /c ""%ROOT%start-mysql.bat""
timeout /t 8 /nobreak >nul

start "Backend" cmd /c ""%ROOT%start-backend.bat""
timeout /t 15 /nobreak >nul

start "Frontend" cmd /c ""%ROOT%start-frontend.bat""

echo 三个服务窗口已启动。首次使用请先执行 scripts\init-mysql.bat 与 scripts\init-db.bat。
