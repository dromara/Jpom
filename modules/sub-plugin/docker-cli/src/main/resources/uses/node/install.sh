#!/bin/bash



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
