#!/bin/bash

Tag="JpomApplication"
MainClass="cn.jiangzeyin.JpomApplication"
Path="/jpom/"
Lib=${Path}"lib/"
Log=${Path}"run.log"
LogBack=${Path}"log/"
JVM="-server "
# 修改项目端口号 和 数据运行目录
ARGS="--server.port=2122 --jpom.path=/jpom/"

echo ${Tag}
RETVAL="0"

# See how we were called.
function start() {
    echo  ${Log}
    if [[ -f ${Log} ]]; then
		if [[ ! -d ${LogBack} ]];then
			mkdir ${LogBack}
		fi
			cur_dateTime="`date +%Y-%m-%d_%H:%M:%S`.log"
			mv ${Log}  ${LogBack}${cur_dateTime}
			echo "mv to $LogBack$cur_dateTime"
		touch ${Log}
	fi
    nohup java  ${JVM} -Dappliction=$Tag -Djava.ext.dirs=${Lib}":${JAVA_HOME}/jre/lib/ext" ${MainClass} ${ARGS}  > ${Log} 2>&1 &
    tailf ${Log}
}


function stop() {
	pid=$(ps -ef | grep -v 'grep' | egrep $Tag| awk '{printf $2 " "}')
	if [[ "$pid" != "" ]]; then
        echo -n "boot ( pid $pid) is running" 
        echo 
        echo -n $"Shutting down boot: "
		pid=$(ps -ef | grep -v 'grep' | egrep $Tag| awk '{printf $2 " "}')
		if [[ "$pid" != "" ]]; then
			echo "kill boot process"
			kill -9 "$pid"
		fi
        else 
             echo "boot is stopped" 
        fi

	status
}

function status()
{
	pid=$(ps -ef | grep -v 'grep' | egrep ${Tag}| awk '{printf $2 " "}')
	#echo "$pid"
	if [[ "$pid" != "" ]]; then
		echo "boot is running,pid is $pid"
	else
		echo "boot is stopped"
	fi
}



function usage()
{
   echo "Usage: $0 {start|stop|restart|status}"
   RETVAL="2"
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
    reload)
        RETVAL="3"
        ;;
    status)
        status
        ;;
    *)
      usage
      ;;
esac

exit $RETVAL
