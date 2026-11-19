@echo off
chcp 65001 >nul
call "%~dp0env.bat"
cd /d "%PROJECT_ROOT%\frontend"

if not exist "node_modules" (
  echo [1/2] 安装前端依赖（npmmirror）...
  call npm install --no-audit --no-fund
  if errorlevel 1 (
    echo npm install 失败。
    exit /b 1
  )
)

echo [2/2] 启动 Vue 开发服务器（http://127.0.0.1:5173）...
call npm run dev
