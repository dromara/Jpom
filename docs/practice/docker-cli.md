![](https://cdn.jsdelivr.net/gh/jiangzeyin/Jpom-site/images/jpom_logo.png)

##  `简而轻的低侵入式在线构建、自动部署、日常运维、项目监控软件`

## 前言

> 本文主要介绍：
> 如何从零开始使用 Jpom 中的 Docker 管理
>
> 文中使用到的依赖环境版本仅供参考，实际使用中请根据业务情况来安装对应的版本

> 注意：本文采用一键安装同时基于 2.8.8 版本讲解,系统为 ubuntu

## 需要准备的环境

1. Jpom 服务端（安装 jpom 需要 java 环境）
2. 一个安装了 docker 的服务器（可以和安装 Jpom 服务端在同一个服务器）


## 安装服务端

```
# 提前创建好文件夹 并且切换到对应到文件夹执行命令
mkdir -p /home/jpom/server/
apt install -y wget && wget -O install.sh https://dromara.gitee.io/jpom/docs/install.sh && bash install.sh Server jdk
```

![install1](https://cdn.jsdelivr.net/gh/jiangzeyin/Jpom-site/tutorial/images/project_dsl_java/install1.png)

![install2](https://cdn.jsdelivr.net/gh/jiangzeyin/Jpom-site/tutorial/images/project_dsl_java/install2.png)

### 初始化服务端

#### 添加超级管理账号

> 添加一个超级管理员账号，请妥善保管此账号同时请设置安全度较强的密码

![install1](https://cdn.jsdelivr.net/gh/jiangzeyin/Jpom-site/tutorial/images/project_dsl_java/inits1.png)

#### 开启账号 MFA

> 为了系统安全，强烈建议超级管理员账号开启 MFA 两步验证
>
![install2](https://cdn.jsdelivr.net/gh/jiangzeyin/Jpom-site/tutorial/images/project_dsl_java/inits2.png)

## 安装 docker

```
# 这里使用了阿里云镜像，可以更新自己业务选择
curl -fsSL https://get.docker.com | bash -s docker --mirror Aliyun
```

### 状态管理 

```
# 查看状态
systemctl status docker
# 停止
systemctl stop docker
# 启动
systemctl start docker
# 重启
systemctl daemon-reload && systemctl restart docker
```

### Docker TLS 证书

Jpom 使用 docker http 接口是实现和 docker 通讯和管理，但是默认没有开启任何认证这样使得 docker 极不安全，如果端口暴露到公网还容易出现挖矿情况

所以这里 我们强烈建议您使用 TLS 证书来连接 docker 提升安全性

这里提供一个快速生成证书到脚本示例：（使用中请一定修改脚本内的 IP 或者可能无法使用的情况奥）

```
#!/bin/bash
#
# -------------------------------------------------------------
# 自动创建 Docker TLS 证书
# -------------------------------------------------------------
# 以下是配置信息
# --[BEGIN]------------------------------
# 请修改为您连接到 IP
IP="172.19.106.253"
# 证书目录请更新自己需求修改
PASSWORD="172.19.106.253"
COUNTRY="CN"
STATE="$IP"
CITY="$IP"
ORGANIZATION="$IP"
ORGANIZATIONAL_UNIT="Dev"
COMMON_NAME="$IP"
EMAIL="$IP@docker-tls.com"
# --[END]--
# Generate CA key
openssl genrsa -aes256 -passout "pass:$PASSWORD" -out "ca-key.pem" 4096
# Generate CA
openssl req -new -x509 -days 365 -key "ca-key.pem" -sha256 -out "ca.pem" -passin "pass:$PASSWORD" -subj "/C=$COUNTRY/ST=$STATE/L=$CITY/O=$ORGANIZATION/OU=$ORGANIZATIONAL_UNIT/CN=$COMMON_NAME/emailAddress=$EMAIL"
# Generate Server key
openssl genrsa -out "server-key.pem" 4096
# Generate Server Certs.
openssl req -subj "/CN=$COMMON_NAME" -sha256 -new -key "server-key.pem" -out server.csr
echo "subjectAltName = IP:$IP,IP:127.0.0.1" >> extfile.cnf
echo "extendedKeyUsage = serverAuth" >> extfile.cnf
openssl x509 -req -days 365 -sha256 -in server.csr -passin "pass:$PASSWORD" -CA "ca.pem" -CAkey "ca-key.pem" -CAcreateserial -out "server-cert.pem" -extfile extfile.cnf
# Generate Client Certs.
rm -f extfile.cnf
openssl genrsa -out "key.pem" 4096
openssl req -subj '/CN=client' -new -key "key.pem" -out client.csr
echo extendedKeyUsage = clientAuth >> extfile.cnf
openssl x509 -req -days 365 -sha256 -in client.csr -passin "pass:$PASSWORD" -CA "ca.pem" -CAkey "ca-key.pem" -CAcreateserial -out "cert.pem" -extfile extfile.cnf
rm -vf client.csr server.csr ca.srl extfile.cnf
```

同时将生成到证书下载到本地 保存使用

证书示例：

![install1](https://cdn.jsdelivr.net/gh/jiangzeyin/Jpom-site/tutorial/images/docker-cli/tls1.png)

## 配置 docker

### 查看 docker.service 路径

systemctl status docker

![install1](https://cdn.jsdelivr.net/gh/jiangzeyin/Jpom-site/tutorial/images/docker-cli/service1.png)

### 配置 tcp + tls

`vim /lib/systemd/system/docker.service`

![install1](https://cdn.jsdelivr.net/gh/jiangzeyin/Jpom-site/tutorial/images/docker-cli/service2.png)

ExecStart 需要添加内容

`--tlsverify --tlscacert=/home/docker/tls-ca/ca.pem --tlscert=/home/docker/tls-ca/server-cert.pem --tlskey=/home/docker/tls-ca/server-key.pem -H tcp://0.0.0.0:2375`

```
# 配置示例
ExecStart=/usr/bin/dockerd -H fd:// --containerd=/run/containerd/containerd.sock --tlsverify --tlscacert=/home/docker/tls-ca/ca.pem --tlscert=/home/docker/tls-ca/server-cert.pem --tlskey=/home/docker/tls-ca/server-key.pem -H tcp://0.0.0.0:2375
```

部分情况需要删除：`-H fd://`

![install1](https://cdn.jsdelivr.net/gh/jiangzeyin/Jpom-site/tutorial/images/docker-cli/service3.png)

## 添加 docker

![install1](https://cdn.jsdelivr.net/gh/jiangzeyin/Jpom-site/tutorial/images/docker-cli/add-docker1.png)

注意这里 host 为：`tcp://127.0.0.1:2375` 这里因为连接本地 docker 所有使用 127.0.0.1 实际中请更换

证书为压缩文件，压缩文件需要包含生成证书文件：`key.pem` `ca.pem` `cert.pem`


![install1](https://cdn.jsdelivr.net/gh/jiangzeyin/Jpom-site/tutorial/images/docker-cli/ca1.png)


![install1](https://cdn.jsdelivr.net/gh/jiangzeyin/Jpom-site/tutorial/images/docker-cli/add-docker2.png)

## Jpom 中使用

![install1](https://cdn.jsdelivr.net/gh/jiangzeyin/Jpom-site/tutorial/images/docker-cli/docker-constole.png)

> docker 相关更多正在努力开发中💪 敬请期待
