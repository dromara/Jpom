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
CHCP 65001
setlocal enabledelayedexpansion

@REM Set environment variables to prevent some servers from failing to taskkill
set PATH = %PATH%;C:\Windows\system32;C:\Windows;C:\Windows\system32\Wbem

set Tag=KeepBx-Agent-System-JpomAgentApplication
set MainClass=org.springframework.boot.loader.JarLauncher
set basePath=%~dp0
set Lib=%basePath%lib\
@REM Do not modify----------------------------------↓
set LogName=agent.log
@REM Online upgrade will automatically modify this attribute
set RUNJAR=
@REM Do not modify----------------------------------↑
@REM Whether to enable console log file backup
set LogBack=true
set JVM=-server -Xms200m -Xmx400m
set ARGS= --jpom.applicationTag=%Tag% --jpom.log=%basePath%log --spring.profiles.active=pro --server.port=2123

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
        echo.  [5] use / help
        echo.  [0] exit 0
    echo.
    @REM enter
    echo. Please enter the selected serial number:
    set /p ID=
    IF "!ID!"=="1" call:start
    IF "!ID!"=="2" call:stop
    IF "!ID!"=="3" call:status
    IF "!ID!"=="4" call:restart
    IF "!ID!"=="5" call:use
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
    if "%JAVA_HOME%"=="" (
        echo please configure [JAVA_HOME] environment variable
        PAUSE
        EXIT 2
    )

	echo Starting..... Closing the window after a successful start does not affect the operation
	echo Please check for startup details：%LogName%
	javaw %JVM% -Djava.class.path="%RUNJAR%" -Dapplication=%Tag% -Dbasedir=%basePath%  %MainClass% %ARGS% %1 >> %basePath%%LogName%
	timeout 3
goto:eof


@REM get jar
:listDir
	if "%RUNJAR%"=="" (
		for /f "delims=" %%I in ('dir /B %Lib%') do (
			if exist %Lib%%%I if not exist %Lib%%%I\nul (
			    if "%%~xI" ==".jar" (
                    if "%RUNJAR%"=="" (
				        set RUNJAR=%Lib%%%I
                    )
                )
			)
		)
	)else (
		set RUNJAR=%Lib%%RUNJAR%
	)
	echo run：%RUNJAR%
goto:eof

@REM stop Jpom
:stop
	java -Djava.class.path="%JAVA_HOME%/lib/tools.jar;%RUNJAR%" %MainClass% %ARGS% --event=stop
goto:eof

@REM view Jpom status
:status
	java -Djava.class.path="%JAVA_HOME%/lib/tools.jar;%RUNJAR%" %MainClass% %ARGS% --event=status
goto:eof

@REM restart Jpom
:restart
	echo Stopping....
	call:stop
	timeout 3
	echo starting....
	call:start %1
goto:eof

@REM use
:use
	echo please use (start、stop、restart、status)
goto:eof


