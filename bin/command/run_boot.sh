#!/bin/bash

# description: Auto-starts boot
Tag="$2"
MainClass="$3"
Lib="$4"
Log="$5"
WebClose="$6"

#组合
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

JVM=${array[0]}
ARGS=${array[1]}
# 截取
JVM=${JVM:1}
ARGS=${ARGS:1}

# See how we were called.
start()
{
    backupLog $Log;

    echo "JVM:$JVM"
    echo "args:$ARGS"
    nohup java $JVM -Dappliction=$Tag -Djava.ext.dirs=$Lib":${JAVA_HOME}/jre/lib/ext" $MainClass $ARGS > $Log 2>&1 & sleep 1s & status
}

stop()
{
    pid=$(getPid)
    if [ "$pid" != "" ]; then

       if [ "$WebClose" != "no" ]; then
         echo $WebClose
         wget "$WebClose"
       fi

       pid=$(getPid)
       if [ "$pid" != "" ]; then
          kill -9 "$pid"
       fi
    fi
    status
}

# 运行状态
status()
{
    pid=$(getPid)
    
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
   fi
}

getPid()
{
  pid=$(ps -ef | grep "Dappliction=$Tag" | grep -v 'grep' | awk '{printf $2 " "}')
  return pid
}

usage()
{
   echo "Usage: $0 {start|stop|restart|status} tag mainclass lib log WebClose JVM args"
   echo "Usage: $0 {backupLog} log"
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
    *)
      usage
      ;;
esac

exit $RETVAL
