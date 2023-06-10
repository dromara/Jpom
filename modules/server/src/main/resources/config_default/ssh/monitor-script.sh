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
