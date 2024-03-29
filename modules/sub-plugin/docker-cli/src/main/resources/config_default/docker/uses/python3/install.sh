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


if [ $PYTHON3_VERSION ]; then
	echo "PYTHON3_VERSION ${PYTHON3_VERSION}"
else
	echo "not found PYTHON3_VERSION"
	exit 1
fi

# https://github.com/docker-library/python/blob/master/3.10/slim-bullseye/Dockerfile

# https://github.com/cdrx/docker-pyinstaller/blob/master/Dockerfile-py3-amd64

pythonPath=/opt/python3/
pythonTempPath=/usr/local/python3

# https://repo.huaweicloud.com/python/3.6.6/Python-3.6.6.tar.xz

apt-get update; \
	apt-get install -y --no-install-recommends \
		ca-certificates \
		netbase \
		tzdata \
	; \
rm -rf /var/lib/apt/lists/*

savedAptMark="$(apt-mark showmanual)"; \
	apt-get update; \
	apt-get install -y --no-install-recommends \
		dpkg-dev \
		gcc \
		gnupg dirmngr \
		libbluetooth-dev \
		libbz2-dev \
		libc6-dev \
		libexpat1-dev \
		libffi-dev \
		libgdbm-dev \
		liblzma-dev \
		libncursesw5-dev \
		libreadline-dev \
		libsqlite3-dev \
		libssl-dev \
		make \
		tk-dev \
		uuid-dev \
		xz-utils \
		zlib1g-dev \
;

wget -O python.tar.xz "https://repo.huaweicloud.com/python/${PYTHON3_VERSION}/Python-${PYTHON3_VERSION}.tar.xz";

mkdir -p ${pythonTempPath} && tar -xvf python.tar.xz --strip-components 1 -C ${pythonTempPath}

cd ${pythonTempPath};

gnuArch="$(dpkg-architecture --query DEB_BUILD_GNU_TYPE)"; \
./configure \
	--prefix=${pythonPath}
	--build="$gnuArch" \
	--enable-loadable-sqlite-extensions \
	--enable-optimizations \
	--enable-option-checking=fatal \
	--enable-shared \
	--with-lto \
	--with-system-expat \
	--with-system-ffi \
	--without-ensurepip \
;

nproc="$(nproc)";
make -j "$nproc" \
	LDFLAGS="-Wl,--strip-all" \
# setting PROFILE_TASK makes "--enable-optimizations" reasonable: https://bugs.python.org/issue36044 / https://github.com/docker-library/python/issues/160#issuecomment-509426916
	PROFILE_TASK='-m test.regrtest --pgo \
		test_array \
		test_base64 \
		test_binascii \
		test_binhex \
		test_binop \
		test_bytes \
		test_c_locale_coercion \
		test_class \
		test_cmath \
		test_codecs \
		test_compile \
		test_complex \
		test_csv \
		test_decimal \
		test_dict \
		test_float \
		test_fstring \
		test_hashlib \
		test_io \
		test_iter \
		test_json \
		test_long \
		test_math \
		test_memoryview \
		test_pickle \
		test_re \
		test_set \
		test_slice \
		test_struct \
		test_threading \
		test_time \
		test_traceback \
		test_unicode \
	' \
;

make install

