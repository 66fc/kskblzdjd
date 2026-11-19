@echo off
chcp 65001 >nul
rem ============================================================
rem 公共环境变量
rem
rem 策略：优先使用便携运行时（RUNTIME_HOME），不存在则沿用系统已安装的
rem       JDK / Maven / MySQL。换机器或组员使用时通常无需修改。
rem ============================================================
set "PROJECT_ROOT=%~dp0.."

if "%RUNTIME_HOME%"=="" set "RUNTIME_HOME=D:\DevTools\badminton-runtime"

rem ---------- JDK ----------
if "%JAVA_HOME%"=="" (
    if exist "%RUNTIME_HOME%\jdk17\jdk-17.0.21+9\bin\java.exe" (
        set "JAVA_HOME=%RUNTIME_HOME%\jdk17\jdk-17.0.21+9"
    )
)

rem ---------- Maven ----------
if "%MAVEN_HOME%"=="" (
    if exist "%RUNTIME_HOME%\maven\apache-maven-3.9.16\bin\mvn.cmd" (
        set "MAVEN_HOME=%RUNTIME_HOME%\maven\apache-maven-3.9.16"
    )
)

rem ---------- MySQL ----------
if "%MYSQL_HOME%"=="" (
    if exist "%RUNTIME_HOME%\mysql\mysql-8.0.28-winx64\bin\mysqld.exe" (
        set "MYSQL_HOME=%RUNTIME_HOME%\mysql\mysql-8.0.28-winx64"
    )
)
if "%MYSQL_DATA%"=="" set "MYSQL_DATA=%RUNTIME_HOME%\mysql-data"
if "%MYSQL_PORT%"=="" set "MYSQL_PORT=3307"
if "%MYSQL_USER%"=="" set "MYSQL_USER=root"
if "%MYSQL_PWD%"=="" set "MYSQL_PWD=root123"

rem ---------- PATH ----------
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"
if defined MAVEN_HOME set "PATH=%MAVEN_HOME%\bin;%PATH%"
if defined MYSQL_HOME set "PATH=%MYSQL_HOME%\bin;%PATH%"

rem ---------- 依赖提示（缺失时仅提示，不中断） ----------
if not defined JAVA_HOME (
    java -version >nul 2>&1 || echo [提示] 未检测到 JDK 17，请安装后重试，或配置 JAVA_HOME。
)
