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

if [ $JAVA_VERSION ]; then
	echo "JAVA_VERSION ${JAVA_VERSION}"
else
	echo "not found JAVA_VERSION"
	exit 1
fi
cd /tmp
download_url=""
ARCH_O=`uname -m`
# https://mirrors.tuna.tsinghua.edu.cn/Adoptium/
# https://github.com/AdoptOpenJDK/openjdk-docker/blob/master/8/jdk/ubuntu/Dockerfile.hotspot.nightly.full
case "${ARCH_O}" in
	aarch64|arm64)
		ARCH='aarch64';
	;;
	armhf|armv7l)
		ARCH='arm';
	;;
	ppc64el|ppc64le)
		ARCH='ppc64le';
	;;
	s390x)
		ARCH='s390x';
	;;
	amd64|x86_64)
	 	ARCH='x64';
	;;
	*)
	 	echo "Unsupported arch: ${ARCH_O}";
	 	exit 1;
	;;
esac;

download_url=`curl -s https://gitee.com/dromara/Jpom/raw/download_link/jdk/${JAVA_VERSION}/${ARCH}`

wget ${download_url} -O jdk.tar.gz
tar -zxf jdk.tar.gz --strip-components 1 -C /opt/java/
