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

if [ $JAVA_VERSION ]; then
	echo "JAVA_VERSION ${JAVA_VERSION}"
else
	echo "not found JAVA_VERSION"
	exit 1
fi
cd /tmp
download_url=""
ARCH_O=`uname -m`
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
case "${JAVA_VERSION}" in
8)
	download_url="https://mirrors.tuna.tsinghua.edu.cn/AdoptOpenJDK/8/jdk/${ARCH}/linux/OpenJDK8U-jdk_${ARCH}_linux_hotspot_8u322b06.tar.gz"
	;;
9)
	download_url="https://mirrors.tuna.tsinghua.edu.cn/AdoptOpenJDK/9/jdk/${ARCH}/linux/OpenJDK9U-jdk_${ARCH}_linux_hotspot_9.0.4_11.tar.gz"
	;;
10)
	download_url="https://mirrors.tuna.tsinghua.edu.cn/AdoptOpenJDK/10/jdk/${ARCH}/linux/OpenJDK10U-jdk_${ARCH}_linux_hotspot_10.0.2_13.tar.gz"
	;;
11)
	download_url="https://mirrors.tuna.tsinghua.edu.cn/AdoptOpenJDK/11/jdk/${ARCH}/linux/OpenJDK11U-jdk_${ARCH}_linux_hotspot_11.0.14_9.tar.gz"
	;;
12)
	download_url="https://mirrors.tuna.tsinghua.edu.cn/AdoptOpenJDK/12/jdk/${ARCH}/linux/OpenJDK12U-jdk_${ARCH}_linux_hotspot_12.0.2_10.tar.gz"
	;;
13)
	download_url="https://mirrors.tuna.tsinghua.edu.cn/AdoptOpenJDK/13/jdk/${ARCH}/linux/OpenJDK13U-jdk_${ARCH}_linux_hotspot_13.0.2_8.tar.gz"
	;;
14)
	download_url="https://mirrors.tuna.tsinghua.edu.cn/AdoptOpenJDK/14/jdk/${ARCH}/linux/OpenJDK14U-jdk_${ARCH}_linux_hotspot_14.0.2_12.tar.gz"
	;;
15)
	download_url="https://mirrors.tuna.tsinghua.edu.cn/AdoptOpenJDK/15/jdk/${ARCH}/linux/OpenJDK15U-jdk_${ARCH}_linux_hotspot_15.0.2_7.tar.gz"
	;;
16)
	download_url="https://mirrors.tuna.tsinghua.edu.cn/AdoptOpenJDK/16/jdk/${ARCH}/linux/OpenJDK16U-jdk_${ARCH}_linux_hotspot_16.0.2_7.tar.gz"
	;;
17)
	download_url="https://mirrors.tuna.tsinghua.edu.cn/AdoptOpenJDK/17/jdk/${ARCH}/linux/OpenJDK17U-jdk_${ARCH}_linux_hotspot_17.0.2_8.tar.gz"
	;;

*)
	echo "目前只支持 8,9,10,11,12,13,14,15,16,17"
	exit 1
	;;
esac
wget ${download_url} -O jdk.tar.gz
tar -zxf jdk.tar.gz --strip-components 1 -C /opt/java/
