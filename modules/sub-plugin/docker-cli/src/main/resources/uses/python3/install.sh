#!/bin/bash

if [ $PYTHON3_VERSION ]; then
	echo "PYTHON3_VERSION ${PYTHON3_VERSION}"
else
	echo "not found PYTHON3_VERSION"
	exit 1
fi

# https://github.com/docker-library/python/blob/master/3.10/slim-bullseye/Dockerfile

pythonPath=/opt/python3/

apt-get update;

apt-get install -y --no-install-recommends ca-certificates netbase tzdata;

rm -rf /var/lib/apt/lists/*;

apt-get install -y --no-install-recommends dpkg-dev gcc  gnupg dirmngr libbluetooth-dev libbz2-dev libc6-dev libexpat1-dev libffi-dev libgdbm-dev liblzma-dev libncursesw5-dev libreadline-dev libsqlite3-dev libssl-dev make tk-dev uuid-dev wget xz-utils zlib1g-dev;

# https://repo.huaweicloud.com/python/3.6.6/Python-3.6.6.tar.xz

wget -O python.tar.xz "https://repo.huaweicloud.com/python/${PYTHON3_VERSION}/Python-${PYTHON3_VERSION}.tar.xz";

tar -zxf python.tar.xz --strip-components 1 -C ${pythonPath}

cd ${pythonPath};

/configure  --build="$gnuArch"  --enable-loadable-sqlite-extensions  --enable-optimizations  --enable-option-checking=fatal  --enable-shared  --with-system-expat  --with-system-ffi  --without-ensurepip;

nproc="$(nproc)"

