#!/usr/bin/bash
# shellcheck disable=SC2016,SC2119,SC2155,SC2206,SC2207,SC2254

node_version=16.13.1

ARCH="$(dpkg --print-architecture)"
case "${ARCH}" in
aarch64 | arm64)
	BINARY_ARCH='arm64'
	;;
amd64 | x86_64)
	BINARY_ARCH='x64'
	;;
*)
	echo "Unsupported arch: ${ARCH}"
	exit 1
	;;
esac

curl -LfsSo /opt/node-v${node_version}-linux-${BINARY_ARCH}.tar.gz https://npmmirror.com/mirrors/node/v${node_version}/node-v${node_version}-linux-${BINARY_ARCH}.tar.gz
tar -zxvf /opt/node-v${node_version}-linux-${BINARY_ARCH}.tar.gz -C /opt/


cat >"/etc/profile" <<EOL
# node js
export NODE_HOME="/opt/node-v${node_version}-linux-${BINARY_ARCH}/"
export PATH=$PATH:$NODE_HOME/bin
EOL

source /etc/profile

node -v
