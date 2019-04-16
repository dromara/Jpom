@REM The MIT License (MIT)
@REM
@REM Copyright (c) 2019 码之科技工作室
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
setlocal enabledelayedexpansion

set Tag=KeepBx-System-JpomApplication
set MainClass=cn.keepbx.jpom.JpomServerApplication
set CloseMainClass=cn.keepbx.jpom.JpomClose
set basePath=%~dp0
set Lib=%basePath%lib\
set Log=%basePath%run.log
set LogBack=%basePath%log\
set JVM=-server
set ARGS= --jpom.applicationTag=%Tag% --jpom.log=%basePath%log --server.port=2122

color 0a
TITLE Jpom����ϵͳBAT����̨
echo. ***** Jpom����ϵͳBAT����̨ *****
::*************************************************************************************************************
echo.
	echo.  [1] ���� start
	echo.  [2] �ر� stop
	echo.  [3] �鿴����״̬ status
	echo.  [4] ���� restart
	echo.  [5] ���� use
	echo.  [0] �� �� 0
echo.

echo.������ѡ������:
set /p ID=
	IF "%id%"=="1" goto start
	IF "%id%"=="2" goto stop
	IF "%id%"=="3" goto status
	IF "%id%"=="4" goto restart
	IF "%id%"=="5" goto use
	IF "%id%"=="0" EXIT
PAUSE
echo �����رմ���
timeout 3
EXIT 1

@REM ����
:start
    if "%JAVA_HOME%"=="" (
        echo �����á�JAVA_HOME����������
        PAUSE
        EXIT 2
    )
	rem ������־
	if exist %Log% (
		if not exist %LogBack% (
			echo %LogBack%
			md %LogBack%
		)
		move %Log% %LogBack%%date:~0,4%%date:~5,2%%date:~8,2%0%time:~1,1%%time:~3,2%%time:~6,2%.log
		del %Log%
	)
	set TEMPCLASSPATH=
	for /f "delims=" %%I in ('dir /B %Lib%') do (set TEMPCLASSPATH=!TEMPCLASSPATH!%Lib%%%I;)
	REM echo �����ɹ����رմ��ڲ�Ӱ������
	echo ������.....�رմ��ڲ�Ӱ������
	javaw %JVM% -classpath %TEMPCLASSPATH%"%JAVA_HOME%"\lib\tools.jar -Dapplication=%Tag% -Dbasedir=%basePath% %MainClass% %ARGS% >> %Log%
	timeout 3
goto:eof

@REM �ر�Jpom
:stop
	set TEMPCLASSPATH=
	for /f "delims=" %%I in ('dir /B %Lib%') do (set TEMPCLASSPATH=!TEMPCLASSPATH!%Lib%%%I;)
	java -classpath %TEMPCLASSPATH%"%JAVA_HOME%"\lib\tools.jar %CloseMainClass% %ARGS% --jpom.applicationTag=%Tag% --event=stop
goto:eof

@REM �鿴Jpom����״̬
:status
	set TEMPCLASSPATH=
	for /f "delims=" %%I in ('dir /B %Lib%') do (set TEMPCLASSPATH=!TEMPCLASSPATH!%Lib%%%I;)
	java -classpath %TEMPCLASSPATH%"%JAVA_HOME%"\lib\tools.jar %CloseMainClass% %ARGS% --jpom.applicationTag=%Tag% --event=status
goto:eof

@REM ����Jpom
:restart
	echo ֹͣ��....
	call:stop
	timeout 3
	echo ������....
	call:start
goto:eof

@REM ���¼���Nginx
:reloadNginx
    nginx -s reload
goto:eof

@REM ��ʾ�÷�
:use
	echo please use (start|stop|restart|status)
goto:eof