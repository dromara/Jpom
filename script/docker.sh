#!/bin/bash
#
# Copyright (c) 2019 Of Him Code Technology Studio
# Jpom is licensed under Mulan PSL v2.
# You can use this software according to the terms and conditions of the Mulan PSL v2.
# You may obtain a copy of Mulan PSL v2 at:
# 			http://license.coscl.org.cn/MulanPSL2
# THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
# See the Mulan PSL v2 for more details.
#

#-----------------------------------------------------------
# 历史本机手工推送到 Docker Hub/Aliyun 的兜底脚本。
# 正常发版由 GitHub Actions 发布，见 .github/workflows/docker-publish.yml；
# `release.sh --docker-local` 走等价命令（本机 buildx 兜底）。
#-----------------------------------------------------------

# https://hub.docker.com/r/jpomdocker/jpom

# docker buildx create --use

# 服务端
docker buildx build --platform linux/amd64,linux/arm64,linux/ppc64le -t jpomdocker/jpom:2.11.13 -t jpomdocker/jpom:latest -f ./modules/server/DockerfileRelease --push .

# 插件端
docker buildx build --platform linux/amd64,linux/arm64,linux/ppc64le -t jpomdocker/jpom-agent:2.11.13 -t jpomdocker/jpom-agent:latest -f ./modules/agent/DockerfileRelease --push .

docker buildx build --platform linux/amd64,linux/arm64,linux/ppc64le -t registry.cn-chengdu.aliyuncs.com/jpomdocker/jpom:2.11.13 -t registry.cn-chengdu.aliyuncs.com/jpomdocker/jpom:latest -f ./modules/server/DockerfileRelease --push .



docker buildx build --platform linux/amd64,linux/arm64,linux/ppc64le,linux/arm64/v8 -t jpomdocker/jpom:2-test -f ./modules/server/DockerfileBeta --push .
#
#docker buildx build --platform linux/amd64,linux/arm64 -t jpomdocker/jpom:latest -f ./modules/server/DockerfileRelease --push .

# docker logs --tail="100" jpom-server
# docker run -d -p 2122:2122 --name jpom-server -v /etc/localtime:/etc/localtime:ro -v jpom-server-vol:/usr/local/jpom-server jpomdocker/jpom:mac-arm-2.11.13.1
# docker run -d -p 2122:2122 --name jpom-server -v D:/home/jpom-server/logs:/usr/local/jpom-server/logs -v D:/home/jpom-server/data:/usr/local/jpom-server/data -v D:/home/jpom-server/conf:/usr/local/jpom-server/conf jpomdocker/jpom
# docker stop jpom-server
# docker rm jpom-server
# docker exec -it jpom-server /bin/bash
#  docker-compose up -d --build
# docker buildx imagetools inspect jpomdocker/jpom

