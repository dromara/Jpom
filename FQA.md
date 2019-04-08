## 常见问题

   ### 如何修改程序运行端口
   
    修改管理程序命令文件中 --server.port=2122
        
   ### 如何修改程序日志路径
   
    修改管理程序命令文件中 --jpom.log=/jpom/log/
        
   ### 如何修改回话超时时长
        
    在管理程序命令文件中 ARGS 变量添加 --tomcat.sessionTimeOut=1800
    
   ### 如何修改Jpom数据存储目录
       
    修改外部配置文件中的 jpom.path 属性
      
        
   ### 在linux 系统中执行Jpom.sh 提示
   
   ![jpom](/doc/error/ff-unix.png)
    
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

```
server {
    #charset koi8-r;
    access_log  /var/log/nginx/jpom.log main;
    listen       80;
    server_name  jpom.xxxxxx.com;
    
    location / {
        proxy_pass   http://127.0.0.1:2122/;
        proxy_set_header Host      $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        client_max_body_size  50000m;
        client_body_buffer_size 128k;
        #  websocket 配置
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

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
   
   针对以上两个问题，Jpom目前采用略过这些进程的解决办法，请更新master分支到2.3.1或dev分支2.2.1以上。 