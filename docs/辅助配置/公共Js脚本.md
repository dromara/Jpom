# 快速在Jpom管理页面中添加自定义js代码

> 比如我们需要对Jpom的后台页面添加一些自定义js代码，来实现自己的业务。如通用的添加cnzz监测代码

### 操作流程如下

#### 1. 在Jpom的Server端的数据目录同级创建【script】目录

#### 2. 在创建的【script】目录中添加【common.js】文件即可

#### common.js 文件中支持html和js混写

如：（此代码为Jpom演示平台的实例）
```
<div style="display: none;">
   <script type="text/javascript" src="https://s96.cnzz.com/z_stat.php?id=0000&web_id=0000"></script>
</div>
<script type="text/javascript">
 if(location.pathname=='/login.html') {
     const user = layui.data('user');
     if(!user.userName){
         layer.msg('演示账号：demo  </br>  密码：demo123');
     }
 }
</script>
```