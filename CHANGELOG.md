# 🚀 版本日志

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
3. 项目管理文件新增备份，自动备份变动的文件（感谢[@少爷123](https://gitee.com/58753101) [Gitee issues I54ZFM](https://gitee.com/dromara/Jpom/issues/I54ZFM) ）
4. 【server】SH配置和节点配置新增跨工作空间同步功能，方便快速同步信息（感谢[@陈旭](https://gitee.com/chenxu8989) ） [Gitee issues I56YTU](https://gitee.com/dromara/Jpom/issues/I56YTU)

### 🐞 解决BUG、优化功能

1. 修复上传文件限制大小异常拦截不生效的问题（感谢@小工匠 ）
2. 【server】新增配置前端消息弹窗位置属性 `jpom.notificationPlacement` （感谢[@Eibons](https://gitee.com/eibons) [Gitee issues I53V8B](https://gitee.com/dromara/Jpom/issues/I53V8B) ）
3. 【server】构建历史新增批量删除
4. 【server】修复关联分发项目，无法选择不同节点下相同的项目的问题（感谢[@宋建平](https://gitee.com/sjping) [Gitee issues I5680N](https://gitee.com/dromara/Jpom/issues/I5680N) ）
5. 【server】调整 docker-compose 使用卷方式存储数据，避免在部分环境中出现无法正常使用情况 (感谢 [@💎ℳ๓₯㎕斌💎💘](https://gitee.com/weihongbin) 贡献解决方案)（感谢[@笨笨巫师](https://gitee.com/zhangxin_gitosc) [Gitee issues I52OAV](https://gitee.com/dromara/Jpom/issues/I52OAV) ）
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

1. 【server】SSH文件管理器中加入创建目录和文件的功能 （感谢 [@wxyShine](https://gitee.com/wxyShine) [Gitee PR 161](https://gitee.com/dromara/Jpom/pulls/161) ）
2. 【server】新增禁用登录图形验证码配置属性 `jpom.disabledCaptcha` （感谢 [@放学后的茶会](https://gitee.com/tt2yui) [Gitee issues I4GD0U](https://gitee.com/dromara/Jpom/issues/I4GD0U) ）
3. 【agent】节点项目文件管理新增创建文件夹/文件功能 （感谢 [@Eibons](https://gitee.com/eibons) [Gitee issues I4ZFFH](https://gitee.com/dromara/Jpom/issues/I4ZFFH) ）

### 🐞 解决BUG、优化功能

1. 【server】本地构建命令、本地命令发布、ssh 发布支持加载仓库目录 `.env` 文件为环境变量 （感谢@z~）
2. 【server】容器相关引用 maven 版本升级为 3.8.5
3. 【server】容器构建 DSL 示例添加镜像地址说明 （感谢 [@wxyShine](https://gitee.com/wxyShine) [Gitee PR 160](https://gitee.com/dromara/Jpom/pulls/160) ）
4. 【server】本地构建命令添加本次构建相关的默认变量（感谢@杨杰）
5. 【server】优化 SHH 文件管理中文件上传,压缩包上传操作（感谢 [@wxyShine](https://gitee.com/wxyShine) [Gitee PR 161](https://gitee.com/dromara/Jpom/pulls/161) ）
6. 【agent】批量获取项目状态新增缓存，避免部分环境获取项目状态超时（感谢@奇奇  ）
7. 远程升级检查地址支持自定义配置，解决没有外网或者网络不同情况下自定义配置升级服务器

------


# 2.8.17 (2022-03-28)

### 🐣 新增功能

### 🐞 解决BUG、优化功能

1. 【server】修复非超级管理员部分下载功能无法正常使用
2. 【server】ssh 私钥连接新增 `private key content` 登录 （感谢 [@震秦](https://gitee.com/zhzhenqin) [Gitee PR 159](https://gitee.com/dromara/Jpom/pulls/159) ）
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
9. 修复检查 Jpom 包中没有释放资源（感谢@[大海](https://gitee.com/hasape) [Gitee issues I4T9L0](https://gitee.com/dromara/Jpom/issues/I4T9L0) ）

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
12. 【agent】项目新增自启动配置项,在 agent 启动时候检查对应项目是否启动，未启动执行启动逻辑 [Gitee issues I4IJFK](https://gitee.com/dromara/Jpom/issues/I4IJFK)
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

# 2.7.3 (2021-12-02)

### 🐣 新增功能

1. 【server】新增自定义系统网页标题配置`jpom.name`
2. 【server】新增自定义系统网页 logo 配置`jpom.logoFile`
3. 【server】新增自定义系统登录页面标题配置`jpom.loginTitle`
4. 【server】新增自定义系统 logo 文字标题配置`jpom.subTitle`
5. 新增在线下载最新版本更新包功能（在线检测最新版本）
6. 【server】新增菜单`系统管理-数据库备份`，支持 Jpom 使用的 H2 数据库备份、还原

### 🐞 解决BUG、优化功能

1. 【server】恢复构建产物为匹配符无法正常发布问题（感谢@Kay）
2. 【server】恢复在线升级页面在二级路径下无法使用的问题 (感谢@hu丶向...🤡)
3. 【server】恢复构建执行命令阻塞问题（感谢@小猿同学）
4. 【server】恢复限制 IP 访问和插件端授权信息不正确状态码冲突（感谢@小龙、@大灰灰）
5. 取消 tools.jar 依赖
6. 【server】优化初始化数据库流程，避免多次执行相同修改，节省启动时间
7. 【fix】恢复项目副本集乱码（感谢@ʟᴊx）
8. 【server】添加在线升级完成后的回调提示
9. 【server】ssh安装节点按钮动态显示
10. 【server】恢复构建信息中脚本过长无法构建的bug（感谢@Dream）
11. 在网页的编辑器中修改配置文件时兼容tab键（感谢@Dream）

> 取消 tools.jar 依赖后，Java 项目状态监控使用 `jps` 命令实现

------

# 2.7.2 (fix)

### 🐣 新增功能

### 🐞 解决BUG、优化功能

1. 【agent】解决 nginx 编辑配置文件 url 编码问题
3. 【server】新增配置构建命令支持不检测删除命令 `build.checkDeleteCommand` (感谢@Dream)

------

# 2.7.1 (fix)

### 🐣 新增功能

### 🐞 解决BUG、优化功能

1. 解决插件端请求参数 url 编码无法解析问题（感谢@知识就是力量）
2. 【agent】项目文件夹为空不再提示错误信息
3. 【server】fix 编辑构建选择 ssh 发布无法保存 （感谢 @Peision [Gitee issues I4CQWA](https://gitee.com/dromara/Jpom/issues/I4CQWA) ）
4. 【server】fix ssh 终端未配置禁用命令不能输入空格问题

------

# 2.7.0 (beta)

### 🐣 新增功能

1. **【server】构建中的仓库独立管理**
2. **【server】构建信息存储方式调整为 h2 数据库，不再存储到 json 文件中**
3. **【server】构建触发器地址变更**
4. 【agent】新增文件管理中允许编辑的文件后缀，以及对应后缀的文件编码
5. 项目文件管理中新增编辑按钮，支持编辑文本文件（ 新版本 UI 同步新增该功能）
6. 程序启动输出默认 IP 地址和当前运行端口信息
7. bat 管理命令（windows）启动后输出日志文件,方便排查当前启动情况
8. 【server】上传文件到插件端（节点）超时配置独立,采用 server 端全局配置,配置参数 `node.uploadFileTimeOut`
   （感谢 @LW 根据 Gitee  [issues I3O8YE](https://gitee.com/dromara/Jpom/issues/I3O8YE) ）
9. 【server】角色新增添加权限配置 （感谢@misaka [Gitee pr](https://gitee.com/dromara/Jpom/pulls/141) ）
10. 【server】节点升级上传新包成功后删除历史包
11. 【server】新版本 UI 菜单系统管理、节点升级只有系统管理员可见
12. 【server】新版本 UI 脚本模板同步添加执行参数（感谢@轻描淡写 [Gitee issues I43G4B](https://gitee.com/dromara/Jpom/issues/I43G4B) ）
13. 【server】新版本 UI 同步添加 common.js
14. 【agent】项目文件管理新增下载远程文件功能
15. 【agent】节点首页监控新增实际使用内存占比（linux系统） （感谢@大灰灰）
16. 【server】ssh 新增操作记录（方便查看执行历史回溯操作）
17. 【server】新增 h2 控制台配置属性,基于 SpringBoot,配置参数`spring.h2.console.enabled`
18. 【server】节点分发支持下载远程文件 （感谢@落泪归枫 [Gitee issues I1LM27](https://gitee.com/dromara/Jpom/issues/I1LM27) ）
19. 【server】节点分发支持 file 类型项目
20. 【agent】项目新增配置日志文件输出到指定目录
21. 【server】构建产物目录支持通配符`AntPathMatcher`模式 （感谢@saysay [Gitee issues I455FM](https://gitee.com/dromara/Jpom/issues/I455FM)
	）
22. 【server】新增 h2 数据库缓存大小配置 [CACHE_SIZE](http://www.h2database.com/html/features.html#cache_settings) `db.cacheSize
23. 【server】构建触发器新增延迟执行参数（感谢@Steve.Liu）
24. 【server】增加全局项目搜索功能
25. 【agent】项目增加批量启动关闭重启
26. 【server】节点分发文件支持上传非压缩包（感谢@Sam、風中飛絮 [Gitee issues I3YNA5](https://gitee.com/dromara/Jpom/issues/I3YNA5) ）
27. 【server】nginx 二级代理无法访问（感谢@hu丶向...🤡）
28. 【server】ssh文件管理新增在线编辑（感谢@嗳啨 [Gitee issues I4ADTA](https://gitee.com/dromara/Jpom/issues/I4ADTA) ）
29. 在线升级支持上传 zip 包自动解析（感谢@Sam）
30. 【server】ssh 安装插件端新增等待次数配置（感谢@hu丶向...🤡）
31. 【server】新增前端接口请求超时配置 `jpom.webApiTimeOut`（感谢@hu丶向...🤡）
32. 【server】构建支持 tag 通配符 （感谢@落泪归枫 [Gitee issues I1LM1V](https://gitee.com/dromara/Jpom/issues/I1LM1V) ）

### 🐞 解决BUG、优化功能

1. 【server】添加节点时候限制超时时间，避免配置错误一直等待情况
2. 【server】优化限制 IP 白名单相关判断，避免手动修改错误后一直限制访问
3. 【server】添加 QQ 邮箱配置参照说明 [QQ邮箱官方文档](https://service.mail.qq.com/cgi-bin/help?subtype=1&&no=369&&id=28)
4. 【server】fix: 删除临时文件出现 `AccessDeniedException` 更新文件权限为可读（取消只读权限）
5. 【server】拉取 GIT 代码根据仓库路径添加 `synchronized`
6. 【server】节点管理页面支持刷新当前节点页面（刷新不再回到首页）
7. 【server】 jpom-service.sh 文件加载环境变量修改为 判断模式
8. 【agent】fix: windows 环境保存配置文件错误问题
9. 【agent】fix: 在线升级页面在没有配置白名单时候无法显示节点信息
10. 【server】ssh 快捷安装插件端检查配置文件不在使用 SpringBoot 非 public 工具类
11. 【server】请求节点发生异常打印具体堆栈、接口异常拦截器里面默认不打印堆栈 （根据 Gitee  [issues I3O8YE](https://gitee.com/dromara/Jpom/issues/I3O8YE) ）
12. 【server】节点升级中偶尔出现无法获取到对应的版本信息问题（感谢@misaka Gitee issues [I41TDY](https://gitee.com/dromara/Jpom/issues/I41TDY) ）
13. 本地运行数据目录位置改为`${user.home}/jpom/xxxx`、日志路径改为项目模块下
14. 【agent】升级 `commons-compress` 依赖 （来自 GitHub [advisories](https://github.com/advisories) ）
15. agent 和 server 间的 websocket 鉴权调整
16. 【server】update: 刷新整个页面的时候重新加载菜单
17. 历史监控图表查询报时间格式化错误(字符串工具类) （感谢@misaka [Gitee pr](https://gitee.com/dromara/Jpom/pulls/142) ）
18. 【agent】nginx 配置文件取消强制检测 server 节点
19. 【server】仓库密码改为隐藏
20. 解决退出登录验证码没有刷新问题 （感谢群友：Steve.Liu）
21. 【agent】节点分发清空发布无效（感谢@Sam）
22. 【server】编写分发项目时，当分发节点做替换、新增的操作后，点击确认，控制台报错（感谢@tan90°）

> 【特别声明】当前版本 仓库和构建并没有接入动态数据权限，如果对权限敏感的用户建议等待下一个版本优化权限后再升级（如有疑问可以微信群沟通）

> ⚠️ 注意1：由于构建信息全部存储到 h2 数据库中，之前到构建信息会自动同步，在升级后到第一次启动需观察控制台信息，启动成功后请检查构建信息，仓库信息是否同步正确
>
> ⚠️ 注意2：构建的触发器地址有更新，需要重新获取触发器地址
>
> ⚠️ 注意3：升级到该版本需要保证 agent、server 都保持同步，如果只升级 server 会出现项目控制台等功能无法正常使用
>
> ⚠️ 注意4：升级 2.7.x 后不建议降级操作,会涉及到数据不兼容到情况

------
