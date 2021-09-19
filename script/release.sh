#!/bin/bash

# 快速打包项目脚本

# 构建前端
cd ../ && cd web-vue && npm i && npm run build

# 构建 Java
cd ../ && mvn clean package
