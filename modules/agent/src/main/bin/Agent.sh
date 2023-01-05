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

# description: Auto-starts jpom agent

case "$(uname)" in
Linux)
	bin_abs_path=$(readlink -f "$(dirname "$0")")
	;;
*)
	bin_abs_path=$(
		cd "$(dirname "$0")" || exit
		pwd
	)
	;;
esac

command_exists() {
	command -v "$@" >/dev/null 2>&1
}

base=${bin_abs_path}/..

conf_path="${base}/conf"
Lib="${base}/lib/"
LogPath="${base}/logs/"
tmpdir="${base}/tmp/"
Log="${LogPath}/stdout.log"
logback_configurationFile="${conf_path}/logback.xml"
application_conf="${conf_path}/application.yml"
pidfile="$base/bin/agent.pid"

export JPOM_LOG=${LogPath}

PID_TAG="JPOM_AGENT_APPLICATION"
agent_log="${LogPath}/agent.log"

## set java path
if [ -z "$JAVA" ]; then
	JAVA=$(which java)
fi
if [ -z "$JAVA" ]; then
	if command_exists java; then
		JAVA="java"
	fi
fi
if [ -z "$JAVA" ]; then
	echo "Cannot find a Java JDK. Please set either set JAVA or put java (>=1.8) in your PATH." 2>&2
	exit 1
fi

JavaVersion=$($JAVA -version 2>&1 | awk 'NR==1{ gsub(/"/,""); print $3 }' | awk -F '.' '{print $1}')
Java64Str=$($JAVA -version 2>&1 | grep -E '64-bit|64-Bit')
JAVA_OPTS="$JAVA_OPTS -Xss256k -XX:+AggressiveOpts -XX:-UseBiasedLocking -XX:-OmitStackTraceInFastThrow -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$LogPath"

if [ "${JavaVersion}" -ge 11 ]; then
	JAVA_OPTS="$JAVA_OPTS"
else
	JAVA_OPTS="$JAVA_OPTS -XX:+UseFastAccessorMethods -XX:+PrintAdaptiveSizePolicy -XX:+PrintTenuringDistribution"
fi

#-Xms500m -Xmx1024m
if [[ -z "${USR_JVM_SIZE}" ]]; then
	USR_JVM_SIZE="-Xms200m -Xmx600m"
fi

if [ -n "$Java64Str" ]; then
	# For G1
	JAVA_OPTS="-server ${USR_JVM_SIZE} -XX:+UseG1GC -XX:MaxGCPauseMillis=250 -XX:+UseGCOverheadLimit -XX:+ExplicitGCInvokesConcurrent $JAVA_OPTS"
else
	JAVA_OPTS="-server ${USR_JVM_SIZE} -XX:NewSize=256m -XX:MaxNewSize=256m -XX:MaxPermSize=128m $JAVA_OPTS"
fi
JAVA_OPTS="$JAVA_OPTS -Djava.awt.headless=true -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8"
JAVA_OPTS="$JAVA_OPTS -Dlogging.config=$logback_configurationFile -Dspring.config.location=$application_conf"
JAVA_OPTS="$JAVA_OPTS -Djava.io.tmpdir=$tmpdir"

if [[ -z "${RUN_ARG}" ]]; then
	MAIN_ARGS="$*"
else
	# docker run
	MAIN_ARGS="${RUN_ARG}"
fi

# mode -s -9
mode="$2"

RUN_JAR=""

function checkConfig() {
	if [ ! -d "$LogPath" ]; then
		mkdir -p "$LogPath"
	fi
	if [[ ! -f "$logback_configurationFile" ]] || [[ ! -f "$application_conf" ]]; then
		echo "Cannot find $application_conf or $logback_configurationFile"
		exit 1
	fi

	if [[ -z "${RUN_JAR}" ]]; then
		if [ -f "$Lib/run.bin" ]; then
			RUN_JAR=$(cat "$Lib/run.bin")
			if [ ! -f "$Lib/$RUN_JAR" ]; then
				echo "Cannot find $Lib/$RUN_JAR jar" 2>&2
				exit 1
			fi
			echo "specify running：${RUN_JAR}"
		else
			RUN_JAR=$(find "${Lib}" -type f -name "*.jar" -exec ls -t {} + | head -1 | sed 's#.*/##')
			# error
			if [[ -z "${RUN_JAR}" ]]; then
				echo "Jar not found"
				exit 2
			fi
			echo "automatic running：${RUN_JAR}"
		fi
	fi
	mkdir -p "$tmpdir"
}

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
			JAVA_PID=$(ps -C java -f --width 1000 | grep "$PID_TAG" | grep -v grep | awk '{print $2}')
		else
			JAVA_PID=$(ps aux | grep "$PID_TAG" | grep -v grep | awk '{print $2}')
		fi
	fi
	echo "$JAVA_PID"
}

# See how we were called.
function start() {
	echo $PID_TAG
	# check running
	pid=$(getPid)
	#echo "$pid"
	if [ "$pid" != "" ]; then
		echo "Running, please do not run repeatedly:$pid"
		exit 0
	fi
	checkConfig

	if [ ! -f "$agent_log" ]; then
		touch "$agent_log"
	fi
	# start
	command="${JAVA} -Djpom.application.tag=${PID_TAG} ${JAVA_OPTS} -jar ${Lib}${RUN_JAR} ${MAIN_ARGS}"
	echo "$command" >"$Log"
	eval "nohup $command >>$Log 2>&1 &"
	echo $! >"$pidfile"

	pid=$(cat "$pidfile")

	if [ "${mode}" == "-s" ] || [ "${mode}" == "upgrade" ]; then
		echo "silence auto exit 0,${pid}"
		exit 0
	fi
	tail -fn 0 --pid="$pid" "$agent_log"
}

function stop() {
	pid=$(getPid)
	killMode=""
	if [ "${mode}" == "-s" ] || [ "${mode}" == "upgrade" ]; then
		#	Compatible with online upgrade ./Agent.sh restart upgrade or ./Agent.sh restart -s
		killMode=""
	else
		killMode=${mode}
	fi
	if [ "$pid" != "" ]; then
		echo -n "jpom agent ( pid $pid) is running"
		echo
		echo -n $"Shutting down (kill $killMode $pid) jpom server: "
		if [ "$killMode" == "" ]; then
			kill "$pid"
		else
			kill "$killMode" "$pid"
		fi
		LOOPS=0
		while (true); do
			pid=$(getPid)
			if [ "$pid" == "" ]; then
				echo "Stop and end, in $LOOPS seconds"
				break
			fi
			((LOOPS++)) || true
			sleep 1
		done
	else
		echo "jpom agent is stopped"
	fi
	eval "$(rm -f "$pidfile")"
}

function status() {
	pid=$(getPid)
	#echo "$pid"
	if [ "$pid" != "" ]; then
		echo "jpom agent running:$pid"
	else
		echo "jpom agent is stopped"
	fi
}

function usage() {
	echo "Usage: $0 {start|stop|restart|status}" 2>&2
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
*)
	usage
	;;
esac

exit $RETVAL
