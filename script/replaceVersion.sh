#!/bin/bash
#
# The MIT License (MIT)
#
# Copyright (c) 2019 Code Technology Studio
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of
# this software and associated documentation files (the "Software"), to deal in
# the Software without restriction, including without limitation the rights to
# use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
# the Software, and to permit persons to whom the Software is furnished to do so,
# subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
# FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
# COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
# IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
# CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#

#-----------------------------------------------------------
# 此脚本用于每次升级时替换相应位置的版本号
#-----------------------------------------------------------

set -o errexit

current_path=$(pwd)
case "$(uname)" in
Linux)
  bin_abs_path=$(readlink -f "$(dirname "$0")")
  ;;
*)
  bin_abs_path=$(
    cd "$(dirname "$0")"
    pwd
  )
  ;;
esac
base=${bin_abs_path}/../

echo "当前路径：${current_path} 脚本路径：${bin_abs_path}"
tag="$2"

if [ -n "$1" ]; then
  new_version="$1"
  old_version=$(cat "${base}/script/tag.$tag.txt")
  echo "$old_version 替换为新版本 $new_version"
else
  # 参数错误，退出
  echo "ERROR: 请指定新版本！"
  exit
fi

if [ ! -n "$old_version" ]; then
  echo "ERROR: 旧版本不存在，请确认 /script/tag.$tag.txt 中信息正确"
  exit
fi

echo "替换配置文件版本号 $new_version"

if [ "$tag" == "release" ]; then
  # 替换 Dockerfile 中的版本
  sed -i.bak "s/${old_version}/${new_version}/g" "$base/modules/server/Dockerfile"
  sed -i.bak "s/${old_version}/${new_version}/g" "$base/modules/agent/Dockerfile"
  sed -i.bak "s/${old_version}/${new_version}/g" "$base/script/docker.sh"
  sed -i.bak "s/${old_version}/${new_version}/g" "$base/modules/server/DockerfileRelease"
  # vue version
  sed -i.bak "s/${old_version}/${new_version}/g" "$base/web-vue/package.json"

  # gitee go
  sed -i.bak "s/${old_version}/${new_version}/g" "$base/.workflow/MasterPipeline.yml"
elif [ "$tag" == "beta" ]; then
  sed -i.bak "s/${old_version}/${new_version}/g" "$base/modules/server/DockerfileBeta"
else
  echo "不支持的模式 $tag"
  exit
fi

# 替换所有模块pom.xml中的版本
cd "${base}" && mvn versions:set -DnewVersion=$new_version

# 替换 docker 中的版本
sed -i.bak "s/${old_version}/${new_version}/g" "$base/env-$tag.env"

# logo
cat >"$base/modules/common/src/main/resources/banner.txt" <<EOF
       _
      | |
      | |_ __   ___  _ __ ___
  _   | | '_ \ / _ \| '_ \` _ \\
 | |__| | |_) | (_) | | | | | |
  \____/| .__/ \___/|_| |_| |_|
        | |
        |_|

 ➜ Jpom \﻿ (•◡•) / (v$new_version)

EOF

# 保留新版本号
echo "$new_version" >"${base}/script/tag.$tag.txt"

echo "版本号替换成功 $new_version"
