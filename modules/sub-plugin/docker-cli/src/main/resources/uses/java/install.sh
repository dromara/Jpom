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
case "${JAVA_VERSION}_${ARCH}" in
11_aarch64)
	download_url="https://mirrors.tuna.tsinghua.edu.cn/Adoptium/11/jdk/aarch64/linux/OpenJDK11U-jdk_aarch64_linux_hotspot_11.0.16_8.tar.gz"
	;;
11_x64)
	download_url="https://mirrors.tuna.tsinghua.edu.cn/Adoptium/11/jdk/x64/linux/OpenJDK11U-jdk_x64_linux_hotspot_11.0.16_8.tar.gz"
	;;
11_s390x)
	download_url="https://mirrors.tuna.tsinghua.edu.cn/Adoptium/11/jdk/s390x/linux/OpenJDK11U-jdk_s390x_linux_hotspot_11.0.16_8.tar.gz"
	;;
11_arm)
	download_url="https://mirrors.tuna.tsinghua.edu.cn/Adoptium/11/jdk/arm/linux/OpenJDK11U-jdk_arm_linux_hotspot_11.0.16_8.tar.gz"
	;;
11_ppc64le)
	download_url="https://mirrors.tuna.tsinghua.edu.cn/Adoptium/11/jdk/ppc64le/linux/OpenJDK11U-jdk_ppc64le_linux_hotspot_11.0.16_8.tar.gz"
	;;
17_aarch64)
	download_url="https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jdk/aarch64/linux/OpenJDK17U-jdk_aarch64_linux_hotspot_17.0.4_8.tar.gz"
	;;
17_x64)
	download_url="https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jdk/x64/linux/OpenJDK17U-jdk_x64_linux_hotspot_17.0.4_8.tar.gz"
	;;
17_s390x)
	download_url="https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jdk/s390x/linux/OpenJDK17U-jdk_s390x_linux_hotspot_17.0.4_8.tar.gz"
	;;
17_arm)
	download_url="https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jdk/arm/linux/OpenJDK17U-jdk_arm_linux_hotspot_17.0.4_8.tar.gz"
	;;
17_ppc64le)
	download_url="https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jdk/ppc64le/linux/OpenJDK17U-jdk_ppc64le_linux_hotspot_17.0.4_8.tar.gz"
	;;
18_aarch64)
	download_url="https://mirrors.tuna.tsinghua.edu.cn/Adoptium/18/jdk/aarch64/linux/OpenJDK18U-jdk_aarch64_linux_hotspot_18.0.2_9.tar.gz"
	;;
18_x64)
	download_url="https://mirrors.tuna.tsinghua.edu.cn/Adoptium/18/jdk/x64/linux/OpenJDK18U-jdk_x64_linux_hotspot_18.0.2_9.tar.gz"
	;;
18_s390x)
	download_url="https://mirrors.tuna.tsinghua.edu.cn/Adoptium/18/jdk/s390x/linux/OpenJDK18U-jdk_s390x_linux_hotspot_18.0.2_9.tar.gz"
	;;
18_arm)
	download_url="https://mirrors.tuna.tsinghua.edu.cn/Adoptium/18/jdk/arm/linux/OpenJDK18U-jdk_arm_linux_hotspot_18.0.2_9.tar.gz"
	;;
18_ppc64le)
	download_url="https://mirrors.tuna.tsinghua.edu.cn/Adoptium/18/jdk/ppc64le/linux/OpenJDK18U-jdk_ppc64le_linux_hotspot_18.0.2_9.tar.gz"
	;;
8_aarch64)
	download_url="https://mirrors.tuna.tsinghua.edu.cn/Adoptium/8/jdk/aarch64/linux/OpenJDK8U-jdk_aarch64_linux_hotspot_8u345b01.tar.gz"
	;;
8_x64)
	download_url="https://mirrors.tuna.tsinghua.edu.cn/Adoptium/8/jdk/x64/linux/OpenJDK8U-jdk_x64_linux_hotspot_8u345b01.tar.gz"
	;;
8_arm)
	download_url="https://mirrors.tuna.tsinghua.edu.cn/Adoptium/8/jdk/arm/linux/OpenJDK8U-jdk_arm_linux_hotspot_8u342b07.tar.gz"
	;;
8_ppc64le)
	download_url="https://mirrors.tuna.tsinghua.edu.cn/Adoptium/8/jdk/ppc64le/linux/OpenJDK8U-jdk_ppc64le_linux_hotspot_8u342b07.tar.gz"
	;;
*)
	echo "目前只支持 11(aarch64,x64,s390x,arm,ppc64le) 17(aarch64,x64,s390x,arm,ppc64le) 18(aarch64,x64,s390x,arm,ppc64le) 8(aarch64,x64,arm,ppc64le) "
	exit 1
	;;
esac
wget ${download_url} -O jdk.tar.gz
tar -zxf jdk.tar.gz --strip-components 1 -C /opt/java/
