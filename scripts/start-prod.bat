@echo off
chcp 65001 >nul
setlocal

rem ============================================================
rem  生产模式一键启动：MySQL + 后端(8080) + 静态服务(8088)
rem  适合演示 / 发给组员访问（局域网）
rem  首次使用请先执行：npm run build（在 frontend 目录）
rem ============================================================

call "%~dp0env.bat"

if "%PROD_PORT%"=="" set "PROD_PORT=8088"

echo.
echo ============================================================
echo   羽球幻想馆 - 生产模式启动
echo ============================================================
echo.

rem ---------- 1. MySQL ----------
echo [1/3] 检查 MySQL（端口 %MYSQL_PORT%）...
powershell -NoProfile -Command "if (Test-NetConnection -ComputerName 127.0.0.1 -Port %MYSQL_PORT% -InformationLevel Quiet -WarningAction SilentlyContinue) { exit 0 } else { exit 1 }" >nul 2>&1
if errorlevel 1 (
    echo       MySQL 未运行，正在启动...
    start "MySQL" /min "%MYSQL_HOME%\bin\mysqld.exe" --defaults-file="%RUNTIME_HOME%\my.ini" --console
    timeout /t 8 /nobreak >nul
) else (
    echo       MySQL 已在运行
)

rem ---------- 2. 后端 ----------
echo [2/3] 检查后端（端口 8080）...
powershell -NoProfile -Command "if (Test-NetConnection -ComputerName 127.0.0.1 -Port 8080 -InformationLevel Quiet -WarningAction SilentlyContinue) { exit 0 } else { exit 1 }" >nul 2>&1
if errorlevel 1 (
    if not exist "%PROJECT_ROOT%\backend\target\badminton-admin-1.0.0.jar" (
        echo [错误] 未找到后端 jar，请先执行：mvn -s scripts\maven-settings.xml -f backend\pom.xml -DskipTests package
        pause
        exit /b 1
    )
    echo       正在启动 Spring Boot...
    start "Backend" /min cmd /c ""%JAVA_HOME%\bin\java.exe" -jar "%PROJECT_ROOT%\backend\target\badminton-admin-1.0.0.jar""
    echo       等待后端就绪（约 15 秒）...
    timeout /t 15 /nobreak >nul
) else (
    echo       后端已在运行
)

rem ---------- 3. 静态服务 ----------
echo [3/3] 检查静态服务（端口 %PROD_PORT%）...
powershell -NoProfile -Command "if (Test-NetConnection -ComputerName 127.0.0.1 -Port %PROD_PORT% -InformationLevel Quiet -WarningAction SilentlyContinue) { exit 0 } else { exit 1 }" >nul 2>&1
if errorlevel 1 (
    if not exist "%PROJECT_ROOT%\frontend\dist\index.html" (
        echo [错误] 未找到前端构建产物，请先执行：cd frontend ^&^& npm run build
        pause
        exit /b 1
    )
    start "Web" /min cmd /c "cd /d "%PROJECT_ROOT%" && node scripts\serve-prod.mjs"
    timeout /t 3 /nobreak >nul
) else (
    echo       静态服务已在运行
)

echo.
echo ============================================================
echo   启动完成
echo ============================================================
cd /d "%PROJECT_ROOT%"
node scripts\print-urls.mjs 2>nul
echo.
echo   公网链接（需另开隧道）：scripts\start-tunnel.bat
echo ============================================================
echo.
pause
