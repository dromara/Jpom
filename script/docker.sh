#!/bin/bash
#-----------------------------------------------------------
# 此脚本用于每次发布 docker 镜像
#-----------------------------------------------------------

# https://hub.docker.com/r/jpomdocker/jpom

# 服务端
docker build -f ./modules/server/Dockerfile -t jpomdocker/jpom:2.8.3 ./modules/server/target
#
docker push jpomdocker/jpom:2.8.3

docker build -f ./modules/server/Dockerfile -t jpomdocker/jpom:latest ./modules/server/target
#
docker push jpomdocker/jpom:latest


# 服务端 - arm
docker build -f ./modules/server/DockerfileMacARM -t jpomdocker/jpom:mac-arm-2.8.3 .
#
docker push jpomdocker/jpom:mac-arm-2.8.3

docker build -f ./modules/server/DockerfileMacARM -t jpomdocker/jpom:mac-arm-latest .
#
docker push jpomdocker/jpom:mac-arm-latest


# docker logs --tail="100" jpom-server
# docker push jpomdocker/jpom:mac-arm-2.8.3
# docker pull jpomdocker/jpom:mac-arm-2.8.3
# docker run -d -p 2122:2122 --name jpom-server -v /etc/localtime:/etc/localtime:ro -v jpom-server-vol:/usr/local/jpom-server jpomdocker/jpom:mac-arm-2.8.3
# docker stop jpom-server
# docker rm jpom-server
# docker exec -it jpom-server /bin/bash
#  docker-compose up -d --build
