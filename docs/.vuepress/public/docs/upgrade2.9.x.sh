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

case "$(uname)" in
Linux)
	bin_abs_path=$(readlink -f $(dirname $0))
	;;
*)
	bin_abs_path=$(
		cd $(dirname $0)
		pwd
	)
	;;
esac

#1. 判断版本
#2. 判断文件夹是否正常
#3. 备份之前的文件夹
#4. 下载新的包
#5. 移动配置文件到新目录
#6. 启动

type="$1"

function check_jar_version() {
	dir_array=(lib log data)
	for element in "${dir_array[@]}"; do
		if [ ! -d "$bin_abs_path/$element" ]; then
			echo "$element directory not found,the current directory may not be the jpom directory" 2>&2
			exit 2
		fi
	done
	dir=$1
	check=$2
	if [ ! -d "$dir" ]; then
		echo "lib directory not found" 2>&2
		exit 2
	fi
	jar_name=$(ls -t "${dir}" | grep '.jar$' | head -1)
	if [ "$jar_name" == "" ]; then
		echo "No $type program found to run jar" 2>&2
		exit 2
	fi
	jar_name=$(echo "$jar_name" | grep -E "$check")
	if [ "$jar_name" == "" ]; then
		echo "No matching version counterparts found,The $type version must be 2.9.x" 2>&2
		exit 2
	fi
}

function download() {
	if [ ! -f "${type}.tar.gz" ]; then
		echo "下载新的安装包"
		# 获取最新的版本号
		url_type=$(echo "${type}" | tr 'A-Z' 'a-z')
		versions=$(curl -LfsS https://jpom.top/docs/versions.release.tag)
		download_url="https://d.jpom.download/release/${versions}/${url_type}-${versions}-release.tar.gz"
		wget -O "${type}.tar.gz" "${download_url}"
	else
		echo "解压离线安装包"
	fi
	tar -zxf "${type}.tar.gz" -C "${bin_abs_path}"
	# 删除安装包
	rm -f "${type}.tar.gz"
}

function upgrade_server() {
	echo "开始初始检查 $type"
	check_jar_version "$bin_abs_path/lib" "Server-2.9|server-2.9"
	dir_array=(Server.sh extConfig.yml)
	for element in "${dir_array[@]}"; do
		if [ ! -f "$bin_abs_path/$element" ]; then
			echo "$element directory not found,the current directory may not be the jpom $type directory" 2>&2
			exit 1
		fi
	done
	echo "开始停止程序"
	bash "$bin_abs_path/Server.sh" stop
	echo "开始备份相关文件"
	mkdir -p "$bin_abs_path"/upgrade_backup
	mv "$bin_abs_path"/log "$bin_abs_path"/upgrade_backup/log/
	mv "$bin_abs_path"/lib "$bin_abs_path"/upgrade_backup/lib/
	if [ -f "$bin_abs_path/server.log" ]; then
		mv "$bin_abs_path"/server.log "$bin_abs_path"/upgrade_backup/server.log
	fi
	if [ -f "$bin_abs_path/Server.bat" ]; then
		mv "$bin_abs_path"/Server.bat "$bin_abs_path"/upgrade_backup/Server.bat
	fi
	mv "$bin_abs_path/Server.sh" "$bin_abs_path/upgrade_backup/Server.sh"
	download
	mv "$bin_abs_path/extConfig.yml" "$bin_abs_path/conf/extConfig.yml"
	# 删除自己
	rm -f "$bin_abs_path/$0"
	echo "开始启动新的程序"
	# 启动
	bash "$bin_abs_path/bin/Server.sh" start
}

function upgrade_agent() {
	echo "开始初始检查 $type"
	check_jar_version "$bin_abs_path/lib" "agent-2.9|Agent-2.9"
	dir_array=(Agent.sh extConfig.yml)
	for element in "${dir_array[@]}"; do
		if [ ! -f "$bin_abs_path/$element" ]; then
			echo "$element directory not found,the current directory may not be the jpom directory" 2>&2
			exit 1
		fi
	done
	echo "开始停止程序"
	bash "$bin_abs_path/Agent.sh" stop
	echo "开始备份相关文件"
	mkdir -p "$bin_abs_path"/upgrade_backup
	mv "$bin_abs_path"/log "$bin_abs_path"/upgrade_backup/log/
	mv "$bin_abs_path"/lib "$bin_abs_path"/upgrade_backup/lib/
	if [ -f "$bin_abs_path/agent.log" ]; then
		mv "$bin_abs_path"/agent.log "$bin_abs_path"/upgrade_backup/agent.log
	fi
	if [ -f "$bin_abs_path/Agent.bat" ]; then
		mv "$bin_abs_path"/Agent.bat "$bin_abs_path"/upgrade_backup/Agent.bat
	fi
	mv "$bin_abs_path/Agent.sh" "$bin_abs_path/upgrade_backup/Agent.sh"
	echo "下载新的安装包"
	download
	mv "$bin_abs_path/extConfig.yml" "$bin_abs_path/conf/extConfig.yml"
	# 删除自己
	rm -f "$bin_abs_path/$0"
	echo "开始启动新的程序"
	# 启动
	bash "$bin_abs_path/bin/Agent.sh" start
}

function usage() {
	echo "Usage: $0 {agent|server}"
	RETVAL="2"
}

# See how we were called.
RETVAL="0"
case "$1" in
agent)
	upgrade_agent
	;;
server)
	upgrade_server
	;;
*)
	usage
	;;
esac

exit $RETVAL
