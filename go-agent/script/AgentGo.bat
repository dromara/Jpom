@echo off
rem Copyright (c) 2019 Of Him Code Technology Studio
rem Jpom is licensed under Mulan PSL v2.
rem 用法: AgentGo.bat {start^|stop^|restart^|status^|upgrade}
setlocal enabledelayedexpansion
set "BIN_DIR=%~dp0"
set "BASE=%BIN_DIR%.."
set "EXE=%BIN_DIR%agent.exe"
set "PIDFILE=%BIN_DIR%agent.pid"

if /I "%1"=="start" goto start
if /I "%1"=="stop" goto stop
if /I "%1"=="restart" goto restart
if /I "%1"=="status" goto status
if /I "%1"=="upgrade" goto upgrade
echo 用法: %0 {start^|stop^|restart^|status^|upgrade}
exit /b 2

:running
set "PID="
if exist "%PIDFILE%" set /p PID=<"%PIDFILE%"
if not defined PID exit /b 1
tasklist /FI "PID eq %PID%" /NH 2>nul | findstr /R /C:" %PID% " >nul
exit /b %ERRORLEVEL%

:stop
call :running
if errorlevel 1 (
  echo Jpom Go Agent 未运行
  del /Q "%PIDFILE%" 2>nul
  exit /b 0
)
taskkill /PID !PID! /F >nul 2>&1
del /Q "%PIDFILE%" 2>nul
echo Jpom Go Agent 已停止
exit /b 0

:restart
call :stop
ping -n 2 127.0.0.1 >nul
goto start

:upgrade
rem 升级包替换后 stop + start（Windows 下新进程会自己写 bin\agent.pid）
call :stop
ping -n 2 127.0.0.1 >nul
goto start

:start
call :running
if not errorlevel 1 (
  echo Jpom Go Agent 已在运行 ^(pid=!PID!^)
  exit /b 0
)
if not exist "%EXE%" (
  echo 找不到可执行文件: %EXE%
  exit /b 1
)
if not exist "%BASE%\logs" mkdir "%BASE%\logs"
if exist "%PIDFILE%" del /Q "%PIDFILE%"
start "" /B "%EXE%" -base "%BASE%" >> "%BASE%\logs\stdout.log" 2>&1
rem 轮询等待 Go 进程自己写 bin\agent.pid（最多 5 秒）；不再把 %ERRORLEVEL% 当 PID。
set "PID="
for /L %%i in (1,1,5) do (
  if not defined PID (
    if exist "%PIDFILE%" set /p PID=<"%PIDFILE%"
  )
  if defined PID goto start_ready
  ping -n 2 127.0.0.1 >nul
)

:start_ready
if not defined PID (
  echo Jpom Go Agent 启动失败: 5 秒内未生成 pid 文件 %PIDFILE%
  exit /b 1
)
call :running
if errorlevel 1 (
  echo Jpom Go Agent 启动失败: pid 文件中的进程不存在 ^(pid=!PID!^)
  del /Q "%PIDFILE%" 2>nul
  exit /b 1
)
echo Jpom Go Agent 启动成功 ^(pid=!PID!^)，日志: %BASE%\logs\stdout.log
exit /b 0

:status
call :running
if errorlevel 1 (
  echo Jpom Go Agent 未运行
  exit /b 1
)
echo Jpom Go Agent 正在运行 ^(pid=!PID!^)
exit /b 0
