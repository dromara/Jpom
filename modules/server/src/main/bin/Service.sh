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


# 兼容通过 sh xxx.sh 调用（脚本依赖 bash 语法）
if [ -z "${BASH_VERSION:-}" ]; then
  exec bash "$0" "$@"
fi

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
		cat >&2 <<-EOF
			Error: this installer needs the ability to run commands as root.
			We are unable to find either "sudo" or "su" available to make this happen.
		EOF
		exit 1
	fi
	exec_user="$user"
fi

# 写 unit 文件（install / reinstall 共用，reinstall 不再先 uninstall 以免停一次项目）
function writeService() {
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
# 服务端默认保持 systemd 的 KillMode=control-group（不写该项）：stop/restart 会一并结束
# 由它启动的构建命令子进程，避免重启后遗留孤儿构建继续占用工作空间（构建本身按异常关闭处理）。
# 插件端不同：项目/脚本进程需要在本体重启后继续运行，插件端的 unit 才写 KillMode=process。
# 如确实需要服务端重启后保留构建子进程，可手工在 unit 的 [Service] 段加 KillMode=process 后 daemon-reload。
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
}

function install() {
	if [ -f "$serviceFile" ]; then
		echo "service file already exists" 2>&2
		exit 2
	fi
	writeService
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
		# 先写新 unit 再 daemon-reload：随后 restart 的 stop 阶段已经按新 unit 的
		# KillMode=process 执行，只重启 Jpom 本体，项目进程不会再被一起停掉。
		# 不再先 uninstall（那会用旧 unit 停一次项目，还会清掉开机自启）。
		writeService
		$sh_c 'systemctl daemon-reload'
		$sh_c "systemctl enable $serviceName"
		$sh_c "systemctl restart $serviceName"
		echo "$serviceName reinstalled and restarted (projects keep running)"
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
