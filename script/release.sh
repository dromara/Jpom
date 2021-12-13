#!/bin/bash

# 快速打包项目脚本

# 删除前端依赖从新安装
cd ../ && cd web-vue && rm -rf node_modules

# 构建前端
cd ../ && cd web-vue && npm i && npm run build

# 构建 Java
cd ../ && mvn clean && mvn clean package
