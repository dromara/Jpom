#
# Copyright (c) 2019 Code Technology Studio
# Jpom is licensed under Mulan PSL v2.
# You can use this software according to the terms and conditions of the Mulan PSL v2.
# You may obtain a copy of Mulan PSL v2 at:
# 			http://license.coscl.org.cn/MulanPSL2
# THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
# See the Mulan PSL v2 for more details.
#

function getPid() {
  cygwin=false
  linux=false
  case "$(uname)" in
  CYGWIN*)
    cygwin=true
    ;;
  Linux*)
    linux=true
    ;;
  esac
  if $cygwin; then
    JAVA_CMD="$JAVA_HOME\bin\java"
    JAVA_CMD=$(cygpath --path --unix "$JAVA_CMD")
    JAVA_PID=$(ps | grep "$JAVA_CMD" | awk '{print $1}')
  else
    if $linux; then
      JAVA_PID=$(ps -C java -f --width 1000 | grep "$1" | grep -v grep | awk '{print $2}')
    else
      JAVA_PID=$(ps aux | grep "$1" | grep -v grep | awk '{print $2}')
    fi
  fi
  echo "$JAVA_PID"
}
if [ ! -f "/usr/bin/lsb_release" ]; then
  echo "os name:$(cat /etc/redhat-release)"
else
  echo "os name:$(/usr/bin/lsb_release -a | grep Description | awk -F : '{print $2}' | sed 's/^[ \t]*//g')"
fi

echo "os version:$(uname -r)"
echo "hostname:$(hostname)"
# 启动时间
echo "uptime:$(uptime -s)"
# 系统负载
uptime | sed 's/,/ /g' | awk '{for(i=NF-2;i<=NF;i++) print "load average:"$i }'
# cpu 型号
LANG=C lscpu | awk -F: '/Model name/ {print$2}' | awk '$1=$1' | awk '{print "model name:"$0}'
# cpu 数量
awk '/processor/{core++} END{print "cpu core:"core}' /proc/cpuinfo
# 内存信息
free -k | grep -iv swap | awk -F ':' '{print $NF}' | awk -F ' ' '{for(i=1;i<=NF;i++) a[i,NR]=$i}END{for(i=1;i<=NF;i++) { printf "memory "; for(j=1;j<=NR;j++) printf a[i,j] ":";print ""}}'
# cpu 利用率
export TERM=xterm
top -b -n 1 -d.2 | grep -i 'cpu' | awk '{for(i=NR;i<=1;i++)print "cpu usage:"$2 }'
# disk
df -k | awk 'NR>1' | awk '/^\/dev/{print "disk info:"$1":"$2":"$3}'
# jpom 相关
jpom_agent_pid=$(getPid "${JPOM_AGENT_PID_TAG}")
echo "jpom agent pid:$jpom_agent_pid"
echo "java version:$(command -v java)"

docker_path=$(command -v docker)
if [[ $docker_path != "" ]]; then
  echo "docker path:$docker_path"
  echo "docker version:$(docker -v)"
fi
