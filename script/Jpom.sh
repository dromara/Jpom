#!/bin/bash

Tag="KeepBx-System-JpomApplication"
MainClass="cn.keepbx.jpom.JpomApplication"
# 自动获取当前路径
Path=$(cd `dirname $0`; pwd)"/"
Lib="${Path}lib/"
Log="${Path}run.log"
LogBack="${Path}log/"
JVM="-server "
# 修改项目端口号 日志路径
ARGS="--server.port=2122  --jpom.log=${Path}log"

echo ${Tag}
RETVAL="0"

# 启动程序
function start() {
    echo  ${Log}
    # 备份日志
    if [[ -f ${Log} ]]; then
		if [[ ! -d ${LogBack} ]];then
			mkdir ${LogBack}
		fi
			cur_dateTime="`date +%Y-%m-%d_%H:%M:%S`.log"
			mv ${Log}  ${LogBack}${cur_dateTime}
			echo "mv to $LogBack$cur_dateTime"
		touch ${Log}
	fi
	# classPath
    CLASSPATH=`listDir ${Lib}`
    nohup java  ${JVM} -classpath ${CLASSPATH}${JAVA_HOME}/lib/tools.jar -Dappliction=$Tag ${MainClass} ${ARGS}  >> ${Log} 2>&1 &
    tailf ${Log}
}

# 拼接所有文件
function listDir()
{
    ALL=""
	for file in `ls $1`
	do
		#得到文件的完整的目录
        ALL="${ALL}${1}/${file}:"
	done
	echo ${ALL}
}

# 停止程序
function stop() {
	pid=$(ps -ef | grep -v 'grep' | egrep $Tag| awk '{printf $2 " "}')
	if [[ "$pid" != "" ]]; then
        echo -n "boot ( pid $pid) is running" 
        echo 
        echo -n $"Shutting down boot: wait"
        kill $(pgrep -f ${Tag}) 2>/dev/null
        sleep 3
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

# 获取程序状态
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

# 提示使用语法
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
