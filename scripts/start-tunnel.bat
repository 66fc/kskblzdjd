@echo off
chcp 65001 >nul
setlocal

rem ============================================================
rem  启动公网隧道（Cloudflare Quick Tunnel）
rem  前提：start-prod.bat 已启动，静态服务在 8088
rem  说明：每次启动都会生成新的随机域名，把新地址发给组员即可
rem ============================================================

set "RUNTIME_HOME=D:\DevTools\badminton-runtime"
set "CF=%RUNTIME_HOME%\tools\cloudflared.exe"
set "PORT=8088"
set "LOG=%RUNTIME_HOME%\tunnel.log"

if not exist "%CF%" (
    echo [错误] 未找到 cloudflared：%CF%
    echo 请先下载：
    echo   curl -L -o "%CF%" https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe
    pause
    exit /b 1
)

powershell -NoProfile -Command "if (Test-NetConnection -ComputerName 127.0.0.1 -Port %PORT% -InformationLevel Quiet -WarningAction SilentlyContinue) { exit 0 } else { exit 1 }" >nul 2>&1
if errorlevel 1 (
    echo [错误] 本地 %PORT% 端口未监听，请先运行 scripts\start-prod.bat
    pause
    exit /b 1
)

echo.
echo ============================================================
echo   正在建立公网隧道...
echo ============================================================
echo.

del /q "%LOG%" 2>nul
start "Tunnel" /min "%CF%" tunnel --url http://localhost:%PORT% --no-autoupdate --logfile "%LOG%"

echo 等待分配公网地址...
timeout /t 12 /nobreak >nul

powershell -NoProfile -Command ^
  "$u = (Select-String -Path '%LOG%' -Pattern 'https://[a-z0-9-]+\.trycloudflare\.com' -AllMatches).Matches.Value | Select-Object -Unique -First 1; " ^
  "if ($u) { " ^
  "  Write-Host ''; Write-Host '  公网地址（发给组员）'; Write-Host \"    $u/\"; Write-Host ''; " ^
  "  Write-Host \"    客户端    $u/club/home\"; " ^
  "  Write-Host \"    后台      $u/login\"; " ^
  "  Write-Host \"    会员登录  $u/club/login\"; Write-Host '' " ^
  "} else { Write-Host '尚未分配地址，请稍等后查看日志：'; Write-Host '%LOG%' }"

echo ============================================================
echo   关闭隧道：直接关闭标题为 Tunnel 的窗口
echo   注意：本机必须保持开机并联网，组员才能访问
echo ============================================================
echo.
pause
