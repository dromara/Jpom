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


