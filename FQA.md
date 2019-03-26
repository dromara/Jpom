## 常见问题

   ### 1. 如何修改程序运行端口
   
    修改管理程序命令文件中 --server.port=2122
        
   ### 2. 如何修改程序日志路径
   
    修改管理程序命令文件中 --jpom.log=/jpom/log/
        
   ### 3. 如何修改回话超时时长
        
    在管理程序命令文件中 ARGS 变量添加 --tomcat.sessionTimeOut=1800
        
   ### 4. jpom 启动提示
   
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
      
   ### 5. 启动提示
   
   > jpom 数据目录权限不足...
   
   请检查当前用户是否拥有对应目录的读写权限