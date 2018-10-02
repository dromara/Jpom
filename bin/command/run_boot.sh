# description: Auto-starts boot
#!/bin/bash
# 变量
Tag="$2"
WebClose="$3"
MainClass="$4"
Lib="$5"
Log="$6"

#获取jvm 和 args
ARGS=""
for ((i=7; i<=$#; i++))
do
    if [ "$ARGS" != "" ]; then
       ARGS=${ARGS}" "   
    fi
    ARGS=${ARGS}${!i}
done
#拆分
OLD_IFS="$IFS"
IFS="]"
array=($ARGS)
IFS="$OLD_IFS"
#
JVM=${array[0]}
ARGS=${array[1]}
# 截取
JVM=${JVM:1}
ARGS=${ARGS:1}

# 启动程序
start()
{
   pid=`getPid`
   if [ "$pid" != "" ]; then
       echo "Please do not repeat the call"
       status
    else
       backupLog $Log;
       echo "JVM:$JVM"
       echo "args:$ARGS"
       # java run
       nohup java $JVM -Dappliction=$Tag -Djava.ext.dirs=$Lib":${JAVA_HOME}/jre/lib/ext" $MainClass $ARGS > $Log 2>&1 & sleep 1s & status
    fi
}

# 关闭程序
stop()
{
    pid=`getPid`
    if [ "$pid" != ""  ]; then
        # 接口关闭
       if [ "$WebClose" != "no" ]; then
         echo $WebClose
         wget "$WebClose"
       fi
       kill -9 "$pid"
    fi
    status
}

# 运行状态
status()
{
    pid=`getPid`
    if [ "$pid" != "" ]; then
        echo "running:$pid"
    else
        echo "stopped"
    fi
}

#  备份日志
backupLog()
{
   Log=$1
   if [ "$Log" = "" ]; then
     echo "logPath empty"
     return 3;
   fi
   LogBack=$Log"_back/"
   echo $Log
   if [ -f $Log ]; then
        if [ ! -d $LogBack ]; then
            mkdir $LogBack
        fi
        cur_dateTime="`date +%Y-%m-%d_%H:%M:%S`.log"
        mv $Log  $LogBack$cur_dateTime
        echo "mv to $LogBack$cur_dateTime"
        touch $Log
   else
     echo "log file notExits"
   fi
}

#获取进程
getPid()
{
  pid=$(ps -ef | grep "Dappliction=$Tag" | grep -v 'grep' | awk '{printf $2}')
  echo $pid;
}

#提示用法
usage()
{
   echo "Usage: $0 {start|restart} tag WebClose mainclass lib log JVM args"
   echo "Usage: $0 {stop} tag WebClose"
   echo "Usage: $0 {backupLog} log"
   echo "Usage: $0 {pid|status} tag"
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
    status)
        status
        ;;
    backupLog)
        backupLog $2
        ;;
     pid)
        getPid
        ;;
    *)
      usage
      ;;
esac

exit $RETVAL

