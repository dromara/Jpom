#!/bin/bash

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
