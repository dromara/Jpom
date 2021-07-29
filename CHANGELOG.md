# 版本日志

# 2.6.1-patch

### 新增功能

### 解决BUG、优化功能

1. 【agent】 当自定义配置授权信息后增加控制台输出信息,避免用户无感（感谢@南）
2. 【server】增加构建日志表构建命令字段长度，变更后长度为5000
3. 【server】调整编辑构建弹窗布局
4. 【server】ssh 发布命令调整为 sh 命令统一执行,避免类似 `nohup` 一直阻塞不响应
5. 【server】拦截器文件权限异常,提醒检查目录权限

------

# 2.6.0-beta

### 新增功能

1. 【server】新增配置 h2 数据账号密码参数（注意之前已经存在的数据不能直接配置、会出现登录不成功情况）
2. 【agent】项目新增配置控制台日志输出目录 （感谢@落泪归枫  [Gitee I22O4N](https://gitee.com/dromara/Jpom/issues/I22O4N)）
3. 【server】新增配置 jwt token 签名 key 参数
4. 【server】ssh 新增配置禁止执行的命令,避免执行高风险命令
5. 【server】构建发布方式为 ssh 检查发布命令是否包含禁止执行的命令
6. 【server】新增 ssh 执行命令初始化环境变量配置 `ssh.initEnv`

### 解决BUG、优化功能

1. 【agent】 修护 nginx 重载判断问题（@大灰灰大 码云 issue [I40UE7](https://gitee.com/dromara/Jpom/issues/I40UE7) ）
2. 【server】修护 ssh 上传文件时候不会自动创建多级文件夹（@大灰灰大）
3. 【server】角色动态权限显示分组
4. 【agent】 新增 stop 项目等待进程关闭时间配置 `project.stopWaitTime`、停止项目输出 kill 执行结果
5. bat 管理命令更新环境变量，避免部分服务器出现无法找到 taskkill 命令（ 感谢@Sunny°晴天、[@zt0330](https://gitee.com/zt0330) ）
6. 升级SpringBoot、Hutool等 第三方依赖版本
7. 去掉旧版本 ui (thymeleaf、layui)
8. 【server】fix： ssh 分发执行命令找不到环境变量问题
9. 【server】在线升级显示打包时间、并发执行分发 jar 包、部分逻辑优化
10. 【server】 构建历史增加下载构建产物按钮（感谢@房东的喵。）
11. 【server】项目控制台新增心跳消息，避免超过一定时间后无法操作的情况
12. 【server】ssh 新增心跳消息，避免超过一定时间后无法操作的情况
13. 【server】系统缓存中的文件占用空间大小调整为定时更新（10分钟）
14. 【server】修复 bug：分发列表页面点击【创建分发项目】按钮之后不能正常显示【分发节点】感谢
    @xingenhi [点击查看提交记录](https://gitee.com/dromara/Jpom/commit/bd38528fbd3067d220b7569f08449d7796e07c74) [@Hotstrip](https://gitee.com/hotstrip)
15. 【server】fix: 编辑管理员时用户名不可修改
16. 【server】折叠显示部分列表操作按钮（减少误操作）

> 注意：当前版本为 beta 版本。项目中升级了较多依赖版本、新增了部分重要配置（建议确认好后再配置）.如果大家在升级后使用中发现任何问题请及时到微信群反馈,我们会尽快协助排查解决
>
> 1. 如果是已经安装 Jpom、升级到当前版本请勿直接配置数据库账号密码,如果需要配置请手动连接数据库人工修改密码后再配置

------

# 2.5.2

### 新增功能

1. 【agent+server】 新增节点批量升级功能（注意，之前的节点版本不支持该功能需要升级当前版本后才能使用该功能）
2. 【server】节点配置的超时时间单位由毫秒改为秒，并且最小值为2秒
3. 【server】新增构建合并分支日志（便于判断分支冲突问题）

### 解决BUG、优化功能

1. 【server】fix bug:
   分发列表页面，展开某个节点之后点击操作按钮会出现新的一行无效数据。[点击查看提交记录](https://gitee.com/dromara/Jpom/commit/e28b14bcf3dce402ce170a40f9bb93c4d25d0935) [@Hotstrip](https://gitee.com/hotstrip)
2. 【server】fix bug:
   项目监控页面，线程数据加载失败问题 [点击查看提交记录](https://gitee.com/dromara/Jpom/commit/b11c5443db6468a2bf7f6a9fa933f8d965899624) [@Hotstrip](https://gitee.com/hotstrip)
3. 【server】fix bug: 修复低版本浏览器不支持 `.replaceAll()`
   方法 [点击查看提交记录](https://gitee.com/dromara/Jpom/commit/0fb475963153b76546409ac3065a0efe9e647541) [@杨巍](https://gitee.com/fat_magpie_beijing_tony)
4. 【server】update: 更新分发列表 --
   关联分发项目页面操作逻辑（跟老版本操作逻辑一致）[点击查看提交记录](https://gitee.com/dromara/Jpom/commit/cd6e4ae89f833e5e7ef11bd12c324a487de27b1a) [@李道甫](https://gitee.com/koushare_dfli)
5. 【server】update: 优化项目文件管理页面，加载目录树时会多次显示 loading
   层 [点击查看提交记录](https://gitee.com/dromara/Jpom/commit/71b3779bffb36259e0980ce25d4e4082a9d7c2e6) [@Hotstrip](https://gitee.com/hotstrip)
6. 【server】fix bug: 修复节点请求超时可能导致节点项目列表为空
   bug [点击查看提交记录](https://gitee.com/dromara/Jpom/commit/e3182dfa04c27e63a29d67b292a7bfef834f875e) [@Hotstrip](https://gitee.com/hotstrip)
7. 【agent】 fix bug: index 获取进程列表 NPE (感谢@夏末秋初)
8. 【server】fix bug: 修复上传项目压缩文件创建项目目录异常[点击这里查看对应 issue](https://gitee.com/dromara/Jpom/issues/I29FRJ)
9. 【server】fix bug：创建构建时，如果选择
   svn，隐藏掉分支选项。[点击这里查看对应 issue](https://gitee.com/dromara/Jpom/issues/I3TA6S) [感谢 Alexa 提出 issue](https://gitee.com/alexa1989) [@Hotstrip](https://gitee.com/dromara/Jpom/compare/180914f4ddda4dc34fa2df9b169bac7b593dedb0...aa6bb065b6f507ad0bf42225a2aad40e2d25597f)
10. 【server】 fix bug: ssh 构建发布清空历史文件失败（感谢@金晨曦）
11. 【server】update 构建初始化仓库拉取指定分支，不先拉取主分支再切换到指定分支（感谢@大灰灰）
12. 【server】程序关闭时候自动关闭 h2 数据连接池，避免数据库文件被损坏
13. 【server】style:
    优化logo，登录页面，初始化页面 [点击查看对应提交记录](https://gitee.com/dromara/Jpom/commit/5d4783f0be7d44bb04275b059ccd1509620c5828) [@长得丑活得久i](https://gitee.com/zsf_008)
14. 【server】fix bug:
    修复在没有配置nginx白名单时访问nginx列表数据一直加载中问题[点击这里查看对应 issue](https://github.com/dromara/Jpom/issues/5) [@长得丑活得久i](https://gitee.com/zsf_008)
15. 新增 .gitattributes 文件控制命令文件的编码格式以及换行符（感谢@ℳ๓₯㎕斌）

------

# 2.5.1

### 新增功能

1. 【Server】保存邮箱信息时候验证邮箱配置是否正确（感谢@maybe）
2. 【Server】Token 机制采用 jwt
3. 【Server】git 构建新增进度日志输出
4. 【Server】添加操作监控相关 api 和页面功能
5. 【Server】完善 JWT token 过期自动续签功能
6. 【Server】添加前端页面引导系统（使用 introJs）
7. 【Server】访问 ip 限制，支持配置白名单和黑名单来控制 ip 访问权限
8. 【Server】添加服务自启动脚本创建方案，下面贴一下 Server 端自启动方式：

### 解决BUG、优化功能

1. 【Server】全局网络请求新增 loading 状态控制
2. 【Server】获取构建日志关闭 loading 状态
3. 【Agent】控制台日志支持定时清空,避免日志文件太大（感谢@南有乔木）
4. 【Server】在线升级状态判断修复
5. 【Server】修复项目获取进程信息失败（感谢@onlyonezhongjinhui GitHub issues#7）
6. 【Server】项目文件管理中显示项目文件存放真实目录
7. 【Server】项目文件管理中文件夹不存在时，loading不消失（感谢@onlyonezhongjinhui GitHub issues#6）
8. 【Server】文件管理列表不能正常加载二级以上的目录
9. 【Server】添加监控判断用户是否配置报警联系方式（感谢@maybe）
10. 【Server】初始化安装不能自动登录
11. 【Server】页面组件采用国际化采用 zh_cn
12. 【Server】服务器中验证码无法加载（感谢@何好听 Gitee issues#I3E7XQ）
13. 【Agent】解决控制台输出 `Failed to check connection: java.net.ConnectException: Connection refused: connect`,因为没有关闭对应的 jmx
14. 【Agent】解决首页控制台 java 进程列表慢的问题（采用定时拉取并缓存）
15. 【server】fix bug:
    节点列表页面，展开某个节点之后点击操作按钮会出现新的一行无效数据。 [点击查看提交记录](https://gitee.com/dromara/Jpom/commit/b9ecdfa649d27c46bca696e6df088a0908056ff6)
16. 【server】fix bug: 节点列表页面，在没有安装节点的情况下，点击终端按钮会在控制台报错。[点击这里查看对应 issue](https://gitee.com/dromara/Jpom/issues/I3J4UI)
17. 【server】fix bug: 节点管理里面的 Nginx 管理，关闭服务的接口参数传递错了。[点击这里查看对应 issue](https://gitee.com/dromara/Jpom/issues/I3IFZY)
18. 【server】优化系统配置页面的样式，在小屏幕设备上会出现多个竖方向上的滚动条，甚至有时候会遮住底部的操作按钮
19. 【server】ssh 终端命令交互优化（改优化取消之前版本快捷解压功能，删除命令检查）
20. 【server】优化表格的排版和高度等样式，适配页面。详情见 [issue](https://gitee.com/dromara/Jpom/issues/I3EE2R)
20. 【server】优化节点分发关联操作界面。

> 注意事项：
> 1. ssh 终端的删除命令检查临时取消（后面版本会重新优化）
> 2. 该版本新增配置 Jpom 服务方式，需要更新 Server.sh、Agent.sh 文件，在线升级仅升级应用程序不会升级对应的管理命令文件，如果需要使用到该功能还需要手动覆盖更新对应的文件。（如果自定义过管理命令文件则需要差异覆盖）

> 开机自启动：
>
> > 1. 在 Server 端找到 Server.sh 文件，执行命令 `./Server.sh create`，会在当前目录下生成 jpom-server 文件，这个文件就是 Server 端的自启动的文件
> > 2. 在 Agent 端找到 Agent.sh 文件，执行命令 `./Agent.sh create`，会在当前目录下生成 jpom-agent 文件，这个文件就是 Agent 端的自启动的文件
> > 3. 把刚刚生成的自启动文件移动到 /etc/init.d/ 目录
> > 4. 到 /etc/init.d/ 目录让自启动文件拥有执行权限，执行命令 `chmod +x jpom-server` 或者 `chmod +x jpom-agent`
> > 5. 注册到 chkconfig 列表里面，就可以实现开机自启，执行命令 `chkconfig --add jpom-server` 或者 `chkconfig --add jpom-agent`
> > 6. 执行完第 4 步就可以通过 `service jpom-xxx {status | start | stop}` 来管理 Jpom 服务
> > 7. 目前仅通过 Cent OS 服务器测试，其他服务器可能会无效

-----------------------------------------------------------

# 2.5.0

### 新增功能

1. 【server】接入全局 loading 控件
2. 【server】默认进入新版UI

### 解决BUG、优化功能

1. 【Server】fix bug: ssh 列表页面编辑弹窗无法加载（当没有设置文件目录时）
2. 【Server】fix bug: 分发列表，项目运行状态显示错误
3. 【Server】fix bug：第一次安装未能正常打开初始化账号密码页面
4. 【server】fix bug: 独立分发项目编辑时，jvm args 等参数不会回显
5. 【server】fix: 点击构建自动打开构建日志、构建日志弹窗自动滚动到底部
6. 【server】add: index.html 添加打包时间
7. 【server】fix bug：添加、编辑用户原始密码进行了sha1
8. 【server】add: 添加构建历史回滚操作（感谢@李道甫）
9. 【server】add: 添加项目文件管理页面上传压缩文件（感谢@李道甫）
10. 【server】fix bug: 文件上传时显示上传进度（感谢@李道甫）
11. 【server】fix bug: 项目文件管理的侧边文件树优化（感谢@李道甫）
12. 【server】fix: 控制台日志弹窗自动滚动到底部（感谢@南有乔木）
13. 【server】add: File方式创建项目 项目控制台互调（感谢@李道甫 贡献）
13. 【server】add: 分发提示修改 分发项目显示 （感谢@李道甫 贡献）

> 注意：目前新版本登录状态采用固定 token 模式，登录后将一直保持在线状态，如需要退出或者离线需要进行退出登录操作。（该问题将于后面版本进行优化调整）

-----------------------------------------------------------

# 2.4.9 - 3.0.0(beta)

> 当前版本为重构页面后的预览版本

### 新增功能

1. 【Server】新增监控用户操作记录
2. 【Agent】新增配置是否禁用根据jmx获取项目状态（默认启用）
3. 项目文件管理支持在线修改文件（感谢@Chen 贡献）
4. 3.0.0bata版本的页面重构[采用vue项目编写]（感谢@Hotstrip）
5. 新增项目启动banner输出（感谢@Hotstrip）

### 解决BUG、优化功能

1. 【Server】 优化判断构建命令中的删除命令关键词
2. 【Server】 优化删除构建历史、构建代码（避免不能删除情况）
3. 【Agent】 调整项目的jvm 和 args参数支持url编码。避免xss后冲突
4. 优化获取项目当前运行路径问题
5. 【Server】开始构建时输出代码目录
6. 【Server】编辑构建类型为SVN没有分组bug（感谢@JAVA-落泪归枫）
7. 更新文档Jpom 的JDK要为1.8.0_40+（感谢@JAVA 企鹅）
8. 【Server】数据库初始化时间前置,打印成功日志，未初始化结束数据库相关操作都忽略
9. 【Server】修复报警恢复后，报警列表中的报警状态显示报警中的错误（感谢@南有乔木）
10. 更新hutool 版本至5.4.x （能避免系统缓存页面里面获取文件大小卡死）
11. 调整Jpom启动输出日志,启动消息采用控制台输出不再打印error级别的启动消息

> 特别感谢：@Hotstrip 对Jpom的前端页面采用vue重构编写
>
> 当前版本为3.x版本前的过渡版本

-----------------------------------------------------------

# 2.4.8

### 新增功能

1. 【Agent】读取进程新增 `ps -ef | grep xxx` 方式（感谢@JAVA-落泪归枫）

### 解决BUG、优化功能

1. 【Server】构建历史中记录字段不全问题（感谢@￡天空之城～龙）
2. 【Server】Java-WebSocket 模块漏洞版本更新 来源 [Github GHSA-gw55-jm4h-x339](https://github.com/advisories/GHSA-gw55-jm4h-x339)
3. 【Server】节点分发列表点击控制台、文件管理404
4. 【Server】节点分发顺序重启休眠时间取构建名称最后的时间（测试构建:10 则睡眠时间为10秒）
5. 【Agent】启动完成打印授权信息日志级别调至error
6. CommandUtil.asyncExeLocalCommand 方法格式化命令中的换行
7. 优化启动读取进程文件目录避免包含node_modules 目录卡死
8. 【Server】修复构建命令中判断是否包含【rm、del、rd】bug （感谢@落泪归枫）
9. 【Server】修改删除节点会修改掉非管理员的账号密码bug
10. 【Server】 构建历史根据权限查询

-----------------------------------------------------------

# 2.4.7

### 新增功能

1. [支持maven快速编辑节点项目](https://gitee.com/keepbx/Jpom-Plugin/tree/master/jpom-maven-plugin) （配合`jpom-maven-plugin`使用）(
   感谢@夜空中最亮的星)
2. 【Agent】 新增jdk 管理，不同项目选择不同的jdk （GITEE@IV8ZZ）
3. 【Server】构建新增分组属性，方便快速选择
4. 【Agent】 新增[JavaExtDirsCp] 运行模式 (感谢@TXpcmgr（Geiger）)
5. 【Server】 ssh 连接方式新增私钥证书连接
6. 【Server】 ssh文件管理新增解压操作（感谢@TXpcmgr（Geiger）贡献）
7. 【Agent】 项目新建副本集，方便单机快速运行多个副本
8. 【Server】构建发布后操作支持副本集相关操作

### 解决BUG、优化功能

1. 完善使用nginx之类代理二级目录，指定端口路径跳转问题（感谢@😯😨😰😱 ）
2. 解决菜单路径不正确问题（GITEE@I15O46）
3. 【Agent】 windows中Agent关闭，Agent中所有项目跟随关闭（感谢@java gods）
4. 【Server】构建命令包含删除命令误判断（感谢@Sawyer）
5. 【Server】构建历史支持配置单个构建最多保存多少个历史
6. 【Server】解决节点分组筛选bug(感谢gitee@I17XEH)
7. 【Server】角色权限动态数据，单个节点异常不影响所有节点配置（感谢@￡天空之城～龙）
8. 【Server】关联节点分发项目支持修改发布后操作
9. 补充说明文档：[详情](https://jpom-site.keepbx.cn/docs/index.html#/) (感谢@TXpcmgr（Geiger）)
10. 更新部分插件依赖版本【hutool、fast-boot、fastjson】

> 注意：如果在2.4.7以下项目运行方式中使用过【War】模式的由于【War】更名为【JarWar】 所有在升级后请重新修改运行方式后再运行对应项目

-----------------------------------------------------------

# 2.4.6

### 新增功能

1. 【Agent】 nginx管理支持自定义编译运行，管理方式变更
2. 【Server】 监控通知新增企业微信（感谢@TinyBao。）
3. 管理脚本支持自动识别环境变量和java路径（linux环境）
4. 项目类型新增File(快速管理纯静态文件)

### 解决BUG、优化功能

1. 【Server】解决分发列表项目状态显示不正确（感谢@群友）
2. 【Server】修复权限选择错乱和无法正确过滤问题【注意此版本的角色动态权限不兼容旧数据，需要重新授权动态数据权限】（感谢@Java-OutMan）
3. 调整项目日志输出
4. 更新【commons-compress】依赖版本[漏洞升级]
5. 【Server】构建弹窗条件构建名称（感谢@Sawyer）
6. json文件读取异常提示（感谢@Taller）
9. 【Server】 优化ssh上传文件、删除文件
10. InternalError 异常捕捉
11. 【Server】优化Nginx 非80、443端口 二级路径代理重定向问题（感谢@😯😨😰😱 ）

### 升级注意

1. 此版本更新控制台日志级别有调整，如果使用管理命令方式运行日志级别将不再打印info级别，如果需要打印info级别的请调整管理命令中的`--spring.profiles.active=pro`
   为 `--spring.profiles.active=dev`
2. 使用Nginx 二级路径代理请一定使用Jpom 推荐nginx配置[查看配置](https://jpom-site.keepbx.cn/docs/index.html#/辅助配置/nginx-config)

-----------------------------------------------------------

# 2.4.5

### 新增功能

1. 【Server】节点列表支持筛选（感谢@￡天空之城～龙）
2. 【Server】新增构建触发器（感谢@java 麦田英雄）
3. 【Server】新增自动清理过量的构建历史记录和文件（感谢@Sawyer、@Jvmlz）
4. 【Server】构建支持ssh发布（感谢@￡天空之城～龙）
5. 【Server】节点新增分组属性，方便多节点快速筛选（感谢@￡天空之城～龙）
6. 新增windows快速升级
7. 【Server】layui升级到最新版，文件上传支持进度条
8. 新增节点内存、cpu、硬盘使用情况采集报表（感谢@￡天空之城～龙）
9. 节点首页新增快速结束进程方式

### 解决BUG、优化功能

1. 【Server】节点分发需要节点数大于二（感谢@Sawyer）
2. 修复未加载到tools.jar判断（感谢@java-磊）
3. 【Server】控制台新增自动清屏开关（感谢@Jvmlz）
4. 上传文件大小限制，配置化
5. 【Server】构建文件copy忽略隐藏文件
6. 【Server】不能清除错误进程缓存（感谢@java 李道甫）
7. 【Agent】长时间运行jpom无法监控到项目运行状态（感谢@java 李道甫、@洋芋）
8. 【Server】节点分发编辑支持修改分发后的操作
9. 【Agent】脚本模板跟随系统编码
10. 【Server】tomcat控制台删除日志文件错误（感谢@Java-iwen）
11. 【Agent】自动备份控制台日志表达式为none,不生成日志备份
12. 【Server】角色授权编辑权限不能创建数据（感谢@Lostshadow）
13. 【Server】tomcat动态权限配置不正确（感谢@Lostshadow）

-----------------------------------------------------------

## 2.4.4

### 新增功能

1. 【Agent】添加对SpringBoot war包支持

### 解决BUG、优化功能

1. 【Server】新项目打开项目控制台页面报错（感谢@黄战虎）
2. 【Server】修改邮箱不及时生效问题（感谢@WeChat）
3. 【Server】修复发布构建产物路径bug（感谢@Sawyer）
4. 优化执行命令方式
5. 脚本模板在linux 不添加权限（采用sh 方式执行）
6. 【Server】修复添加节点分发项目报错的数据异常（感谢@WeChat）

-----------------------------------------------------------

## 2.4.3

### 新增功能

1. SpringBoot 升级到2.1.x
2. 【Server】velocity模板引擎升级为thymeleaf
3. 【Server】构建支持svn类型仓库（感谢@群友 .）
4. 插件端自动注册到服务端（感谢@群友 .）
5. 新增在线修改配置并可及时重启
6. 新增WebSSH 管理功能
7. 【Server】用户新增邮箱和钉钉群webhook 属性
8. 【Server】监控报警通知改为联系人
9. 【Server】引人netty插件（感谢@夜空中最亮的星）
10. 支持docker 容器运行（感谢@24k）
11. 【Server】 新增清空构建代码（解决代码冲突）(感谢@xieyue200810)
12. 搭建插件化基础架构
13. 用户权限重构，使用角色支持更细粒的权限控制
14. 新增ssh快速部署插件端
15. 新增一键安装脚本[详情](https://gitee.com/dromara/Jpom/#%E4%B8%80%E9%94%AE%E5%AE%89%E8%A3%85)

### 解决BUG、优化功能

1. 【Server】未登录重定向带入参数
2. 【Server】页面登录方法调整支持自定义事件登录
3. 【Server】删除节点、分发验证是否存在关联数据,分发释放分发关系
4. 项目白名单目录调整为属性
5. 【Server】编辑用户回显节点选中错乱问题
6. 调整linux管理命令脚本防止在线升级产生tail 进程
7. 【Agent】插件端的脚本模板路径切换到数据目录下
8. 【Agent】Windows异步执行命令调整不使用[INHERIT]（防止插件端进程阻塞）
9. 【Server】分页查询会存在字段not found
10. 【Server】构建命令不能包含删除命令（del,rd,rm）
11. 支持配置初始读取日志文件最后多少行【log.intiReadLine】(感谢@夜空中最亮的星)
12. 优化节点首页饼状图统计
13. 取消用户输入脚本模板id
14. 重定向支持自动识别 Proto（解决http-> https iframe报错）
15. 构建执行命令存在错误只是提示，不取消执行（感谢@Sawyer）
16. 构建打包目录没有文件名异常（感谢@Sawyer）
17. 修改为专属包名【io.jpom】

### 升级注意事项

1. 由于修改包名引起：如果在旧版本中使用过在线升级，本次升级需要手动上传jar到到服务器中执行命令升级，并且删除旧包并且覆盖管理命令文件

-----------------------------------------------------------

## 2.4.2

### 新增功能

1. 新增实时查看tomcat日志
2. 【Server】分发包支持更多压缩格式
3. 页面菜单采用json文件配置(支持二级菜单)
4. 【Server】分发包支持更多类型的压缩格式
5. 【Server】节点支持配置请求超时时间
6. 支持配置是否记录请求、响应日志【consoleLog.reqXss、consoleLog.reqResponse】
7. 新增日志记录最大记录条数【默认100000】
8. 【Server】layui 升级到2.5.4
9. 【Server】新增项目监控功能
10. 【Server】新增在线构建项目功能
11. 【Server】新增查看项目实际执行的命令行
12. 【Server】新增分发日志
13. 新增清空文件缓存、临时数据缓存
14. 在线查看、下载Jpom运行日志(windows不能实时查看)
15. 新增linux在线升级

### 解决BUG、优化功能

1. 【Agent】logBack页面最后修改时间不能正确显示（感谢@JAVA jesion）
2. 【Agent】nginx修改内容截断，不正确情况（感谢@JAVA jesion）
3. 【Agent】nginx、脚本模板保存内容xss标签还原
4. 【Server】节点分发页面的交互方式
5. 【Server】页面菜单分类整理
6. 【Agent】修复SpringBoot相对文件夹下无法读取配置问题
7. 【Agent】缓存异常的jvm进程，避免卡死状态（感谢@java 李道甫）
8. 【Server】节点分发状态更新到所有节点状态
9. 【Server】节点分发白名单独立页面配置
10. 【Server】项目控制台未运行能查看已经存在的最后的日志
11. 【Agent】删除阿里云oss构建，已经有在线构建功能代替
12. 【Server】修改证书名称和导出证书问题
13. 打包方式改为一个可执行的jar
14. 【Server】解决编辑用户页面json转换异常（感谢@JAVA jesion）
15. 分发项目新增清空发布防止新旧jar冲突
16. 【Server】优化节点列表页面加载速度[不显示运行的项目数]（感谢@java 李道甫）
17. 【Agent】调整启动，关闭进程命令执行方式[解决重启不能监控项目状态]（感谢@java 李道甫）
18. 【Agent】调整进程标识传入参数到JVM参数中，避免和部分框架冲突（感谢@java-杨侨）

### 升级注意事项

1. 需要删除旧lib目录所有文件
2. 覆盖旧版管理命令文件

-----------------------------------------------------------

## 2.4.1

### 新增功能

1. 【Agent】新增线程列表监控(感谢@其锋)
2. 【Agent】新增节点脚本模板(感谢@其锋)
3. 【Server】新增所有页面添加公共Html代码
4. 新增Tomcat管理
5. 【Agent】导入证书文件新增对cer、crt文件支持
6. 【Agent】导入项目包时指出多压缩包[tar|bz2|gz|zip|tar.bz2|tar.gz] (感谢@群友)
7. 【Agent】新增配置控制台日志文件编码格式（详情查看extConfig.yml）

### 解决BUG、优化功能

1. 【Server】节点首页，右上角管理路径错误(感谢@其锋)
2. 【Server】查看用户操作日志支持筛选用户
3. 【Server】页面数据路径权限判断修复(感谢@Will)
4. 【Agent】优化获取进程监听端口的，防止卡死
5. 文件的读写锁不使用 synchronized关键字提高效率
6. 优化数据id字段的输入限制，数字+字母+中划线+下划线（感谢@JAVA jesion）
7. 【Agent】连接JVM失败则跳过（感谢@JAVA jesion）
8. 【Server】编辑用户页面优化选择授权项目
9. 【Agent】项目Jvm参数和Args参数兼容回车符（感谢@牛旺）

-----------------------------------------------------------

## 2.4.0

### 新增功能

1. 首页进程列表显示属于Jpom项目名称(感谢@〓下页)
2. 多节点统一管理（插件模式）
3. 证书解析支持cer 证书(感谢@JAVA jesion)
4. 新增记录用户操作日志[采用H2数据库]（感谢@〓下页）
5. 节点分发功能、合并管理项目(感谢@其锋)

### 解决BUG、优化功能

1. 解析端口信息兼容`:::8084`(感谢@Agoni 、)
2. 进程id解析端口、解析项目名称带缓存
3. 项目分组变更，项目列表及时刷新(感谢@〓下页)
4. 批量上传文件数量进度显示(感谢@群友)
5. linux udp端口信息解析失败(感谢@Ruby)
6. jar模式读取主jar包错误(感谢@其锋)

-----------------------------------------------------------

## 2.3.2

### 新增功能

1. 控制台日志支持配置保留天数
2. 项目列表状态跟随控制台刷新
3. 项目配置页面优化交互流程
4. 项目列表显示正在运行的项目的端口号(感谢@洋芋)
5. 新版的Windows管理命令(感谢@洋芋)
6. 支持类似于Nginx二级代理配置(感谢@№譜樋)
7. 记录启动、重启、停止项目的操作人
8. Jpom 数据路径默认为程序运行的路径(感谢@〓下页)
9. 首页进程监听表格显示端口号(感谢@洋芋)
10. 保存时检查Oss信息是否正确
11. Jpom管理命令新增判断`JAVA_HOME`环境变量
12. 修改用户信息，在线用户需要重新登录

### 解决BUG、优化功能

1. 修改WebHooks 不生效
2. 初始化系统白名单初始化失败(感谢@洋芋)
3. 指定Cookie名称防止名称相同被踢下线(感谢@洋芋)
4. 优化未加载到tools.jar的提示(感谢@№譜樋)
5. 构建按钮移动到文件管理页面中
6. 优化nginx列表显示数据、取消nginx快捷配置
7. 证书管理页面交互优化
8. 取消安全模式功能（有更完善的权限代替）
9. 管理员不能修改自己的信息

-----------------------------------------------------------

## 2.3.1

#### 新增功能

1. 添加创建项目判断项目id是否被占用
2. 项目列表中添加悬停突出显示效果
3. 生产环境中检查Jpom 运行标识和项目id是否冲突
4. windows 管理命令支持停止Jpom
5. 防止暴力登录新增限制ip登录失败次数
6. 用户前台输入密码传输加密（感谢@JAVA jesion）
7. 首页页面自动刷新按钮状态记忆功能（感谢@Mark）
8. Jpom启动成功会自动在数据目录中创建进程id信息文件如`pid.27936`
9. 证书管理支持导出、查看代码模板功能

#### 解决BUG

1. 解决配置JVM、ARGS时，不能获取到程序运行信息bug(感谢@Agoni 、)
2. 减少登录图形验证码干扰线(感谢@Mark)
3. 项目编辑页面JVM、ARGS调整为多行文本(感谢@JAVA jesion)
4. jar模式MainClass非必填
4. 优化JDK32位和64位冲突时自动跳过(感谢@13145597)
5. 用户授权项目权限不足问题

#### 升级注意事项

1. 由2.2及以下升级到 2.3.x 需要手动删除Jpom数据目录中的`data/user.json` 文件、所有用户账户信息将失效需要重新添加

-----------------------------------------------------------

## 2.2

1. 解决批量上传文件造成卡死的问题
2. 控制台读取自动识别文件编码格式
3. 退出登录出现异常页面
4. 根据对应权限显示对应菜单
5. 系统管理员可以在线解锁锁定的用户

-----------------------------------------------------------

## 2.1

1. 全面取消调用命令文件执行
2. 静态资源缓存问题
3. 首页监控图表更新
4. 多处细节优化
5. 分别支持ClassPath和Jar模式
6. 证书文件支持验证私钥是否匹配

-----------------------------------------------------------

## 2.0

1. 优化安全问题
2. 兼容windows
3. 使用JVM获取运行状态
