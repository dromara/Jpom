@echo off

@REM 快速打包项目脚本

@REM 构建前端
call cd ../ && cd web-vue && npm i && npm run build

@REM 构建 Java
call cd ../ && mvn clean package
