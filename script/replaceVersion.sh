#!/bin/bash
#-----------------------------------------------------------
# 此脚本用于每次升级时替换相应位置的版本号
#-----------------------------------------------------------

set -o errexit

pwd=$(pwd)

echo "当前路径：${pwd}"

if [ -n "$1" ];then
    new_version="$1"
    old_version=`cat ${pwd}/../docs/version.txt`
    echo "$old_version 替换为新版本 $new_version"
else
    # 参数错误，退出
    echo "ERROR: 请指定新版本！"
    exit
fi

if [ ! -n "$old_version" ]; then
    echo "ERROR: 旧版本不存在，请确认 /docs/version.txt 中信息正确"
    exit
fi

# 替换所有模块pom.xml中的版本
mvn versions:set -DnewVersion=$1

# 替换 jpom.io 主页版本号
sed -i "s/${old_version}/${new_version}/g" $pwd/../docs/js/version.js
# 替换远程更新包的版本号
sed -i "s/${old_version}/${new_version}/g" $pwd/../docs/version.json
# 替换 docker 中的版本
sed -i "s/${old_version}/${new_version}/g" $pwd/../.env

# 保留新版本号
echo "$new_version" > $pwd/../docs/version.txt
