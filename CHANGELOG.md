# 🚀 版本日志

### 2.10.42 (2023-08-24)

### 🐣 新增功能

1. 【server】新增 集群化管理工作空间（感谢@定格、[@paobu](https://gitee.com/iniushi) [Gitee issues I7UG5V](https://gitee.com/dromara/Jpom/issues/I7UG5V)）
2. 【server】优化 ssh 相关功能支持 openssh8+
   （感谢 [@孤城落寞](https://gitee.com/gclm) [Gitee pr 193](https://gitee.com/dromara/Jpom/pulls/193) ）
3. 【server】新增 SSH
   文件管理修改文件权限功能（感谢 [@MichelleChung](https://gitee.com/michelle1028) [Gitee issues I6VDXS](https://gitee.com/dromara/Jpom/issues/I6VDXS) ）
4. 【server】新增 Docker 容器重建功能，即删除原有的容器，重新创建一个新的容器
5. 【server】新增 Docker 管理增加 SSH 连接

### 🐞 解决BUG、优化功能

1. 【server】修复 资产管理 SSH 管理系统名称显示未知问题（感谢@勤思·）
2. 【server】优化 资产管理 Docker 管理支持配置分组
3. 【server】优化 仓库管理支持配置分组
4. 【server】优化 SSH 文件夹支持前端排序（感谢@勤思·）
5. 【server】优化 仓库账号、 SSH 证书密码支持选择环境变量
6. 【all】升级 commons-compress、fastjson、hutool 版本
7. 【server】优化 maven 依赖冲突
8. 【server】优化 文件发布-节点发布文件名使用真实名称（感谢@勤思·）
9. 【server】优化 文件发布-ssh发布新增变量：FILE_NAME、FILE_EXT_NAME
10. 【server】升级 h2、SpringBoot 版本
11. 【server】使用系统git时，无法克隆tag问题优化 （感谢@唐明）
12. 【server】优化 SSH 和 代码仓库的密码从工作空间变量中读取
13. 【server】优化
	删除工作空间前预检查关联数据存在情况（感谢 [@陈旭](https://gitee.com/chenxu8989) [Gitee issues I7F0ZN](https://gitee.com/dromara/Jpom/issues/I7F0ZN) ）
14. 【server】优化
	退出登录支持彻底退出、切换账号退出（感谢 [@wangfubiao](https://gitee.com/wangfubiao) [Gitee issues I7GA5Q](https://gitee.com/dromara/Jpom/issues/I7GA5Q) ）
15. 【server】优化 IP 白名单验证忽略 IPV6 情况
16. 【server】优化 服务端缓存管理支持查看黑名单 IP 详细信息（感谢@酱总）
17. 【server】修复 SSH
	编辑输入框出现部分关键词时保持报错（感谢 [@一只羊](https://gitee.com/hjdyzy) [Gitee issues I7E3UG](https://gitee.com/dromara/Jpom/issues/I7E3UG) ）
18. 【server】优化 日志组件支持显示 \t 制表符、清空缓冲区滚动到顶部
19. 【server】修复 彻底删除节点分发时未自动删除关联日志（感谢@ccx2480）
20. 【server】修复
	节点管理中脚本模板翻页无效（感谢 [@wangfubiao](https://gitee.com/wangfubiao) [Gitee issues I7F0RS](https://gitee.com/dromara/Jpom/issues/I7F0RS) ）
21. 【server】优化
	工作空间配置页面中新增节点分发白名单配置入口（感谢 [@陈旭](https://gitee.com/chenxu8989) [Gitee issues I7F0W0](https://gitee.com/dromara/Jpom/issues/I7F0W0) ）
22. 【server】优化 构建附加环境变量支持解析 URL 参数格式
	（感谢 [@爱琳琳真是太好了](https://gitee.com/qiqi513_admin) [Gitee issues I7FROG](https://gitee.com/dromara/Jpom/issues/I7FROG) ）
23. 【server】优化 构建支持单个配置保留天数和保留个数
	（感谢 [@阿超](https://gitee.com/VampireAchao) [Gitee issues I7FOG2](https://gitee.com/dromara/Jpom/issues/I7FOG2) ）

------

### 2.10.41 (2023-06-16)

### 🐣 新增功能

1. 【server】新增 SSH 列表支持显示 docker 版本信息
2. 【server】优化 Docker 镜像增加批量删除（已经被容器使用的镜像不会删除）
3. 【server】优化 启用 Jpom 新版专属 logo
4. 【server】新增 工作空间新增分组字段（存在多个分组时页面切换将使用二级选择）（感谢@酱总）
5. 【server】新增 仓库支持导入导出
6. 【server】新增 镜像创建容器支持配置 hostname、集群服务支持配置 hostname（感谢@心光）

### 🐞 解决BUG、优化功能

1. 【server】修复 查看 docker 容器日志 web socket 线程被阻塞问题
2. 【server】优化 日志组件显示高亮、滚动条样式优化
3. 【server】优化 web socket 会话关闭显示分类
4. 【server】优化 页面滚动条样式
5. 【server】优化 编辑关联分发，选择项目下拉框不能显示项目全名称（tooltip）（感谢@LYY）
6. 【server】优化 监听页面关闭事件，主动关闭 websocket
7. 【server】修复 批量构建触发器无法正常使用（感谢 [@botboy](https://github.com/cheakin) [Github issues 48](https://github.com/dromara/Jpom/issues/48) ）
8. 【server】修复 页面关闭 docker 终端未主动关闭后台终端进程问题
9. 【server】优化 docker 终端页面缓冲区大小自动适应
10. 【server】优化 项目列表可以查看项目日志（避免控制台卡顿无法操作下载日志）(感谢@阿超)
11. 【server】优化 日志组件采用虚拟滚动渲染，避免日志过多浏览器卡死
12. 【server】优化 资产管理支持管理共享仓库
13. 【server】优化 增大验证码检测功能异常捕捉范围
14. 【server】修复 令牌导入仓库令牌长度不足问题（感谢 [@Sherman Chu](https://github.com/yeliulee) [Github issues 45](https://github.com/dromara/Jpom/issues/45) ）
15. 【server】修复 分发列表配置功能无法使用（感谢 [@Free](https://gitee.com/fjlyy321) [Gitee issues I716UI](https://gitee.com/dromara/Jpom/issues/I716UI) ）
16. 【server】修复 构建卡片布局、构建详情中构建方式显示不正确（感谢@A）

### ⚠️ 注意

1. 如果自定义过 SSH 监控脚本需要自行同步获取 docker 信息脚本

------

## 2.10.40 (2023-04-19)

### 🐣 新增功能

1. 【server】新增 容器构建中对 gradle 插件的支持（感谢 [@xiaozhi](https://gitee.com/XiaoZhiGongChengShi) [Gitee pr 188](https://gitee.com/dromara/Jpom/pulls/188) ）

### 🐞 解决BUG、优化功能

1. 【server】修复 日志搜索控制台无法正常使用（感谢@左手生活，右手浪漫）
2. 【server】修复 项目文件跟踪控制台无法正常使用（感谢@左手生活，右手浪漫）
3. 【server】修复 插件端日志无法正常差异
4. 【server】修复 docker 拉取镜像不能识别私有仓库地址（@章强）
5. 【server】优化 编辑构建无法重置已经选择的事件脚本 （感谢@左手生活，右手浪漫）
6. 【server】优化 登录页面切换验证码自动清空验证码输入框（感谢@TrouBles）
7. 【server】修复 docker 集群日志查看后未自动关闭造成日志文件继续增长的问题（@无味。）
8. 【server】优化 服务端缓存项目信息的创建时间和修改时间同步为节点中的数据创建、修改时间
9. 【server】优化 文件管理支持批量删除（感谢@左手生活，右手浪漫）
10. 【agent】优化 取消 hutool-cache 包依赖
11. 【server】优化 JustAuth fastjson 依赖配置为 fastjson2
12. 【agent】修复 获取项目状态部分情况出现 NPE （感谢@酱总）
13. 【server】修复 清空浏览器缓存未跳转到登录页面
14. 【server】优化 构建拉取 git 仓库支持使用服务器中的 git 插件，实现配置克隆深度参数
15. 【server】修复 删除节点脚本报错（感谢 [@xiaozhi](https://gitee.com/XiaoZhiGongChengShi) [Gitee issues I6USMY](https://gitee.com/dromara/Jpom/issues/I6USMY) ）
16. 【server】优化 构建 SSH 发布命令支持 `SSH_RELEASE_PATH` 环境变量（感谢@定格）
17. 【server】修复 全屏终端无法打开文件管理（感谢@Pluto）
18. 【server】优化 自动探测服务端登录验证码是否可用
19. 【all】优化 文件编辑后缀识别支持配置文件名或者正则表达式（感谢@MichelleChung）
20. 【server】优化 支持自动执行触发器清理
21. 【server】优化 重新登录未加载管理员菜单（@A）
22. 【server】修复 第三方登录跳转测试丢失
23. 【server】修复 仓库编辑清除密码按钮弹窗层级问题（感谢 [@轩辕豆豆](https://gitee.com/xuanyuandoudou) [Gitee issues I6VSCR](https://gitee.com/dromara/Jpom/issues/I6VSCR) ）
24. 【server】修复 优化构建列表卡片布局存在未构建数据布局错乱问题（感谢 [@lin_yeqi](https://gitee.com/lin_yeqi) [Gitee issues I6VUB7](https://gitee.com/dromara/Jpom/issues/I6VUB7) ）

------

## 2.10.39 (2023-04-04)

### 🐞 解决BUG、优化功能

1. 【server】修复 资产管理机器管理单个分配工作空间无法正常使用（感谢@咻咻咻秀啊）
2. 【server】修复 资产管理相关权限、操作日志无法记录问题（感谢@咻咻咻秀啊）
3. 【server】修复 docker 控制台 、日志无法正常使用
4. 【server】优化 docker 控制台页面布局优化，支持单独查看 docker-compose
5. 【server】优化 docker 实时查看日志支持配置是否显示时间戳
6. 【server】修复 查看文件发布详情节点名称未显示
7. 【server】优化 发布记录重建不能选中节点
8. 【server】修复 构建同步到文件管理中心失败（感谢@破冰）
9. 【server】优化 登录成功主动刷新菜单缓存、切换账号登录工作空间无权限页面白屏（感谢@A、@零壹）
10. 【all】更名 变更包名为 `org.dromara.jpom`
11. 【server】修复 编辑 docker 导入证书弹窗无法正常显示问题（感谢@左手生活，右手浪漫）
12. 【server】修复 工作空间中资产管理相关页面搜索无数据时出现操作引导提示（感谢@酱总）

------

## 2.10.38 (2023-03-31)

### 🐣 新增功能

1. 【server】新增 证书管理全部迁移到服务端统一导入 （感谢@.）
2. 【server】新增 节点项目支持导入，导出（感谢@酱总）
3. 【server】新增 支持 oauth2 登录（maxkey、gitee、github） （感谢 [@MaxKeyTop](https://gitee.com/maxkeytop_admin) [Gitee pr 183](https://gitee.com/dromara/Jpom/pulls/183) 、@A）
4. 【all】新增 文件管理发布支持发布到节点指定目录
5. 【server】新增 构建新增配置排除发布目录表达式（感谢@毛毛虫）
6. 【all】新增 节点脚本支持全局共享（感谢@奇奇）
7. 【server】新增 构建状态新增队列等待，用于标记当前构建存于线程排队中（感谢@酱总）

### 🐞 解决BUG、优化功能

1. 【server】优化 清理单项构建历史保留个数只判断（构建结束、发布中、发布失败、发布失败）有效构建状态，避免无法保留有效构建历史（感谢@张飞鸿）
2. 【server】优化 节点监控超时时间调整为 30 秒（避免 windows 服务器频繁超时）（感谢@波比）
3. 【server】优化 打开节点管理页面不刷新节点列表
4. 【agent】修复 未配置节点白名单时直接创建分发项目报错（感谢@奋起的大牛）
5. 【server】修复 SSH 关联工作空间的授权目录无法取消
6. 【server】优化 查看分发项目状态取消折叠 table，调整为独立页面
7. 【server】优化 逻辑节点没有显示快速安装按钮问题（感谢@酱总）
8. 【server】优化 docker TLS 证书全部迁移到证书管理，配置证书支持快捷选择 （感谢@.）
9. 【server】修复 仓库 ssh 协议配置超时时间无法正常拉取代码（感谢@毛毛虫）
10. 【server】优化 环境管理页面支持查看间隔任务统计信息
11. 【server】优化 令牌导入仓库模块统一调整为模板配置（部分方式不支持搜索）（感谢@魏宏斌）
12. 【agent】优化 DSL 项目报警内容添加状态消息（感谢@核桃）
13. 【server】优化 服务端脚本支持配置全局共享（感谢@酱总）
14. 【server】优化 删除管理脚本中的 `-XX:+AggressiveOpts` 参数
    （感谢 [@牛孝祖](https://gitee.com/niuxiaozu) [Gitee issues I6PUNM](https://gitee.com/dromara/Jpom/issues/I6PUNM) ）
15. 【all】升级 springboot、hutool、fastjson2、svnkit 版本
16. 【server】修复 资产管理 ssh 分组不生效问题（感谢@A）
17. 【server】优化 构建详情页面布局（构建触发器、查看构建历史）
18. 【server】优化 新增构建状态描述来记录构建异常信息
19. 【server】优化 构建页面新增卡片布局方式
20. 【server】修复 SSH 分组无法正常搜索、排序异常（感谢@A）
21. 【server】优化 构建命令支持引用脚本模板内容（便于复杂构建命令管理）（感谢@毛毛虫）
22. 【server】新增 构建状态新增`队列等待`，用于标记当前构建存于线程排队中（感谢@酱总）
23. 【server】修复 创建构建选择命令模板无法修改（感谢@定格）
24. 【server】优化 构建新增配置是否发布隐藏文件属性（感谢@简单）

### ⚠️ 注意

1. 如果节点已经配置过项目文件下载远程地址白名单需要统一配置到服务端的工作空间的白名单。
2. 已经配置节点项目远程下载白名单将保留只读，不做实际判断
3. 构建触发器变动，发生异常时 type 为 error,并且新增：statusMsg 字段

### ❌ 不兼容功能

1. 【agent】取消 节点管理证书管理取消上传编辑功能（保留查询删除功能）
2. 【agent】取消 节点白名单配置取消 ssl 证书路径配置
3. 【agent】取消 节点项目文件下载远程文件白名单统一调整到服务端白名单配置

------

## 2.10.37 (2023-03-21)

### 🐣 新增功能

1. 【server】新增 文件中心添加别名码来为文件进行分类下载，构建添加别名码可以同步到文件中心
   （感谢 [@大灰灰大](https://gitee.com/linjianhui) [Gitee issues I6OUC8](https://gitee.com/dromara/Jpom/issues/I6OUC8) ）
2. 【server】新增 服务端在线升级支持配置 beta 计划（”妈妈“再也不用担心没有稳定版了）（感谢@罗俊）

### 🐞 解决BUG、优化功能

1. 【server】优化 容器构建 maven 插件版本错误提示可用版本号，如果构建容器已经存在则忽略远程版本（感谢@大灰灰）
2. 【server】优化 脚本列表显示脚本 ID，方便快速查看复制
   （感谢 [@大灰灰大](https://gitee.com/linjianhui) [Gitee issues I6OUDT](https://gitee.com/dromara/Jpom/issues/I6OUDT) ）
3. 【server】优化 文件管理列表显示，小屏幕部分字段被隐藏（感谢@tinsang）
4. 【server】优化 docker 拉取镜像自动解析 tag，避免拉取所有镜像，如果没有配置 tag 默认使用 latest（感谢@Again...                       .）
5. 【server】修复 数据库迁移到 mysql 报错（字段不存在）（感谢@轩辕豆豆）
6. 【server】修复 节点统计页面错乱问题
  （感谢 [@轩辕豆豆](https://gitee.com/xuanyuandoudou) [Gitee issues I6OYSU](https://gitee.com/dromara/Jpom/issues/I6OYSU) ）

------

## 2.10.36 (2023-03-20)

### 🐞 解决BUG、优化功能

1. 【all】优化 缓存管理统一全局任务刷新
2. 【server】优化 修复数据关联的工作空间ID sql（避免 '' 或者 'null' 无法修复）
3. 【server】优化 支持手动清理错误工作空间 ID 的数据
4. 【server】修复 构建 git 仓库无法正常获取问题（感谢@小翼哥）

------

## 2.10.35 (2023-03-20)

### 🐞 解决BUG、优化功能

1. 【server】修复 mysql 数据库无法正常加载（感谢@酱总）

------

## 2.10.34 (2023-03-20)

### 🐣 新增功能

1. 【server】新增 资产管理 SSH 管理支持导入导出数据（感谢@吃葫芦娃的土拨鼠）
2. 【server】新增 文件管理中心（用于统一存储管理公共文件）
3. 【server】新增 仓库令牌导入支持 gogs （gogs 和 gitea 标准一致）
   （感谢 [@爱琳琳真是太好了](https://gitee.com/qiqi513_admin) [Gitee issues I6CRPS](https://gitee.com/dromara/Jpom/issues/I6CRPS) ）

### 🐞 解决BUG、优化功能

1. 【server】优化 构建 SSH 发布上传文件执行输出上传进度
2. 【server】优化 在线构建产物支持同步到文件管理中心
3. 【server】优化 节点分发、在线构建 webhook 添加 `triggerUser` 参数（感谢@酱总）
4. 【server】优化 SSH 文件夹管理支持重命名文件夹（感谢@零壹）
5. 【server】优化 机器名称和 hostname、SSH 机器名称和 hostname 限制字段长度
6. 【server】优化 DSL 项目支持解析多 PID ：`running:109,205:8080,8082`（感谢@酱总）
7. 【server】优化 缓存管理页面支持查看运行中的线程同步器、正在构建的ID
8. 【server】优化 SSH 脚本批量执行采用线程同步器执行（避免线程数大于 CPU 核心数）
9. 【server】优化 构建 SSH 发布命令响应方式调整为逐行（避免长时间没有任何信息输出）
10. 【server】优化 资产管理支持批量分配到工作空间

### ⚠️ 注意

1. 【server】节点管理和项目管理菜单合并到一个菜单
2. 【server】节点统计页面合并到逻辑节点中不同视图模式查看

### ❌ 不兼容功能

1. 【server】取消 低版本（2.9.x 及其一下）的构建触发器 token 自动同步为新版本

------

## 2.10.33 (2023-03-16)

### 🐣 新增功能

1. 【server】新增 ssh 基础信息监控（非报警监控）
2. 【agent】新增 DSL 项目支持解析端口号：`running:109:8080,8082`
   （感谢 [@大灰灰大](https://gitee.com/linjianhui) [Gitee issues I6N35H](https://gitee.com/dromara/Jpom/issues/I6N35H) ）
3. 【server】新增 用户支持自定义工作空间名，排序 （感谢@酱总）
4. 【server】新增 节点分发项目支持排序，设置项目启用/禁用状态（感谢@酱总）
5. 【server】新增 节点分发支持手动释放删除指定项目
6. 【server】新增 docker 镜像创建容器新增 runtime 参数

### 🐞 解决BUG、优化功能

1. 【server】修复 插件端在线升级页面无法正常使用（调用到服务端在线升级接口）
2. 【server】优化 节点在线升级统一管理避免出现 null
3. 【server】修复 节点信息编码在部分接口出现 NPE （感谢@酱总）
4. 【server】优化 工作空间中不存在资产管理相关的数据添加默认缺省页（仅管理员显示）
5. 【server】优化 支持手动释放节点项目的分发属性

### ❌ 不兼容功能

1. 【agent】取消 节点进程列表显示 jpom 项目名

### ⚠️ 注意

1. 【server】优化 在线工具菜单更名为其他管理

------

## 2.10.32 (2023-03-14)

### 🐞 解决BUG、优化功能

1. 【server】修复 mysql 数据库因为字段长度问题初始化失败（感谢@xuejun）

------

## 2.10.31 (2023-03-14)

### 🐞 解决BUG、优化功能

1. 【server】修复 未配置节点编码方式无法正常保存问题（感谢@初凡 ³）

------

## 2.10.30 (2023-03-14)

### 🐣 新增功能

1. 【all】新增 插件端支持配置发送请求消息编码方式（编码、混淆明文、规避防火墙）
   （感谢 [@Mr_loyal](https://gitee.com/Mr_loyal) [Gitee pr 179](https://gitee.com/dromara/Jpom/pulls/179) ）

### 🐞 解决BUG、优化功能

1. 【server】修复 导入 gitea 仓库搜索、分页无法正常使用问题
   （感谢 [@Smith](https://gitee.com/autools) [Gitee pr 175](https://gitee.com/dromara/Jpom/pulls/175) [Gitee pr 174](https://gitee.com/dromara/Jpom/pulls/174) ）
2. 【server】优化 镜像启动容器不填写运行命令行导致容器启动失败（部分低版本）
   （感谢 [@失落的世界](https://gitee.com/marmotgo) [Gitee pr 176](https://gitee.com/dromara/Jpom/pulls/176) ）
3. 【server】修复 节点分发 webhook 输入框的错别字（感谢 @大灰灰 ）
4. 【server】修复 工作空间环境变量操作日志记录错误问题
5. 【all】更新 fastjson2 版本
6. 【all】优化 SSH 命令脚本、服务端脚本、插件端脚本执行参数优化
   （感谢 [@大灰灰大](https://gitee.com/linjianhui) [Gitee issues I6IPDY](https://gitee.com/dromara/Jpom/issues/I6IPDY) ）
7. 【server】优化 导入仓库页面提示信息错乱（感谢@零壹）
8. 【agent】修复 项目修改路径为子目录时 mv 文件触发死循环（感谢@D¹⁹⁹¹）
9. 【server】修复 查询构建日志可能出现 NPE 问题
   （感谢 [@Tom Xin](https://gitee.com/meiMingle) [Gitee issues I6MX9G](https://gitee.com/dromara/Jpom/issues/I6MX9G) ）
10. 【server】优化 系统缓存页面显示当前服务器时间、时区信息
11. 【server】修复 还原数据后备份状态错误问题
    （感谢 [@lin_yeqi](https://gitee.com/lin_yeqi) [Gitee issues I6MVL7](https://gitee.com/dromara/Jpom/issues/I6MVL7) ）
12. 【agent】修复 DSL 项目状态不判断 jps 命令是否正常（感谢@大灰灰）
13. 【agent】修复 未配置节点白名单时直接创建分发项目报错（感谢@波比）

### ❌ 不兼容功能

1. 【server】删除 COMMAND_INFO 表 type 字段

### ⚠️ 注意

SSH 命令脚本、服务端脚本、插件端脚本默认参数规则变化：参数描述将必填，默认参数在手动执行时无法删除并且可以查看对应参数描述

------

## 2.10.29 (2023-03-10)

### 🐣 新增功能

1. 【server】新增 导入仓库支持 `gitea` 系统
   （感谢 [@Smith](https://gitee.com/autools) [Gitee pr 173](https://gitee.com/dromara/Jpom/pulls/173) ）
2. 【server】新增 用户登录日志（取消用户登录生成操作日志的执行日志）
3. 【server】新增 在线工具验证 cron 表达式 （感谢@奇奇）

### 🐞 解决BUG、优化功能

1. 【server】修复 SSH 并发执行脚本引起脚本丢失错误（感谢@墨汁）
2. 【server】优化 docker 编辑无法连接提示异常详情信息（感谢@章强）
3. 【agent】优化 节点分发配置白名单到插件端需要验证合法性
4. 【server】优化 docker 创建容器忽略未配置存储选项参数（感谢@D¹⁹⁹¹）
5. 【server】优化 docker 管理裁剪功能独立菜单
6. 【server】修复 资产管理未记录操作日志的问题
7. 【server】优化 操作日志存储用户名、工作空间名字段
8. 【server】优化 容器构建查询可用标签容器相关提示
9. 【server】优化 构建历史列表页面在小屏幕数据显示不全
   （感谢 [@一只羊](https://gitee.com/hjdyzy) [Gitee issues I6LLA0](https://gitee.com/dromara/Jpom/issues/I6LLA0) ）
10. 【server】修复 在线构建发布到集群无法正常选择集群服务（感谢@心光）

------

## 2.10.28 (2023-03-08)

### 🐣 新增功能

1. 【agent】新增 项目触发器新增 fileChange 事件（文件变动对应触发点：上传、删除、远程下载、编辑、新增目录或者文件、重命名）
   （感谢 [@胡明](https://gitee.com/pig_home) [Gitee issues I6KKEK](https://gitee.com/dromara/Jpom/issues/I6KKEK) ）
2. 【server】新增 镜像创建容器支持配置存储选项（感谢@topsuder、@章强）

### 🐞 解决BUG、优化功能

1. 【server】修复 新增 docker 无法使用在线构建功能
   （感谢 [@失落的世界](https://gitee.com/marmotgo) [Gitee issues I6KTLQ](https://gitee.com/dromara/Jpom/issues/I6KTLQ) ）
2. 【server】优化 项目文件列表支持前端排序（文件大小、修改时间）
3. 【server】优化 关闭程序时依次关闭线程池
4. 【server】优化 工作空间环境变量开放给普通用户编辑

### ⚠️ 注意

插件端需要同步升级，否则项目文件列表排序无法正常使用

------

## 2.10.27 (2023-03-06)

### 🐣 新增功能

1. 【server】新增 资产管理新增 docker 、集群管理

### 🐞 解决BUG、优化功能

1. 【all】升级 springboot 版本
2. 【server】优化 系统自动同步 docker 已经安装的集群信息
3. 【server】更新 mysql maven 坐标：`mysql-connector-j`
4. 【server】修复 构建产物模糊匹配二级剔除配置 `/` 无效

### ⚠️ 注意

新增 docker 资产管理,系统会自动将已经存在的 docker 信息根据 host 去重同步到资产管理中（如果 host
存在多个工作空间将根据最后更新时间排序使用最新的一条数据）

更新后 docker、集群列表中状态如果出现：`信息丢失` 表示关联数据存在异常不能正常使用，需要删除对应数据重新关联

------

## 2.10.26 (2023-03-03)

### 🐞 解决BUG、优化功能

1. 【server】修复 初始化数据库未删除完整问题（感谢@酱总）
2. 【server】优化 日志阅读选项卡 tab 名称添加项目名称（感谢@tinsang）

------

## 2.10.25 (2023-03-03)

### 🐣 新增功能

1. 【server】新增 构建历史新增产物文件大小
2. 【all】新增 机器安装 ID 文件（请勿删除数据目录 `INSTALL.json` 文件）
3. 【agent】新增 插件端新增虚拟内存和交互内存监控趋势

### 🐞 解决BUG、优化功能

1. 【server】优化 构建发布完成，自动删除压缩包文件（节省空间占用大小）（感谢@轩辕豆豆）
2. 【server】修复 更新构建历史环境变量失败
3. 【server】取消 SSH 脚本命令参数描述（避免误导用户）
   （感谢 [@大灰灰大](https://gitee.com/linjianhui) [Gitee issues I6IPDY](https://gitee.com/dromara/Jpom/issues/I6IPDY) ）
4. 【server】优化 编辑项目文件回显错乱问题
5. 【server】优化 日志阅读菜单更名日志搜索
6. 【server】优化 差异构建时，触发取消构建标记构建状态为`构建中断` （感谢@张飞鸿）
7. 【server】优化 部分窄下拉框新增 tooltip,避免内容过长无法查看 （感谢@墨汁）

### ❌ 不兼容功能

1. 【server】删除 弃用表 NODE_STAT
2. 【server】删除 弃用表 SYSTEMMONITORLOG
3. 【server】删除 相关表中的 strike 字段

------

## 2.10.24 (2023-03-01)

### 🐞 解决BUG、优化功能

1. 【server】优化 在线构建容器镜像构建参数和镜像标签支持解析环境变量
2. 【server】优化 替换环境变量，支持 $xxx ${xxx} （感谢@大锅饭集团）
3. 【server】修复 配置节点分发白名单报错 （感谢@酱总）
4. 【server】优化 节点分发配置【配置管理-白名单配置】菜单移动到功能管理中【项目管理-分发白名单】
5. 【server】修复 非管理员无法使用 SSH 终端问题
   （感谢 [@lilinLue](https://gitee.com/ljlToTlj) [Gitee issues I6IRJV](https://gitee.com/dromara/Jpom/issues/I6IRJV) ）

### ⚠️ 注意

节点分发白名单可能失效，需要重新配置

------

## 2.10.23 (2023-03-01)

### 🐣 新增功能

1. 【server】新增 控制台输出工作空间关联数据错误未关联的表和条数
2. 【server】新增 资产管理-SSH管理
3. 【server】新增 构建 SSH 发布支持配置发布前执行命令 （感谢@daniel）

### 🐞 解决BUG、优化功能

1. 【server】修复 使用 ANT 产物目录会自动生成模糊匹配表达式文件夹（感谢@leonchen21）
2. 【server】修复 启动时候未自动触发修复数据逻辑
3. 【server】修复 SSH 文件管理二级目录以下无法重命名
4. 【server】优化 SSH 配置授权目录、允许编辑文件后缀、禁止命令移动到资产管理中
5. 【all】优化 SSH文件、项目文件允许编辑文件的后缀支持配置 * (前提编辑格式统一)
6. 【server】优化 升级 docker-java 、svnkit 依赖版本
7. 【server】优化 SSH 支持清空隐藏字段

### ⚠️ 注意

由于新增 SSH 资产管理，之前ssh 配置如果引用的工作空间变量的配置信息可能将失效（作用域不同）.
如果仍需要变量信息还需要将对应的信息迁移到全局变量中才可以正常使用

------

## 2.10.22 (2023-02-24)

### 🐣 新增功能

1. 【server】新增 仓库新增配置超时属性（避免仓库拉取代码超时）（感谢 [@阿超](https://gitee.com/VampireAchao) ）

### 🐞 解决BUG、优化功能

1. 【server】修复 容器构建无法下载产物（感谢@张飞鸿）

------

## 2.10.21 (2023-02-23)

### 🐞 解决BUG、优化功能

1. 【server】优化 容器构建自动删除构建容器
2. 【server】优化 系统管理菜单名：变更为`插件端配置`，`服务端配置` （感谢@ccx2480）
3. 【server】修复 机器管理节点配置同步获取信息错乱（使用到服务端配置）（感谢@ccx2480）

------

## 2.10.20 (2023-02-23)

### 🐞 解决BUG、优化功能

1. 【agent】修复 插件端验证项目白名单路径失败（感谢@ccblandy）

------

## 2.10.19 (2023-02-22)

### 🐣 新增功能

1. 【server】新增 容器构建缓存插件支持按照 `path` 全局缓存 `type: global`
2. 【server】新增 容器构建缓存插件支持缓存 node_modules `mode: copy`
   (避免出现：[https://github.com/npm/cli/issues/3669](https://github.com/npm/cli/issues/3669))
3. 【server】新增 构建列表新增批量构建
   （感谢 [@爱笑的眼睛](https://gitee.com/175cm75kg18cm) [Gitee issues I6GNV2](https://gitee.com/dromara/Jpom/issues/I6GNV2) ）
4. 【server】新增 机器管理新增查看关联节点功能
5. 【server】新增 机器新增网络、硬件硬盘查看
6. 【server】新增 机器管理列表新增表格视图
7. 【server】新增 手动分发文件、构建分发弹窗新增筛选指定项目进行分发
   （感谢 [@Smith](https://gitee.com/autools) [Gitee issues I6GQNG](https://gitee.com/dromara/Jpom/issues/I6GQNG) ）

### 🐞 解决BUG、优化功能

1. 【server】修复 构建读取附件环境变量时机调整到 pull 后
2. 【agent】优化 白名单路径原样保存（避免部分安全组件拦截）
3. 【server】修复 编辑机器分组名失效问题
4. 【server】优化 工作空间菜单配置由系统管理移动到工作空间列表管理中
5. 【server】优化 节点白名单配置分发功能移动到机器管理表格视图中（模板节点）
6. 【server】优化 节点配置分发功能移动到机器管理表格视图中（模板节点）

------

## 2.10.18 (2023-02-20)

### 🐣 新增功能

1. 【server】新增 资产管理->机器管理
2. 【server】新增 配置属性：jpom.node.stat-log-keep-days（节点统计日志保留天数）
3. 【all】新增 机器节点硬盘信息统计
4. 【all】新增 机器节点网络流量信息统计
5. 【server】新增 构建触发器新增获取构建日志接口
   （感谢 [@黑黑](https://gitee.com/c180) [Gitee issues I6G0AT](https://gitee.com/dromara/Jpom/issues/I6G0AT) ）

### 🐞 解决BUG、优化功能

1. 【server】更名 节点列表更名逻辑节点
2. 【server】修复 节点分发编辑 webhook 字段回显（感谢@酱总）
3. 【server】优化 在线升级统一机器管理（无需切换工作空间）
4. 【server】优化 节点管理>在线升级菜单移动到机器管理中

### ❌ 不兼容功能

1. 【server】删除 node_info unLockType 字段
2. 【server】取消 节点解绑功能
3. 【server】停止 使用 NODE_STAT 表（暂时保留相关数据）
4. 【server】替代 MACHINE_NODE_STAT_LOG 表替代 SYSTEMMONITORLOG 表（并暂时保留 SYSTEMMONITORLOG 数据）

### ⚠️ 注意

由于新增机器管理，程序将自动同步节点表中的所有数据`以节点地址去重`后保存到机器表中，如果同一个节点地址出现多条数据（节点存在不同的工作空间）将跟进节点更新时间最新的为准

插件端需要同步更新，否则节点状态、机器状态为：`状态码错误`

如果更新当前版本后出现节点授权码错误：可能原因是之前同一个机器添加多个节点到不同的工作空间并且最后更新的节点中保存的授权信息是错误，导致数据自动同步后仍然是错误的授权信息

------

## 2.10.17 (2023-02-16)

### 🐣 新增功能

1. 【server】新增 构建配置新增严格执行命令模式（判断命令执行状态码是否为0）
   （感谢@阿克苏市姑墨信息科技有限公司） [Gitee pr 169](https://gitee.com/dromara/Jpom/pulls/169) ）
2. 【server】新增 节点分发新增 webhook 配置属性（感谢@酱总）

### 🐞 解决BUG、优化功能

1. 【server】修复 构建产物配置单属性时，二次匹配不能匹配到文件问题
   （感谢 [@伤感的风铃草](https://gitee.com/bwy-flc) [Gitee issues I6FETS](https://gitee.com/dromara/Jpom/issues/I6FETS) ）
2. 【server】优化 构建历史回滚输出相关操作日志（感谢@酱总）
3. 【server】修复 windows 容器构建无法上传文件到容器问题

------

## 2.10.16 (2023-02-14)

### 🐣 新增功能

1. 【server】新增 docker 列表支持跨工作空间同步
   （感谢 @[清风柳絮II号](https://gitee.com/zhangfeihong_597) [Gitee issues I6EOIR](https://gitee.com/dromara/Jpom/issues/I6EOIR) ）
2. 【server】新增 构建历史保存构建环境变量（为回滚流程使用）

### 🐞 解决BUG、优化功能

1. 【all】优化 解压工具支持多种编码格式（GBK、UTF8）（感谢@Again... . ）
2. 【server】优化 在线构建新增配置文件环境变量测试（`BUILD_CONFIG_BRANCH_NAME`）(感谢@阿克苏市姑墨信息科技有限公司)
3. 【server】修复 节点分发回滚 NPE （感谢@酱总）
4. 【server】优化 构建弹窗部分下拉支持手动刷新数据（感谢@张飞鸿）

------

## 2.10.15 (2023-02-13)

### 🐣 新增功能

1. 【server】新增 构建 pull 流程之后新增 `BUILD_COMMIT_ID` 变量
2. 【server】新增 执行脚本输出可用环境变量（服务端脚本、节点脚本、SSH 脚本、在线构建 pull 成功之后、构建事件脚本）
3. 【server】新增 构建确认弹窗新增配置构建环境变量

### 🐞 解决BUG、优化功能

1. 【server】修复 节点分发二级路径不能删除问题（感谢@张飞鸿）
2. 【agent】优化 服务端环境隐私变量字段传递到插件端（已经存在的插件端环境变量默认为隐私变量）
3. 【agent】修复 DSL 项目模式 status 事件写入日志编码格式跟随系统配置,避免编码格式不正确（已经存在的日志文件可能会乱码,可以删除文件解决）
4. 【server】优化 提前构建加载附加环境变量（startReady 事件）
5. 【agent】优化 节点进程列表、内存、cpu、硬盘加载方式采用 oshi
6. 【server】优化 在线升级页面新版本检测支持本地网络检测

### ⚠️ 注意

插件端需要同步更新，否则节点首页进程列表数据将不能正常显示

------

## 2.10.14 (2023-02-10)

### 🐣 新增功能

1. 【server】新增 构建状态新增`构建中断`（执行事件脚本返回中断构建）
2. 【server】新增 构建事件脚本支持返回指定关键词中断构建（需要执行事件脚本输出的最后一行，`interrupt $type`）
3. 【server】新增 构建触发器将请求参数传入构建环境变量（`triggerContentType`、`triggerBodyData`）

### 🐞 解决BUG、优化功能

1. 【server】优化 构建产物为文件夹打包位置优化（避免存放位置错乱）
2. 【server】修复 构建触发修改构建产物路径未验证 slip 问题
3. 【server】优化 本地构建产物模糊匹配（ant path）支持配置截取路径、合并文件
4. 【server】优化 构建日志输出信息（部分调整为中文、消息标签和级别）
5. 【server】优化 切换工作空间刷新菜单（感谢@ccx2480）
6. 【server】优化 用户密码提示改为弹窗并且可以快捷复制
7. 【agent】修复 保存 DSL 项目判断是否存在 status 节点,避免无法删除情况（感谢@张飞鸿）
8. 【agent】修复 节点项目修改路径移动文件不生效问题
9. 【agent】取消 编辑项目校验目录存在情况
10. 【server】优化 项目ID、节点分发ID 支持前端快捷生成
11. 【server】优化 构建执行事件脚本描述匹配支持 all 关键词 (匹配所有事件)
12. 【server】修复 执行脚本文件的换行符合跟随系统，避免 windows 中出现异常
13. 【server】优化 解绑操作提示弹窗更明确（减少误操作）（感谢@酱总）

### ⚠️ 注意

如果使用到产物模糊匹配的请关注是否需要重新调整匹配符。

新版本匹配符支持配置三个属性：

属性1:属性2[可选]:属性3[可选]

**属性1**：为模糊匹配的表达式 ( `Ant-style` )

**属性2**：匹配到的文件保留方式，可用值：`KEEP_DIR`、`SAME_DIR`。（大小写均兼容、配置错误默认为 KEEP_DIR）

KEEP_DIR: 保留匹配到的文件的文件层级

SAME_DIR: 将匹配到的文件均保留到同一个层级（合并到一个文件夹下）。慎用该方式，如果多目录存在相同的文件名会出现合并后只保留匹配到的最后一个文件

**属性3**： 需要剔除匹配到多级文件夹的指定目录,(可以配置为空)。建议配合属性2的`KEEP_DIR`使用。剔除目录可以理解为二次过滤前缀匹配文件

#### 🌰 举个栗子

##### 栗子1： `/web*/**/*.html:KEEP_DIR:/web2/`

表示匹配执行构建后，对应目录下的：已 web 开头的目录下面的所有 html 文件，并且保留文件夹层级关系，最后发布时候需要剔除 /web2/

假设：目录下有如下文件

```log
/vue/vue.html
/web/web1.html
/a/b/t.html
/web2/a.html
/web2/b/a.html
/web1/aa/t.html
```

执行匹配后的文件

```log
a.html
/b/a.html
```

##### 栗子2： `/web*/**/*.html:SAME_DIR:`

表示匹配执行构建后，对应目录下的：已 web 开头的目录下面的所有 html 文件，并且合并文件到同一个目录，最后发布时候需要剔除
/web2/

假设：目录下有如下文件

```log
/vue/vue.html
/web/web1.html
/a/b/t.html
/web2/a.html
/web2/b/a.html
/web1/aa/t.html
```

执行匹配后的文件

```log
web1.html
a.html
t.html
```

##### 栗子3： `/web*/**/*.html:KEEP_DIR:`

表示匹配执行构建后，对应目录下的：已 web 开头的目录下面的所有 html 文件，并且保留文件夹层级关系，最后发布时候按照原目录结构发布

假设：目录下有如下文件

```log
/vue/vue.html
/web/web1.html
/a/b/t.html
/web2/a.html
/web2/b/a.html
/web1/aa/t.html
```

执行匹配后的文件

```log
/web/web1.html
/web2/a.html
/web2/b/a.html
/web1/aa/t.html
```

------

## 2.10.13 (2023-02-08)

### 🐣 新增功能

1. 【server】新增 项目支持配置分组属性，方便项目列表筛选
   （感谢 @[hjk2008](https://gitee.com/hjk2008) [Gitee issues I63PEN](https://gitee.com/dromara/Jpom/issues/I63PEN) ）
2. 【server】新增 节点分发支持配置分组属性，方便列表筛选
3. 【agent】新增 DSL 项目支持配置自定义备份路径
   （感谢 @[陈旭](https://gitee.com/chenxu8989) [Gitee issues I57ZKJ](https://gitee.com/dromara/Jpom/issues/I57ZKJ) ）

### 🐞 解决BUG、优化功能

1. 【all】修复 linux 无法正常安装 service （感谢@山上雪）
2. 【server】优化 构建的节点分发模式增加二级目录
   （感谢 [@爱琳琳真是太好了](https://gitee.com/qiqi513_admin) [Gitee issues I6DNMX](https://gitee.com/dromara/Jpom/issues/I6DNMX) ）
3. 【server】优化 构建不保留产物时自动删除产物为目录时的压缩包文件
4. 【server】优化 构建状态等待`节点分发`完成（阻塞执行节点分发）
5. 【server】修复 构建选择`节点分发`并关闭`保留产物`，会导致分发失败。
   （感谢 [@爱琳琳真是太好了](https://gitee.com/qiqi513_admin) [Gitee issues I6DII6](https://gitee.com/dromara/Jpom/issues/I6DII6) ）
6. 【server】修复 构建分发为`节点分发`，产物为文件时导致的不能回滚
   （感谢 [@Smith](https://gitee.com/mrsmith) [Gitee issues I6DNSM](https://gitee.com/dromara/Jpom/issues/I6DNSM) ）
7. 【server】优化 定时构建支持配置禁用表达式，方便临时关闭定时执行
   （感谢 [@阿超](https://gitee.com/VampireAchao) [Gitee issues I6DNBW](https://gitee.com/dromara/Jpom/issues/I6DNBW) ）
8. 【server】修复 DSL 项目配置文件备份数量不生效问题

### ⚠️ 注意

Linux 环境 已经安装的需要手动更新一下服务管理脚本

**服务端**：(需要到安装目录的 bin 下执行)

```shell
curl -LfsSo Service.sh https://gitee.com/dromara/Jpom/raw/master/modules/server/src/main/bin/Service.sh
```

**插件端** ：(需要到安装目录的 bin 下执行)

```shell
curl -LfsSo Service.sh https://gitee.com/dromara/Jpom/raw/master/modules/agent/src/main/bin/Service.sh
```

------

## 2.10.12 (2023-01-29)

### 🐞 解决BUG、优化功能

1. 【server】优化 在线终端断开连接时提醒并支持重连
2. 【server】修复 线程同步器，避免任务过多造成线程数不可控（节点分发相关功能）
3. 【server】优化 前端打包取消 .map 文件，缩少发布包大小
   （感谢 [@金技](https://gitee.com/jinjiG) [Gitee issues I6AK0N](https://gitee.com/dromara/Jpom/issues/I6AK0N) ）
4. 【all】优化 分片上传文件名采用分片序号（伪装文件后缀）（感谢@冷月）
5. 【all】优化 分片上传文件签名由 sha1 改为 md5 提升效率
6. 【server】优化 构建历史页面鼠标移到名称下拉项显示文字
   （感谢 [@伤感的风铃草](https://gitee.com/bwy-flc) [Gitee pr 167](https://gitee.com/dromara/Jpom/pulls/167) ）
7. 【all】修复 日志监听器 catch 异常日志造成会话未自动删除问题
   （感谢 [@金技](https://gitee.com/jinjiG) [Gitee issues I6A5QW](https://gitee.com/dromara/Jpom/issues/I6A5QW) ）
8. 【server】修复 仓库地址 https 证书验证问题（自动忽略验证）
   （感谢 [@arstercz](https://github.com/arstercz) [Github issues 32](https://github.com/dromara/Jpom/issues/32) ）

### ⚠️ 注意

1. 插件端需要同步升级，否则不能正常使用节点上传文件相关功能

------

## 2.10.11 (2023-01-10)

### 🐣 新增功能

1. 【server】新增 系统缓存新增分片操作数查看
2. 【server】新增 节点分片上传支持配置并发数：`jpom.node.upload-file-concurrent`

### 🐞 解决BUG、优化功能

1. 【server】优化 迁移数据添加更多日志输出
2. 【server】优化 分片上传解析文件数据采用分片形式，避免大文件造成浏览器奔溃
3. 【server】优化 插件端在线升级管理页面错误信息提示由弹窗改到对应节点
4. 【server】修复 迁移数据出现监控报警记录表字段不全问题 （感谢@loyal）
5. 【server】修复 迁移系统参数表中的 sync_trigger_token 数据重复问题（感谢@loyal）
6. 【server】优化 取消迁移数据忽略处理（避免默认工作空间名称不迁移）（感谢@loyal）
7. 【server】优化 获取项目运行状态失败弹窗提醒改为单条数据异常提醒
8. 【server】优化 服务端项目管理项目列表获取运行状态改为并发执行,缩短加载时间
9. 【server】优化 分片上传文件中文件选择器禁用

### ❌ 不兼容功能

1. 【server】取消 监控记录实体中的 logId 字段 （感谢@loyal）
2. 【all】取消 启动时候判断重复启动

------

## 2.10.10 (2023-01-09)

### 🐣 新增功能

1. 【all】新增 在线升级是否允许降级操作配置属性`jpom.system.allowed-downgrade`
2. 【server】新增 分发整体状态新增`分发失败`
3. 【server】新增 构建日志显示进度折叠率配置：`jpom.build.log-reduce-progress-ratio`

### 🐞 解决BUG、优化功能

1. 【server】修复 mysql 环境非`allowMultiQueries`初始化表结构失败（感谢@丿幼儿园逃犯）
2. 【server】修复 部分表字段缺失问题（strike）
3. 【server】优化 迁移数据到 mysql 字段大小写跟随实体（感谢@丿幼儿园逃犯）
4. 【server】修复 导入数据库备份文件目录不存在时报错（感谢@丿幼儿园逃犯）
5. 【all】优化 节点上传项目文件采用分片上传、并且支持进度显示
6. 【all】优化 在线升级上传项目包采用分片上传、并且支持进度显示
7. 【all】优化 在线升级，默认禁止降级操作
8. 【server】优化 节点分发上传文件采用分片上传、并且支持进度显示
9. 【server】优化 分发单项的状态信息存储于日志记录中（取消 json 字段存储）
10. 【server】优化 节点分发子项展示逻辑（同步改异步加载,避免长时间加载）
11. 【server】优化 构建日志输出各个流程耗时
12. 【server】优化 构建发布项目文件采用分片上传、并且支持进度显示
13. 【agent】优化 配置文件中上传文件大小限制由 1G 改为 10MB 节省插件端占用内存大小（采用分片代替）
14. 【server】优化 手动上传的节点分发文件将自动删除，节省存储空间
15. 【server】优化 节点分发日志支持显示进度信息

### ⚠️ 注意

1. 插件端需要同步升级，否则节点分发项目无法显示项目名称
2. 插件端需要同步升级，否则会出现部分接口 404 或者参数不正确的情况
3. 建议升级验证上传项目文件无问题后，将插件端上传文件大小限制配置属性大改小
    1. spring.servlet.multipart.max-file-size=5MB
    2. spring.servlet.multipart.max-request-size=20MB

**如果需要使用 mysql 存储，则需要修改配置**

1. 修改 `jpom.db.mode` 为 `MYSQL`
2. 修改 `jpom.db.url` 为你 mysql 的 jdbc 地址( jdbc:mysql://127.0.0.1:
   3306/jpom?useUnicode=true&characterEncoding=UTF-8&useSSL=false)
3. 修改 `jpom.db.user-name` 为对应 mysql 账户
4. 修改 `jpom.db.user-pwd` 为对应 mysql 密码

如果您需要迁移之前 h2 数据库中的数据到 mysql（需要先将 mysql 的连接信息配置好后才能迁移）

```shell
bash ./bin/Server.sh restart -15 --h2-migrate-mysql --h2-user=jpom --h2-pass=jpom

```

------

## 2.10.9 (2023-01-06)

### 🐣 新增功能

1. 【server】新增 服务端数据存储支持 mysql

### 🐞 解决BUG、优化功能

1. 【server】修复 在线编辑配置文件报错并修改数据库密码问题
2. 【server】~~三次修复~~ 在线终端输入部分字符后自动断开连接问题
3. 【server】升级 svnkit 依赖版本
4. 【server】优化 docker 标签查询精准查询
5. 【server】更名 阅读文件更名为跟踪文件

### ❌ 不兼容功能

1. 【server】删除 数据库中多个数据表中弃用字段

### ⚠️ 注意

如果需要使用 mysql 存储，则需要修改配置：

1. 修改 `jpom.db.mode` 为 `MYSQL`
2. 修改 `jpom.db.url` 为你 mysql 的 jdbc 地址( jdbc:mysql://127.0.0.1:
   3306/jpom?useUnicode=true&characterEncoding=UTF-8&useSSL=false)
3. 修改 `jpom.db.user-name` 为对应 mysql 账户
4. 修改 `jpom.db.user-pwd` 为对应 mysql 密码

如果您需要迁移之前 h2 数据库中的数据到 mysql（需要先将 mysql 的连接信息配置好后才能迁移）

```shell
bash ./bin/Server.sh restart -15 --h2-migrate-mysql --h2-user=jpom --h2-pass=jpom

```

------

## 2.10.8 (2023-01-05)

### 🐞 解决BUG、优化功能

1. 【all】优化 程序运行的 tmp 文件夹（`java.io.tmpdir`）跟随项目目录
2. 【all】优化 判断目录越级 `checkSlip` 目录转义至 tmpdir，避免在用户目录生成空白文件夹

### ❌ 不兼容功能

1. 【all】取消 程序启动写入全局临时信息
2. 【server】取消 服务端没有节点自动探测本地节点功能

### ⚠️ 注意

Linux、Windows 环境 已经安装 2.10.0 ~ 2.10.7 的需要手动更新一下管理脚本

> 建议先更新脚本再升级插件端或者服务端
>
> Windows 用户需要自行下载脚本替换

**服务端**：(需要到安装目录的 bin 下执行)

```shell
curl -LfsSo Server.sh https://gitee.com/dromara/Jpom/raw/master/modules/server/src/main/bin/Server.sh
```

**插件端** ：(需要到安装目录的 bin 下执行)

```shell
curl -LfsSo Agent.sh https://gitee.com/dromara/Jpom/raw/master/modules/agent/src/main/bin/Agent.sh
```

------

## 2.10.7 (2023-01-04)

### 🐣 新增功能

1. 【server】新增 配置管理新增配置目录在线编辑功能
2. 【server】新增 容器构建新增 `ubuntu-git` 镜像

### 🐞 解决BUG、优化功能

1. 【server】修复 在线终端输入部分字符后自动断开连接问题（感谢 @Again.... ）
2. 【server】修复 执行 SSH 脚本未正常加载环境变量问题
3. 【server】修复 快速安装(绑定)插件端的命令特殊字符转义问题 （感谢@张飞鸿）
4. 【server】优化 节点在线升级确认操作提醒要升级的目标版本号（感谢@木迷榖）
5. 【server】优化 modal 弹窗新增 destroyOnClose , 优化页面卡顿和组件样式冲突
6. 【server】修复 windows nginx 配置文件编辑白名单路径非绝对路径时出现名称错误

### ❌ 不兼容功能

1. 【server】下架 构建配置管理功能（请使用配置目录管理功能代替）

------

## 2.10.6 (2022-12-29)

### 🐣 新增功能

1. 【agent】新增 上传项目文件，下载远程文件 压缩包支持自动剔除文件夹
2. 【server】新增 节点分发新增手动取消分发任务功能
   （感谢 [@gxw](https://gitee.com/yinxianer) [Gitee issues I61SBB](https://gitee.com/dromara/Jpom/issues/I61SBB) ）

### 🐞 解决BUG、优化功能

1. 【server】优化 SSH 终端 JSCH 新增日志实现，方便排查问题
2. 【agent】优化 部分下载接口取消返回值，避免控制台出现错误日志
3. 【server】优化 服务端代理插件端的 websocket 超时问题
4. 【server】修复 在线终端输入部分字符后自动断开连接问题（感谢 @Again.... ）
5. 【server】修复 部分下拉框无法正常搜索文件（感谢 @Again.... ）
6. 【agent】优化 同时上传相同的文件名时可能异常
7. 【server】优化 节点分发状态新增（等待分发、手动取消状态）
8. 【server】修复 状态为未分发时分发失败引起的状态错误

------

## 2.10.5 (2022-12-27)

### 🐣 新增功能

1. 【server】新增 操作日志新增数据名称字段

### 🐞 解决BUG、优化功能

1. 【agent】修复 项目文件夹不存在时不能下载远程文件
2. 【all】升级 fastjson 升级为 fastjson2
3. 【all】升级 SpringBoot 2.7.7 、commons-compress
4. 【server】移除 空闲依赖 jaxb-api
5. 【all】优化 启动加载流程，保存顺序加载
6. 【all】修复 启动成功写入全局信息由于没有权限造成的异常
   （感谢 [@LeonChen21](https://gitee.com/leonchen21) [Gitee issues I67C3C](https://gitee.com/dromara/Jpom/issues/I67C3C) ）
7. 【server】优化 websocket 控制台操作日志记录
8. 【server】修复 超级管理的 websocket 操作日志记录工作空间不正确
9. 【agent】优化 插件端删除 spring-boot-starter-websocket 依赖
10. 【server】优化 服务端删除 Java-WebSocket 依赖（采用统一模块管理）
11. 【server】修复 更新构建状态互斥，避免状态被异步更新冲突
12. 【server】优化 下载文件采用标签页面形式取消 blob

### ❌ 不兼容功能

1. 【server】取消 兼容低版本插件端的 websocket 授权信息传输方式（低版本插件端请同步升级到最新）
2. 【server】取消 服务端取消向插件端传递操作人的用户名
3. 【server】取消 服务端数据库用户操作日志表对 REQID 字段兼容（2.9.1 以下）

------

## 2.10.4 (2022-12-23)

### 🐞 解决BUG、优化功能

1. 【all】修复 linux 管理脚本中的 pid 文件内容与真实进程不一致问题
2. 【all】恢复 linux 管理脚本支持创建服务管理

### ⚠️ 注意

Linux 环境 已经安装 2.10.3 ~ 2.10.0 的需要手动更新一下管理脚本

> 需要`创建服务来管理`的需要更新后才能正常使用在线升级和保存配置并重启

> 建议先更新脚本再升级插件端或者服务端

**服务端**：(需要到安装目录的 bin 下执行)

```shell
curl -LfsSo Server.sh https://gitee.com/dromara/Jpom/raw/master/modules/server/src/main/bin/Server.sh
```

```shell
curl -LfsSo Service.sh https://gitee.com/dromara/Jpom/raw/master/modules/server/src/main/bin/Service.sh
```

**插件端** ：(需要到安装目录的 bin 下执行)

```shell
curl -LfsSo Agent.sh https://gitee.com/dromara/Jpom/raw/master/modules/agent/src/main/bin/Agent.sh
```

```shell
curl -LfsSo Service.sh https://gitee.com/dromara/Jpom/raw/master/modules/agent/src/main/bin/Service.sh
```

------

## 2.10.3 (2022-12-22)

### 🐣 新增功能

1. 【server】新增 在线构建新增 `packageFile` 流程 编译 webhook 或者事件脚本调用

### 🐞 解决BUG、优化功能

1. 【server】修复 快速导入节点工作空间id `undefined`
2. 【server】修复 本地运行脚本默认找不到的情况
3. 【agent】优化 项目控制台日志文件默认编码格式判断系统 windows 默认 GBK,其他默认 UTF-8
   （感谢 [@gf_666](https://gitee.com/gf_666) [Gitee issues I66ZZZ](https://gitee.com/dromara/Jpom/issues/I66ZZZ) ）
4. 【server】优化 在线构建 ssh 清空产物异常不标记发布异常

### ⚠️ 注意

Linux 环境 已经安装 2.10.2 ~ 2.10.0 的需要手动更新一下管理脚本，之前管理脚本存在部分场景日志输出错乱的问题

> 建议先更新脚本再升级插件端或者服务端

**服务端**：(需要到安装目录的 bin 下执行)

```shell
curl -LfsSo Server.sh https://gitee.com/dromara/Jpom/raw/master/modules/server/src/main/bin/Server.sh
```

**插件端** ：(需要到安装目录的 bin 下执行)

```shell
curl -LfsSo Agent.sh https://gitee.com/dromara/Jpom/raw/master/modules/agent/src/main/bin/Agent.sh
```

------

## 2.10.2 (2022-12-21)

### 🐞 解决BUG、优化功能

1. 【server】节点快速安装命令示例提供默认安装命令
2. 【server】修复 docker 插件未正常加载问题（感谢@顺子）
3. 【server】优化 本地构建命令执行方式由逐行改为脚本执行
4. 【server】修复 构建未配置 webhook 控制台报错
5. 【server】修复 构建未配置 webhook 不触发事件脚本

### ❌ 不兼容功能

1. 【server】下架 SSH 上传文件安装插件端方式，采用快速安装命令代替
2. 【server】取消 构建命令和本地命令发布 不支持 #{} 变量替换
3. 【server】取消 SSH 命令模板 不支持 #{} 变量替换（仅支持 ${} 替换）

------

## 2.10.1 (2022-12-20)

### 🐣 新增功能

1. 【server】新增 节点项目支持快速复制操作
   （感谢[@mt-mored](https://gitee.com/mt-mored) [Gitee issues I653O3](https://gitee.com/dromara/Jpom/issues/I653O3) ）
2. 【all】新增 节点项目、独立节点分发支持彻底删除
3. 【agent】新增 DSL 项目模式执行脚本支持节点环境变量
   （感谢[@苏生不语](https://gitee.com/sushengbuyu) [Gitee issues I66MNP](https://gitee.com/dromara/Jpom/issues/I66MNP) ）
4. 【all】新增 构建项目发布、节点分发支持配置发布前先停止（避免 windows 环境文件被占用）
   （感谢 [@yiziyu](https://gitee.com/yiziyu) [Gitee issues I65MS1](https://gitee.com/dromara/Jpom/issues/I65MS1)、[@all-around-badass](https://gitee.com/all-around-badass) [Gitee issues I66PYU](https://gitee.com/dromara/Jpom/issues/I66PYU) ）

### 🐞 解决BUG、优化功能

1. 【server】优化 节点分发菜单更名为项目管理
2. 【server】优化 节点分发添加项目限制数量由 2 调整为 1
   （感谢[@苏生不语](https://gitee.com/sushengbuyu) [Gitee issues I66R73](https://gitee.com/dromara/Jpom/issues/I66R73) ）
3. 【server】修复 节点分发手动上传文件二级目录出现 `undefined`
4. 【agent】修复 默认项目模式执行命令存在 `null` 字符串
5. 【server】修复 初次安装服务端初始化数据库失败问题 （感谢@lg）
6. 【server】优化 日志显示组件（取消正则搜索），日志删除 `ansi` 颜色
   （感谢[@苏生不语](https://gitee.com/sushengbuyu) [Gitee issues I657JR](https://gitee.com/dromara/Jpom/issues/I657JR) ）
7. 【server】优化 编辑组件可能出现行错和内容错乱问题
8. 【server】优化 查看系统日志的多次切换内容返回错乱问题

### ❌ 不兼容功能

1. 【agent】取消 DSL 项目脚本的 #{} 替换变量

### ⚠️ 注意

Linux 环境 已经安装 2.10.0 的需要手动更新一下管理脚本，2.10.0 管理脚本存在在线升级和在线重启日志输出重复问题

> 建议先更新脚本再升级插件端或者服务端

**服务端**：(需要到安装目录的 bin 下执行)

```shell
curl -LfsSo Server.sh https://gitee.com/dromara/Jpom/raw/master/modules/server/src/main/bin/Server.sh
```

**插件端** ：(需要到安装目录的 bin 下执行)

```shell
curl -LfsSo Agent.sh https://gitee.com/dromara/Jpom/raw/master/modules/agent/src/main/bin/Agent.sh
```

------

## 2.10.0 (2022-12-19)

### 🐣 新增功能

1. 【all】外置 `logback` 配置文件
2. 【server】服务端管理相关功能独立页面菜单
3. 【server】新增项目触发器用于管理项目状态
4. 【all】新增 构建项目发布支持配置发布到二级目录
5. 【server】新增 节点分发发布支持配置发布到二级目录

### 🐞 解决BUG、优化功能

1. 【all】启动相关信息由控制台输出改为 `logback`
2. 【all】节点管理中 `其他功能` 菜单更名为 `脚本管理`
3. 【all】优化版本升级修改管理脚本里变量,采用文件记录方式
4. 【server】优化容器启动脚本，支持监听进程已经终端重启操作
5. 【server】修复 自动刷新页面已经关闭的标签页，后台仍然在发送请求
   （感谢[@苏生不语](https://gitee.com/sushengbuyu) [Gitee issues I664OP](https://gitee.com/dromara/Jpom/issues/I664OP) ）
6. 【server】修正触发器说明错别字

### ❌ 不兼容功能

1. 【server】取消支持 2.8.0 以下 json 文件转存数据库
2. 【all】下架 JDK 管理模块（请使用 DSL 项目模式代替）
3. 【all】下架 TOMCAT 管理模块（请使用 DSL 项目模式代替）
4. 【all】删除 项目内存监控页面
5. 【all】配置文件名称由 `extConfig.yml` 变更为 `application.yml`
6. 【all】调整项目打包目录结构
7. 【all】取消兼容低版本数据目录文件迁移（调试运行）
8. 【all】取消自动识别文件编码格式模块 `auto-charset-jchardet`
9. 【all】更新管理脚本，进程标识更新（已经存在的需要手动停止）
10. 【all】取消插件端配置化向服务端注册功能（采用快速导入方式替代）
11. 【server】取消服务端授权 token 配置
12. 【all】下架 节点脚本导入功能
13. 【server】取消限制创建用户最大数配置：`user.maxCount`
14. 【server】删除 node_info 表 cycle 字段
15. 【agent】删除项目回收记录功能

### ❌ 不兼容的属性配置变更

> 属性配置支持驼峰和下划线

1. 【agent】`whitelistDirectory.checkStartsWith` -> `jpom.whitelist-directory.check-starts-with`
2. 【agent】`project.stopWaitTime` -> `jpom.project.statusWaitTime`
3. 【agent】`project.*` -> `jpom.project.*`
4. 【agent】修正拼写错误 `log.*back*` -> `jpom.project.log.*backup*`
5. 【agent】`log.*` -> `jpom.project.log.*`
6. 【agent】`log.intiReadLine` -> `jpom.init-read-line`
7. 【agent】 `log.autoBackConsoleCron` 不支持配置 none (none 使用 `jpom.project.log.autoBackupToFile` 代替)
8. 【all】删除 `consoleLog.reqXss` 、`consoleLog.reqResponse`
9. 【all】`consoleLog.charset` -> `jpom.system.console-charset`
10. 【server】`node.uploadFileTimeOut` -> `jpom.node.uploadFileTimeout`
11. 【server】`system.nodeHeartSecond` -> `jpom.node.heartSecond`
12. 【server】`user.*` -> `jpom.user.*`
13. 【server】`jpom.authorize.expired` -> `jpom.user.tokenExpired`
14. 【server】`jpom.authorize.renewal` -> `jpom.user.tokenRenewal`
15. 【server】`jpom.authorize.key` -> `jpom.user.tokenJwtKey`
16. 【server】`jpom.webApiTimeout` -> `jpom.web.api-timeout`
17. 【server】删除 `ssh.initEnv`
18. 【server】批量修正前端相关配置属性均修改到 `jpom.web.*`
19. 【server】`db.*` -> `jpom.db.*`
20. 【server】`build.*` -> `jpom.build.*`

### ⚠️ 注意

> 此版本为不兼容升级，需要手动升级修改相关配置才能正常使用

#### 简洁的升级流程

1. 停止正在运行的程序插件端或者服务端
2. 备份已经存在的插件端或者服务端的数据目录
3. 手动安装新版本 `2.10.0+`
4. 还原数据：将备份的数据目录迁移到新安装的数据目录（需要再未运行的状态下操作）
5. 重启程序

详细的升级文档：[https://jpom.top/pages/upgrade/2.9.x-to-2.10.x/](https://jpom.top/pages/upgrade/2.9.x-to-2.10.x/)

------

