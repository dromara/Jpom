@REM
@REM Copyright (c) 2019 Of Him Code Technology Studio
@REM Jpom is licensed under Mulan PSL v2.
@REM You can use this software according to the terms and conditions of the Mulan PSL v2.
@REM You may obtain a copy of Mulan PSL v2 at:
@REM 			http://license.coscl.org.cn/MulanPSL2
@REM THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
@REM See the Mulan PSL v2 for more details.
@REM

@echo off
@if not "%ECHO%" == ""  echo %ECHO%
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
set tmpdir="%ENV_PATH%/../tmp/"
if not exist %tmpdir% md %tmpdir%

@REM see org.springframework.util.StringUtils.cleanPath
@REM set org.springframework.boot.context.config.StandardConfigDataLocationResolver.getResourceLocation
cd %conf_dir%
set conf_dir=%cd%
cd %tmpdir%
set tmpdir=%cd%
cd %ENV_PATH%

set log_dir=%ENV_PATH%\..\logs
set logback_configurationFile=%conf_dir%\logback.xml
set application_conf=%conf_dir%\application.yml

set Lib=%ENV_PATH%\..\lib\
set "RUN_JAR="
set "JAR_MSG="
set server_log="%log_dir%\server.log"
set stdout_log="%log_dir%\stdout.log"

set JAVA_MEM_OPTS= -Xms1024m -Xmx2048m -XX:+UseG1GC
set JAVA_OPTS_EXT= -Djava.awt.headless=true -Djava.net.preferIPv4Stack=true -Dapplication.codeset=UTF-8 -Dfile.encoding=UTF-8 -Djava.io.tmpdir="%tmpdir%"
set APP_OPTS= -Djpom.application.tag="%PID_TAG%" -Dlogging.config="%logback_configurationFile%" -Dspring.config.location="%application_conf%"
set JAVA_OPTS= %JAVA_MEM_OPTS% %JAVA_OPTS_EXT% %APP_OPTS%

java -version 2>&1 | findstr "version" > temp.txt
set /p temp_version= < temp.txt
rem del temp_version.txt
echo %version% | findstr "1.8." > nul
if %errorlevel% equ 0 (
    set JAVA_OPTS= %JAVA_OPTS%
) else (
	set JAVA_OPTS= %JAVA_OPTS% --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/jdk.internal.ref=ALL-UNNAMED
)

set ARGS=%*
set JPOM_LOG=%log_dir%
if not exist %log_dir% md %log_dir%

@REM get list jar
call:listDir

if "%1"=="" (
    color 0a
    TITLE Jpom management system BAT console
    echo. ***** Jpom management system BAT console *****
    echo. !JAR_MSG!
    ::*************************************************************************************************************
    echo.
        echo.  [1] start
        echo.  [2] status
        echo.  [3] restart
        echo.  [4] stop
        echo.  [0] exit 0
    echo.
    @REM enter
    for /l %%i in (1,1,10000) do (
		echo. Please enter the selected serial number:
		set /p ID=
		IF "!ID!"=="1" call:start
		IF "!ID!"=="2" call:status
		IF "!ID!"=="3" call:restart
		IF "!ID!"=="4" call:stop
		IF "!ID!"=="0" EXIT
    )
)else (
     if "%1"=="restart" (
        call:restart
     )else if "%1"=="start" (
     	call:start
     )else if "%1"=="status" (
		call:status
	 )else if "%1"=="stop" (
		call:stop
     )else (
        call:use
     )
)
if "%2" == "upgrade" (
    @REM The upgrade ends directly
    EXIT 0
)

:end
goto:eof

@REM start
:start
	echo Starting..... Closing the window after a successful start does not affect the operation
	echo Please check for startup details:%server_log% or !stdout_log%!
	start /b javaw %JAVA_OPTS% -jar %Lib%!RUN_JAR! %ARGS% > "!stdout_log!" 2>&1
	@REM timeout 3 > NUL
	ping 127.0.0.1 -n 3 > nul
goto:eof


@REM get jar
:listDir
	if "%RUN_JAR%"=="" (
		if exist "%Lib%\run.bin" (
			set /P RUN_JAR=<"%Lib%\run.bin"
			set JAR_MSG=specify running !RUN_JAR!
		)else (
			for /f "delims=" %%I in ('dir /B %Lib%') do (
				if exist %Lib%%%I if not exist %Lib%%%I\nul (
					if "%%~xI" ==".jar" (
						if "%RUN_JAR%"=="" (
							set "RUN_JAR=%%I"
						)
					)
				)
			)
			set JAR_MSG=auto running !RUN_JAR!
		)
	)else (
		set JAR_MSG=specify2 running %RUN_JAR%
	)
	if not exist %Lib%!RUN_JAR! (
	 	echo %JAR_MSG%
		echo file not exist %Lib%!RUN_JAR!
		PAUSE
		EXIT -1
	)
	@REM stdout_log
	if exist "%Lib%\run.bin" (
		set /P RUN_LOG=<"%Lib%\run.log"
		set stdout_log="%log_dir%\!RUN_LOG!"
	)
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
