# 🚀 版本日志

## 2.9.3 (2022-07-08)

### 🐣 新增功能

1. 【server】新增容器 stats ,方便实时查看容器内存、网络信息
2. 【server】SSH 文件管理新增重命名文件功能
3. 【server】新增全局配置,解决 git httpProxy （感谢[@五六](https://gitee.com/tutu-father) [Gitee issues I5EY03](https://gitee.com/dromara/Jpom/issues/I5EY03) ）
4. 【server】新增更新容器配置（CPU、内存等）
5. 【server】新增页面 ICON 配置属性：`jpom.iconFile` （感谢[@flyhigh318](https://gitee.com/flyhigh318) [Gitee issues I5FKMW](https://gitee.com/dromara/Jpom/issues/I5FKMW) ）
6. 【server】SSH 脚本新增跨工作空间同步功能 （感谢[@flyhigh318](https://gitee.com/flyhigh318) [Gitee issues I5FC9R](https://gitee.com/dromara/Jpom/issues/I5FC9R) ）
7. 【server】服务端脚本模版新增跨工作空间同步功能
8. 【server】构建新增事件脚本属性,在构建环节可以执行指定脚本来实现部分功能（感谢[@沈世举](https://gitee.com/shen-shiju) [Gitee issues I5FKFM](https://gitee.com/dromara/Jpom/issues/I5FKFM) ）
9. 【server】优化构建任务独立线程池,并且新增配置属性 `build.poolSize`、`build.poolWaitQueue` （感谢@小翼哥）
10. 【agent】配置项目是否备份控制台日志属性独立：`log.autoBackToFile` （感谢@Vergil。）

### 🐞 解决BUG、优化功能

1. 升级 SpringBoot、Hutool、jgit、svnkit
2. 【server】docker 加入集群无法正常使用问题
3. 【server】项目文件备份列表不能取消弹窗（点击关闭依然执行）问题（感谢@ʟᴊx💎💎）
4. 【server】修复编辑构建仓库切换事件重复问题 （感谢[@五六](https://gitee.com/tutu-father) [Gitee issues I5F35E](https://gitee.com/dromara/Jpom/issues/I5F35E) ）
5. 【server】修复 windows 执行脚本出现异常（感谢[@all-around-badass](https://gitee.com/all-around-badass) [Gitee issues I5FK0K](https://gitee.com/dromara/Jpom/issues/I5FK0K) ）

------

## 2.9.2 (2022-06-27)

### 🐣 新增功能

1. 【agent】插件端白名单新增 nginx 安装路径,解决 nginx reload 问题（感谢[@all-around-badass](https://gitee.com/all-around-badass) [Gitee issues I5CJR7](https://gitee.com/dromara/Jpom/issues/I5CJR7) ）
2. 【server】通过私人令牌导入仓库支持自建 GitLab
3. 【agent】项目文件管理新增重命名文件功能
4. 快速安装节点支持配置网卡名称（感谢@Elliot）

### 🐞 解决BUG、优化功能

1. 【server】构建历史分页查询不生效（感谢[@PQ宝剑](https://gitee.com/pqbaojian) [Gitee issues I5CYOD](https://gitee.com/dromara/Jpom/issues/I5CYOD) ）
2. 【server】优化编辑 ssh 判断重复,支持多账号配置SSH（感谢[@xiaofangkang](https://gitee.com/xiaofangkang) [Gitee issues I5D0EY](https://gitee.com/dromara/Jpom/issues/I5D0EY) ）
3. 【agent】文件备份对比流程异步处理,避免大文件对比耗时阻塞（感谢@ʟᴊx💎💎）
4. 【server】修复通过私人令牌导入仓库表格主键指定错误
5. 【server】修复 GitLab 通过私人令牌导入仓库接口分页错误
6. 【agent】优化新增文件、删除文件目录加载两次问题（感谢[@all-around-badass](https://gitee.com/all-around-badass) [Gitee issues I5DMKG](https://gitee.com/dromara/Jpom/issues/I5DMKG) ）
7. 【server】优化下拉菜单、下拉框跟随页面滚动 （感谢[@pl.com](https://gitee.com/pl.com) [Gitee issues I5D6I0](https://gitee.com/dromara/Jpom/issues/I5D6I0) ）
8. 【server】导入仓库支持按仓库名搜索
9. 【server】修复导入仓库 GitLab 私有列显示错误
10. 副本功能优化,新增名称字段,项目列表快速查看 （感谢[@爱琳琳真是太好了](https://gitee.com/qiqi513_admin) [Gitee issues I5E52A](https://gitee.com/dromara/Jpom/issues/I5E52A) ） 
11. 【server】修复非默认工作空间快速安装节点未正常绑定到对应工作空间问题

------

## 2.9.1 (2022-06-16)

### 🐣 新增功能

1. 【server】SSH 终端新增标签页打开方式（感谢@hu丶向...🤡）

### 🐞 解决BUG、优化功能

1. 【server】db 安全检查时机前置(是否开启 web 访问),避免突然关闭数据库（感谢@信徒）
2. 【server】修复部分终端弹窗太小问题（感谢@syso）
3. 【server】修复重新初始化数据库异常问题（感谢@Dream、hu丶向...🤡）
4. 【server】修复系统管理中查看白名单配置报错（感谢[@all-around-badass](https://gitee.com/all-around-badass) [Gitee issues I5CGO7](https://gitee.com/dromara/Jpom/issues/I5CGO7) ）
5. 【server】优化监听到路由错误 刷新页面
6. 【server】修复控制台按钮不能正常使用的情况（感谢@😱 会 🎉、ccx2480）

------

## 2.9.0 (2022-06-13)

### 🐣 新增功能

1. 【server】SSH 终端新增全屏方式打开
   （感谢[@jaseeon](https://gitee.com/jaseeon) [Gitee issues I5BS52](https://gitee.com/dromara/Jpom/issues/I5BS52) ）
2. 【server】SSH 新增超时时间配置（感谢@带刺的玫瑰）
3. 【server】SFTP 新增超时时间（感谢@带刺的玫瑰）（特别感谢 [@hutool](https://hutool.cn) 作者紧急发版支持）

### 🐞 解决BUG、优化功能

1. 【server】升级 h2 版本，低版本存在漏洞(CVE-2021-23463)
2. 升级 SpringBoot、Hutool 版本
3. 【server】修复监控日志未存储到对应的工作空间（感谢@带刺的玫瑰）

### ⚠️ 注意

> 此版本为不兼容升级，需要手动升级操作数据相关迁移，操作流程如下：

**（下述流程仅供简单思路参考，不同版本间存在部分差异，详细流程还请差异完整文档：[https://jpom.io/pages/upgrade/2.8.x-to-2.9.x](https://jpom.io/pages/upgrade/2.8.x-to-2.9.x) ）**

1. 导出低版本数据
	1. 启动程序参数里面添加 --backup-h2
	2. linux 环境举例：`sh /xxxx/Server.sh restart --backup-h2`
2. 将导出的低版本数据( sql 文件) 导入到新版本中
	1. 启动程序参数里面添加 `--replace-import-h2-sql=/xxxx.sql --transform-sql` (路径需要替换为第一步控制台输出的 sql 文件保存路径)
	2. linux 环境举例：`sh /xxxx/Server.sh restart --replace-import-h2-sql=/xxxx.sql --transform-sql`

✈️ [更详细的升级说明文档](https://jpom.io/pages/upgrade/2.8.x-to-2.9.x)

------

# 2.8.25 (2022-06-10)

### 🐣 新增功能

### 🐞 解决BUG、优化功能

1.【server】修复 docker 集群服务编辑端口配置不能区分开问题（感谢@Y.）

------

# 2.8.24 (2022-06-09)

### 🐣 新增功能

1. 【server】新增命令行创建数据库备份参数 `--backup-h2` `sh /xxxx/Server.sh restart --backup-h2`
2. 【server】新增命令行导入 sql 数据文件参数 `--import-h2-sql=/xxxx.sql` `sh /xxxx/Server.sh restart --import-h2-sql=/xxxx.sql`

### 🐞 解决BUG、优化功能

1. 【server】修复切换分页限制数再排序数据查询条数不正确问题
   （感谢[@Eibons](https://gitee.com/eibons) [Gitee issues I5B47O](https://gitee.com/dromara/Jpom/issues/I5B47O) ）
2. 【server】优化编辑构建时发布选择 docker 镜像服务名输入改为下拉框（感谢@W）
3. 【agent】重启项目操作返回停止项目相关信息
4. 【server】缓存用户选择的每页条数

------

# 2.8.23 (2022-06-01)

### 🐣 新增功能

### 🐞 解决BUG、优化功能

1. 修复构建产物目录输入框限制（50 改为 200）（感谢@H）
2. 【server】修复节点分发白名单多个节点无法正常保存问题（感谢@ccx2480 ）
3. 【agent】修改项目目录在不存在时候无法上传文件问题（感谢@ccx2480 ）

------

# 2.8.22 (2022-05-27)

### 🐣 新增功能

### 🐞 解决BUG、优化功能

1. 修复监听日志后线程阻塞造成部分功能无法正常使用（控制台等）（感谢@ʟᴊx💎💎）

------

# 2.8.21 (2022-05-23)

### 🐣 新增功能

1. 新增日志查询功能 （感谢 @
   、 [@漫步青春的日子](https://gitee.com/imoom) [Gitee issues I54GDY](https://gitee.com/dromara/Jpom/issues/I54GDY) ）

### 🐞 解决BUG、优化功能

1. 【server】修复构建读取 .env
   文件空时候无法正常使用（感谢[@wangfeng2228952430](https://gitee.com/wangfeng2228952430) [Gitee issues I57DC1](https://gitee.com/dromara/Jpom/issues/I57DC1)
   ）
2. 【server】优化构建拉取仓库`锁对象`,避免出现死锁问题（感谢@信徒 ）
3. 【server】修复节点分发配置多个节点无法正常保存问题（感谢@李道甫 ）
4. 【agent】上传文件大小限制配置属性默认值恢复为 1GB,避免用户升级后需要逐一配置 （感谢@李道甫 ）
5. 【server】修复服务端脚本自动删除执行记录未删除日志文件

------

# 2.8.20 (2022-05-13)

### 🐣 新增功能

### 🐞 解决BUG、优化功能

1. 【server】优化部分弹窗数据和列表数据共用问题（感谢@晴天飛雪 ）
2. 【server】修复构建产物越级判断问题（感谢@Randy ）

------

# 2.8.19 (2022-05-13)

### 🐣 新增功能

1. 【server】节点新增代理配置，实现使用代理访问插件端（感谢@背着砍刀的诗人）
2. 【server】构建新增差异构建配置选择（如果仓库代码未变动则不执行构建）
3.

项目管理文件新增备份，自动备份变动的文件（感谢[@少爷123](https://gitee.com/58753101) [Gitee issues I54ZFM](https://gitee.com/dromara/Jpom/issues/I54ZFM)
）

4. 【server】SH配置和节点配置新增跨工作空间同步功能，方便快速同步信息（感谢[@陈旭](https://gitee.com/chenxu8989)
   ） [Gitee issues I56YTU](https://gitee.com/dromara/Jpom/issues/I56YTU)

### 🐞 解决BUG、优化功能

1. 修复上传文件限制大小异常拦截不生效的问题（感谢@小工匠 ）
2. 【server】新增配置前端消息弹窗位置属性 `jpom.notificationPlacement`
   （感谢[@Eibons](https://gitee.com/eibons) [Gitee issues I53V8B](https://gitee.com/dromara/Jpom/issues/I53V8B) ）
3. 【server】构建历史新增批量删除
4.

【server】修复关联分发项目，无法选择不同节点下相同的项目的问题（感谢[@宋建平](https://gitee.com/sjping) [Gitee issues I5680N](https://gitee.com/dromara/Jpom/issues/I5680N)
）

5. 【server】调整 docker-compose 使用卷方式存储数据，避免在部分环境中出现无法正常使用情况 (感谢 [@💎ℳ๓₯㎕斌💎💘](https://gitee.com/weihongbin) 贡献解决方案)
   （感谢[@笨笨巫师](https://gitee.com/zhangxin_gitosc) [Gitee issues I52OAV](https://gitee.com/dromara/Jpom/issues/I52OAV) ）
6. 【server】调整节点里面在部分情况下会出现空白行 （感谢[@💎ℳ๓₯㎕斌💎💘](https://gitee.com/weihongbin) ）
7. 【server】前端部分输入框添加`maxLength` 限制避免出现数据库字段长度不足问题（感谢@ccx2480 ）
8. 【agent】修复项目下载远程文件解压方法错误（感谢@背着砍刀的诗人 ）
9. 升级依赖 SpringBoot、Hutool、Docker-Java 版本号
10. 【server】优化报错构建时未判断构建产物越级问题（感谢@奇奇 ）

> 使用项目文件备份说明：
>
> 1. 默认未开启文件备份功能
> 2. 可以配置全局开启，插件端配置（ `extConfig.yml` ）文件中配置`project.fileBackupCount`属性
> 3. DSL 项目可以在配置内容新增 `file.backupCount` 来开启（DSL 配置优先级最高）
> 4. 如果配置值小于等于 0 则不开启备份功能
> 5. 备份文件保留规则为，只保留有差异的文件
> 6. 同时支持配置仅备份指定文件后缀的文件（详情查看配置文件说明）

### 注意：⚠️

本版本调整了上传文件大小配置参数位置，在插件端升级后可能出现上传文件失败。

可能提示如下:

- 节点上传失败,请优先检查限制上传大小配置是否合理
- 上传文件太大了,请重新选择一个较小的文件上传吧

出现如上提示信息需要对插件端或者服务端进行配置合理对上传文件大小限制。
配置方式：在对应端的 `extConfig.yml` 配置文件中配置如下代码

```yaml
spring:
  servlet:
    multipart:
      # 上传文件大小限制
      max-request-size: 2GB
      max-file-size: 1GB
```

------

# 2.8.18 (2022-04-12)

### 🐣 新增功能

1. 【server】SSH文件管理器中加入创建目录和文件的功能
   （感谢 [@wxyShine](https://gitee.com/wxyShine) [Gitee PR 161](https://gitee.com/dromara/Jpom/pulls/161) ）
2. 【server】新增禁用登录图形验证码配置属性 `jpom.disabledCaptcha`
   （感谢 [@放学后的茶会](https://gitee.com/tt2yui) [Gitee issues I4GD0U](https://gitee.com/dromara/Jpom/issues/I4GD0U) ）
3. 【agent】节点项目文件管理新增创建文件夹/文件功能
   （感谢 [@Eibons](https://gitee.com/eibons) [Gitee issues I4ZFFH](https://gitee.com/dromara/Jpom/issues/I4ZFFH) ）

### 🐞 解决BUG、优化功能

1. 【server】本地构建命令、本地命令发布、ssh 发布支持加载仓库目录 `.env` 文件为环境变量 （感谢@z~）
2. 【server】容器相关引用 maven 版本升级为 3.8.5
3. 【server】容器构建 DSL 示例添加镜像地址说明
   （感谢 [@wxyShine](https://gitee.com/wxyShine) [Gitee PR 160](https://gitee.com/dromara/Jpom/pulls/160) ）
4. 【server】本地构建命令添加本次构建相关的默认变量（感谢@杨杰）
5. 【server】优化 SHH
   文件管理中文件上传,压缩包上传操作（感谢 [@wxyShine](https://gitee.com/wxyShine) [Gitee PR 161](https://gitee.com/dromara/Jpom/pulls/161)
   ）
6. 【agent】批量获取项目状态新增缓存，避免部分环境获取项目状态超时（感谢@奇奇 ）
7. 远程升级检查地址支持自定义配置，解决没有外网或者网络不同情况下自定义配置升级服务器

------

# 2.8.17 (2022-03-28)

### 🐣 新增功能

### 🐞 解决BUG、优化功能

1. 【server】修复非超级管理员部分下载功能无法正常使用
2. 【server】ssh 私钥连接新增 `private key content` 登录
   （感谢 [@震秦](https://gitee.com/zhzhenqin) [Gitee PR 159](https://gitee.com/dromara/Jpom/pulls/159) ）
3. 【server】修复非默认工作空间节点分发白名单无法保存问题（感谢@愿好）

------

# 2.8.16 (2022-03-10)

### 🐣 新增功能

1. 【server】docker 创建容器添加执行命令参数

### 🐞 解决BUG、优化功能

1. 【server】修复工作空间非隐私变量不切换无法创建问题
2. 【server】修复无法删除工作空间变量
3. 【server】修复编辑构建数据回显错乱情况（感谢@tan90°）
4. 【server】修复配置二级路径无法下载日志相关问题（感谢@hu丶向...🤡）
5. 【server】调整服务端 docker 默认时区为：`Asia/Shanghai` (感谢@z~)

------

# 2.8.15 (2022-03-09)

### 🐣 新增功能

1. 【server】监控新增 webhook 字段（感谢@[wxyShine](https://gitee.com/wxyShine) ）
2. 【server】新增自动添加本机 docker (感谢@💎ℳ๓₯㎕斌💎💘)
3. 【server】docker 镜像创建容器支持环境变量 (感谢@💎ℳ๓₯㎕斌💎💘)
4. 工作空间变量新增同步到指定节点
5. 【server】工作空间变量新增隐私字段（来控制关键字段不会二次显示提升字段安全）

### 🐞 解决BUG、优化功能

1. 【server】节点统计图表内存占用率取错字段（感谢@[wxyShine](https://gitee.com/wxyShine) ）
2. 【server】修复 ssh 上传文件资源未释放问题（感谢@也许是个意外 ）
3. 【server】备份数据不将 sql 输出到控制台
4. 【server】取消信号量监听,会出现 kill pid 无法关闭进程的情况
5. 【server】优化操作报警，能获取到更多到数据名称
6. 【server】修护工作空间变量编辑使用默认工作空间相关问题（感谢@lidaofu）
7. 【server】配置自定义 logo 支持远程地址（http）(感谢@wxyShine)

------

# 2.8.14 (2022-02-25)

### 🐣 新增功能

1. 【server】构建触发器新增获取当前状态接口 （感谢@wxyShine）
2. 【server】构建确认流程支持修改分支（感谢@）
3. 【server】服务端脚本新增解绑操作,用于释放关联的过期节点

### 🐞 解决BUG、优化功能

1. 【server】修复编辑构建 svn 类型仓库无法提交问题（感谢@杰 ）
2. 【server】修复无法使用 git ssh 问题

------

# 2.8.13 (2022-02-24)

### 🐣 新增功能

1. 【agent】项目 DSL 支持配置项目路径下的脚本

### 🐞 解决BUG、优化功能

1. 【server】新增个性化配置,导航菜单打开方式（感谢@以为）
2. 【server】工作空间没有变量不能使用容器构建问题（感谢@杨杰）
3. 【server】在线构建 ssh 发布选择授权目录切换不生效问题（感谢@天天）
4. 【server】在线构建本地构建命令不能换行问题（感谢@华仔）
5. 【server】日志弹窗新增行号
6. 【server】在线升级执行脚本、脚本模版等兼容 `debian` （感谢@wxyShine [Gitee issues I4UQBD](https://gitee.com/dromara/Jpom/issues/I4UQBD) ）
7. 【agent】修复 windows 环境，节点首页进程列表切换不生效问题（感谢@neoch [Gitee issues I4UZA7](https://gitee.com/dromara/Jpom/issues/I4UZA7) ）
8. 【server】脚本模版补充权限

------

# 2.8.12 (2022-02-18)

### 🐣 新增功能

### 🐞 解决BUG、优化功能

1. 【server】紧急修复初始化系统后没有选择工作空间问题

------

# 2.8.11 (2022-02-18)

### 🐣 新增功能

### 🐞 解决BUG、优化功能

1. 【server】修复操作监控没有判断操作用户
2. 【server】用户编辑不能修改自己的信息
3. 【server】优化日志查看器布局错乱
4. 【agent】修复进程名缓存不全问题
5. 【server】节点统计新增分组字段，自动同步节点分组（感谢@ʟᴊx💎💎）
6. 【server】docker 添加说明端口安全问题

------

# 2.8.10 (2022-02-16)

### 🐣 新增功能

1. nginx 删除操作新增还原配置
2. 【server】新增用户登录日志记录
3. 【server】log view 新增正则参考
4. 【server】docker 控制台新增 docker 信息查看
5. 【server】新增 docker swarm 集群管理
6. 【server】构建新增是否保存产物配置
7. 【server】构建 Dockerfile 镜像 tag 支持仓库目录 `.env` 环境变量文件
8. 【server】docker 控制台新增 docker 网络信息查看
9. 【server】构建 Dockerfile 镜像支持发布 swarm service

### 🐞 解决BUG、优化功能

1. 【server】 ssh 授权目录不能为根目录
2. 【server】 在线构建-构建列表中，选择构建命令时新增多环境下打包的命令
3. 【server】 在线构建-构建列表中，选择构建命令时提供不同的跳过执行测试类的方式
4. 【server】 编辑用户工作空间权限判断没有选择父级的空权限
5. 【server】 修改后分发后 **并发重启** 描述改为 **并发执行**
6. 【server】 自动构建触无法执行（感谢@[zhangfuhua](https://gitee.com/zhangfuhua) ）

------

# 2.8.9 (2022-02-12)

### 🐣 新增功能

1. 【server】容器构建新增 go 环境支持
2. 【server】新增查看 docker 容器日志
3. 【server】新增在线进入 docker 容器终端
4. 【server】构建 ssh 发布支持发布到多个服务器（感谢@[老诗人](https://gitee.com/laoshirenggo) ）
5. 【server】构建发布方式新增 docker 镜像
6. 【server】容器管理新增在线镜像创建容器功能
7. 【server】容器管理新增在线拉取镜像功能
8. 【server】构建新增是否缓存构建目录

### 🐞 解决BUG、优化功能

1. 在线升级新增验证兼容最小版本号
2. 【server】支持在线修改数据库账户密码
3. 执行脚本文件由 `/bin/sh` 改为 `/bin/bash` 兼容 ubuntu
4. 【agent】项目 dsl 模式执行脚本变量支持直接引入 ${PROJECT_ID}、同时保留 #{PROJECT_ID} 引用
5. 【server】多处日志查看弹窗新增高亮搜索
6. 【server】本地构建命令 容器构建支持引用工作空间变量
7. 【server】修复构建触发器无法执行（感谢@[老诗人](https://gitee.com/laoshirenggo) ）
8. 【server】服务端脚本新增工作空间环境变量
9. 修复检查 Jpom
   包中没有释放资源（感谢@[大海](https://gitee.com/hasape) [Gitee issues I4T9L0](https://gitee.com/dromara/Jpom/issues/I4T9L0) ）

------

# 2.8.8 (2022-02-08)

### 🐣 新增功能

1. 【server】新增容器构建(感谢@💎ℳ๓₯㎕斌💎💘)
2. 【server】新增容器管理基础版
3. 【server】节点脚本列表新增快速查看日志入口（感谢@ʟᴊx💎💎）
4. 【server】构建新增备注字段,可以用于记录版本日志或者本次构建备注（感谢@Alex）
5. 【server】新增解绑节点、节点分发功能 用于服务器过期或者已经确定不在使用直接删除节点相关数据（感谢@🐠）
6. 【server】构建命令新增预设命令提示输入,减少用户输入（感谢@hjk2008 [Gitee issues I4SHC9](https://gitee.com/dromara/Jpom/issues/I4SHC9) ）
7. 【server】批量构建支持指定部分参数使构建更灵活（感谢@hjk2008 [Gitee issues I4SHB4](https://gitee.com/dromara/Jpom/issues/I4SHB4) ）
8. 【server】用户账号新增两步验证(MFA) 提升账号安全性(感谢@💎ℳ๓₯㎕斌💎💘)

### 🐞 解决BUG、优化功能

1. 【server】优化定时任务检查逻辑,避免不能正常关闭定时任务
2. 【server】数据库备份新增修改人字段（可以表示备份人和还原操作人）
3. 【server】邮箱配置权限修改为超级管理员
4. 【server】修复服务端脚本分发到节点特殊字符编码问题（感谢@ʟᴊx💎💎）
5. 【server】修复删除节点未删除节点统计数据（感谢@以为）
6. 升级 SpringBoot 到 2.6.3 (感谢@💎ℳ๓₯㎕斌💎💘)
7. 【server】解除 SSH 终端禁止命令权限保存失败（感谢@Alex）
8. 【server】本地构建模式模糊匹配支持匹配多个结果
9. 【server】修复节点分发不能删除节点问题（感谢@a19920714liou [Gitee issues I4SHSP](https://gitee.com/dromara/Jpom/issues/I4SHSP) ）
10. 【server】ssh 快捷安装插件端保存安装包避免多次上传 （感谢@a19920714liou [Gitee issues I4SHJC](https://gitee.com/dromara/Jpom/issues/I4SHJC)
	）
11. 【server】ssh 快捷安装插件端权限改为管理员
12. 【server】构建 ssh 发布授权目录采用下拉模式,提升用户操作感知 (
	感谢@hjk2008 [Gitee issues I4SICE](https://gitee.com/dromara/Jpom/issues/I4SICE) )
13. 【server】修复数据库自动备份失败问题

> 🙏 特别感谢：[💎ℳ๓₯㎕斌💎💘](https://gitee.com/weihongbin) 贡献容器构建相关架构

------

# 2.8.7 (2022-01-24)

### 🐣 新增功能

1. 【server】新增系统配置-节点白名单、节点系统配置分发功能，方便集群节点统一配置
2. 【server】新增构建快捷复制功能,方便快速创建类型一致的项目
3. 【server】新增系统配置-配置菜单是否显示,用于非超级管理员页面菜单控制
4. 【server】新增节点统计功能，快速了解当前所有节点状态
5. 【server】新增节点心跳检测配置`system.nodeHeartSecond`
6. 新增缓存管理查看定时任务执行统计
7. 【server】新增解除 SSH 终端禁止命令权限（感谢@ooooooam）

### 🐞 解决BUG、优化功能

1. 【server】新增全局关闭引导导航配置`jpom.disabledGuide`（感谢@南有乔木）
2. 【server】修复快速安装服务端 ping 检查超时时间 5ms to 5s
3. 项目文本文件支持在线实时阅读（感谢@）
4. 【server】控制台日志支持搜索高亮
5. 【server】跨工作空间更新节点授权将自动同步更新
6. 【server】取消节点监控周期字段（采用全局统一配置）
7. 【server】监控周期调整为 cron 表达式配置,用户可以自定义监控频率
8. 【server】邮箱配置菜单移动到监控管理
9. 【server】节点分发白名单配置区分工作空间（不同工作空间不能配置）
10. 【server】升级 SpringBoot 版本 2.6.2
11. 脚本模版执行目录修改为脚本所在目录
12. 【server】SSH 命令模版支持取消默认加载环境变量：`#disabled-template-auto-evn`
13. 【server】优化页面分页交互逻辑,只有一页不显示分页条
14. 【server】修复删除 SSH 没有删除执行日志

> ⚠️ 特别提醒：强烈建议升级该版本,当前版本完善了权限拦截相关问题
------

# 2.8.6 (2022-01-21)

### 🐣 新增功能

1. 项目运行模式新增 Dsl，配合脚本模版实现自定义项目管理
2. 【server】新增快捷恢复 h2 奔溃数据（启动参数添加：`--recover:h2db`）（感谢@大土豆）
3. 【server】邮箱配置新增超时时间配置（感谢@Y.）
4. 新增快速安装导入节点（插件端）
5. 【server】新增服务端脚本模版（区别于节点脚本模版）

### 🐞 解决BUG、优化功能

1. 【server】数据库备份自定义表显示中文描述
2. 【server】配置 ip 白名单判断是否合法,并且支持 ip/掩码位： `192.168.1.0/24` 格式（感谢@skyou）
3. 【server】脚本模版独立菜单
4. 【server】执行升级前自动执行备份数据逻辑（保障数据稳定）
5. 【server】节点分发项目状态显示节点 ID 更正为节点名称 （感谢@hu丶向...🤡）
6. 【server】升级 jgit 到 5.13
7. 【server】fix：恢复删除脚本模版、ssh 命令模版未删除日志数据
8. 查看项目控制台日志优化（新增滚动和显示行数配置）

> ⚠️ 注意：此次更新了项目控制台日志需要更新插件端后才能正常使用项目控制台否则会出现控制台按钮不可用的情况

------

# 2.8.5 (2022-01-14)

### 🐣 新增功能

1. 新增保留旧包个数配置参数`system.oldJarsCount`

### 🐞 解决BUG、优化功能

1. 【server】恢复删除脚本模版、ssh 命令模版未删除日志数据
2. 【server】项目副本集没有显示运行端口+进程ID（感谢@ʟᴊx💎💎）
3. 【server】ssh 发布清除产物目录忽略`No such file`异常
4. 【server】节点升级中远程下载插件包存储路径更改，并优化更新包后页面显示问题（感谢@hu丶向...🤡）
5. 脚本模版新增描述字段（感谢@ʟᴊx💎💎）
6. 在线升级取消重复 jar 包判断,改为自动重命名（感谢@大土豆）
7. 项目文件管理调整为支持清空当前目录（感谢@ʟᴊx💎💎）
8. 【server】ssh 列表安装节点按钮判断 java 环境

------

# 2.8.4 (2022-01-06)

### 🐣 新增功能

### 🐞 解决BUG、优化功能

1. 【server】数据库新增连接池配置参数（感谢@ʟᴊx💎💎）
2. 【agent】fix：解压 tar.gz 文件
3. 【server】fix：恢复导航助手不能正常显示问题
4. 【agent】fix 插件端首页不能正常关闭进程（感谢@平安茹意）
5. 【agent】fix 插件端首页支持自定义进程名（感谢@者羽）
6. 【server】恢复节点、构建分组字段（感谢@ʟᴊx💎💎）
7. 【server】后端默认开启 gzip 提升访问速度（感谢@小夜）

------

# 2.8.3 (2021-12-30)

### 🐣 新增功能

1. 脚本模版新增日志管理
2. 【server】ssh 文件管理新增导入压缩包自动解压（感谢@刘志远）
3. 脚本模版新增定时执行（感谢@大土豆）

### 🐞 解决BUG、优化功能

1. 【server】节点分发数据新增状态字段,启动程序时候触发恢复异常数据
2. 【server】定时执行相关 cron 表达式输入提示示例数据
3. 【server】节点升级菜单取消，整合到【系统管理】-> 【在线升级】中
4. 【server】邮箱配置优化,新增 smtp、port 示例数据，取消 SSL 端口字段（感谢@💎ℳ๓₯㎕斌💎💘）
5. 【server】fix：新增用户无法正常输入bug（感谢@在路上）
6. 【server】fix：ssh 命令执行记录菜单没有独立显示问题（感谢@刘志远）

------

# 2.8.2 (2021-12-24)

### 🐣 新增功能

1. 【server】仓库新增导入 Gitee、Github 仓库信息（感谢@💎ℳ๓₯㎕斌💎💘）
2. 【server】ssh 新增命令模版、可以用于批量执行命令脚本
3. 新增配置属性 `system.timerMatchSecond` 调度(定时任务)是否开启秒级匹配（感谢@大土豆）
4. 缓存管理新增清除旧版本程序包功能
5. 【server】用户权限新增绑定工作空间权限（指定工作空间的修改、删除、上传、执行等权限）

### 🐞 解决BUG、优化功能

1. 【server】nginx 列表显示不全，无法滚动问题（感谢@）
2. 【server】独立节点分发显示节点名称（感谢@奥特曼打猪）
3. 【server】用户ID（登录名）支持邮箱格式（感谢@陈力）
4. 【server】优化清除构建和删除构建时候删除相关文件操作（使用系统命令快速删除）（感谢@大土豆、[Gitee PR](https://gitee.com/dromara/Jpom/pulls/155) ）
5. 【server】项目搜索菜单名变更为项目列表
6. 【server】调整自动清理日志数据逻辑、默认保留日志数据条数修改为 `10000`
7. 【server】脚本模版在服务端统一查看、编辑、执行（感谢@ʟᴊx）
8. 【server】ssh 私钥支持配置文件和加载用户目录下的私钥文件
9. 【server】初始化超级管理员不能使用 `demo` 关键词（感谢@A.Nevermore）

> ⚠️ 注意：
> 1. 已经添加的用户重新绑定工作空间权限（默认没有工作空间操作权限）

------

# 2.8.1  (2021-12-17)

### 🐣 新增功能

1. 节点缓存页面新增定时作业列表
2. 节点首页新增其他类型进程监控（感谢@大土豆）
3. 构建中的项目发布新增差异发布（多文件项目或者网络不佳情况只发布有变化的文件节省项目发布时间）（感谢@大灰灰）

### 🐞 解决BUG、优化功能

1. 【server】解决节点未配置监控周期接口报错+页面循环提示（感谢@周健全）
2. Windows 无法关闭 Jpom 程序（感谢@……）
3. 【server】恢复项目搜索、节点分发项目的文件、控制管理无法正常使用（感谢@刘志远）
4. 脚本文件提示内容取消中文，修改为英文
5. 【agent】新增检查 jps 命令执行是否存在异常,异常则提示用户（感谢@……）
6. 部分控制台输出日志调整为英文
7. 【server】优化 ssh 安装插件端,不输入节点ID、没有配置权限报错（感谢@大土豆）
8. 【agent】恢复项目 `JavaExtDirsCp` 模式加载非 Jar 文件问题（感谢@大灰灰）
9. 升级 SpringBoot 版本 2.6.1
10. 【agent】恢复项目配置 webhook 后无法关闭进程的情况（感谢@大土豆）
11. 【server】ssh 命令日志低版本字段类型文件恢复（感谢@大土豆）
12. 【server】释放独立分发和删除分发项目提示更明确（感谢@周健全）
13. 【server】恢复自动导入节点异常（感谢@平安茹意）
14. 恢复节点密码包含特殊字符时节点控制台等相关功能无法正常使用问题（感谢@魔方技术-李广生）
15. 恢复解锁节点没有指定到对应的工作空间（感谢@周健全）

------

# 2.8.0  (2021-12-14)

### 🐣 新增功能

1. 【server】新增工作空间概念（取代角色相关）【系统将自动创建默认工作空间、默认工作空间是不能删除】
2. 【server】用户新增可以配置管理员功能【管理员可以管理系统中的账号、系统管理等功能（除升级系统、导入数据外）】
3. 【server】新增超级管理员（第一次初始化系统等账号为超级管理员），超级可以拥有整个系统权限不受任何限制
4. 【server】列表数据都新增分页、搜索、排序功能（搜索字段、排序字段正在完善补充中）
5. 【server】新增通过命令行重置 IP 白名单配置参数 `--rest:ip_config`
6. 【server】新增通过命令行重置超级管理员参数 `--rest:super_user_pwd`
7. 【server】新增通过命令行重新加载数据库初始化操作参数 `--rest:load_init_db`
8. 【server】构建新增`本地命令`发布方式 用户在服务端执行相关命令进行发布操作
9. 【server】发布命令（SSH发布命令、本地命令）支持变量替换：`#{BUILD_ID}`、`#{BUILD_NAME}`、`#{BUILD_RESULT_FILE}`、`#{BUILD_NUMBER_ID}`
10. 【server】新增自动备份全量数据配置 `db.autoBackupIntervalDay` 默认一天备份一次,执行备份时间 凌晨0点或者中午12点
11. 【agent】项目的 webhook 新增项目启动成功后通知，并且参数新增 `type` 指包括：`beforeStop`,`start`,`stop`,`beforeRestart`
12. 【agent】项目新增自启动配置项,在 agent
	启动时候检查对应项目是否启动，未启动执行启动逻辑 [Gitee issues I4IJFK](https://gitee.com/dromara/Jpom/issues/I4IJFK)
13. 【server】构建新增 webhook，实时通知构建进度
14. 【server】节点分发新增分发间隔时间配置
15. 新增控制台日志配置数据 `consoleLog.charset` 避免部分服务器执行命令响应乱码 （感谢@……）
16. 【server】构建触发器新增批量触发 [Gitee issues I4A37G](https://gitee.com/dromara/Jpom/issues/I4A37G)
17. 【server】构建支持定时触发 [Gitee issues I4FY5C](https://gitee.com/dromara/Jpom/issues/I4FY5C)

### 🐞 解决BUG、优化功能

1. 【server】用户账号、节点、SSH、监控、节点分发等数据由 JSON 文件转存 h2
2. 【server】取消节点、构建分组字段
3. 【server】取消角色概念（新增工作空间取代）
4. 【server】操作监控数据由于数据字段不兼容将不自动升级需要用户重新配置
5. 【server】系统参数相关配置都由 JSON 转存 h2（邮箱配置、IP白名单、节点分发白名单、节点升级）
6. 【server】关联节点项目支持绑定单个节点不同项目
7. 【server】构建触发器新增跟随创建用户走，历史 url 将失效,需要重新生成
8. 【server】仓库`假删`功能下线，已经删除的仓库将恢复正常（假删功能后续重新开发）
9. 【agent】项目数据新增工作空间字段、取消分组字段
10. 【server】节点 ID 取消用户自定义采用系统生成
11. 【server】优化节点弹窗和菜单折叠页面布局
12. 【server】编辑节点、SSH、邮箱配置不回显密码字段
13. 【server】优化 SSH 终端不能自动换行问题
14. 【agent】脚本模版新增工作空间字段、列表数据并缓存到服务端、新增执行日志
15. 【server】优化批量操作项目启动、关闭、重启交互
16. 【agent】恢复在线升级插件端提示 [Agent-.jar] 已经存在啦,需要手动到服务器去上传新包
17. 自动注册对节点需要手动绑定工作空间后,节点才能正常使用 (感谢@💎ℳ๓₯㎕斌💎💘)

> 🙏 特别感谢：Jpom 社区测试组成员【🐠】、【ʟᴊx】、【hu丶向...🤡】等参与内测的人员

> ⚠️ 注意：
>
> 【特别说明】：分组字段将失效，目前所有数据在升级后都将默认跟随`默认工作空间`。
>
> 1: 升级该版本会自动将原 JSON 文件数据转存到 h2 中，如果转存成功旧数据文件将自动移动到数据目录中的 `backup_old_data` 文件夹中
>
> 2: 升级过程请注意控制台日志是否出现异常
>
> 3: 操作监控数据由于数据字段不兼容将不自动升级需要用户重新配置
>
> 4: 监控报警记录、构建记录、操作记录由于字段兼容问题存在部分字段为空的情况
>
> 5：非超级管理员用户会出现由于未分配工作空间不能正常登录或者不能使用的情况，需要分配工作空间才能登录
>
> 6: 用户绑定工作空间后，用户在对应工作空间下可以创建、修改、删除对应的数据（包括但不限于管理节点）
>
> 7: 此次升级启动耗时可能需要2分钟以上（耗时根据数据量来决定），请耐心等待和观察控制台日志输出
>
> 8: 一个节点建议不要被多个服务端绑定（可能出现数据工作空间错乱情况）
------
