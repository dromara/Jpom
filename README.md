# Jpom(Java Project Online Management)Java项目在线管理

> 在服务器中管理jar包运行，如SpringBoot、Jboot等项目如果是打包为Jar那么我们一般是使用shell、bat 命令来管理, 
> 在单服务器中多项目新增、更新、删除、排查日志等操作如此都不是很方便。
> 再者想不使用电脑控制重启、关闭、开启项目。直接使用管理员软件登录服务器管理显然不是最简单的方法
> 甚至在没有专业的服务器管理软件电脑前Jpom都是你们选择


###  在这里[Jpom](https://gitee.com/keepbx/Jpom) 项目将如上问题统一解决

![GitHub](https://img.shields.io/github/license/jiangzeyin/jpom.svg?style=flat)
![jdk](https://img.shields.io/badge/JDK-1.8+-green.svg)
[![travis](https://travis-ci.org/jiangzeyin/Jpom.svg?branch=master)](https://travis-ci.org/jiangzeyin/Jpom)
[![QQ群](https://img.shields.io/badge/QQ%E7%BE%A4-136715345-yellowgreen.svg)](https://shang.qq.com/wpa/qunwpa?idkey=7be1882a2e2f07cd4af28bbb1f13362af270ba4615f2a6c7aaf9605fc0563d1b)
[![码云](https://img.shields.io/badge/Gitee-%E7%A0%81%E4%BA%91-yellow.svg)](https://gitee.com/keepbx/Jpom)
[![Github](https://img.shields.io/badge/Github-Github-red.svg)](https://github.com/jiangzeyin/Jpom)

> Jpom采用SpringBoot开发web项目(前台采用layui)+websocket来统一管理，只要能使用浏览器就ok，同时Jpom可以在linux和windows服务器上运行

![logo](/doc/logo.png)

#### 你为什么需要Jpom

> SpringBoot、Jboot等框架开发的项目通常是以Jar的方式在后台运行的，如果只有一两个项目，管理起来不是太麻烦，但是当项目多了以后，管理起来就不是那么方便了，当项目出现问题时，能够通过Jpom即时排查问题，问题解决后还可以直接上传修改后的Jar，项目的堆栈信息，服务器CPU、内存使用情况一目了然，不必再登录服务器管理。

> 当多个项目运行在同一台服务器时，运维人员通常也不只一个，如果每个人都登录服务器管理项目，难免会造成一些不必要的麻烦，甚至给服务器的安全性带来问题（服务器密码知道的人越多，越容易泄露），因为不需要登录服务器管理项目，维护人员不需要知道服务器的登录密码，只需要有Jpom的账号就行，Jpom本身可以通过权限管理，给不同用户不同的权限，这样也使得项目的稳定性得到提升。

### Jpom 目标

> 一款低侵入式Java运维、监控软件,要做到简而轻、低侵入

#### 项目主要功能及特点

1. 创建、修改、删除项目、Jar包管理
2. 实时查看控制台日志
3. cpu、ram 监控、导出堆栈信息、查看项目进程端口、服务器状态监控
4. [阿里云 CodePipeline + Oss在线管理](/doc/CodePipeline-Oss.md)
5. 多用户管理，用户项目权限独立(上传、删除权限可控制)
6. Jpom完全不依赖数据库，随装随用
7. 项目路径白名单模式，杜绝用户误操作系统文件


#### 项目操作演示

![Jpom](http://s.keepbx.cn/jpom-demo-gif.gif)

### 下载安装

1. 下载安装包 [https://gitee.com/keepbx/Jpom/attach_files](https://gitee.com/keepbx/Jpom/attach_files)
2. 上传到服务解压到根目录
3. 执行目录下[Jpom.sh](/script/Jpom.sh)文件（记得给命令文件添加权限奥[*chmod  755 /jpom/Jpom.sh*]）
    如
```
/jpom/Jpom.sh start
```
4. 如有需要修改项目端口请查看 Jpom.sh
5. windows 下则执行Jpom.bat 命令即可

### 编译安装

1. 访问[Jpom](https://gitee.com/keepbx/Jpom)的码云主页,拉取最新完整代码：
2. 进入项目目录执行，然后在目录中查看 target/jpom-x.x.x-release
```
mvn clean package
```
3. 打包上传服务器运行
4. 命令运行

### 编译运行

1. 访问[Jpom](https://gitee.com/keepbx/Jpom)的码云主页,拉取最新完整代码：
2. 运行`cn.keepbx.jpom.JpomApplication`
3. 浏览器访问（如：http://127.0.0.1:2122）

### 管理命令
1. windows中Jpom.bat
```
Jpom.bat     启动管理面板(按照面板提示输入操作)
```
2. linux中Jpom.sh
```
Jpom.sh start     启动Jpom 
Jpom.sh stop      停止Jpom
Jpom.sh restart   重启Jpom
Jpom.sh status    查看Jpom运行状态
```

### 视频教程

[教程列表](https://mp.weixin.qq.com/mp/homepage?__biz=Mzg2OTEzMDIwNg==&hid=5&sn=3712b3edbe0af22c88ac3178a840a799)

[直播回放](https://yq.aliyun.com/live/970)

### Jpom 的参数配置

   在项目运行的根路径下创建[`extConfig.yml`](/src/main/resources/bin/extConfig.yml) 具体的配置说明和示例请查看：[`extConfig.yml`](/src/main/resources/bin/extConfig.yml) 

### 演示项目

   [https://jpom.keepbx.cn](https://jpom.keepbx.cn)
```   
账号：demo
密码：demo123
```    
   > 演示系统有部分功能做了限制，完整功能请自行部署体验
   
   > 如果出现登录不上，请联系我们重置密码，联系方式在最底部
    
   1. [Jboot案例代码](https://gitee.com/keepbx/Jpom-demo-case/tree/master/jboot-test)
   2. [SpringBoot案例代码(ClassPath)](https://gitee.com/keepbx/Jpom-demo-case/tree/master/springboot-test)
   3. [SpringBoot案例代码(Jar)](https://gitee.com/keepbx/Jpom-demo-case/tree/master/springboot-test-jar)

### 常见问题

[常见问题>>](/FQA.md) 
 
[启动失败问题>>](https://gitee.com/keepbx/Jpom/wikis/pages?sort_id=1395625&doc_id=264493)

[安装说明>>](https://gitee.com/keepbx/Jpom/wikis/pages?sort_id=1395348&doc_id=264493)

[更新日志>>](/CHANGELOG.md)

[开发计划>>](/PLANS.md)

[用户角色说明>>](/doc/userRole.md#用户权限说明)

[阿里云Oss配置>>](/doc/CodePipeline-Oss.md)


### 感谢
 Jpom使用以下开源项目
  - [Spring Boot](https://github.com/spring-projects/spring-boot)：核心框架
  - [Fast-Boot](https://gitee.com/keepbx/common-parent)：针对SpringBoot 封装的一系列的快捷包 提供公共的Controller、自动化拦截器、启动加载资源接口、线程池管理
  - [Fastjson](https://github.com/alibaba/fastjson)：用于Java的快速JSON解析器/生成器
  - [Hutool](https://gitee.com/loolly/hutool)：一个Java工具包，也只是一个工具包，它帮助我们简化每一行代码，减少每一个方法，让Java语言也可以“甜甜的”
  - [Layui](https://gitee.com/sentsin/layui)：前端UI框架
  

### 提供bug反馈或建议

- [码云](https://gitee.com/keepbx/Jpom/issues)

### 贡献代码
  欢迎任何人为Jpom添砖加瓦，贡献代码
  
### 交流讨论

  1. QQ群：[136715345](https://shang.qq.com/wpa/qunwpa?idkey=93ff8d8a37a436b752fe38d32075bb1b32a8e0b3d3ff19d0b541ca840433f561)
  
  2. 微信公众号：[CodeGzh](/doc/CodeGzh-QrCode.jpg)
  