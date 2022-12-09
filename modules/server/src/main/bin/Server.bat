@REM
@REM The MIT License (MIT)
@REM
@REM Copyright (c) 2019 Code Technology Studio
@REM
@REM Permission is hereby granted, free of charge, to any person obtaining a copy of
@REM this software and associated documentation files (the "Software"), to deal in
@REM the Software without restriction, including without limitation the rights to
@REM use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
@REM the Software, and to permit persons to whom the Software is furnished to do so,
@REM subject to the following conditions:
@REM
@REM The above copyright notice and this permission notice shall be included in all
@REM copies or substantial portions of the Software.
@REM
@REM THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
@REM IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
@REM FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
@REM COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
@REM IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
@REM CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
@REM

@echo off
@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT"  setlocal
setlocal enabledelayedexpansion

set ENV_PATH=.\
if "%OS%" == "Windows_NT" set ENV_PATH=%~dp0%

@REM Set environment variables to prevent some servers from failing to taskkill
set PATH = %PATH%;C:\Windows\system32;C:\Windows;C:\Windows\system32\Wbem

if "%JAVA_HOME%"=="" (
	echo please configure [JAVA_HOME] environment variable
	PAUSE
	EXIT 2
)

set PID_TAG="JPOM_SERVER_APPLICATION"
set conf_dir="%ENV_PATH%/../conf/"

@REM see org.springframework.util.StringUtils.cleanPath
@REM set org.springframework.boot.context.config.StandardConfigDataLocationResolver.getResourceLocation
cd %conf_dir%
set conf_dir=%cd%
cd %ENV_PATH%

set log_dir=%ENV_PATH%\..\logs
set logback_configurationFile=%conf_dir%\logback.xml
set application_conf=%conf_dir%\application.yml

set Lib=%ENV_PATH%\..\lib\
set RUN_JAR=
set server_log="%log_dir%\server.log"
set stdout_log="%log_dir%\stdout.log"

set JAVA_MEM_OPTS= -Xms1024m -Xmx2048m -XX:PermSize=128m -XX:+UseG1GC
set JAVA_OPTS_EXT= -Djava.awt.headless=true -Djava.net.preferIPv4Stack=true -Dapplication.codeset=UTF-8 -Dfile.encoding=UTF-8
set APP_OPTS= -Dapplication="%PID_TAG%" -Dlogging.config="%logback_configurationFile%" -Dspring.config.location="%application_conf%"
set JAVA_OPTS= %JAVA_MEM_OPTS% %JAVA_OPTS_EXT% %APP_OPTS%

set ARGS=%*
set JPOM_LOG=%log_dir%
if not exist %log_dir% md %log_dir%

@REM get list jar
call:listDir

if "%1"=="" (
    color 0a
    TITLE Jpom management system BAT console
    echo. ***** Jpom management system BAT console *****
    ::*************************************************************************************************************
    echo.
        echo.  [1] start
        echo.  [2] stop
        echo.  [3] status
        echo.  [4] restart
        echo.  [0] exit 0
    echo.
    @REM enter
    echo. Please enter the selected serial number:
    set /p ID=
    IF "!ID!"=="1" call:start
    IF "!ID!"=="2" call:stop
    IF "!ID!"=="3" call:status
    IF "!ID!"=="4" call:restart
    IF "!ID!"=="0" EXIT
)else (
     if "%1"=="restart" (
        call:restart
     )else (
        call:use
     )
)
if "%2" NEQ "upgrade" (
    PAUSE
)else (
 @REM The upgrade ends directly
)
EXIT 0

@REM start
:start
	echo Starting..... Closing the window after a successful start does not affect the operation
	echo Please check for startup details:%server_log% or %stdout_log%
	start /b javaw %JAVA_OPTS% -jar %RUN_JAR% %ARGS% > "%stdout_log%" 2>&1
	@REM timeout 3 > NUL
	ping 127.0.0.1 -n 3 > nul
goto:eof


@REM get jar
:listDir
	if "%RUN_JAR%"=="" (

		for /f "delims=" %%I in ('dir /B %Lib%') do (
			if exist %Lib%%%I if not exist %Lib%%%I\nul (
			    if "%%~xI" ==".jar" (
                    if "%RUN_JAR%"=="" (
				        set RUN_JAR=%Lib%%%I
                    )
                )
			)
		)
	)else (
		set RUN_JAR=%Lib%%RUN_JAR%
	)
	echo run:%RUN_JAR%
goto:eof

@REM stop Jpom
:stop
	echo "jpom server stop "
	for /f "tokens=1 delims= " %%I in ('jps -v ^| findstr "%PID_TAG%"') do taskkill /F /PID %%I
goto:eof

@REM view Jpom status
:status
	echo "jpom server status "
	set pid=
	for /f "tokens=1 delims= " %%I in ('jps -v ^| findstr "%PID_TAG%"') do set pid=%%I
	echo "running: %pid%"
goto:eof

@REM restart Jpom
:restart
	echo Stopping....
	call:stop
	@REM timeout 3 > NUL
	ping 127.0.0.1 -n 3 > nul
	echo starting....
	call:start %1
goto:eof
