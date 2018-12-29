# Spring Boot 线上项目管理

> 在linux 中管理SpringBoot 项目如果是打包为Jar那么我们一般是使用shell 命令来管理, 
> 如果有多个项目管理起来显得比较麻烦。
> 如果新增、更新、删除项目都不是很方便。
> 再或者想不使用电脑控制重启、关闭、开启项目。直接使用shell 登录服务器管理显然不是最简单的方法
> 如果休息日在朋友家玩，公司领导联系说需要控制一下某个项目，这是你身边有电脑，但是没有专业的管理服务器的软件。


###  在这里boot-online 项目将如上问题统一解决

> 这里就采用SpringBoot开发web项目(前台采用layui)+websocket来统一管理，只要电脑能使用浏览器就ok

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
![创建项目](/doc/images/create.png)

2. 修改、删除项目
![修改、删除项目](/doc/images/edit_del.png)

3. 文件管理
![文件管理](/doc/images/file.png)

4. 项目列表
![项目列表](/doc/images/list.png)

5. 监控cpu 内存
![ 监控cpu 内存](/doc/images/cup_ram.png)

6. 控制台日志实时查看
![控制台日志实时查看](/doc/images/console.png)

7. 用户管理
![用户管理](/doc/images/user_list.png)

8. top监听
![top监听](/doc/images/top.png)

贡献人员：
    
    F7575
    bwcx_jzy
    arno
    