#!/bin/bash

# description: Auto-starts boot
Tag="$2"
MainClass="$3"
Lib="$4"
Log="$5"
LogBack=$Log"/../log/"
WebClose="$6"
JVM="$7"
ARGS=""

for ((i=8; i<=$#; i++))
do
    if [ "$ARGS" != "" ]; then
       ARGS=${ARGS}" "   
    fi
    ARGS=${ARGS}${!i}
done

RETVAL="0"
# See how we were called.
start()
{
    echo $Log

    if [ ! -f $Log ]; then
        if [ ! -d $LogBack ];then
            mkdir $LogBack
        fi
        cur_dateTime="`date +%Y-%m-%d_%H:%M:%S`.log"
        mv $Log  $LogBack$cur_dateTime
        echo "mv to $LogBack$cur_dateTime"
        touch $Log
    fi
    echo "$JVM"
    echo "$ARGS"
    nohup java $JVM -Dappliction=$Tag -Djava.ext.dirs=$Lib":${JAVA_HOME}/jre/lib/ext" $MainClass $ARGS > $Log 2>&1 & sleep 1s & status
}

stop()
{
    pid=$(ps -ef | grep "Dappliction=$Tag" | grep -v 'grep' | awk '{printf $2 " "}')
    if [ "$pid" != "" ]; then

       if [ "$WebClose" != "no" ]; then
         echo $WebClose
         wget "$WebClose"
       fi

       pid=$(ps -ef | grep "Dappliction=$Tag" | grep -v 'grep' | awk '{printf $2 " "}')
       if [ "$pid" != "" ]; then
          kill -9 "$pid"
       fi
    fi
    status
}

status()
{
    pid=$(ps -ef | grep "Dappliction=$Tag" | grep -v 'grep' | awk '{printf $2 " "}')
    
    if [ "$pid" != "" ]; then
        echo "running:$pid"
    else
        echo "stopped"
    fi
}



usage()
{
   echo "Usage: $0 {start|stop|restart|status} tag mainclass lib log WebClose JVM args"
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
