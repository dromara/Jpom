#!/bin/bash
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
  # 获取最新的版本号
  url_type=$(echo "${type}" | tr 'A-Z' 'a-z')
  versions=$(curl -LfsS https://jpom.top/docs/versions.tag)
  download_url="https://download.jpom.top/release/${versions}/${url_type}-${versions}-release.tar.gz"
  wget -O "${type}.tar.gz" "${download_url}"
  tar -zxf "${type}.tar.gz" -C "${bin_abs_path}"
  # 删除安装包
  rm -f ${type}.tar.gz
}

function upgrade_server() {
  check_jar_version "$bin_abs_path/lib" "Server-2.9|server-2.9"
  dir_array=(Server.sh extConfig.yml)
  for element in "${dir_array[@]}"; do
    if [ ! -f "$bin_abs_path/$element" ]; then
      echo "$element directory not found,the current directory may not be the jpom $type directory" 2>&2
      exit 1
    fi
  done
  bash "$bin_abs_path/Server.sh" stop
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
  mv "$bin_abs_path/extConfig.yml" "$bin_abs_path/conf/extConfig.yml.old"
  # 删除自己
  rm -f "$bin_abs_path/$0"
  # 启动
  bash "$bin_abs_path/bin/Server.sh" start
}

function upgrade_agent() {
  check_jar_version "$bin_abs_path/lib" "agent-2.9|Agent-2.9"
  dir_array=(Agent.sh extConfig.yml)
  for element in "${dir_array[@]}"; do
    if [ ! -f "$bin_abs_path/$element" ]; then
      echo "$element directory not found,the current directory may not be the jpom directory" 2>&2
      exit 1
    fi
  done
  bash "$bin_abs_path/Agent.sh" stop
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
  download
  mv "$bin_abs_path/extConfig.yml" "$bin_abs_path/conf/extConfig.yml.old"
  # 删除自己
  rm -f "$bin_abs_path/$0"
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
