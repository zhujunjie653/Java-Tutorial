@echo off
setlocal EnableExtensions
chcp 65001 >nul

rem 源码必须在 D:\java 下。产物、配置备份、Maven 仓库和临时文件都不写到 C 盘用户目录。
set "HERE=%~dp0"
if /I not "%HERE:~0,8%"=="D:\java\" (
    echo 请把源码放在 D:\java 下再运行本脚本。
    exit /b 1
)

cd /d "%~dp0"

set "MVN=C:\Program Files\Apache\apache-maven-3.9.16\bin\mvn.cmd"
set "JDK_ROOT=C:\Program Files\Microsoft"
set "JDK="
for /d %%D in ("%JDK_ROOT%\jdk-21*") do (
    if exist "%%~D\bin\jpackage.exe" set "JDK=%%~D"
)

if not exist "%MVN%" (
    echo 找不到 Maven：%MVN%
    exit /b 1
)
if not defined JDK (
    echo 找不到 JDK 21。请确认 %JDK_ROOT% 下有 jdk-21 开头的目录。
    exit /b 1
)
if not exist "%JDK%\bin\jpackage.exe" (
    echo 找不到 jpackage：%JDK%\bin\jpackage.exe
    exit /b 1
)

if not exist "D:\java" mkdir "D:\java"
if not exist "D:\java\m2" mkdir "D:\java\m2"
if not exist "D:\java\tmp" mkdir "D:\java\tmp"
set "TEMP=D:\java\tmp"
set "TMP=D:\java\tmp"

set "JAVA_HOME=%JDK%"
set "PATH=%JDK%\bin;%PATH%"

echo 使用 JDK：%JDK%
call "%MVN%" -q -f "%~dp0pom.xml" "-Dmaven.repo.local=D:\java\m2" package
if errorlevel 1 exit /b 1

set "STAGE=%~dp0target\jpackage-input"
if exist "%STAGE%" rmdir /s /q "%STAGE%"
mkdir "%STAGE%"
copy /Y "%~dp0target\kol2-host.jar" "%STAGE%\kol2-host.jar" >nul
if errorlevel 1 exit /b 1

set "DIST=D:\java"
set "OUT=%DIST%\2KOL2Host"
set "KEEP=D:\java\2KOL2Host-application.properties"
if exist "%KEEP%" del /q "%KEEP%"
if exist "%OUT%\application.properties" copy /Y "%OUT%\application.properties" "%KEEP%" >nul
if exist "%OUT%" rmdir /s /q "%OUT%"

"%JDK%\bin\jpackage.exe" --type app-image --dest "%DIST%" --name "2KOL2Host" --input "%STAGE%" --main-jar kol2-host.jar --main-class kol2.Kol2HostApp
if errorlevel 1 exit /b 1

copy /Y "%~dp0config\application.example.properties" "%OUT%\application.example.properties" >nul
if exist "%KEEP%" (
    copy /Y "%KEEP%" "%OUT%\application.properties" >nul
    del /q "%KEEP%"
)

echo 已生成：%OUT%\2KOL2Host.exe
echo 配置和日志在 exe 旁边：%OUT%
echo 第一次打开时，如果还没有 application.properties，会从示例复制。
exit /b 0
