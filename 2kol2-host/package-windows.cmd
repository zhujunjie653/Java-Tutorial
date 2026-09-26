@echo off
setlocal EnableExtensions

rem Source must live under D:\java. Output, config backup, Maven repo, and temp stay off the user profile.
set "HERE=%~dp0"
if /I not "%HERE:~0,8%"=="D:\java\" (
    echo Put the source under D:\java\ before running this script.
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
    echo Maven not found: %MVN%
    exit /b 1
)
if not defined JDK (
    echo JDK 21 not found. Expected a jdk-21* directory under %JDK_ROOT%.
    exit /b 1
)
if not exist "%JDK%\bin\jpackage.exe" (
    echo jpackage not found: %JDK%\bin\jpackage.exe
    exit /b 1
)

if not exist "D:\java" mkdir "D:\java"
if not exist "D:\java\m2" mkdir "D:\java\m2"
if not exist "D:\java\tmp" mkdir "D:\java\tmp"
set "TEMP=D:\java\tmp"
set "TMP=D:\java\tmp"

set "JAVA_HOME=%JDK%"
set "PATH=%JDK%\bin;%PATH%"

echo Using JDK: %JDK%
call "%MVN%" -f "%~dp0pom.xml" "-Dmaven.repo.local=D:\java\m2" package
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

echo Built: %OUT%\2KOL2Host.exe
echo Config and logs stay next to the exe: %OUT%
echo On first launch, application.properties is copied from the example if it is missing.
exit /b 0
