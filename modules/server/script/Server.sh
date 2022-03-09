#!/bin/bash
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

if [ -f /etc/profile ]; then
    . /etc/profile
fi
if [ -f /etc/bashrc ]; then
    . /etc/bashrc
fi
if [ -f ~/.bash_profile ]; then
    . ~/.bash_profile
fi
if [ -f ~/.bashrc ]; then
    . ~/.bashrc
fi
# Please do not modify the value of the tag attribute, the modification will affect the stop and view status of the program
Tag="KeepBx-System-JpomServerApplication"
# Obtain the current path automatically
Path=$(cd `dirname $0`; pwd)"/"
Lib="${Path}lib/"
# Online upgrade will automatically modify this attribute
RUNJAR=""
Log="${Path}server.log"
LogBack="${Path}log/"
JVM="-server -XX:+UseG1GC -Xms254m -Xmx1024m"
# Modify project port number Log path
ARGS="--jpom.applicationTag=${Tag} --spring.profiles.active=pro --server.port=2122  --jpom.log=${Path}log $@"

echo ${Tag}
echo ${Path}
RETVAL="0"
# upgrade tag
upgrade="$2"

# now set the path to java
if [[ -x "${JAVA_HOME}/bin/java" ]]; then
  JAVA="${JAVA_HOME}/bin/java"
  NOW_JAVA_HOME="${JAVA_HOME}"
else
  set +e
  JAVA=`which java`
  NOW_JAVA_HOME="${JAVA}/../"
  set -e
fi

if [[ ! -x "$JAVA" ]]; then
  echo "JAVA file is not found, please configure [JAVA_HOME] environment variable"
  exit 1
fi

# start
function start() {
    pid=`getPid`
	if [[ "$pid" != "" ]]; then
	   echo "Program is running：${pid}"
	   exit 2
	fi
    echo  ${Log}
    # Backup log
    if [[ -f ${Log} ]]; then
		if [[ ! -d ${LogBack} ]];then
			mkdir ${LogBack}
		fi
		cur_dateTime="`date +%Y-%m-%d_%H:%M:%S`.log"
		mv ${Log}  ${LogBack}${cur_dateTime}
		echo "mv to $LogBack$cur_dateTime"
		touch ${Log}
	fi
	# jar
	if [[ -z "${RUNJAR}" ]] ; then
		RUNJAR=`listDir ${Lib}`
		echo "automatic running：${RUNJAR}"
	fi
    # error
	if [[ -z "${RUNJAR}" ]] ; then
        echo "Jar not found"
        exit 2
	fi

    nohup ${JAVA}  ${JVM} -jar ${Lib}${RUNJAR} -Dapplication=${Tag} -Dbasedir=${Path} ${ARGS}  >> ${Log} 2>&1 &
    # The upgrade is not executed. View the log
    if [[ ${upgrade} == "upgrade" ]] ; then
        exit 0
    fi
    if [[ -f ${Log} ]]; then
        tail -f ${Log}
    else
        sleep 3
        if [[  -f ${Log} ]]; then
           tail -f ${Log}
        else
           echo "No log files have been generated:${Log}"
        fi
    fi
}

# Find the first jar package
function listDir()
{
    ALL=""
	for file in `ls $1`
	do
		if [[ -f "${1}/${file}" ]] &&  [[ "${file##*.}"x = "jar"x ]] ; then
			# Get the complete list of files
			ALL="${file}"
			break
		fi
	done
	echo ${ALL}
}

# stop
function stop() {
	pid=`getPid`
	if [[ "$pid" != "" ]]; then
        echo -n "boot ( pid $pid) is running"
        echo
        echo -n $"Shutting down boot: wait"
        kill $(pgrep -f ${Tag}) 2>/dev/null
        sleep 3
		pid=`getPid`
		if [[ "$pid" != "" ]]; then
			echo "kill boot process"
			kill -9 "$pid"
		fi
    else
        echo "boot is stopped"
    fi

	status
}

# status
function status()
{
	pid=`getPid`
	#echo "$pid"
	if [[ "$pid" != "" ]]; then
		echo "boot is running,pid is $pid"
	else
		echo "boot is stopped"
	fi
}

function getPid(){
    pid=$(ps -ef | grep -v 'grep' | egrep ${Tag}| awk '{printf $2 " "}')
    echo ${pid}
}

# usage
function usage()
{
   echo "Usage: $0 {start|stop|restart|status|create}"
   RETVAL="2"
}


# Create a self-starting service file
function create() {
	yum install -y wget && wget -O jpom-server https://dromara.gitee.io/jpom/docs/jpom-service.sh
	# Determine whether the current script is an absolute path, matching all beginning with /
	if [[ $0 =~ ^\/.* ]]
    then
      selfpath=$0
    else
      selfpath=$(pwd)/$0
    fi
    # Get the complete list of files
    selfpath=`readlink -f $selfpath`
    # Replacement path
    sed -i "s|JPOM_RUN_PATH|${selfpath}|g" jpom-server
    echo 'create jpom-server file done'
    mv -f jpom-server /etc/init.d/jpom-server
    chmod +x /etc/init.d/jpom-server
    chkconfig --add jpom-server
    echo 'create jpom-server success'
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
