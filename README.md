# Java jar包 线上项目管理

> 在linux 中管理jar包运行，如SpringBoot 项目如果是打包为Jar那么我们一般是使用shell 命令来管理, 
> 如果有多个项目管理起来显得比较麻烦。
> 如果新增、更新、删除项目都不是很方便。
> 再或者想不使用电脑控制重启、关闭、开启项目。直接使用shell 登录服务器管理显然不是最简单的方法
> 如果休息日在朋友家玩，公司领导联系说需要控制一下某个项目，此时你身边有电脑，但是没有专业的管理服务器的软件。


###  在这里[boot-online](https://gitee.com/jiangzeyin/boot-online) 项目将如上问题统一解决

> boot-online采用SpringBoot开发web项目(前台采用layui)+websocket来统一管理，只要电脑能使用浏览器就ok

#### 项目主要功能
1. 创建、修改、删除项目
2. 实时查看控制台
3. jar包文件夹管理
4. cpu、ram 监控
5. 导出堆栈信息
6. 阿里云 CodePipeline + Oss在线管理
7. 多用户管理
8. 此项目完全不依赖数据库，随装随用
9. 整个服务器top监听


#### 项目截图

1. 创建项目
![创建项目](https://gitee.com/jiangzeyin/boot-online/raw/master/doc/images/create.png)

2. 修改、删除项目
![修改、删除项目](https://gitee.com/jiangzeyin/boot-online/raw/master/doc/images/edit_del.png)

3. 文件管理
![文件管理](https://gitee.com/jiangzeyin/boot-online/raw/master/doc/images/file.png)

4. 项目列表
![项目列表](https://gitee.com/jiangzeyin/boot-online/raw/master/doc/images/list.png)

5. 监控cpu 内存
![ 监控cpu 内存](https://gitee.com/jiangzeyin/boot-online/raw/master/doc/images/cup_ram.png)

6. 控制台日志实时查看
![控制台日志实时查看](https://gitee.com/jiangzeyin/boot-online/raw/master/doc/images/console.png)

7. 用户管理
![用户管理](https://gitee.com/jiangzeyin/boot-online/raw/master/doc/images/user_list.png)

8. top监听
![top监听](https://gitee.com/jiangzeyin/boot-online/raw/master/doc/images/top.png)


### 编译安装

访问[Boot-online](https://gitee.com/jiangzeyin/boot-online)的码云主页,拉取最新完整代码：

1. 端口修改确认（默认：2122）
2. 项目运行数据目录修改确认（默认：/boot-online/data/）数据路径存储用户信息和项目配置信息，基础格查看项目目录[/bin/data]
3. 项目运行命令目录修改确认（默认：/boot-online/command/）完整命令查看项目目录[/bin/command] 在部署是需要完整copy此目录到配置的命令目录中 
4. 项目运行日志目录确认（默认：/boot-online/log/）

然后进入项目目录执行：

    mvn package

然后在目录中查看 target/boot-online-1.0-lib 

### 贡献人员
    
1.  [F7575](https://gitee.com/F7575)
2.  [bwcx_jzy](https://gitee.com/jiangzeyin)
3.  [arno](https://gitee.com/arnohand)

### 感谢
 boot-online使用以下开源项目
  - [Spring Boot](https://github.com/spring-projects/spring-boot)：核心框架
  - [Fast-Boot](https://gitee.com/jiangzeyin/common-parent)：针对SpringBoot 封装的一系列的快捷包 提供公共的Controller、自动化拦截器、启动加载资源接口、线程池管理
  - [alibaba/fastjson](https://github.com/alibaba/fastjson)：用于Java的快速JSON解析器/生成器
  - [Hutool](https://gitee.com/looly/hutool)：一个Java工具包，也只是一个工具包，它帮助我们简化每一行代码，减少每一个方法，让Java语言也可以“甜甜的”
  - [Layui](https://gitee.com/sentsin/layui)：前端UI框架
  

### 提供bug反馈或建议

- [码云](https://gitee.com/jiangzeyin/boot-online/issues)

### 贡献代码
  欢迎任何人为Boot-online添砖加瓦，贡献代码
