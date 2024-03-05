#!/bin/bash
#
# Copyright (c) 2019 Of Him Code Technology Studio
# Jpom is licensed under Mulan PSL v2.
# You can use this software according to the terms and conditions of the Mulan PSL v2.
# You may obtain a copy of Mulan PSL v2 at:
# 			http://license.coscl.org.cn/MulanPSL2
# THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
# See the Mulan PSL v2 for more details.
#

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

base=${bin_abs_path}/..
pidfile="$base/bin/server.pid"
LogPath="${base}/logs/"
stdout_log="${LogPath}/stdout.log"
LOOPS=0

# wait_term_pid "${PIDFILE}".
#   monitor process by pidfile && wait TERM/INT signal.
#   if the process disappeared, return 1, means exit with ERROR.
#   if TERM or INT signal received, return 0, means OK to exit.
function wait_term_pid() {
	local PIDFILE PID do_run error
	PIDFILE="${1?}"
	PID="$(cat "${PIDFILE}")"
	do_run=true
	error=0
	trap "do_run=false" TERM INT
	while "${do_run}"; do
		PID="$(cat "${PIDFILE}")"
		if ! ps -p "${PID}" >/dev/null 2>&1; then
			do_run=false
			error=1
		else
			# rest loops
			LOOPS=0
			tail_log
		fi
	done
	trap - TERM INT
	return "${error}"
}

function tail_log() {
	if [ -f "$stdout_log" ]; then
		PID="$(cat "${pidfile}")"
		tail -fn 0 --pid="$PID" "$stdout_log"
	else
		echo "stdout_log not found $stdout_log"
	fi
}

function check_conf() {

	conf_path=$base/conf
	conf_default_path=$base/conf_default
	conf_array=(application.yml logback.xml)

	if [[ ! -d "$conf_path" ]]; then
		mkdir -p "${conf_path}"
	fi
	for element in "${conf_array[@]}"; do
		if [[ ! -f "$conf_path/$element" ]]; then
			if [[ ! -f "$conf_default_path/$element" ]]; then
				echo "Cannot find $conf_path/$element && not found default conf in : $conf_default_path/$element" 2>&2
				exit 1
			else
				echo "copy default conf to $conf_path/$element"
				cp -r "$conf_default_path/$element" "$conf_path/$element"
			fi
		fi
	done
}

check_conf

bash "$bin_abs_path/Server.sh" start -s

while (true); do
	if [ -f "$pidfile" ]; then
		tail_log
		wait_term_pid "$pidfile"
	else
		echo "pidfile not found $pidfile"
	fi

	if [ $LOOPS -gt 120 ]; then
		echo "wait timeout $LOOPS" 2>&2
		break
	fi
	LOOPS=$((LOOPS + 1))
	sleep 1
done

echo "edit"

exit 1
