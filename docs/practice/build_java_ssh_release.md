![](https://cdn.jsdelivr.net/gh/dromara/Jpom@docs/images/jpom_logo.png)

##  `简而轻的低侵入式在线构建、自动部署、日常运维、项目监控软件`

## 前言

> 本文主要介绍：如何从零开始使用 Jpom 中的构建功能快速实现将项目从仓库中构建并发布到服务器中并启动项目。

> 「那么如何从零开始使用 Jpom 中的构建功能(`不安装插件端 Agent`)快速实现将项目从仓库中构建并发布到服务器中启动项目呢？」下面我们一起具体来看看👇👇

> 本文基于 2.6.1 版本讲解

# 操作步骤

## 第一步 安装服务端

> 使用一键安装的命令安装服务端

```
# 这里我们选择快速安装 jdk 和 maven ,实际中请根据自己情况选择
yum install -y wget && wget -O install.sh https://dromara.gitee.io/jpom/docs/install.sh && bash install.sh Server jdk+mvn
```

#### 执行命令后控制台输出如下

![](https://cdn.jsdelivr.net/gh/dromara/Jpom@docs/tutorial/images/ssh_release2/setp1.1.png)

![](https://cdn.jsdelivr.net/gh/dromara/Jpom@docs/tutorial/images/ssh_release2/setp1.2.png)

![](https://cdn.jsdelivr.net/gh/dromara/Jpom@docs/tutorial/images/ssh_release2/setp1.3.png)


#### 服务端访问地址

>> `http://IP:2122`  IP则为部署 Jpom 服务端的IP地址，也可以使用 nginx 代理访问

## 第二步 初始化系统

> 为 Jpom 系统设备一个系统管理员账号密码，方便日后登录使用

1. `需要设置 Jpom 系统中的管理员账号密码`
2. `注意超级管理的密码强度`

![](https://cdn.jsdelivr.net/gh/dromara/Jpom@docs/tutorial/images/ssh_release2/setp2.png)

## 第三步 配置 ssh 信息

> 配置 ssh 信息、便于构建后发布到对应服务器中

1. 服务器IP
2. ssh 端口
3. 登录的账号信息（账号、密码）
4. 文件目录

![](https://cdn.jsdelivr.net/gh/dromara/Jpom@docs/tutorial/images/ssh_release2/setp3.png)

#### 备注：`文件目录为给对应 ssh 授权部分目录可以在 Jpom 中使用，一般用于构建发布目录、ssh 文件管理`

## 第四步 配置构建信息

> 配置项目查看构建信息，用于管理项目发布流程

![](https://cdn.jsdelivr.net/gh/dromara/Jpom@docs/tutorial/images/ssh_release2/setp4.png)

1. 构建的项目（仓库）信息
2. 项目的构建命令（依赖的构建环境：maven、vue、python、php）
   1. mvn clean package
   2. npm run build
   3. ......
3. 构建产物的目录（项目中的相对路径）
   1. java 项目一般位于当前项目 target 目录下的莫个文件
   2. vue 项目一般位于当前项目 dist 文件夹
4. 发布目录：项目将发布服务器到哪个目录（`需要已经配置到 ssh 文件目录中`）
5. 发布命令：执行发布后执行的命令，如：配置停止、启动项目

#### Java 项目发布命令示例
```
Tag="jpom-test-jar"

# 检查进程状态
pid=$(ps -ef | grep -v 'grep' | egrep $Tag| awk '{printf $2 " "}')
if [ "$pid" != "" ]; then      
  echo -n "boot ( pid $pid) is running" 
  echo 
  echo -n $"Shutting down boot: "
  pid=$(ps -ef | grep -v 'grep' | egrep $Tag| awk '{printf $2 " "}')
  if [ "$pid" != "" ]; then
    echo "kill boot process"
    # kill  "$pid"
    kill -9 "$pid"
  fi
else 
   echo "boot is stopped" 
fi
# 启动项目
nohup java -Dappliction=$Tag -jar /home/java/test/springboot-test-jar-0.0.1-SNAPSHOT.jar > /dev/null 2>&1 &
```

#### 注意

> 构建命令、发布命令都需要使用非阻塞命令

# Jpom 链接

官网：`https://jpom.io`

Gitee: `https://gitee.com/dromara/Jpom`

Github: `https://github.com/dromara/Jpom`

常见问题：`https://jpom-docs.keepbx.cn/docs/#/FQA/FQA`

 ![微信群：jpom66 (请备注 Jpom)](https://cdn.jsdelivr.net/gh/dromara/Jpom@docs/images/wx_qrcode.jpg)