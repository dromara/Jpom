# description: Auto-starts boot
Tag="$2"
MainClass="$3"
Lib="$4"
Log="$5"
Port="$6"
#Token="$7"

RETVAL="0"
# See how we were called.
start() 
{
    if [ ! -f $Log ]; then
        touch $Log
    fi
    nohup java -Dappliction=$Tag -Djava.ext.dirs=$Lib":${JAVA_HOME}/jre/lib/ext" $MainClass > $Log 2>&1 & sleep 1s & status
}

stop() 
{
    pid=$(ps -ef | grep "Dappliction=$Tag" | grep -v 'grep' | awk '{printf $2 " "}')
    if [ "$pid" != "" ]; then

        #wget http://127.0.0.1:$Port/sys/shutdown?token=$Token

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
   echo "Usage: $0 {start|stop|restart|status} tag mainclass lib log port token"
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
