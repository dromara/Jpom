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

# description: manage jpom agent Service

function absPath() {
	dir="$1"
	case "$(uname)" in
	Linux)
		abs_path=$(readlink -f "$dir")
		;;
	*)
		abs_path=$(
			cd "$dir" || exit
			pwd
		)
		;;
	esac
	#
	echo "$abs_path"
}

command_exists() {
	command -v "$@" >/dev/null 2>&1
}

binAbsPath=$(absPath "$(dirname "$0")")
serviceName="jpom-agent.service"
serviceFile="/etc/systemd/system/$serviceName"
binAbsName=$(absPath "$binAbsPath/Agent.sh")
pidfile="$binAbsPath/agent.pid"

#
user="$(id -un 2>/dev/null || true)"
user_group="$(id -gn 2>/dev/null || true)"

sh_c='sh -c'
exec_user=""
if [ "$user" != 'root' ]; then
	if command_exists sudo; then
		sh_c='sudo -E sh -c'
	elif command_exists su; then
		sh_c='su -c'
	else
		cat >&2 <<-EOF
			Error: this installer needs the ability to run commands as root.
			We are unable to find either "sudo" or "su" available to make this happen.
		EOF
		exit 1
	fi
	exec_user="$user"
fi

function install() {

	if [ -f "$serviceFile" ]; then
		echo "service file already exists" 2>&2
		exit 2
	fi
	if [ ! -f "$binAbsName" ]; then
		echo "$binAbsName not found" 2>&2
		exit 2
	fi
	if [ -z "$JAVA_HOME" ]; then
		echo "JAVA_HOME variable not found" 2>&2
		exit 2
	fi
	if [ -z "$CLASSPATH" ]; then
		echo "CLASSPATH variable not found" 2>&2
		exit 2
	fi

	$sh_c "cat >$serviceFile" <<EOF
[Unit]
Description=jpom $serviceName
After=network.target
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
Environment="JAVA_HOME=$JAVA_HOME"
Environment="PATH=$PATH"
Environment="CLASSPATH=$CLASSPATH"
Environment="JPOM_SERVICE=$serviceName"
Environment="HOME=$HOME"
ExecStart=/bin/bash $binAbsName start -s
ExecStop=/bin/bash $binAbsName stop
ExecReload=/bin/kill -s HUP \$MAINPID
User=$exec_user
Group=$user_group
PIDFile=$pidfile

[Install]
WantedBy=multi-user.target
EOF
	if [ ! -f "$serviceFile" ]; then
		cat >&2 <<-EOF
			ERROR: $serviceName write failed Installing the service requires the ability to run commands as root.
		EOF
		exit 1
	fi

	echo "$serviceName write success :$serviceFile"

	$sh_c 'systemctl daemon-reload'

	cat >&2 <<-EOF
		INFO: You can execute the following commands to manage $serviceName.
		INFO: systemctl start $serviceName　(Start the service )
		INFO: systemctl stop $serviceName　(Stop the service)
		INFO: systemctl enable $serviceName (Set up autostart)
		INFO: systemctl disable $serviceName (stop autostart)
		INFO: systemctl status $serviceName (View the current status of the service)
		INFO: systemctl restart $serviceName　(Restart the service）
	EOF
}

function uninstall() {
	if [ -f "$serviceFile" ]; then
		$sh_c "systemctl disable $serviceName"
		$sh_c "systemctl stop $serviceName"
		$sh_c "rm -f $serviceFile"
		if [ -f "$serviceFile" ]; then
			cat >&2 <<-EOF
				ERROR: $serviceName write uninstall .
			EOF
			exit 1
		fi
		echo "$serviceName uninstalled successfully"
		$sh_c 'systemctl daemon-reload'
	else
		echo "$serviceFile not found"
	fi
}

function enable() {
	if [ -f "$serviceFile" ]; then
		$sh_c "systemctl enable $serviceName"
	else
		echo "$serviceFile not found" 2>&2
		echo "Usage: $0 install" 2>&2
	fi
}

function action() {
	case "$1" in
	install)
		install
		;;
	uninstall)
		uninstall
		;;
	reinstall)
		uninstall
		echo "--------------------------------------"
		install
		;;
	enable)
		enable
		;;
	*)
		echo "Usage: $0 {install|uninstall|reinstall|enable}" 2>&2
		exit 1
		;;
	esac

}

if [ -z "$1" ]; then
	echo "Usage: $0 {install|uninstall|reinstall|enable}" 2>&2
	exit 1
fi

for i in "$@"; do
	action "$i"
done
