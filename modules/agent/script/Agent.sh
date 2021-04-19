#!/bin/bash
# The MIT License (MIT)
#
# Copyright (c) 2019 码之科技工作室
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
# ssh 支持读取环境变量
if [ -f ~/.bashrc ]; then
    . ~/.bashrc
fi
if [ -f /etc/profile ]; then
    . /etc/profile
fi
if [ -f /etc/bashrc ]; then
    . /etc/bashrc
fi
Tag="KeepBx-Agent-System-JpomAgentApplication"
# 自动获取当前路径
Path=$(
  cd $(dirname $0)
  pwd
)"/"
Lib="${Path}lib/"
RUNJAR=""
Log="${Path}agent.log"
LogBack="${Path}log/"
JVM="-server "
# 修改项目端口号 日志路径
ARGS="--jpom.applicationTag=${Tag} --spring.profiles.active=pro --server.port=2123  --jpom.log=${Path}log"

echo ${Tag}
echo ${Path}
RETVAL="0"
# 升级执行命令标识
upgrade="$2"

# now set the path to java
if [[ -x "${JAVA_HOME}/bin/java" ]]; then
  JAVA="${JAVA_HOME}/bin/java"
  NOW_JAVA_HOME="${JAVA_HOME}"
else
  set +e
  JAVA=$(which java)
  NOW_JAVA_HOME="${JAVA}/../"
  set -e
fi

if [[ ! -x "$JAVA" ]]; then
  echo "没有找到JAVA 文件,请配置【JAVA_HOME】环境变量"
  exit 1
fi

# 启动程序
function start() {
  pid=$(getPid)
  if [[ "$pid" != "" ]]; then
    echo "程序正在运行中：${pid}"
    exit 2
  fi
  echo ${Log}
  # 备份日志
  if [[ -f ${Log} ]]; then
    if [[ ! -d ${LogBack} ]]; then
      mkdir ${LogBack}
    fi
    cur_dateTime="$(date +%Y-%m-%d_%H:%M:%S).log"
    mv ${Log} ${LogBack}${cur_dateTime}
    echo "mv to $LogBack$cur_dateTime"
    touch ${Log}
  fi
  # jar
  if [[ -z "${RUNJAR}" ]]; then
    RUNJAR=$(listDir ${Lib})
    echo "自动运行：${RUNJAR}"
  fi
  # error
  if [[ -z "${RUNJAR}" ]]; then
    echo "没有找到jar"
    exit 2
  fi
  nohup ${JAVA} ${JVM} -Xbootclasspath/a:${NOW_JAVA_HOME}/lib/tools.jar -jar ${Lib}${RUNJAR} -Dapplication=${Tag} -Dbasedir=${Path} ${ARGS} >>${Log} 2>&1 &
  # 升级不执行查看日志
  if [[ ${upgrade} == "upgrade" ]]; then
    exit 0
  fi
  if [[ -f ${Log} ]]; then
    tail -f ${Log}
  else
    sleep 3
    if [[ -f ${Log} ]]; then
      tail -f ${Log}
    else
      echo "还没有生成日志文件:${Log}"
    fi
  fi
}

# 找出第一个jar包
function listDir() {
  ALL=""
  for file in "$1"/*.jar; do
    if [[ -f "${file}" ]]; then
      #得到文件的完整的目录
      ALL="${file}"
      break
    fi
  done
  echo ${ALL##*/}
}

# 停止程序
function stop() {
  pid=$(getPid)
  if [[ "$pid" != "" ]]; then
    echo -n "boot ( pid $pid) is running"
    echo
    echo -n $"Shutting down boot: wait"
    kill $(pgrep -f ${Tag}) 2>/dev/null
    sleep 3
    pid=$(getPid)
    if [[ "$pid" != "" ]]; then
      echo "kill boot process"
      kill -9 "$pid"
    fi
  else
    echo "boot is stopped"
  fi

  status
}

# 获取程序状态
function status() {
  pid=$(getPid)
  #echo "$pid"
  if [[ "$pid" != "" ]]; then
    echo "boot is running,pid is $pid"
  else
    echo "boot is stopped"
  fi
}

function getPid() {
  pid=$(ps -ef | grep -v 'grep' | egrep ${Tag} | awk '{printf $2 " "}')
  echo ${pid}
}

# 提示使用语法
function usage() {
  echo "Usage: $0 {start|stop|restart|status|create}"
  RETVAL="2"
}

# 创建自启动服务文件
function create() {
	yum install -y wget && wget -O jpom-agent https://dromara.gitee.io/jpom/docs/jpom-service.sh
	#判断当前脚本是否为绝对路径，匹配以/开头下的所有
	if [[ $0 =~ ^\/.* ]]
    then
      selfpath=$0
    else
      selfpath=$(pwd)/$0
    fi
    #获取文件的真实路径
    selfpath=`readlink -f $selfpath`
    # 替换路径
    sed -i "s|JPOM_RUN_PATH|${selfpath}|g" jpom-agent
    echo 'create jpom-agent file done'
    mv -f jpom-agent /etc/init.d/jpom-agent
    chmod +x /etc/init.d/jpom-agent
    chkconfig --add jpom-agent
    echo 'create jpom-agent success'
}

# See how we were called.
RETVAL="0"
case "$1" in
start)
  start
  ;;
stop)
  stop
  ;;
restart)
  stop
  start
  ;;
status)
  status
  ;;
create)
  create
  ;;
*)
  usage
  ;;
esac

exit $RETVAL
