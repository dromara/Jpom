### 常见问题

   1. 如何修改程序运行端口：修改管理程序命令文件中 --server.port=2122
   2. 如何修改程序日志路径：建议自行编译安装修改 [/src/main/resources/logback-spring.xml](/src/main/resources/logback-spring.xml) 文件
   3. 如何修改回话超时时长：在管理程序命令文件中 ARGS 变量添加 --tomcat.sessionTimeOut=1800
   