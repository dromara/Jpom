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

set Tag=KeepBx-System-JpomApplication1
set MainClass=cn.keepbx.jpom.JpomApplication
set basePath=%~dp0
set Lib=%basePath%lib\
set Log=%basePath%run.log
set JVM=-server
set ARGS= --jpom.applicationTag=%Tag% --jpom.log=%basePath%log --server.port=2122

if "%1"=="start" (
    call:start
)else (
    if "%1"=="stop" (
        call:stop
    )else (
         if "%1"=="restart" (
            call:restart
         )else (
            if "%1"=="status" (
               call:status
            )else (
               call:use
            )
         )
    )
)
pause
exit

@REM 启动
:start
set TEMPCLASSPATH=
for /f "delims=" %%I in ('dir /B %Lib%') do (set TEMPCLASSPATH=!TEMPCLASSPATH!%Lib%%%I;)
@REM echo 启动成功，关闭窗口不影响运行
cmd /S /C "javaw %JVM% -classpath %TEMPCLASSPATH%"%JAVA_HOME%"\lib\tools.jar -Dapplication=%Tag% -Dbasedir=%basePath% %MainClass% %ARGS% >> %Log%"
echo 启动中.....
goto:eof

@REM 关闭Jpom
:stop
set TEMPCLASSPATH=
for /f "delims=" %%I in ('dir /B %Lib%') do (set TEMPCLASSPATH=!TEMPCLASSPATH!%Lib%%%I;)
set MainClass=cn.keepbx.jpom.JpomClose
java -classpath %TEMPCLASSPATH%"%JAVA_HOME%"\lib\tools.jar %MainClass% %ARGS%" stop"
goto:eof

@REM 查看Jpom运行状态
:status
set TEMPCLASSPATH=
for /f "delims=" %%I in ('dir /B %Lib%') do (set TEMPCLASSPATH=!TEMPCLASSPATH!%Lib%%%I;)
set MainClass=cn.keepbx.jpom.JpomClose
java -classpath %TEMPCLASSPATH%"%JAVA_HOME%"\lib\tools.jar %MainClass% %ARGS%" status"
goto:eof

@REM 重启Jpom
:restart
call:stop
call:start
goto:eof

@REM 提示用法
:use
echo please use (start|stop|restart|status)
goto:eof