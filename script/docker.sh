#!/bin/bash
#-----------------------------------------------------------
# 此脚本用于每次发布 docker 镜像
#-----------------------------------------------------------

# https://hub.docker.com/r/jpomdocker/jpom

# 服务端
docker buildx build --platform linux/amd64,linux/arm64 -t jpomdocker/jpom:2.8.15 -f ./modules/server/DockerfileRelease --push .
#
docker buildx build --platform linux/amd64,linux/arm64 -t jpomdocker/jpom:latest -f ./modules/server/DockerfileRelease --push .

# docker logs --tail="100" jpom-server
# docker run -d -p 2122:2122 --name jpom-server -v /etc/localtime:/etc/localtime:ro -v jpom-server-vol:/usr/local/jpom-server jpomdocker/jpom:mac-arm-2.8.15
# docker stop jpom-server
# docker rm jpom-server
# docker exec -it jpom-server /bin/bash
#  docker-compose up -d --build
# docker buildx imagetools inspect jpomdocker/jpom

