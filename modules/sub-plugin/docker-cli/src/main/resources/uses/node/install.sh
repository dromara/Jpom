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


if [ $NODE_VERSION ]; then
  echo "NODE_VERSION ${NODE_VERSION}"
else
  echo "not found NODE_VERSION"
  exit 1
fi
# https://github.com/nodejs/docker-node/blob/main/12/buster/Dockerfile
ARCH=`uname -m`
case "${ARCH}" in
	aarch64|arm64)
	 	BINARY_ARCH='arm64';
 	;;
   	amd64|x86_64)
	 	BINARY_ARCH='x64';
 	;;
    ppc64el)
    	BINARY_ARCH='ppc64le'
	;;
    s390x)
    	BINARY_ARCH='s390x'
    ;;
    armhf)
    	BINARY_ARCH='armv7l'
    ;;
    i386)
   	 	BINARY_ARCH='x86'
    ;;
   	*)
	 	echo "Unsupported arch: ${ARCH}";
	 	exit 1;
 	;;
esac;
cd /tmp
wget https://registry.npmmirror.com/-/binary/node/v${NODE_VERSION}/node-v${NODE_VERSION}-linux-${BINARY_ARCH}.tar.gz -O node.tar.gz
tar -zxf node.tar.gz --strip-components 1 -C /opt/node/
