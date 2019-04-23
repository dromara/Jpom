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

set Tag=KeepBx-Agent-System-JpomAgentApplication
set MainClass=cn.keepbx.jpom.JpomAgentApplication
set CloseMainClass=cn.keepbx.jpom.JpomClose
set basePath=%~dp0
set Lib=%basePath%lib\
set Log=%basePath%agent.log
set LogBack=%basePath%log\
set JVM=-server
set ARGS= --jpom.applicationTag=%Tag% --jpom.log=%basePath%log --server.port=2123

color 0a
TITLE Jpom管理系统BAT控制台
echo. ***** Jpom管理系统BAT控制台 *****
::*************************************************************************************************************
echo.
	echo.  [1] 启动 start
	echo.  [2] 关闭 stop
	echo.  [3] 查看运行状态 status
	echo.  [4] 重启 restart
	echo.  [5] 帮助 use
	echo.  [0] 退 出 0
echo.

echo.请输入选择的序号:
set /p ID=
	IF "%id%"=="1" goto start
	IF "%id%"=="2" goto stop
	IF "%id%"=="3" goto status
	IF "%id%"=="4" goto restart
	IF "%id%"=="5" goto use
	IF "%id%"=="0" EXIT
PAUSE
echo 即将关闭窗口
timeout 3
EXIT 1

@REM 启动
:start
    if "%JAVA_HOME%"=="" (
        echo 请配置【JAVA_HOME】环境变量
        PAUSE
        EXIT 2
    )
	rem 备份日志
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
	REM echo 启动成功，关闭窗口不影响运行
	echo 启动中.....关闭窗口不影响运行
	javaw %JVM% -classpath %TEMPCLASSPATH%"%JAVA_HOME%"\lib\tools.jar -Dapplication=%Tag% -Dbasedir=%basePath% %MainClass% %ARGS% >> %Log%
	timeout 3
goto:eof

@REM 关闭Jpom
:stop
	set TEMPCLASSPATH=
	for /f "delims=" %%I in ('dir /B %Lib%') do (set TEMPCLASSPATH=!TEMPCLASSPATH!%Lib%%%I;)
	java -classpath %TEMPCLASSPATH%"%JAVA_HOME%"\lib\tools.jar %CloseMainClass% %ARGS% --jpom.applicationTag=%Tag% --event=stop
goto:eof

@REM 查看Jpom运行状态
:status
	set TEMPCLASSPATH=
	for /f "delims=" %%I in ('dir /B %Lib%') do (set TEMPCLASSPATH=!TEMPCLASSPATH!%Lib%%%I;)
	java -classpath %TEMPCLASSPATH%"%JAVA_HOME%"\lib\tools.jar %CloseMainClass% %ARGS% --jpom.applicationTag=%Tag% --event=status
goto:eof

@REM 重启Jpom
:restart
	echo 停止中....
	call:stop
	timeout 3
	echo 启动中....
	call:start
goto:eof

@REM 重新加载Nginx
:reloadNginx
    nginx -s reload
goto:eof

@REM 提示用法
:use
	echo please use (start|stop|restart|status)
goto:eof


