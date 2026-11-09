@echo off
rem Copyright (c) 2019 Of Him Code Technology Studio
rem Jpom is licensed under Mulan PSL v2.
rem 用法: AgentGo.bat {start^|stop^|restart^|status}
setlocal enabledelayedexpansion
set "BIN_DIR=%~dp0"
set "BASE=%BIN_DIR%.."
set "EXE=%BIN_DIR%agent.exe"
set "PIDFILE=%BIN_DIR%agent.pid"

if /I "%1"=="start" goto start
if /I "%1"=="stop" goto stop
if /I "%1"=="restart" goto restart
if /I "%1"=="status" goto status
echo 用法: %0 {start^|stop^|restart^|status}
exit /b 2

:running
set "PID="
if exist "%PIDFILE%" set /p PID=<"%PIDFILE%"
if defined PID (
  tasklist /FI "PID eq %PID%" 2>nul | findstr /R /C:" %PID% " >nul && exit /b 0
)
exit /b 1

:start
call :running
if not errorlevel 1 (
  echo Jpom Go Agent 已在运行 ^(pid=%PID%^)
  exit /b 0
)
if not exist "%EXE%" (
  echo 找不到可执行文件: %EXE%
  exit /b 1
)
if not exist "%BASE%\logs" mkdir "%BASE%\logs"
start "" /B "%EXE%" -base "%BASE%" >> "%BASE%\logs\stdout.log" 2>&1
echo %ERRORLEVEL% > "%PIDFILE%"
echo Jpom Go Agent 启动中，pid 文件: %PIDFILE%
exit /b 0

:stop
call :running
if errorlevel 1 (
  echo Jpom Go Agent 未运行
  del /Q "%PIDFILE%" 2>nul
  exit /b 0
)
taskkill /PID %PID% /F >nul 2>&1
del /Q "%PIDFILE%" 2>nul
echo Jpom Go Agent 已停止
exit /b 0

:restart
call :stop
timeout /T 1 /NOBREAK >nul
goto start

:status
call :running
if errorlevel 1 (
  echo Jpom Go Agent 未运行
  exit /b 1
)
echo Jpom Go Agent 正在运行 ^(pid=%PID%^)
exit /b 0
