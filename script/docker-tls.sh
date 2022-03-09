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

#
# -------------------------------------------------------------
# 自动创建 Docker TLS 证书
# wget https://dromara.gitee.io/jpom/script/docker-tls.sh
# curl -fsSL https://get.docker.com | bash -s docker --mirror Aliyun
# systemctl daemon-reload && systemctl restart docker
# -------------------------------------------------------------
# 以下是配置信息
# --[BEGIN]------------------------------
NOW_PATH=$(cd `dirname $0`; pwd)"/"
echo "当前目录：${NOW_PATH} 证书文件将保存在此文件夹下"
read -p "请输入证书使用的 IP 地址或者 HOST: " HOST
#
echo "您输入的是：${HOST} 证书只能在这个 IP 或者 HOST 下使用,证书密码和输入的一致"
# --[INIT PARAMETER]------------------------------
PASSWORD="$HOST"
COUNTRY="CN"
STATE="$HOST"
CITY="$HOST"
ORGANIZATION="$HOST"
ORGANIZATIONAL_UNIT="Dev"
COMMON_NAME="$HOST"
EMAIL="$HOST@docker-tls.com"
# --[END]--
# Generate CA key
openssl genrsa -aes256 -passout "pass:$PASSWORD" -out "ca-key.pem" 4096
# Generate CA
openssl req -new -x509 -days 365 -key "ca-key.pem" -sha256 -out "ca.pem" -passin "pass:$PASSWORD" -subj "/C=$COUNTRY/ST=$STATE/L=$CITY/O=$ORGANIZATION/OU=$ORGANIZATIONAL_UNIT/CN=$COMMON_NAME/emailAddress=$EMAIL"
# Generate Server key
openssl genrsa -out "server-key.pem" 4096
# Generate Server Certs.
openssl req -subj "/CN=$COMMON_NAME" -sha256 -new -key "server-key.pem" -out server.csr
rm -f extfile.cnf
echo "subjectAltName = DNS.1:$HOST,IP.1:127.0.0.1" >> extfile.cnf
echo "extendedKeyUsage = serverAuth" >> extfile.cnf
openssl x509 -req -days 365 -sha256 -in server.csr -passin "pass:$PASSWORD" -CA "ca.pem" -CAkey "ca-key.pem" -CAcreateserial -out "server-cert.pem" -extfile extfile.cnf
# Generate Client Certs.
rm -f extfile.cnf
openssl genrsa -out "key.pem" 4096
openssl req -subj '/CN=client' -new -key "key.pem" -out client.csr
echo "extendedKeyUsage = clientAuth" >> extfile.cnf
openssl x509 -req -days 365 -sha256 -in client.csr -passin "pass:$PASSWORD" -CA "ca.pem" -CAkey "ca-key.pem" -CAcreateserial -out "cert.pem" -extfile extfile.cnf
rm -f client.csr server.csr ca.srl extfile.cnf

# check
if [ -f "${NOW_PATH}key.pem" -a -f "${NOW_PATH}ca.pem" -a -f "${NOW_PATH}ca-key.pem" -a -f "${NOW_PATH}server-cert.pem" -a -f "${NOW_PATH}server-key.pem" ]; then
	echo "证书生成完成"
	echo "客户端使用文件：key.pem ca.pem cert.pem"
	echo "Docker 端使用文件：ca.pem server-cert.pem server-key.pem"
	echo "Docker 推荐配置内容：-H tcp://0.0.0.0:2375 --tlsverify --tlscacert=${NOW_PATH}ca.pem --tlscert=${NOW_PATH}server-cert.pem --tlskey=${NOW_PATH}server-key.pem"
else
	echo "证书生成不完成,请检查配置和根据错误日志排查"
fi
