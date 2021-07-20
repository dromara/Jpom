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

@REM 设置环境变量，避免部分服务器没有 taskkill
set PATH = %PATH%;C:\Windows\system32;C:\Windows;C:\Windows\system32\Wbem

set Tag=KeepBx-Agent-System-JpomAgentApplication
set MainClass=org.springframework.boot.loader.JarLauncher
set basePath=%~dp0
set Lib=%basePath%lib\
@REM 请勿修改----------------------------------↓
set LogName=agent.log
@REM 在线升级会自动修改此属性
set RUNJAR=
@REM 请勿修改----------------------------------↑
@REM 是否开启控制台日志文件备份
set LogBack=true
set JVM=-server
set ARGS= --jpom.applicationTag=%Tag% --jpom.log=%basePath%log --spring.profiles.active=pro --server.port=2123

@REM 读取jar
call:listDir

if "%1"=="" (
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
    @REM 输入
    echo.请输入选择的序号:
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
 @REM 升级直接结束
)
EXIT 0

@REM 启动
:start
    if "%JAVA_HOME%"=="" (
        echo 请配置【JAVA_HOME】环境变量
        PAUSE
        EXIT 2
    )

	echo 启动中.....关闭窗口不影响运行
	javaw %JVM% -Djava.class.path="%JAVA_HOME%/lib/tools.jar;%RUNJAR%" -Dapplication=%Tag% -Dbasedir=%basePath%  %MainClass% %ARGS% >> %basePath%%LogName%
	timeout 3
goto:eof


@REM 获取jar
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
	echo 运行：%RUNJAR%
goto:eof

@REM 关闭Jpom
:stop
	java -Djava.class.path="%JAVA_HOME%/lib/tools.jar;%RUNJAR%" %MainClass% %ARGS% --event=stop
goto:eof

@REM 查看Jpom运行状态
:status
	java -Djava.class.path="%JAVA_HOME%/lib/tools.jar;%RUNJAR%" %MainClass% %ARGS% --event=status
goto:eof

@REM 重启Jpom
:restart
	echo 停止中....
	call:stop
	timeout 3
	echo 启动中....
	call:start
goto:eof

@REM 提示用法
:use
	echo please use (start、stop、restart、status)
goto:eof


