---
title: SSH管理
date: 2022-06-11 09:57:14
permalink: /pages/c6da0f/
tags: 
  - null
categories: 
  - docs
  - 文档
  - 节点管理
---
# SSH管理

#### 功能介绍

   配置和管理通过SSH渠道远程控制访问的服务器列表和文件管理功能入口，SSH管理模块所有功能不依赖于插件端
    
#### 添加SSH
    
   编辑ssh配置信息<br><br>
   
   ![sshedite](docs/images/node/server_sshedit.png)<br><br>
   
   `SSH名称:`当前SSH连接名称，建议使用IP/域名/hostname命名<br><br>
   `Hostname:`当前SSH连接服务器的IP `Port:`当前SSH端口，默认22，请填22<br><br>
   `Username:`当前服务器登陆账户<br><br>
   `Password:`服务器登陆密码，或者证书密钥<br><br>
   `认证方式:`可选择 PASS 或者 PUBKEY<br><br>
   `编码方式:`可选择SSH控制台字符集编码，编码请您根据服务器编码设置<br><br>
   `文件目录:`授权可直接访问的目录，多目录回车换行即可<br><br>
      
#### SSH列表展示
   展示当前已配置正确的SSH列表信息<br><br>
   ![sshlist](docs/images/node/server_sshlist.png)<br><br>
   `名称:`SSH的名称 `host:`SSH的IP`user:`登陆用户`port`SSH端口<br><br>
   *`关联节点:`若已当前SSH对应管理的服务器上已安装插件端，可在添加节点配置时候关联SSH，成功关联后，当前SSH列表
   信息的`关联节点`字段展示的将会是被关联节点的名称，可通过该名称快速跳转管理节点功能相关页面<br><br>
   ![sshlistinstall](docs/images/node/server_sshlistinstall.png)<br><br>
   
   若当前SSH对应管理的服务器上未安装插件端，即节点列表尚未配置相关服务器节点信息时。`关联节点`字段展示的将会是
   `安装`按钮控件，可通过此按钮快速安装当前服务器插件端和配置当前服务器的节点信息，详情请查阅下文**【SSH快捷安装配置插件端】**<br><br>
   ![sshlistnoinstall](docs/images/node/server_sshlistnoinstall.png)<br><br>
   
   `操作:`提供当前SSH支持的三种功能入口<br><br>
       1. `编辑:`编辑SSH配置信息<br><br>
       2. `终端:`打开当前SSH渠道的远程访问控制台<br><br>
       ![sshterm](docs/images/node/server_sshterm.png)<br><br>
       3. `文件管理:`基于JSCH的文件管理工具，详情请查阅下文**【SSH文件管理】**

#### SSH快捷安装配置插件端
    
   快速安装SSH对应服务器的插件端并快速配置节点信息<br><br>
   
   点击`安装`控件，打开安装插件端<br><br>
   ![sshinstalledit](docs/images/node/server_sshinstalledit.png)<br><br>
   
   `节点ID:`遵守唯一性约束,首次创建成功后`不可再次修改`<br><br>
   `节点名称:`当前节点名称描述,不限定 建议以服务器`域名/IP/hostname`命名<br><br>
   `节点协议:`支持http/https`节点地址:`插件端服务的ip+port(或者域名),非url`不带协议`<br>
   >注意：如上即为[创建节点配置参数](/节点管理/添加节点.md)的简化配置信息，安装成功后将按照如上所配置快速创建
   >默认分组、默认启用节点和默认不开启监控的节点**【节点管理列表可见可二次修改】**

   `安装路径:`远程安装插件端的安装目录**【目录必须在授权目录层级之下，目录不存在会自动创建，注意对存在目录的覆盖情况】**<br><br>
   `上传zip包:`上传插件端安装包（agent-x.x.x-release.zip）
   >注意：安装包**必须**是Jpom官方发布结构类型的插件包`agent-x.x.x-release.zip`<br>
   >可自行解压修改相关启动参数，请参阅[启动参数](/安装使用/启动参数.md)<br>
   >修改完请**必须**按照Jpom官方发布的结构类型压缩回插件包<br>
   >若不自定义修改官方发布的Jpom插件包（agent-x.x.x-release.zip），则将以默认参数方式安装，详情请查阅[开始安装-简易安装流程](/安装使用/开始安装.md)
   
   
   
#### SSH文件管理

使用JSCH不依赖于插件端的文件管理系统，点击`文件`进入SSH授权目录起的文件目录<br><br>
   ![wjgllist](docs/images/node/wjgl_list.png)<br><br>

   * 点击文件路径下子目录可快速跳转到对应目录
   

   * 点击表格中文件类型的行即可进入该目录下   

   * 上传的文件会采用SFTP协议并自动保存到与文件路径对应的目录中
   
   * `文件夹`显示文件类型为文件夹，可进行**删除**操作
   
   * `压缩包`可进行**删除**、**解压**和**下载**操作
   
   * `普通文件`可进行**删除**、**终端（查看）**和**下载**操作