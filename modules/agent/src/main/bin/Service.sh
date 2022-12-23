#!/bin/bash

# description: manage jpom server Service

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
serviceName="jpom-server.service"
serviceFile="/etc/systemd/system/$serviceName"
binAbsName=$(absPath "$binAbsPath/Server.sh")
pidfile="$binAbsPath/server.pid"

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
		cat >&2 <<-'EOF'
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
Description=jpom server service
After=network.target
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
Environment="JAVA_HOME=$JAVA_HOME"
Environment="PATH=$PATH"
Environment="CLASSPATH=$CLASSPATH"
Environment="JPOM_SERVICE=$serviceName"
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
		cat >&2 <<-'EOF'
			ERROR: $serviceName write failed Installing the service requires the ability to run commands as root.
		EOF
		exit 1
	fi

	echo "$serviceName write success :$serviceFile"

	$sh_c 'systemctl daemon-reload'
	$sh_c "systemctl start $serviceName"
	cat >&2 <<-'EOF'
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
			cat >&2 <<-'EOF'
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
*)
	echo "Usage: $0 {install|uninstall|reinstall}" 2>&2
	exit 1
	;;
esac
