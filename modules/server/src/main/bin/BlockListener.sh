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

case "$(uname)" in
Linux)
	bin_abs_path=$(readlink -f $(dirname $0))
	;;
*)
	bin_abs_path=$(
		cd $(dirname $0)
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
		tail -f --pid="$PID" "$stdout_log"
	else
		echo stdout_log not found $stdout_log
	fi
}

bash $bin_abs_path/Server.sh start -s

while (true); do
	if [ -f "$pidfile" ]; then
		tail_log
		wait_term_pid "$pidfile"
	else
		echo pidfile not found $pidfile
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
