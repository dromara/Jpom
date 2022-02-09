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


if [ $GO_VERSION ]; then
	echo "GO_VERSION ${GO_VERSION}"
else
	echo "not found GO_VERSION"
	exit 1
fi

cd /tmp
download_url=""
ARCH_O=`uname -m`
# https://studygolang.com/dl
case "${ARCH_O}" in
	aarch64|arm64)
		ARCH='arm64';
	;;
	amd64|x86_64)
	 	ARCH='amd64';
	;;
	*)
	 	echo "Unsupported arch: ${ARCH_O}";
	 	exit 1;
	;;
esac;

# https://studygolang.com/dl/golang/go1.17.6.linux-amd64.tar.gz

wget https://studygolang.com/dl/golang/go${GO_VERSION}.linux-${ARCH}.tar.gz -O go.tar.gz
tar -zxf go.tar.gz --strip-components 1 -C /opt/go/
