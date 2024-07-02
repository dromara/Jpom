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

# https://github.com/AdoptOpenJDK/openjdk-docker/blob/master/8/jdk/centos/Dockerfile.hotspot.releases.full
# https://github.com/carlossg/docker-maven/blob/master/openjdk-8/Dockerfile

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

JPOM_TYPE="$1"
ARGS="$*"
module="$2"
offline=$(echo "$module" | grep "offline")
binAbsPath=$(absPath "$(dirname "$0")")

function errorExit() {
	msg="$1"
	echo "$msg" 2>&2
	if [ "$offline" == "" ]; then
		# 删除安装命令
		rm -f "${binAbsPath}/install.sh"
	fi
	rmfile=("jdk.tar.gz" "maven.tar.gz" "node.tar.gz")
	for element in "${rmfile[@]}"; do
		rm -f "$element"
	done
	exit 1
}

function findProfile() {
	user="$(id -un 2>/dev/null || true)"
	profileName=""
	if [ "$user" != 'root' ]; then
		array=("$HOME/.bash_profile" "$HOME/.bashrc" "$HOME/.bash_login")
		for element in "${array[@]}"; do
			if [ -f "$element" ]; then
				profileName=$element
				break
			fi
		done
		if [ -z "$profileName" ]; then
			cat >&2 <<-EOF
				ERROR: 没有找到可用的环境变量文件
			EOF
			exit 1
		fi
	else
		profileName="/etc/profile"
	fi
	echo "$profileName"
}

command_exists() {
	command -v "$@" >/dev/null 2>&1
}

function checkCommand() {
	command=$(which "$1")
	if [[ ! -x "$command" ]]; then
		if command_exists "$1"; then
			echo "$1"
		else
			echo "not found"
		fi
	else
		echo "$command"
	fi
}

function mustMkdir() {
	dir="$1"
	name="$2"
	mkdir -p "$dir"
	if [ ! -d "$dir" ]; then
		cat >&2 <<-EOF
			ERROR: 目录创建失败: $dir
		EOF
		while (true); do
			cat >&2 <<-EOF
				INFO: 请手动输入安装 $name 目录
			EOF
			read -r userOption
			useAbsPath=$(absPath "$userOption")
			mkdir -p "$useAbsPath"
			if [ -d "$useAbsPath" ]; then
				dir=$useAbsPath
				break
			fi
			cat >&2 <<-EOF
				 ERROR: 输入的目录创建失败: $useAbsPath
				 ERROR: 请重新手动输入安装 $name 目录
			EOF
		done
	fi
	echo "$dir"
}

# 安装 jdk
function installJdkFn() {
	javaCommand=$(checkCommand java)
	if [[ "$javaCommand" == "not found" ]]; then
		echo "不存在 java 环境,开始尝试安装"
		ARCH_O=$(uname -m)
		# 判断系统架构
		case "${ARCH_O}" in
		aarch64 | arm64)
			ARCH='aarch64'
			;;
		armhf | armv7l)
			ARCH='arm'
			;;
		ppc64el | ppc64le)
			ARCH='ppc64le'
			;;
		s390x)
			ARCH='s390x'
			;;
		amd64 | x86_64)
			ARCH='x64'
			;;
		*)
			errorExit "Unsupported arch: ${ARCH_O}"
			;;
		esac
		useDir=$(mustMkdir /usr/java/ java)
		userProfileName=$(findProfile)

		if grep -q "CLASSPATH" "$userProfileName"; then
			errorExit "系统环境变量中已经存在 CLASSPATH，请检查配置是否正确.或者终端是否重新加载环境变量：source $userProfileName"
		fi
		if grep -q "JAVA_HOME" "$userProfileName"; then
			errorExit "系统环境变量中已经存在 JAVA_HOME，请检查配置是否正确.或者终端是否重新加载环境变量：source $userProfileName"
		fi

		download_url=$(curl -s https://d.jpom.download/jdk-url/8/${ARCH})

		curl -LfSo jdk.tar.gz "${download_url}"

		#	检查是否下载成功
		if [[ ! -f jdk.tar.gz ]]; then
			errorExit "JDK下载失败，请检查下载地址 : $download_url"
		fi

		#
		jdk_name=$(tar -tf jdk.tar.gz | grep 'jdk' | head -1)

		#	检查环境变量
		if grep -q "$useDir/${jdk_name}" "$userProfileName"; then
			errorExit "系统环境变量中已经存在 jdk 路径，请检查配置是否正确.或者终端是否重新加载环境变量：source $userProfileName"
		fi

		tar -zxf jdk.tar.gz -C "$useDir"
		echo "安装jdk,路径 $useDir/${jdk_name}"
		cat >>"$userProfileName" <<EOF

export JAVA_HOME=$useDir/${jdk_name}
export CLASSPATH=.:\$JAVA_HOME/lib/dt.jar:\$JAVA_HOME/lib/tools.jar
export PATH=\$PATH:\$JAVA_HOME/bin
EOF
		echo "use profile $userProfileName"
		# shellcheck source=.profile
		source "$userProfileName"
		# 删除jdk压缩包
		rm -f jdk.tar.gz
	else
		echo "已经存在java环境${javaCommand}"
	fi

}

# 安装 mvn
function installMvnFn() {
	mvnCommand=$(checkCommand mvn)
	if [[ "$mvnCommand" == "not found" ]]; then
		echo "不存在 mvn 环境,开始尝试安装"
		useDir=$(mustMkdir /usr/maven/ maven)
		userProfileName=$(findProfile)
		if grep -q "$useDir/apache-maven-3.9.6/" /etc/profile; then
			errorExit "系统环境变量中已经存在 mvn 路径，请检查配置是否正确.或者终端是否重新加载环境变量：source $userProfileName"
		fi
		if grep -q "MAVEN_HOME" /etc/profile; then
			errorExit "系统环境变量中已经存在 MAVEN_HOME，请检查配置是否正确.或者终端是否重新加载环境变量：source $userProfileName"
		fi
		curl -LfSo maven.tar.gz https://mirrors.aliyun.com/apache/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz
		#
		tar -zxf maven.tar.gz -C "$useDir"
		#
		echo "安装maven,路径 $useDir/apache-maven-3.9.6/"
		cat >>"$userProfileName" <<EOF

export MAVEN_HOME=$useDir/apache-maven-3.9.6/
export PATH=\$PATH:\$MAVEN_HOME/bin
EOF
		echo "use profile $userProfileName"
		# shellcheck source=.profile
		source "$userProfileName"
		# 删除maven压缩包
		rm -f maven.tar.gz
	else
		echo "已经存在maven环境${mvnCommand}"
	fi

}

function installNodeFn() {
	nodeCommand=$(checkCommand node)
	if [[ "$nodeCommand" == "not found" ]]; then
		echo "不存在 node 环境,开始尝试安装"
		ARCH_O=$(uname -m)
		case "${ARCH_O}" in
		aarch64 | arm64)
			BINARY_ARCH='arm64'
			;;
		amd64 | x86_64)
			BINARY_ARCH='x64'
			;;
		*)
			errorExit "Unsupported arch: ${ARCH_O}"
			;;
		esac

		NODE_VERSION="16.13.1"
		useDir=$(mustMkdir /usr/node/ node)
		userProfileName=$(findProfile)
		if grep -q "NODE_HOME" "$userProfileName"; then
			errorExit "系统环境变量中已经存在 NODE_HOME，请检查配置是否正确.或者终端是否重新加载环境变量：source $userProfileName"
		fi

		curl -LfSo node.tar.gz https://npmmirror.com/mirrors/node/v${NODE_VERSION}/node-v${NODE_VERSION}-linux-${BINARY_ARCH}.tar.gz
		#
		node_name=$(tar -tf node.tar.gz | grep 'node' | head -1)

		#	检查环境变量
		if grep -q "$useDir/${node_name}" "$userProfileName"; then
			errorExit "系统环境变量中已经存在 node 路径，请检查配置是否正确.或者终端是否重新加载环境变量：source $userProfileName"
		fi

		tar -zxf node.tar.gz -C "$useDir"
		echo "安装node,路径 $useDir/${node_name}"

		cat >>"$userProfileName" <<EOF

export NODE_HOME=$useDir/${node_name}
export PATH=\$NODE_HOME/bin:\$PATH
EOF
		echo "use profile $userProfileName"
		# shellcheck source=.profile
		source "$userProfileName"

		npm config set registry https://registry.npmmirror.com/
		# 删除 node 压缩包
		rm -f node.tar.gz

	else

		echo "已经存在node环境${nodeCommand}"
	fi
}

function checkModule() {
	# 判断是否包含jdk
	if [[ $(echo "$module" | grep "jdk") != "" ]]; then
		echo "开始检查 jdk"
		installJdkFn
		echo "=> jdk 安装完成!"
    echo "=> 最后您需要重启终端才能正常使用 java 环境"
    echo
	fi

	# 判断是否包含mvn
	if [[ $(echo "$module" | grep "mvn") != "" ]]; then
		echo "开始检查 mvn"
		installMvnFn
		echo "=> maven 安装完成!"
    echo "=> 最后您需要重启终端才能正常使用 mvn 环境"
    echo
	fi

	# 判断是否包含 node
	if [[ $(echo "$module" | grep "node") != "" ]]; then
		echo "开始检查 node"
		installNodeFn
		echo "=> node 安装完成!"
    echo "=> 最后您需要重启终端才能正常使用 node 环境"
    echo
	fi

	if [[ $(echo "$module" | grep "only-module") != "" ]]; then
		echo "检查依赖结束"
		exit 0
	fi
}

command_exists() {
	command -v "$@" >/dev/null 2>&1
}

# 判断是否填写了安装类型
if [[ -z "${JPOM_TYPE}" ]]; then
	JPOM_TYPE="Server"
fi

# 截取指定版本
versions=""
temp_result=$(echo ${JPOM_TYPE} | grep "-")
if [ "${temp_result}" != "" ]; then
	JPOM_TYPE=$(echo "${temp_result}" | awk -F '-' '{print $1}')
	versions=$(echo "${temp_result}" | awk -F '-' '{print $2}')
fi
# 下载类型转小写
url_type=$(echo "${JPOM_TYPE}" | tr '[:upper:]' '[:lower:]')
# 判断类型是否合法
if [ "$url_type" == "server" ]; then
	JPOM_TYPE="Server"
elif [ "$url_type" == "agent" ]; then
	JPOM_TYPE="Agent"
else
	errorExit "不支持的 $url_type 类型,请检查是否填写正确的参数"
fi

fileName="${JPOM_TYPE}.tar.gz"

#判断非 root 用户不能使用默认方式安装
if [[ $(echo "$module" | grep "default") != "" ]]; then
	user="$(id -un 2>/dev/null || true)"
	if [ "$user" != 'root' ]; then
		errorExit "非 root 用户不能使用 default 形式安装,请使用普通安装方式"
	fi
fi

#开始准备安装相关依赖、判断是否未离线安装

if [ "$offline" == "" ]; then
	checkModule
else
	fileName="$3"
	if [ "$fileName" == "" ]; then
		errorExit "请指定离线安装包参数：./install.sh $JPOM_TYPE offline $JPOM_TYPE.tar.gz"
	fi
	if [[ ! -f "$fileName" ]]; then
		errorExit "安装包文件不存在"
	fi
fi

## check java path
javaCommand=$(checkCommand java)
if [[ "$javaCommand" == "not found" ]]; then
	echo "不能正常安装"
	errorExit "Cannot find a Java JDK. Please set either set JAVA or put java (>=1.8) in your PATH."
fi

echo "use java : $javaCommand"

jpom_dir=/usr/local/jpom-${url_type}
# 提示用户安装目录
echo ">>>>>默认安装目录 ${jpom_dir}<<<<<"

function askInstallPath() {
	echo "是否使用此目录作为安装目录? "
	echo "需要使用绝对路径 (注意: agent 和 server 不能装到同一个目录!)"
	while (true); do
		echo "输入 y 确定使用默认路径, 否则请输入安装目录："
		read -r userInstallPath
		if [ "$userInstallPath" == "" ]; then
			continue
		else
			if [ "$userInstallPath" == "y" ]; then
				jpom_dir=$jpom_dir
				if [ -e "$jpom_dir" ]; then
					echo "$jpom_dir 目录已经被占用请输入其他路径"
					continue
				fi
				break
			else
				# 检测安装目录是否为空
				while (true); do
					if [[ -e "$userInstallPath" ]]; then
						echo "目录已存在, 你确定继续吗? 输入 y 继续(注意：agent 和 server 不能装到同一个目录!), 否则, 请指定一个新的安装路径："
						read -r userOption
						if [[ "$userOption" == "" ]]; then
							continue
						fi
						if [[ "$userOption" == "y" ]]; then
							jpom_dir=$userInstallPath
							break
						else
							#	继续等待输入
							userInstallPath=$userOption
						fi
					else
						# 目录不存在，直接安装
						jpom_dir=$userInstallPath
						break
					fi
				done
				break
			fi
		fi
	done
}

if [[ $(echo "$module" | grep "default") == "" ]]; then
	askInstallPath
fi

use_tag="release"
if [[ $(echo "$module" | grep "beta") != "" ]]; then
	use_tag="beta"
fi

if [ "$offline" == "" ]; then
	if [[ -z "${versions}" ]]; then
		# 获取最新的版本号
		versions=$(curl -LfsS https://jpom.top/docs/versions.$use_tag.tag)
	fi

	if [[ -z "${versions}" ]]; then
		errorExit "没有可以的版本号"
	fi
fi
jpom_dir=$(absPath "$jpom_dir")
jpom_dir=$(mustMkdir "$jpom_dir" "jpom $JPOM_TYPE")
echo "开始安装：${JPOM_TYPE} ${versions}, 安装目录 ${jpom_dir}"
# 创建指定目录
mkdir -p "${jpom_dir}" && cd "${jpom_dir}" || exit

sh_array=("./bin/Agent.sh" "./Agent.sh" "./bin/Server.sh" "./Server.sh")
for element in "${sh_array[@]}"; do
	if [ -f "$element" ] || [ -f "./${JPOM_TYPE}.sh" ]; then
		errorExit "${jpom_dir} 目录下已经存在管理 $element 命令,请不要重复安装"
	fi
done

# 判断是否存在文件
if [[ ! -f "${fileName}" ]]; then
	echo "================开始下载 $fileName================"
	curl -LfSo "${fileName}" "https://d.jpom.download/$use_tag/${versions}/${url_type}-${versions}-release.tar.gz"
fi
# 解压
tar -zxf "${fileName}" -C "${jpom_dir}"
# 删除安装包
rm -f "${fileName}"

shName=""
sh_array=("./bin/${JPOM_TYPE}.sh" "./${JPOM_TYPE}.sh")
for element in "${sh_array[@]}"; do
	if [ -f "$element" ]; then
		shName=$element
		break
	fi
done
if [ -z "$shName" ]; then
	errorExit "没有找到对应的管理命令"
fi

# 判断是否需要安装服务
if [[ $(echo "$module" | grep "service") != "" ]]; then
	echo "================开始安装服务================"
	if [ ! -f "./bin/Service.sh" ]; then
		cat >&2 <<-EOF
			ERROR: Service.sh not found .
		EOF
	fi
	# 安装并设置开机自启
	bash "./bin/Service.sh" install enable
fi
if [ "$offline" == "" ]; then
	# 删除安装命令
	rm -f "${binAbsPath}/install.sh"
fi
echo "================开始启动================"
# 启动
bash "$shName" start "$ARGS"
