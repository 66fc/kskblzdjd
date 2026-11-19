@echo off
chcp 65001 >nul
call "%~dp0env.bat"
cd /d "%PROJECT_ROOT%\backend"

if not exist "target\badminton-admin-1.0.0.jar" (
  echo [1/2] 首次运行，使用 Maven 打包后端...
  call mvn -s "%PROJECT_ROOT%\scripts\maven-settings.xml" -DskipTests package
  if errorlevel 1 (
    echo Maven 打包失败。
    exit /b 1
  )
)

echo [2/2] 启动 Spring Boot（http://127.0.0.1:8080）...
echo Swagger: http://127.0.0.1:8080/swagger-ui.html
java -jar target\badminton-admin-1.0.0.jar
