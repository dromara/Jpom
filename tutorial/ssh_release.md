# Jpom 快速发布项目到服务器中

> 从零开始如何使用 Jpom 中的构建功能快速实现将项目从仓库中构建并发布到服务器中并启动项目

## 实操步骤

### 第一步 （安装服务端）

> 使用一键安装的命令：
> 
> yum install -y wget && wget -O install.sh https://dromara.gitee.io/jpom/docs/install.sh && bash install.sh Server jdk+mvn

### 第二步（初始化系统管理员账号密码）

> 1. Jpom 系统中超级管理员信息

### 第三步（配置 ssh 信息）

> 1. 服务器IP
> 2. ssh 端口
> 3. 登录的账号信息（账号、密码）

### 第四步（配置构建信息） 

> 1. 构建的项目（仓库）信息
> 2. 项目的构建命令（依赖的构建环境：maven、node、python、php）
>  mvn clean package  
> 3. 构建产物的目录（项目中的相对路径）
>  target 
> 4. 项目将发布到哪个目录
> 5. 如果停止、启动项目（发布命令）

> 注意：构建命令、发布命令都需要使用非阻塞命令