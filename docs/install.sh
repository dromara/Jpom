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

JPOM_TYPE="$1"

PATH=$PATH:/bin:/sbin:/usr/bin:/usr/sbin:/usr/local/bin:/usr/local/sbin:~/bin:$(cd `dirname $0`; pwd)
export PATH
LANG=en_US.UTF-8

# loading env
if [ -f /etc/profile ]; then
  . /etc/profile
fi
if [ -f /etc/bashrc ]; then
  . /etc/bashrc
fi
if [ -f ~/.bashrc ]; then
  . ~/.bashrc
fi
if [ -f ~/.bash_profile ]; then
  . ~/.bash_profile
fi

# 安装 jdk
function installJdkFn() {
	if [[ ! -x "${JAVA_HOME}/bin/java" ]]; then
        JAVA=`which java`
        if [[ ! -x "$JAVA" ]]; then
        	# 判断是否存在文件
			if [[ ! -f "jdk-8u251-linux-x64.tar.gz" ]]; then
			 wget -O jdk-8u251-linux-x64.tar.gz https://jpom-releases.oss-cn-hangzhou.aliyuncs.com/jdk-8u251-linux-x64.tar.gz
			fi
			mkdir /usr/java/
			#
			tar -zxf jdk-8u251-linux-x64.tar.gz  -C /usr/java/
			#
			#PATH=$PATH:/usr/java/jdk1.8.0_251/bin
			#export PATH
			echo '安装jdk,路径/usr/java/jdk1.8.0_251/'
			# 修改环境变量
			echo ''>>/etc/profile
			echo 'export JAVA_HOME=/usr/java/jdk1.8.0_251'>>/etc/profile
			echo 'export CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar'>>/etc/profile
			echo 'export PATH=$PATH:$JAVA_HOME/bin'>>/etc/profile
			#export JAVA_HOME=/usr/java/jdk1.8.0_251
			# 更新环境变量
			source /etc/profile
			# 删除jdk压缩包
			rm -f jdk-8u251-linux-x64.tar.gz
         else
            echo "已经存在java环境${JAVA}/bin/java"
        fi
    else
        echo "已经存在java环境${JAVA_HOME}/bin/java"
    fi
}

# 安装 mvn
function installMvnFn() {
	if [[ ! -x "${MAVEN_HOME}/bin/mvn" ]]; then
        MVN=`which mvn`
        if [[ ! -x "$MVN" ]]; then
            # 判断是否存在文件
            if [[ ! -f "apache-maven-3.6.3-bin.tar.gz" ]]; then
             wget -O apache-maven-3.6.3-bin.tar.gz https://mirrors.aliyun.com/apache/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz
            fi
            mkdir /usr/maven/
            #
            tar -zxf apache-maven-3.6.3-bin.tar.gz  -C /usr/maven/
            #
            echo '安装maven,路径/usr/maven/apache-maven-3.6.3/'
            # 修改环境变量
            echo ''>>/etc/profile
            echo 'export MAVEN_HOME=/usr/maven/apache-maven-3.6.3/'>>/etc/profile
            echo 'export PATH=$PATH:$MAVEN_HOME/bin'>>/etc/profile
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

# 解压命令
if [[ ! -f "/usr/bin/unzip" ]];then
	#rm -f /etc/yum.repos.d/epel.repo
	yum install unzip -y
fi


module="$2"

# 判断是否包含jdk
temp_result=$(echo $module | grep "jdk")
if [[ "$temp_result" != "" ]]; then
	echo "开始检查 jdk"
    installJdkFn
fi

# 判断是否包含mvn
temp_result=$(echo $module | grep "mvn")
if [[ "$temp_result" != "" ]]; then
	echo "开始检查 mvn"
	installMvnFn
fi

# 判断
if [[ -z "${JPOM_TYPE}" ]] ; then
    TYPE="Server";
fi
echo "开始安装：${JPOM_TYPE}"
# 判断是否在文件
if [[ ! -f "${JPOM_TYPE}.zip" ]]; then
  # 转小写
  url_type=`echo ${JPOM_TYPE} | tr 'A-Z' 'a-z'`
  # 获取最新的版本号
  versions=`curl -LfsS https://jpom-docs.keepbx.cn/docs/versions.tag`
  download_url="https://jpom-releases.oss-cn-hangzhou.aliyuncs.com/${url_type}-${versions}-release.zip"
  wget -O ${JPOM_TYPE}.zip ${download_url}
fi
# 解压
unzip -o ${JPOM_TYPE}.zip
# 删除安装包
rm -f ${JPOM_TYPE}.zip
# 删除安装命令
rm -f install.sh
# 添加权限
chmod 755 ${JPOM_TYPE}.sh
# 启动
bash ${JPOM_TYPE}.sh start $@
