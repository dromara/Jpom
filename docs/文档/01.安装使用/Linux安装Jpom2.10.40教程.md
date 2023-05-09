# Linux安装Jpom2.10.40教程

## 一、前言

> 本文主要介绍📣
> 如何从零开始采用离线的方式安装 Jpom 服务端+插件端
>
> 本文中服务端和插件端是安装在同一个服务器中的，实际操作时根据业务情况来安装

> 注意📣
>
> 1. 本文基于 2.10. 版本讲解,系统为 centos7
> 2. 本文默认使用的H2数据库，可更换成MySQL数据库（自行选择）


### 1、需要准备的环境

1. 一台 centos7 的服务器
2. 服务器中已经安装 jdk（建议 HotSpot JDK 1.8）
3. 服务器中已经安装 MySQL5.7.38数据库


### 2、获取安装包

Jpom 目前安装包都会发布到：[https://jpom.top/pages/all-downloads/](https://jpom.top/pages/all-downloads/) 提供大家下载

发布的包一般包含如下文件：

- jpom-x.x.x.zip
- server-x.x.x-release.tar.gz
- server-x.x.x-release.tar.gz.sha1（sha1sum）
- server-x.x.x-release.zip
- server-x.x.x-release.zip.sha1（sha1sum）
- agent-x.x.x-release.tar.gz
- agent-x.x.x-release.tar.gz.sha1（sha1sum）
- agent-x.x.x-release.zip
- agent-x.x.x-release.zip.sha1（sha1sum）


文件大致有：`zip 包`、`tar.gz 包`、`sha1sum` 校验文件

`jpom-x.x.x.zip` 文件为：server-x.x.x-release.zip、agent-x.x.x-release.zip 合并压缩包，一般用于一并下载服务端和插件端安装包。

本文使用 `tar.gz 包` 来安装：

1. server-x.x.x-release.tar.gz
2. agent-x.x.x-release.tar.gz

### 3、上传安装包到服务器中

![image-20230508100531987](C:\Users\13233\AppData\Roaming\Typora\typora-user-images\image-20230508100531987.png)

## 二、安装JDK1.8.211

```bash
# 1、首先检查Linux服务器是否有OpenJDK
rpm -qa|grep java
# 2、删除openJDK版本
复制查出来的文件进行删除
rpm -e --nodeps
# 3、将Java压缩包解压到software目录下
cd /opt/package
tar -zxvf jdk-8u211-linux-x64.tar.gz -C /usr/local/
# 4、修改jdk目录名称
mv /usr/local/jdk1.8.0_211 /usr/local/java
# 5、配置环境变量
# 修改配置文件
vim /etc/profile
# 在最后面加入
export JAVA_HOME=/usr/local/java
export PATH=$PATH:$JAVA_HOME/bin
# 使配置文件立即生效
source /etc/profile
```

## 三、安装Jpom插件端

```bash
# 进到安装包存放目录
cd /opt/package/Jpom/
# 创建插件端的安装目录
mkdir -p /usr/local/jpom-agent/
# 解压安装包(注意：需要切回到安装包存放路径奥)
tar -zxf agent-2.10.40-release.tar.gz -C /usr/local/jpom-agent/
# 切换到安装目录
cd /usr/local/jpom-agent/
```

![image-20230508132110178](C:\Users\13233\AppData\Roaming\Typora\typora-user-images\image-20230508132110178.png)

````bash
# 启动插件端
sh ./bin/Agent.sh start
````

![image-20230508132303096](C:\Users\13233\AppData\Roaming\Typora\typora-user-images\image-20230508132303096.png)

> 等待执行结果，查看控制台输出插件端账号密码📣
>
> 1. Automatically generate authorized account:jpomAgent  password:x17kmn9s81
> 2. Current node address => http://1.0.0.11:2123
> 3. 插件端默认端口为：2123

## 四、安装Jpom服务端

### 1、部署Jpom服务端

```bash
# 进到安装包存放目录
cd /opt/package/Jpom/
# 创建服务端的安装目录
mkdir -p /usr/local/jpom-server/
# 解压安装包(注意：需要切回到安装包存放路径奥)
tar -zxf server-2.10.40-release.tar.gz -C /usr/local/jpom-server/
# 切换到安装目录
cd /usr/local/jpom-server/
```

![image-20230508104632580](C:\Users\13233\AppData\Roaming\Typora\typora-user-images\image-20230508104632580.png)

```bash
# 启动服务端
sh ./bin/Server.sh start
```

![image-20230508105438971](C:\Users\13233\AppData\Roaming\Typora\typora-user-images\image-20230508105438971.png)

注意：如果服务器中没有安装 docker 控制台中可以输出：`There is no docker service local java.io.IOException: xxxx` 相关错误忽略即可，因为服务端默认会自动添加本机 docker 服务到 Jpom 中方便后续使用。


### 2、访问Jpom服务端

> 等待执行结果，查看控制台输出服务端信息📣
>
> 1.  Server Successfully started,Can use happily => http://1.0.0.11:2122 【The current address is for reference only】
> 2. 服务端默认端口为：2122

### 3、初始化服务端


#### 1、添加超级管理账号

> 📣添加一个超级管理员账号，请妥善保管此账号同时请设置安全度较强的密码

![image-20230508130659479](C:\Users\13233\AppData\Roaming\Typora\typora-user-images\image-20230508130659479.png)

#### 2、开启账号 MFA

> 📣为了系统安全，强烈建议超级管理员账号开启 MFA 两步验证
>

![image-20230508130814961](C:\Users\13233\AppData\Roaming\Typora\typora-user-images\image-20230508130814961.png)

> 温馨提示📣
>
> Jpom 默认会自动检查当前服务器是否存在未被绑定的插件端，如果存在插件端未添加到服务端中，程序将自动添加插件端到节点中（工作空间为默认）
>
> 此方式仅在节点列表为空并且在启动服务端时候存在运行中的插件端才生效

![image-20230508131253552](C:\Users\13233\AppData\Roaming\Typora\typora-user-images\image-20230508131253552.png)

## 五、数据库切换成MySQL（非必须）

> 温馨提示📣
>
> Jpom 服务端的数据默认存储到 H2database 中
>
> 部分用户有需求期望数据存储到 mysql 中
>
> 使用 mysql 模式，系统管理中的数据库备份功能将不可用，数据的备份管理需要自行维护

1、在登录后Jpom后【系统管理=>服务端配置=>系统配置】修改db模块

![image-20230508141005140](C:\Users\13233\AppData\Roaming\Typora\typora-user-images\image-20230508141005140.png)

配置文件：**conf/application.yml**

- 修改jpom.db.mode为 MYSQL

- 修改jpom.db.url为你mysql的jdbc地址如下（根据实际内容修改）

- jdbc:mysql://127.0.0.1:3306/jpom?useUnicode=true&characterEncoding=UTF-8&useSSL=false

- 修改 jpom.db.user-name 为对应 mysql 账户
- 修改 jpom.db.user-pwd 为对应 mysql 密码

```bash
  db: 
    # 数据库默认 支持 ：H2、MYSQL
    mode: MYSQL
    # 日志存储条数，将自动清理旧数据,配置小于等于零则不清理
    log-storage-count: 10000
    # H2 模式无需配置 mysql 配置 jdbc 地址
    url: jdbc:mysql://1.0.0.11:6603/jpom?useUnicode=true&characterEncoding=UTF-8&useSSL=false
    # 数据库账号 默认 jpom
    user-name: root
    # 数据库密码 默认 jpom 如果自行配置请保证密码强度
    user-pwd: root
    # h2 数据库缓存大小  kilobyte 1KB 1,024 megabyte 1MB 1,048,576
    cache-size: 50MB
    # 自动备份间隔天数 小于等于 0 不自动备份
    auto-backup-interval-day: 1
    # 自动备份保留天数 小于等于 0，不自动删除自动备份数据
    auto-backup-reserve-day: 5
    # 数据库连接池相关配置
    max-active: 500
    initial-size: 10
    max-wait: 10
    min-idle: 1
    # 控制台是否打印 sql 信息
    show-sql: false
```

2、修改后保存并重启即可。

## 愉快的使用

这样就完成 Jpom 的安装流程了，可以根据需要使用其他功能。

