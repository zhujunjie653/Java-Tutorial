@echo off
setlocal EnableExtensions
chcp 65001 >nul
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

set "JAVA_HOME=%JDK%"
set "PATH=%JDK%\bin;%PATH%"

echo 使用 JDK：%JDK%
call "%MVN%" -q -f "%~dp0pom.xml" package
if errorlevel 1 exit /b 1

set "STAGE=%~dp0target\jpackage-input"
if exist "%STAGE%" rmdir /s /q "%STAGE%"
mkdir "%STAGE%"
copy /Y "%~dp0target\kol2-host.jar" "%STAGE%\kol2-host.jar" >nul
if errorlevel 1 exit /b 1

set "DIST=%~dp0dist"
set "OUT=%DIST%\2KOL2Host"
set "KEEP=%TEMP%\kol2-host-application.properties"
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
echo 配置在 exe 旁边。第一次打开时，如果还没有 application.properties，会从示例复制。
exit /b 0
