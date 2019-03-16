# windows 开启控制台实时查看日志

Jpom中实时查看控制台日志使用 tail -f 实现,windows 不支持tail 命令。所以需要使用插件

安装方式复制项目路径script目录中的 [tail.exe](/script/tail.exe) 到系统目录中

如：系统在C:盘

    C:\Windows\System32
    
则复制到：
 
    C:\Windows\System32\tail.exe
    
###  ok