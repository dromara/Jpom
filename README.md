# Jpom(Java Project Online Management)Java项目在线管理

<a target="_blank" href="https://gitee.com/keepbx/Jpom">
    <img src='https://img.shields.io/github/license/jiangzeyin/jpom.svg?style=flat' alt='license'></img>
</a>
<a target="_blank" href="https://gitee.com/keepbx/Jpom">
    <img src='https://img.shields.io/badge/JDK-1.8+-green.svg' alt='jdk'></img>
</a>
<a target="_blank" href="https://travis-ci.org/jiangzeyin/Jpom">
    <img src='https://travis-ci.org/jiangzeyin/Jpom.svg?branch=master' alt='travis'></img>
</a>
<a target="_blank" href="https://www.codacy.com/app/jiangzeyin/Jpom?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=jiangzeyin/Jpom&amp;utm_campaign=Badge_Grade">
    <img src="https://api.codacy.com/project/badge/Grade/9c97dc9925c84404b63e15fefbacc984"></img>
</a>
<a target="_blank" href="https://shang.qq.com/wpa/qunwpa?idkey=7be1882a2e2f07cd4af28bbb1f13362af270ba4615f2a6c7aaf9605fc0563d1b">
    <img src='https://img.shields.io/badge/QQ%E7%BE%A4-136715345-yellowgreen.svg' alt='136715345'></img>
</a>
<a target="_blank" href="https://gitee.com/keepbx/Jpom">
    <img src='https://img.shields.io/badge/Gitee-%E7%A0%81%E4%BA%91-yellow.svg' alt='gitee'></img>
</a>
<a target="_blank" href="https://github.com/jiangzeyin/Jpom">
    <img src='https://img.shields.io/badge/Github-Github-red.svg' alt='github'></img>
</a>
<a target="_blank" href="https://gitee.com/keepbx/Jpom">
    <img src='https://gitee.com/keepbx/Jpom/badge/star.svg?theme=dark' alt='gitee star'></img>
</a>

#### 你为什么需要[Jpom](https://gitee.com/keepbx/Jpom)

> SpringBoot、Jboot等框架开发的项目通常是以Jar的方式在后台运行的，如果只有一两个项目，管理起来不是太麻烦，但是当项目多了以后，管理起来就不是那么方便了，当项目出现问题时，能够通过Jpom即时排查问题，问题解决后还可以直接上传修改后的Jar，项目的堆栈信息，服务器CPU、内存使用情况一目了然，不必再登录服务器管理。

> 当多个项目运行在同一台服务器时，运维人员通常也不只一个，如果每个人都登录服务器管理项目，难免会造成一些不必要的麻烦，甚至给服务器的安全性带来问题（服务器密码知道的人越多，越容易泄露），因为不需要登录服务器管理项目，维护人员不需要知道服务器的登录密码，只需要有Jpom的账号就行，Jpom本身可以通过权限管理，给不同用户不同的权限，这样也使得项目的稳定性得到提升。

> Jpom可以在Linux和Windows服务器上运行，并且Jpom采用多节点模式，随时开启关闭节点服务器，节点分发减少运维人员上传、修改操作

### Jpom 目标

> 一款简而轻的低侵入式Java运维、监控软件

#### 项目主要功能及特点

1. 创建、修改、删除项目、Jar包管理
2. 实时查看控制台日志、备份日志、删除日志、导出日志
3. cpu、ram 监控、导出堆栈信息、查看项目进程端口、服务器状态监控
4. 多节点管理、多节点自动分发
5. 多用户管理，用户项目权限独立(上传、删除权限可控制),完善的操作日志
6. 系统路径[白名单模式](/doc/whitelist.md)，杜绝用户误操作系统文件
7. [在线管理Nginx配置](/doc/nginx-manager.md)、ssl证书文件
8. [阿里云 CodePipeline + Oss在线管理](/doc/CodePipeline-Oss.md)

#### 基本操作演示

[![jpom-server](http://s.keepbx.cn/jpom-server.gif)](http://s.keepbx.cn/jpom-server.gif)

[![jpom-node-agent](http://s.keepbx.cn/jpom-node-agent.gif)](http://s.keepbx.cn/jpom-node-agent.gif)

### 下载安装

> [帮助文档](/doc/install.md)

1. 下载安装包 [https://gitee.com/keepbx/Jpom/attach_files](https://gitee.com/keepbx/Jpom/attach_files)
2. 解压文件
3. 安装插件端（[流程说明](/doc/install.md#安装插件端)）
    1. agent-x.x.x-release 目录为插件端的全部安装文件
    2. 上传到对应服务器
    3. 命令运行（Agent.sh、Agent.bat）
4. 安装服务端（[流程说明](/doc/install.md#安装服务端)）
    1. server-x.x.x-release 目录为服务端的全部安装文件
    2. 上传到对应服务器
    3. 命令运行（Server.sh、Server.bat）

### 编译安装

> [帮助文档](/doc/install.md)

1. 访问[Jpom](https://gitee.com/keepbx/Jpom)的码云主页,拉取最新完整代码(建议使用master分支)
2. 进入项目目录执行:`mvn clean package`
3. 安装插件端（[流程说明](/doc/install.md#安装插件端)）
    1. 查看插件端安装包 modules/agent/target/agent-x.x.x-release
    2. 打包上传服务器运行
    3. 命令运行（Agent.sh、Agent.bat）
4. 安装服务端（[流程说明](/doc/install.md#安装服务端)）
    1. 查看插件端安装包 modules/server/target/server-x.x.x-release
    2. 打包上传服务器运行
    3. 命令运行（Server.sh、Server.bat）

### 编译运行

1. 访问[Jpom](https://gitee.com/keepbx/Jpom)的码云主页,拉取最新完整代码(建议使用master分支)
2. 运行插件端   
    1. 运行`cn.keepbx.jpom.JpomAgentApplication`
    2. 注意控制台打印的默认账号密码信息
3. 运行服务端
    1. 运行`cn.keepbx.jpom.JpomServerApplication`
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

### 视频教程

[教程列表](https://mp.weixin.qq.com/mp/homepage?__biz=Mzg2OTEzMDIwNg==&hid=5&sn=3712b3edbe0af22c88ac3178a840a799)

[直播回放](https://yq.aliyun.com/live/970)

### Jpom 的参数配置

   在项目运行的根路径下的`extConfig.yml`文件
   1. 插件端示例：[`extConfig.yml`](/modules/agent/src/main/resources/bin/extConfig.yml) 
   2. 服务端示例：[`extConfig.yml`](/modules/server/src/main/resources/bin/extConfig.yml)

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

| 必看 | 选看1 | 选看2 |
| -- | -- | -- | 
| [安装文档>>](/doc/install.md)       |  [常见问题>>](/FQA.md)   |[更新日志>>](/CHANGELOG.md)   |
| [启动失败问题>>](/doc/startFail.md) | [阿里云Oss配置>>](/doc/CodePipeline-Oss.md)|  [自定义公共Js>>](/doc/addScript.md) |
| [项目属性说明>>](/doc/project.md)   | [删除项目说明>>](/doc/deleteProject.md) | |
| [白名单规则>>](/doc/whitelist.md)   | [Nginx管理规则>>](/doc/nginx-manager.md) | |
| [用户角色说明>>](/doc/userRole.md)  | [推荐Nginx配置>>](/doc/nginx-config.md)  | |
| [部署说明](/doc/deploy-doc.md)      | [开发计划>>](/PLANS.md) | |


### 交流讨论 、提供bug反馈或建议

  1. QQ群：[136715345](https://shang.qq.com/wpa/qunwpa?idkey=93ff8d8a37a436b752fe38d32075bb1b32a8e0b3d3ff19d0b541ca840433f561)
  
  2. 微信公众号：[CodeGzh](/doc/CodeGzh-QrCode.jpg)
  
  3. 码云： [issues](https://gitee.com/keepbx/Jpom/issues)
  