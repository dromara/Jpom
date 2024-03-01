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

tag="$2"
echo "当前路径：${current_path} 脚本路径：${bin_abs_path} $tag"

if [ -n "$1" ]; then
	new_version="$1"
	old_version=$(cat "${base}/script/tag.$tag.txt")
	echo "$old_version 替换为新版本 $new_version"
else
	# 参数错误，退出
	echo "ERROR: 请指定新版本！" 2>&2
	exit 1
fi

if [ ! -n "$old_version" ]; then
	echo "ERROR: 旧版本不存在，请确认 /script/tag.$tag.txt 中信息正确" 2>&2
	exit 1
fi

echo "替换配置文件版本号 $new_version"

if [ "$tag" == "release" ]; then
	# 替换 Dockerfile 中的版本
	sed -i.bak "s/${old_version}/${new_version}/g" "$base/modules/server/Dockerfile"
	sed -i.bak "s/${old_version}/${new_version}/g" "$base/modules/agent/Dockerfile"
	sed -i.bak "s/${old_version}/${new_version}/g" "$base/script/docker.sh"
	sed -i.bak "s/${old_version}/${new_version}/g" "$base/modules/server/DockerfileRelease"
	sed -i.bak "s/${old_version}/${new_version}/g" "$base/modules/server/DockerfileReleaseJdk17"
	# vue version
	sed -i.bak "s/${old_version}/${new_version}/g" "$base/web-vue/package.json"
	# 替换 docker 中的版本
	sed -i.bak "s/${old_version}/${new_version}/g" "$base/.env"
	# gitee go
	sed -i.bak "s/${old_version}/${new_version}/g" "$base/.workflow/MasterPipeline.yml"
elif [ "$tag" == "beta" ]; then
	sed -i.bak "s/${old_version}/${new_version}/g" "$base/modules/server/DockerfileBeta"
	sed -i.bak "s/${old_version}/${new_version}/g" "$base/modules/server/DockerfileBetaJdk17"
else
	echo "不支持的模式 $tag" 2>&2
	exit 2
fi

# 替换所有模块pom.xml中的版本
cd "${base}" && mvn -s "$base/script/settings.xml" versions:set -DnewVersion=$new_version

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
