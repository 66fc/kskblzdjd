@echo off
chcp 65001 >nul
setlocal

rem ============================================================
rem  开放局域网访问端口（需要管理员权限）
rem  用法：右键本文件 -> 以管理员身份运行
rem ============================================================

set "PORT=8088"
set "RULE_NAME=Badminton Club %PORT%"

rem --- 自提权 ---
net session >nul 2>&1
if errorlevel 1 (
    echo [提示] 需要管理员权限，正在请求提升...
    powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
    exit /b
)

echo.
echo ============================================================
echo   开放端口 %PORT% 给局域网
echo ============================================================
echo.

powershell -NoProfile -Command ^
  "if (Get-NetFirewallRule -DisplayName '%RULE_NAME%' -ErrorAction SilentlyContinue) { " ^
  "  Set-NetFirewallRule -DisplayName '%RULE_NAME%' -Enabled True -Action Allow -Profile Any; " ^
  "  '规则已存在，已重新启用' " ^
  "} else { " ^
  "  New-NetFirewallRule -DisplayName '%RULE_NAME%' -Direction Inbound -Protocol TCP -LocalPort %PORT% -Action Allow -Profile Any | Out-Null; " ^
  "  '规则已创建' " ^
  "}"

if errorlevel 1 (
    echo [失败] 未能添加防火墙规则，请确认已以管理员身份运行。
    pause
    exit /b 1
)

echo.
echo [完成] 端口 %PORT% 已对局域网开放。
echo.
echo 本机局域网地址：
powershell -NoProfile -Command ^
  "Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -notlike '127.*' -and $_.IPAddress -notlike '169.254.*' } | ForEach-Object { '  http://' + $_.IPAddress + ':%PORT%' }"

echo.
echo 若组员仍无法访问，请检查本机所在 WiFi 是否为「公用网络」。
echo 也可以把网络类型改为「专用」：设置 - 网络和 Internet - WLAN - 属性 - 网络配置文件。
echo.
pause
