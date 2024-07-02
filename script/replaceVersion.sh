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
		cd "$(dirname "$0")" || exit
		pwd
	)
	;;
esac

pwd=${bin_abs_path}/../

echo "当前路径：${current_path},脚本路径：${bin_abs_path}"

use_tag=""

if [ "$2" == "release" ]; then
	use_tag="release"
elif [ "$2" == "beta" ]; then
	use_tag="beta"
else
	echo "不支持的模式 $2"
	exit
fi

if [ -n "$1" ]; then
	new_version="$1"
	old_version=$(cat "${pwd}/docs/.vuepress/public/docs/versions.$use_tag.tag")
	echo "$old_version 替换为新版本 $new_version"
else
	# 参数错误，退出
	echo "ERROR: 请指定新版本！"
	exit
fi

if [ -z "$old_version" ]; then
	echo "ERROR: 旧版本不存在，请确认 /docs/.vuepress/public/docs/versions.$use_tag.tag 中信息正确"
	exit
fi

downloads=""

if [ "$2" == "release" ]; then
	# 替换远程更新包的版本号
	sed -i.bak "s/${old_version}/${new_version}/g" "${pwd}/docs/.vuepress/public/docs/versions.json"
	sed -i.bak "s/${old_version}/${new_version}/g" "${pwd}/docs/.vuepress/public/docs/release-versions.json"
	sed -i.bak "s/${old_version}/${new_version}/g" "${pwd}/package.json"
	sed -i.bak "s/${old_version}/${new_version}/g" "${pwd}/docs/.vuepress/public/docs/versions.show"
	sed -i.bak "s/${old_version}/${new_version}/g" "${pwd}/docs/.vuepress/public/assets/js/common.js"
	downloads="01.下载链接.md"
elif [ "$2" == "beta" ]; then
	# 替换远程更新包的版本号
	sed -i.bak "s/${old_version}/${new_version}/g" "${pwd}/docs/.vuepress/public/docs/beta-versions.json"
	downloads="02.下载链接beta.md"
else
	echo "不支持的模式 $2"
	exit
fi

function updateDocUrlItem() {

	mdPath="${pwd}/docs/日志&下载/02.下载链接/${downloads}"

	if [[ $(grep "$1" "${mdPath}") != "" ]]; then
		echo "下载地址已经更新啦"
	else
		cat >"${pwd}/temp-docs.log" <<EOF
## ${1}-${use_tag}

- [jpom-$1.zip](https://d.jpom.download/$use_tag/$1/jpom-$1.zip)
- [server-$1-release.tar.gz](https://d.jpom.download/$use_tag/$1/server-$1-release.tar.gz) | [sha1sum](https://d.jpom.download/$use_tag/$1/server-$1-release.tar.gz.sha1)
- [server-$1-release.zip](https://d.jpom.download/$use_tag/$1/server-$1-release.zip) | [sha1sum](https://d.jpom.download/$use_tag/$1/server-$1-release.zip.sha1)
- [agent-$1-release.tar.gz](https://d.jpom.download/$use_tag/$1/agent-$1-release.tar.gz) | [sha1sum](https://d.jpom.download/$use_tag/$1/agent-$1-release.tar.gz.sha1)
- [agent-$1-release.zip](https://d.jpom.download/$use_tag/$1/agent-$1-release.zip) | [sha1sum](https://d.jpom.download/$use_tag/$1/agent-$1-release.zip.sha1)

--------

EOF
		#		插入第 14 行
		sed -i.bak "14r ${pwd}/temp-docs.log" "${mdPath}"
	fi
}

updateDocUrlItem "$new_version"

# 保留新版本号
echo "$new_version" >"${pwd}/docs/.vuepress/public/docs/versions.$use_tag.tag"
