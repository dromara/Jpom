<p align="center">
	<a href="https://jpom.io/"  target="_blank">
	    <img src="https://cdn.jsdelivr.net/gh/jiangzeyin/Jpom-site/images/jpom_logo.jpg" width="400" alt="logo">
	</a>
</p>
<p align="center">
	<strong>一款简而轻的低侵入式在线构建、自动部署、日常运维、项目监控软件</strong>
</p>

<p align="center">
    <a target="_blank" href="https://gitee.com/keepbx/Jpom">
        <img src='https://img.shields.io/github/license/jiangzeyin/jpom.svg?style=flat' alt='license'/>
    </a>
    <a target="_blank" href="https://gitee.com/keepbx/Jpom">
        <img src='https://img.shields.io/badge/JDK-1.8+-green.svg' alt='jdk'/>
    </a>
    <a target="_blank" href="https://travis-ci.org/jiangzeyin/Jpom">
        <img src='https://travis-ci.org/jiangzeyin/Jpom.svg?branch=master' alt='travis'/>
    </a>
    <a target="_blank" href="https://www.codacy.com/app/jiangzeyin/Jpom?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=jiangzeyin/Jpom&amp;utm_campaign=Badge_Grade">
        <img src="https://api.codacy.com/project/badge/Grade/9c97dc9925c84404b63e15fefbacc984" alt="codacy"/>
    </a>
    <a target="_blank" href="https://shang.qq.com/wpa/qunwpa?idkey=7be1882a2e2f07cd4af28bbb1f13362af270ba4615f2a6c7aaf9605fc0563d1b">
        <img src='https://img.shields.io/badge/QQ%E7%BE%A4-136715345-yellowgreen.svg' alt='136715345'/>
    </a>
    <a target="_blank" href="https://gitee.com/keepbx/Jpom">
        <img src='https://img.shields.io/badge/Gitee-%E7%A0%81%E4%BA%91-yellow.svg' alt='gitee'/>
    </a>
    <a target="_blank" href="https://github.com/jiangzeyin/Jpom">
        <img src='https://img.shields.io/badge/Github-Github-red.svg' alt='github'/>
    </a>
    <a target="_blank" href="https://gitee.com/keepbx/Jpom">
        <img src='https://gitee.com/keepbx/Jpom/badge/star.svg?theme=dark' alt='gitee star'/>
    </a>
</p>
<p align="center">
	<a href="https://jpom.io/">https://jpom.io/</a> | <a href="https://jpom-site.keepbx.cn/">https://jpom-site.keepbx.cn/</a> | <a href="https://jpom.keepbx.cn/">https://jpom.keepbx.cn/</a>
</p>

 
#### 你为什么需要[Jpom](https://gitee.com/keepbx/Jpom)

> Java 项目在实际部署运维，通用的方法是登录服务器上传新的项目包，执行相应命令管理，如果管理多个项目则重复操作上述步骤

> 此方法不足的是：
> 1. 需要每次登录服务器（专业软件）
> 2. 多个项目有多个管理命令（不易记、易混淆）
> 3. 查看项目运行状态需要再次使用命令
> 4. 同时面对多个运维都需要知道服务器密码（安全性低）
> 5. 集群项目需要挨个操作项目步骤

> 在使用Jpom后：
> 1. 使用浏览器登录方便快捷管理项目
> 2. 界面形式实时查看项目运行状态以及控制台日志
> 3. 运维有对应的账号密码不需要知道服务器密码（并且有操作日志）
> 4. 集群项目使用项目分发一键搞定多机部署
> 5. 项目状态监控异常自动报警
> 6. 在线构建不用手动上传项目包

#### 项目主要功能及特点

1. 创建、修改、删除项目、Jar包管理
2. 实时查看控制台日志、备份日志、删除日志、导出日志
3. cpu、ram 监控、导出堆栈信息、查看项目进程端口、服务器状态监控
4. 多节点管理、多节点自动分发
5. 实时监控项目状态异常自动报警
6. 在线构建项目发布项目一键搞定
7. 多用户管理，用户项目权限独立(上传、删除权限可控制),完善的操作日志
8. 系统路径白名单模式，杜绝用户误操作系统文件
9. 在线管理Nginx配置、ssl证书文件
10. Tomcat状态、文件、war包在线实时管理

### 一键安装

#### 服务端

```
yum install -y wget && wget -O install.sh https://keepbx.gitee.io/jpom/install.sh && bash install.sh Server
```

#### 插件端

```
yum install -y wget && wget -O install.sh https://keepbx.gitee.io/jpom/install.sh && bash install.sh Agent
```

### 下载安装

> [帮助文档](https://jpom-site.keepbx.cn/docs/#/安装使用/开始安装)

1. 下载安装包 [https://gitee.com/keepbx/Jpom/attach_files](https://gitee.com/keepbx/Jpom/attach_files)
2. 解压文件
3. 安装插件端（[流程说明](https://jpom-site.keepbx.cn/docs/#/安装使用/开始安装?id=安装插件端)）
    1. agent-x.x.x-release 目录为插件端的全部安装文件
    2. 上传到对应服务器
    3. 命令运行（Agent.sh、Agent.bat）
4. 安装服务端（[流程说明](https://jpom-site.keepbx.cn/docs/#/安装使用/开始安装?id=安装服务端)）
    1. server-x.x.x-release 目录为服务端的全部安装文件
    2. 上传到对应服务器
    3. 命令运行（Server.sh、Server.bat）

### 编译安装

> [帮助文档](https://jpom-site.keepbx.cn/docs/#/安装使用/开始安装)

1. 访问[Jpom](https://gitee.com/keepbx/Jpom)的码云主页,拉取最新完整代码(建议使用master分支)
2. 进入项目目录执行:`mvn clean package`
3. 安装插件端（[流程说明](https://jpom-site.keepbx.cn/docs/#/安装使用/开始安装?id=安装插件端)）
    1. 查看插件端安装包 modules/agent/target/agent-x.x.x-release
    2. 打包上传服务器运行
    3. 命令运行（Agent.sh、Agent.bat）
4. 安装服务端（[流程说明](https://jpom-site.keepbx.cn/docs/#/安装使用/开始安装?id=安装服务端)）
    1. 查看插件端安装包 modules/server/target/server-x.x.x-release
    2. 打包上传服务器运行
    3. 命令运行（Server.sh、Server.bat）

### 编译运行

1. 访问[Jpom](https://gitee.com/keepbx/Jpom)的码云主页,拉取最新完整代码(建议使用master分支、如果想体验新功能请使用dev分支)
2. 运行插件端   
    1. 运行`io.jpom.JpomAgentApplication`
    2. 注意控制台打印的默认账号密码信息
3. 运行服务端
    1. 运行`io.jpom.JpomServerApplication`
    2. 浏览器访问（如：http://127.0.0.1:2122）

### 管理命令
1. windows中Agent.bat 、Server.bat
```
# 服务端
Server.bat     启动管理面板(按照面板提示输入操作)

# 插件端
Agent.bat     启动管理面板(按照面板提示输入操作)
```
2. linux中Agent.sh 、Server.sh
```
# 服务端
Server.sh start     启动Jpom服务端
Server.sh stop      停止Jpom服务端
Server.sh restart   重启Jpom服务端
Server.sh status    查看Jpom服务端运行状态

# 插件端
Agent.sh start     启动Jpom插件端
Agent.sh stop      停止Jpom插件端
Agent.sh restart   重启Jpom插件端
Agent.sh status    查看Jpom插件端运行状态
```

### Jpom 的参数配置

   在项目运行的根路径下的`extConfig.yml`文件
   1. 插件端示例：[`extConfig.yml`](https://gitee.com/keepbx/Jpom/blob/master/modules/agent/src/main/resources/bin/extConfig.yml) 
   2. 服务端示例：[`extConfig.yml`](https://gitee.com/keepbx/Jpom/blob/master/modules/server/src/main/resources/bin/extConfig.yml)

### 演示项目

   [https://jpom.keepbx.cn](https://jpom.keepbx.cn)
```   
账号：demo
密码：demo123
```    
   > 演示系统有部分功能做了限制，完整功能请自行部署体验
   
   > 如果出现登录不上，请联系我们，联系方式在最底部
    
   1. [Jboot案例代码](https://gitee.com/keepbx/Jpom-demo-case/tree/master/jboot-test)
   2. [SpringBoot案例代码(ClassPath)](https://gitee.com/keepbx/Jpom-demo-case/tree/master/springboot-test)
   3. [SpringBoot案例代码(Jar)](https://gitee.com/keepbx/Jpom-demo-case/tree/master/springboot-test-jar)

### 常见问题、操作说明

[https://jpom-site.keepbx.cn/docs/](https://jpom-site.keepbx.cn/docs/) 

[https://jpom-site.keepbx.cn/docs/#/FQA/FQA](https://jpom-site.keepbx.cn/docs/#/FQA/FQA)

[Jpom 插件开发](https://gitee.com/keepbx/Jpom-Plugin)

### 交流讨论 、提供bug反馈或建议

  1. QQ群：[136715345](https://shang.qq.com/wpa/qunwpa?idkey=93ff8d8a37a436b752fe38d32075bb1b32a8e0b3d3ff19d0b541ca840433f561)
  
  2. 微信公众号：[CodeGzh](http://cdn.jsdelivr.net/gh/jiangzeyin/Jpom-site/docs/images/CodeGzh-QrCode.jpg)
  
  3. 码云： [issues](https://gitee.com/keepbx/Jpom/issues)
