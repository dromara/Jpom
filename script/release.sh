#!/bin/bash
#
# Copyright (c) 2019 Code Technology Studio
# Jpom is licensed under Mulan PSL v2.
# You can use this software according to the terms and conditions of the Mulan PSL v2.
# You may obtain a copy of Mulan PSL v2 at:
# 			http://license.coscl.org.cn/MulanPSL2
# THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
# See the Mulan PSL v2 for more details.
#


# 快速打包项目脚本

# 删除前端依赖从新安装
cd ../ && cd web-vue && rm -rf node_modules

# 构建前端
cd ../ && cd web-vue && npm i && npm run build

# 构建 Java
cd ../ && mvn clean && mvn clean package
