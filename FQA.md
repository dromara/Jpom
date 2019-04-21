## 常见问题

   ### 如何修改程序运行端口
   
    修改管理程序命令文件中 --server.port=2122
        
   ### 如何修改程序日志路径
   
    修改管理程序命令文件中 --jpom.log=/jpom/log/
        
   ### 如何修改会话超时时长
        
    在管理程序命令文件中 ARGS 变量添加 --tomcat.sessionTimeOut=1800
    
   ### 如何修改Jpom数据存储目录
       
    修改外部配置文件中的 jpom.path 属性
      
        
   ### 在linux 系统中执行Jpom.sh 提示
   
   ![jpom](/doc/error/ff-unix.png)
   
   ![jpom](/doc/error/command-not-found.png)
    
   执行如下命令：(https://blog.csdn.net/perter_liao/article/details/76757605)
   
    1.编辑文件
      #vim filename（文件名）
      
    2.进入末行模式（按esc键）
    
    3.设置文件格式
     ：set fileformat=unix
     
    4.保存退出
     ：wq
     
    5.#sh filename
      OK!
      
   
   ### 启动Jpom提示
   ![jpom](/doc/error/jdk-error.png)
   
   Jpom需要Jdk1.8以上，请检查jdk版本
   
   ###  Jpom使用Nginx代理推荐配置

   [查看>>](/doc/nginx-config.md)

   ### 启动提示数据目录权限不足
   
   > jpom 数据目录权限不足...
       
   请检查当前用户是否拥有对应目录的读写权限
   [https://gitee.com/keepbx/Jpom/wikis/pages?sort_id=1395625&doc_id=264493](https://gitee.com/keepbx/Jpom/wikis/pages?sort_id=1395625&doc_id=264493)
   
   ### 启动提示JDK没有找到tools.jar
   
   [https://gitee.com/keepbx/Jpom/wikis/pages?sort_id=1398788&doc_id=264493](https://gitee.com/keepbx/Jpom/wikis/pages?sort_id=1398788&doc_id=264493)
   
   
   
   ### Jpom添加项目、启动、查看项目报错
   1.运行的java进程有32位和64位的
   
   ![jpom](/doc/error/32bit.jpg)
   
   2.抛出异常Unable to open socket file: target process not responding or HotSpot VM not load。
   
   ![jpom](/doc/error/can't-open-socket-file.jpg)
   
   针对以上两个问题，Jpom目前采用略过这些进程的解决办法，请更新到2.3.1以上。 
   
   
   ### SpringBoot 读取jar包同路径配置文件失败
   
   由于目前Jpom 启动下面的方式限制SpringBoot未能读取到对应配置文件，目前解决方案是在配置项目的args参数解决
   
   > --spring.config.location=D:\config\config.properties  
   
   
   ### windows 环境 com.sun.tools.attach.AttachNotSupportedException: no providers installed 之类异常
   
   > 问题原因${JAVA_HOME}/jre/bin/attach.dll 文件没有找到，检查当前Jdk环境是否安装完整
   
   ### 常见问题未知问题
   
   https://github.com/alibaba/arthas/issues/347