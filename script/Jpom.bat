@REM ----------------------------------------------------------------------------
@REM  Copyright 2001-2006 The Apache Software Foundation.
@REM
@REM  Licensed under the Apache License, Version 2.0 (the "License");
@REM  you may not use this file except in compliance with the License.
@REM  You may obtain a copy of the License at
@REM
@REM       http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM  Unless required by applicable law or agreed to in writing, software
@REM  distributed under the License is distributed on an "AS IS" BASIS,
@REM  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM  See the License for the specific language governing permissions and
@REM  limitations under the License.
@REM ----------------------------------------------------------------------------
@REM
@REM   Copyright (c) 2001-2006 The Apache Software Foundation.  All rights
@REM   reserved.


@echo off
setlocal enabledelayedexpansion

set Tag=KeepBx-System-JpomApplication1
set MainClass=cn.keepbx.jpom.JpomApplication
set basePath=%~dp0
set Lib=%basePath%lib\
set Log=%basePath%run.log
set JVM=-server
set ARGS=--server.port=2123 --jpom.path=%basePath% --jpom.log=%basePath%log --jpom.safeMode=false

set TEMPCLASSPATH=
for /f "delims=" %%I in ('dir /B %Lib%') do (set TEMPCLASSPATH=!TEMPCLASSPATH!%Lib%%%I;)
@REM echo 启动成功，关闭窗口不影响运行
cmd /S /C "javaw %JVM% -classpath %TEMPCLASSPATH%"%JAVA_HOME%"\lib\tools.jar -Dappliction=%Tag% -Dbasedir=%basePath% %MainClass% %ARGS% >> %Log%"

:end