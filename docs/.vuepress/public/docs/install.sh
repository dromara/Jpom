#!/bin/bash
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

# https://github.com/AdoptOpenJDK/openjdk-docker/blob/master/8/jdk/centos/Dockerfile.hotspot.releases.full
# https://github.com/carlossg/docker-maven/blob/master/openjdk-8/Dockerfile

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

JPOM_TYPE="$1"
ARGS="$*"
module="$2"
offline=$(echo "$module" | grep "offline")

function errorExit() {
	msg="$1"
	echo "$msg" 2>&2
	if [ "$offline" == "" ]; then
		# 删除安装命令
		rm -f "${bin_abs_path}/install.sh"
	fi
	exit 1
}

# 安装 jdk
function installJdkFn() {
	if [[ ! -x "${JAVA_HOME}/bin/java" ]]; then
		JAVA=$(which java)
		if [[ ! -x "$JAVA" ]]; then

			download_url=""
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

			download_url=$(curl -s https://gitee.com/dromara/Jpom/raw/download_link/jdk/8/${ARCH})

			curl -LfSo jdk.tar.gz "${download_url}"
			mkdir -p /usr/java/
			#
			jdk_name=$(tar -tf jdk.tar.gz | grep 'jdk' | head -1)

			#	检查环境变量
			if grep -q "/usr/java/${jdk_name}" /etc/profile; then
				errorExit "系统环境变量中已经存在 jdk 路径，请检查配置是否正确.或者终端是否重新加载环境变量：source /etc/profile"
			else
				tar -zxf jdk.tar.gz -C /usr/java/
				echo "安装jdk,路径 /usr/java/${jdk_name}"
				# 修改环境变量
				{
					echo ""
					echo "export JAVA_HOME=/usr/java/${jdk_name}"
					echo "export CLASSPATH=.:\$JAVA_HOME/lib/dt.jar:\$JAVA_HOME/lib/tools.jar"
					echo "export PATH=\$PATH:\$JAVA_HOME/bin"
				} >>"/etc/profile"
			fi

			# 更新环境变量
			source /etc/profile
			# 删除jdk压缩包
			rm -f jdk.tar.gz
		else
			echo "已经存在java环境${JAVA}"
		fi
	else
		echo "已经存在java环境${JAVA_HOME}/bin/java"
	fi
}

# 安装 mvn
function installMvnFn() {
	if [[ ! -x "${MAVEN_HOME}/bin/mvn" ]]; then
		MVN=$(which mvn)
		if [[ ! -x "$MVN" ]]; then
			if grep -q "/usr/maven/apache-maven-3.6.3/" /etc/profile; then
				errorExit "系统环境变量中已经存在 mvn 路径，请检查配置是否正确.或者终端是否重新加载环境变量：source /etc/profile"
			fi
			curl -LfSo apache-maven-3.6.3-bin.tar.gz https://mirrors.aliyun.com/apache/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz

			mkdir -p /usr/maven/
			#
			tar -zxf apache-maven-3.6.3-bin.tar.gz -C /usr/maven/
			#
			echo '安装maven,路径/usr/maven/apache-maven-3.6.3/'
			# 修改环境变量
			{
				echo ""
				echo "export MAVEN_HOME=/usr/maven/apache-maven-3.6.3/"
				echo "export PATH=\$PATH:\$MAVEN_HOME/bin"
			} >>/etc/profile

			# 更新环境变量
			source /etc/profile
			# 删除maven压缩包
			rm -f apache-maven-3.6.3-bin.tar.gz
		else
			echo "已经存在maven环境${MVN}/bin/mvn"
		fi
	else
		echo "已经存在maven环境${MAVEN_HOME}/bin/mvn"
	fi
}

function installNodeFn() {
	NODE=$(which node)
	if [[ ! -x "$NODE" ]]; then
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

		curl -LfSo node.tar.gz https://npmmirror.com/mirrors/node/v${NODE_VERSION}/node-v${NODE_VERSION}-linux-${BINARY_ARCH}.tar.gz
		#
		node_name=$(tar -tf node.tar.gz | grep 'node' | head -1)
		#	检查环境变量
		if grep -q "/usr/node/${node_name}" /etc/profile; then
			errorExit "系统环境变量中已经存在 node 路径，请检查配置是否正确.或者终端是否重新加载环境变量：source /etc/profile"
		else
			mkdir -p /usr/node/
			tar -zxf node.tar.gz -C /usr/node
			echo "安装node,路径 /usr/node/${node_name}"
			# 修改环境变量
			{
				echo ""
				echo "export NODE_HOME=/usr/node/${node_name}"
				echo "export PATH=\$NODE_HOME/bin:\$PATH"
			} >>"/etc/profile"

			# 更新环境变量
			source /etc/profile
			npm config set registry https://registry.npmmirror.com/
			# 删除 node 压缩包
			rm -f node.tar.gz
		fi
	else
		echo "已经存在node环境${NODE}"
	fi
}

function checkModule() {
	# 判断是否包含jdk
	if [[ $(echo "$module" | grep "jdk") != "" ]]; then
		echo "开始检查 jdk"
		installJdkFn
	fi

	# 判断是否包含mvn
	if [[ $(echo "$module" | grep "mvn") != "" ]]; then
		echo "开始检查 mvn"
		installMvnFn
	fi

	# 判断是否包含 node
	if [[ $(echo "$module" | grep "node") != "" ]]; then
		echo "开始检查 node"
		installNodeFn
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
if [ -z "$JAVA" ]; then
	JAVA=$(which java)
fi
if [ -z "$JAVA" ]; then
	if command_exists java; then
		JAVA="java"
	fi
fi
if [ -z "$JAVA" ]; then
	errorExit "Cannot find a Java JDK. Please set either set JAVA or put java (>=1.8) in your PATH."
fi

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
				if [ -e "$userInstallPath" ]; then
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
							userInstallPath=$userOption
						fi
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

if [ "$offline" == "" ]; then
	if [[ -z "${versions}" ]]; then
		# 获取最新的版本号
		versions=$(curl -LfsS https://jpom.top/docs/versions.tag)
	fi

	if [[ -z "${versions}" ]]; then
		errorExit "没有可以的版本号"
	fi
fi

echo "开始安装：${JPOM_TYPE} ${versions}, 安装目录 ${jpom_dir}"
# 创建指定目录
mkdir -p "${jpom_dir}" && cd "${jpom_dir}" || exit

if [ -f "./bin/${JPOM_TYPE}.sh" ] || [ -f "./${JPOM_TYPE}.sh" ]; then
	errorExit "${jpom_dir} 目录下已经存在管理命令,请不要重复安装"
fi

# 判断是否存在文件
if [[ ! -f "${fileName}" ]]; then
	download_url="https://download.jpom.top/release/${versions}/${url_type}-${versions}-release.tar.gz"
	wget -O "${fileName}" "${download_url}"
fi
# 解压
tar -zxf "${fileName}" -C "${jpom_dir}"
# 删除安装包
rm -f "${fileName}"

shName=""
if [ -f "./bin/${JPOM_TYPE}.sh" ]; then
	shName="./bin/${JPOM_TYPE}.sh"
elif [ -f "./${JPOM_TYPE}.sh" ]; then
	shName="./${JPOM_TYPE}.sh"
else
	errorExit "没有找到对应的管理命令"
fi
if [ "$offline" == "" ]; then
	# 删除安装命令
	rm -f "${bin_abs_path}/install.sh"
fi
# 添加权限
chmod 755 "$shName"
# 启动
bash "$shName" start "$ARGS"
