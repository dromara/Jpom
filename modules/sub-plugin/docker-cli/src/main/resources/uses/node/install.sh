#!/bin/bash

if [ $NODE_VERSION ]; then
  echo "NODE_VERSION ${NODE_VERSION}"
else
  echo "not found NODE_VERSION"
  exit 1
fi
ARCH=`uname -m`
case "${ARCH}" in
   aarch64|arm64)
	 BINARY_ARCH='arm64';
	 ;;
   amd64|x86_64)
	 BINARY_ARCH='x64';
	 ;;
   *)
	 echo "Unsupported arch: ${ARCH}";
	 exit 1;
	 ;;
esac;
cd /tmp
wget https://registry.npmmirror.com/-/binary/node/v${NODE_VERSION}/node-v${NODE_VERSION}-linux-${BINARY_ARCH}.tar.gz
tar -zxf node-v${NODE_VERSION}-linux-${BINARY_ARCH}.tar.gz
cp -r node-v${NODE_VERSION}-linux-${BINARY_ARCH}/* /opt/node/
