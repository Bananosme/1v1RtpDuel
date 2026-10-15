@echo off
rem Сборка плагина локально скачанным Maven (tools\apache-maven-3.9.9).
rem Использование: build.cmd            -> mvn clean package
rem                 build.cmd <args...> -> mvn <args...>
setlocal
set "MVN=%~dp0tools\apache-maven-3.9.9\bin\mvn.cmd"
if not exist "%MVN%" (
    echo [build] Maven not found at "%MVN%"
    exit /b 1
)
if "%~1"=="" (
    call "%MVN%" -B clean package
) else (
    call "%MVN%" %*
)
endlocal
