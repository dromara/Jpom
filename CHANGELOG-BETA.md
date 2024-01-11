# 🚀 版本日志

### 2.11.0.12-beta

### 🐣 新增功能

1. 【server】新增  资产总览统计

### 🐞 解决BUG、优化功能

1. 【server】修复 mysql 存储使用游标查询报错（不使用游标）（感谢@🇩）
2. 【server】修复 新版 UI 下拉框搜索不生效
3. 【server】优化 **添加**关键词替换为**新增**
4. 【server】优化 部分页面无数据提示
5. 【server】修复 节点分发构建产物选择构建历史为匹配对应构建
6. 【server】修复 部分操作构建环境变量丢失（保存并构建、后台构建、直接构建）

------

### 2.11.0.11-beta (2024-01-11)

### 🐞 解决BUG、优化功能

1. 【server】修复 编辑器无法加载样式
2. 【server】优化 本地 git 联动严格执行开关
3. 【server】修复 打包后终端弹窗样式错乱（感谢@🇩）
4. 【server】修复 切换账户、退出登录菜单未刷新问题（感谢@ccx2480）
5. 【server】修复 登录账户未跳转配置的第一个工作空间（未遵循自定义配置）

------

### 2.11.0.10-beta (2024-01-10)

### 🐞 解决BUG、优化功能

1. 【server】优化 系统Git拉取代码时，强制云端最新代码覆盖本地代码
2. 【server】修复 升级页面更新日志样式错乱
3. 【server】修复 切换账户后用户信息未自动刷新
4. 【agent】修复 **部分操作引起项目节点分发属性丢失问题**
5. 【server】修复 无法新增项目（权限判断异常）（感谢@群友）
6. 【agent】修复 插件端环境变量值获取异常
7. 【agent】优化 插件端 java 项目启动支持读取环境变量
8. 【server】修复 项目列表选择错乱、批量操作无法正常使用（感谢@🇩）

------

### 2.11.0.9-beta (2024-01-10)

### 🐣 新增功能

1. 【all】新增 孤独数据管理（查看孤独数据、修正孤独数据）（感谢[@陈旭](https://gitee.com/chenxu8989) [Gitee issues I8UNXZ](https://gitee.com/dromara/Jpom/issues/I8UNXZ)）

### 🐞 解决BUG、优化功能

1. 【server】优化 上传文件前解析文件信息采用全局 loading
2. 【server】优化 构建流程交互（采用步骤条）
3. 【server】修复 部分 icon 未更新、部分弹窗列表数据不能正常显示
4. 【server】修复 docker-compose 容器状态无非正确显示
5. 【agent】修复 低版本项目数据未存储节点ID
6. 【server】优化 节点项目、节点脚本操作鉴权（需要服务端缓存中有对应的数据）

------

### 2.11.0.8-beta (2024-01-09)

### 🐣 新增功能

1. 【server】新增 前端 UI 支持配置浅色、深色主题、左边菜单主题

### 🐞 解决BUG、优化功能

1. 【server】修复 容器构建 DSL 未回显任何内容
2. 【server】修复 登录页面禁用验证码失效（感谢@ccx2480）

------

### 2.11.0.7-beta (2024-01-08)

### 🐞 解决BUG、优化功能

1. 【server】升级 页面 UI 组件、VUE 版本升级到最新
2. 【server】修复 部分低频功能无法正常使用（项目备份文件管理等）
3. 【server】修复 部分执行异常未输出到操作日志文件中（感谢@闫淼淼）

### ⚠️ 注意

1. 取消全局 loading（局部loading）
2. 编辑器延迟 1 秒加载（避免样式错乱）
3. 所有快捷复制区域变小为一个点击复制图标
4. 弹窗、抽屉样式变动
5. 取消操作引导（临时）
6. 表格将跟随列内容长度自动拉伸出现横向滚动（不会折叠）
7. 个性化配置取消：【页面自动撑开、滚动条显示、页面导航】
8. 新版本前端 node 版本推荐：18.19.0
9. json viewer 还未实现

------

### 2.11.0.6-beta (2024-01-05)

### 🐞 解决BUG、优化功能

1. 【all】优化 日志记录器提升日志记录性能
2. 【server】优化 取消/停止构建采用异常来打断子进程
3. 【server】修复 本地构建无法取消
4. 【server】修复 服务端脚本触发器、节点脚本触发器提示找不到用户（感谢@LYY）

------

### 2.11.0.5-beta (2024-01-04)

### 🐣 新增功能

1. 【server】新增 工作空间管理中新增概括总览页面

### 🐞 解决BUG、优化功能

1. 【server】优化 支持批量删除构建信息（感谢@奇奇） 
2. 【server】修复 删除项目、删除分发检查关联构建失败问题
3. 【all】优化 关闭 Process 方式
4. 【server】优化 节点方法相关页面问题（感谢[@陈旭](https://gitee.com/chenxu8989) [Gitee issues I8TMDW](https://gitee.com/dromara/Jpom/issues/I8TMDW)）

------

### 2.11.0.4-beta (2024-01-03)

### 🐞 解决BUG、优化功能

1. 【server】修复 工作空间菜单配置无法使用（感谢@新）
2. 【server】优化 重新同步节点项目、节点脚本缓存交互
3. 【server】优化 SSH 脚本执行模板独立（`/exec/template.sh` -> `/ssh/template.sh`）
4. 【server】优化 服务端脚本支持加载脚本模板来实现自动加载部分环境变量

### ⚠️ 注意

如果您自定义过 SSH 脚本默认那么您需要重新同步一下脚本模板`/exec/template.sh` -> `/ssh/template.sh`

新版本 `/exec/template.sh` 中仅在服务端中生效（本地构建脚本、服务端脚本、本地发布脚本）

------

### 2.11.0.3-beta (2024-01-02)

### 🐞 解决BUG、优化功能

1. 【server】修复 没有对应的工作空间权限

------

### 2.11.0.2-beta (2024-01-02)

### 🐞 解决BUG、优化功能

1. 【all】修复 环境变量为 null 是未忽略

------

### 2.11.0.1-beta (2024-01-02)

### 🐣 新增功能

1. 【all】新增 项目支持软链其他项目（代替项目副本）

### 🐞 解决BUG、优化功能

1. 【server】修复 新版页面漏掉项目复制按钮 
2. 【server】优化 逻辑节点中项目数和脚本数仅显示当前工作空间数量
3. 【server】优化 项目编辑和节点分发页面支持快捷配置授权目录
4. 【server】优化 项目编辑支持切换节点（快速同步其他节点项目）
5. 【server】修复 没有工作空间权限时页面循环跳转（感谢[@王先生](https://gitee.com/whz_gmg1) [Gitee issues I8RR01](https://gitee.com/dromara/Jpom/issues/I8RR01)）
6. 【all】优化 授权目录判断逻辑
7. 【agent】取消 插件端授权目录关闭包含判断(`jpom.whitelist.check-starts-with`)
8. 【server】优化 触发器清理优化、删除用户主动删除关联触发器
9. 【server】优化 DSL 项目控制台支持快捷编辑节点脚本（查看流程信息）
10. 【server】修复 项目触发器无法调用

### ⚠️ 注意

1. 如果您配置了授权目录但是保存项目报错您可以尝试重新报错一下授权目录来自动修复授权目录配置数据
2. 项目控制台日志默认路径调整为插件端数据目录下`project-log/${projectId}/${projectId}.log`
3. 项目控制台日志备份默认路径调整为插件端数据目录下`project-log/${projectId}/back/${projectId}-xxxxxxx.log`

------

### 2.11.0.0-beta (2023-12-29)

### 🐣 新增功能

1. 【server】新增 节点分发可以指定构建历史产物发布
2. 【server】新增 节点分发可以指定文件中心发布
3. 【server】新增 DSL 项目新增 reload 事件（可以开启文件变动触发）
4. 【server】新增 静态文件授权服务端指定目录到工作空间来管理（分发）(感谢@*斌)
5. 【server】新增 节点分发可以指定静态文件发布

### 🐞 解决BUG、优化功能

1. 【server】修复 项目列表批量操作弹窗定时刷新引起异常（感谢@曾梦想仗剑走天涯）
2. 【all】下架 全面下架项目副本功能（请使用 DSL 模式代替）
3. 【all】下架 全面节点证书管理功能（请使用工作空间证书代替）
4. 【all】下架 全面架节点 NGINX 管理功能（请使用 DSL 模式代替）
5. 【server】优化 **节点管理仅保留项目管理、脚本管理、脚本日志（其他功能迁移到机器资产管理）**
6. 【server】修复 项目复制按钮点击无响应
7. 【all】优化 查看插件端和服务端的系统日志 websocket 地址
8. 【server】优化 监控机器系统负载保留2位小数
9. 【server】下架 取消节点管理员权限
10. 【server】修复 文件变动触发器不生效的问题
11. 【all】优化 项目操作接口合并（4 合 1）
12. 【server】优化 配置授权目录需要使用到绝对路径

### ⚠️ 注意

1. 全面下架项目副本功能（请使用 DSL 模式代替）如果您当前使用到此功能请先手动备份相关数据
2. 升级后项目副本数据会被人工或者系统更新项目数据自动删除（请一定提前做好备份操作）
3. 全面下架节点证书管理功能（请使用工作空间证书代替）如果您当前使用到此功能请先手动备份相关数据
4. 全面下架全下架节点 NGINX 管理功能（请使用 DSL 模式代替）如果您当前使用到此功能请先手动备份相关数据

>❓ 为什么要下架上述功能：由于版本迭代已经有更好的新功能可以代替之前旧功能，并且新功能从另一种角度更方便。下架也是为了我们后续版本维护迭代更高效


- 【白名单】关键词统一调整为【授权】
- 【黑名单】关键词统一调整为【禁止】

------

### 2.10.47.7-beta (2023-12-25)

### 🐣 新增功能

1. 【server】优化 资产管理 SSH 支持配置禁用监控（避免频繁登录）`jpom.assets.ssh.disable-monitor-group-name`（感谢@Again...）
2. 【server】优化 资产管理 SSH 支持配置监控周期：`jpom.assets.ssh.monitor-cron`

### 🐞 解决BUG、优化功能

1. 【server】优化 导入 SSH、项目 CSV 数据自动识别编码格式 
2. 【server】优化 执行 SSH 脚本获取流异常：getInputStream() should be called before connect()
3. 【server】升级 mwiede 依赖版本
4. 【server】优化 资产管理 SSH 支持配置禁用监控（避免频繁登录）`jpom.assets.ssh.disable-monitor-group-name`（感谢@Again...）
5. 【server】优化 资产管理 SSH 支持配置监控周期：`jpom.assets.ssh.monitor-cron`

------

### 2.10.47.6-beta (2023-12-25)

### 🐞 解决BUG、优化功能

1. 【server】修复 系统日志页面空白
2. 【server】优化 资产机器卡片试图部分场景未对齐问题
3. 【server】优化 部分页面在小屏兼容（资产 Docker、节点分发）
4. 【server】优化 节点脚本支持解绑（避免无非使用的服务器无非删除脚本）
5. 【server】优化 白名单配置提示 nginx、证书功能将下线
6. 【all】移除 插件端配置远程下载 host 输入框
7. 【server】修复 项目、脚本、发布任务、日志阅读控制台无法正常使用问题（感谢@奇奇）

------

### 2.10.47.5-beta (2023-12-22)

### 🐞 解决BUG、优化功能

1. 【server】修复 迁移项目关联构建关联的全局仓库无法完整迁移问题（感谢@奇奇）
2. 【server】优化 仓库支持查看关联的构建
3. 【server】修复 删除服务端脚本日志如果脚本不存在不能删除

------

### 2.10.47.4-beta (2023-12-22)

### 🐞 解决BUG、优化功能

1. 【server】升级 web axios 版本
2. 【server】修复 迁移项目判断条件不充足（同一个工作空间不迁移仓库和构建）

------

### 2.10.47.3-beta (2023-12-22)

### 🐣 新增功能

1. 【all】新增 部分项目支持迁移工作空间和逻辑节点（感谢@奇奇）

### 🐞 解决BUG、优化功能

1. 【server】优化 逻辑节点节目取消全局 loading（感谢@小菜鸡）
2. 【server】优化 新增个性化配置全屏打开日志弹窗（构建、SSH、脚本、Docker等日志）（感谢@张飞鸿）
3. 【server】修复 项目副本无法保存（修改中不能删除副本集、请到副本集中删除）
4. 【server】优化 服务端中可以支持创建编辑项目、创建节点脚本啦！！！
5. 【server】优化 项目列表支持删除项目、自动刷新项目

------

### 2.10.47.2-beta (2023-12-21)

### 🐣 新增功能

1. 【server】优化 容器日志、集群任务日志支持下载（感谢@在时间里流浪）

### 🐞 解决BUG、优化功能

1. 【agent】删除 项目副本中弃用兼容字段 `parendId`
2. 【server】优化 Docker 集群任务日志支持筛选行数、是否显示时间戳（感谢@在时间里流浪）
3. 【server】优化 修复容器构建中 cache 插件异常、默认插件无法正常使用问题（感谢@张飞鸿）
4. 【server】优化 项目控制台日志输出 N 人查看改为 N 个会话（@冬）
5. 【server】优化 添加超级管理员账号提醒勿使用常用账号

------

### 2.10.47.1-beta (2023-12-19)

### 🐣 新增功能

1. 【server】新增 容器构建支持自定义插件（感谢[@远方](https://gitee.com/WaHaHaqqww) [Gitee issues I8PEWI](https://gitee.com/dromara/Jpom/issues/I8PEWI)）
2. 【server】新增 容器管理新增导出、导入镜像
3. 【server】新增 环境变量支持触发器获取、修改

### 🐞 解决BUG、优化功能

1. 【server】升级 数据库 h2 版本
2. 【server】修复 构建事件脚本未修改执行状态和退出码问题
3. 【server】优化 构建事件脚本支持多选（顺序执行其中有一个中断将结束执行后续脚本）（感谢@loyal）
4. 【server】优化 服务端脚本触发类型新增构建事件

### ⚠️ 注意

新增容器构建自定义插件说明：

1. 到 【系统管理】->【配置管理】->【系统配置目录】 中找到 `docker/uses` 目录
2. 添加插件配置文件 `/docker/uses/java8/install.sh` 其中 `java8` 为新增的插件名
3. 注意脚本中需要自行控制插件相关依赖的文件，需要将最新的文件保持到 `/opt/${name}/` 目录下，其中 ${name} 为插件名
4. 辅助说明1：插件支持自定义环境变量
5. 辅助说明2：自定义环境变量中需要引用插件目录请使用 `${JPOM_PLUGIN_PATH}`
6. 系统默认集成了：`java`、`maven`、`node`、`go`、`python3`、`gradle`、`cache` 默认插件有依赖版本检查如果您的网络不通建议自行创建插件来规避默认检查

------

### 2.10.46.2-beta (2023-12-15)

### 🐞 解决BUG、优化功能

1. 【all】优化 ConcurrentHashMap 修改为线程安全的 hutoll[SafeConcurrentHashMap]（感谢@在时间里流浪）
2. 【all】升级 mwiede.jsch、oshi、fastjson、hutool、spring-boot、docker-java
3. 【server】优化 SSH 脚本在部分场景阻塞卡死（ChannelType.EXEC 不添加超时时间）
4. 【server】优化 SSH 脚本执行输出退出码
5. 【server】优化 解决构建流程,环境变量env里出现value==null出现null报错
   （感谢 [@周冰](https://gitee.com/NineAsk) [Gitee pr 197](https://gitee.com/dromara/Jpom/pulls/197) ）
6. 【server】优化 SSH 脚本执行记录退出码
7. 【server】优化 服务端脚本执行记录新增执行状态和退出码

------

### 2.10.46.1-beta (2023-10-20)

### 🐞 解决BUG、优化功能

1. 【server】修复 容器构建下载产物未关闭文件流占用句柄问题（感谢@在时间里流浪）

------

### 2.10.45.3-beta (2023-10-17)

### 🐞 解决BUG、优化功能

1. 【server】优化 构建产物同步到文件中心支持独立配置保留天数（感谢[@zhangxin2477](https://gitee.com/zhangxin1229) [Gitee issues I82G2F](https://gitee.com/dromara/Jpom/issues/I82G2F)）
2. 【server】优化 不能删除超级管理员账号
3. 【agent】修复 Agent.sh 脚本的缺少方法问题（感谢[@Siwen Yu](https://github.com/yusiwen) [Github issues 64](https://github.com/dromara/Jpom/issues/64)）
4. 【server】优化 系统管理查看操作日志显示全部工作空间

------

### 2.10.45.2-beta (2023-09-18)

### 🐞 解决BUG、优化功能

1. 【server】优化 容器构建判断构建异常（严格模式异常中断构建）（感谢@在时间里流浪）
2. 【server】修复 构建流程中断触发 success 事件（感谢@在时间里流浪）
3. 【server】优化 SSH 独立管理面板支持快捷使用文件管理

------

### 2.10.45.1-beta (2023-09-15)

### 🐣 新增功能

1. 【server】新增 SSH 新增独立管理面板（感谢[@超人那个超i](https://gitee.com/chao_a) [Gitee issues I7UFEX](https://gitee.com/dromara/Jpom/issues/I7UFEX)）
2. 【agent】新增 DSL 项目支持配置脚本环境变量（感谢[@陈旭](https://gitee.com/chenxu8989) [Gitee issues I80PTK](https://gitee.com/dromara/Jpom/issues/I80PTK)）

### 🐞 解决BUG、优化功能

1. 【server】优化 构建详情页面支持快捷回滚、查看构建日志（感谢[@縁來只爲伱](https://gitee.com/taochach) [Gitee issues I7YSNH](https://gitee.com/dromara/Jpom/issues/I7YSNH)）
2. 【all】升级 hutool、commons-compress
3. 【agent】修复 重启项目偶发 NPE（监听日志关闭事件）（感谢[@caiqy](https://gitee.com/caiqiaoyu) [Gitee issues I7Z2U6](https://gitee.com/dromara/Jpom/issues/I7Z2U6)）
4. 【server】优化 构建支持配置环境变量实现产物打包为 `tar.gz` (**USE_TAR_GZ=1**)
5. 【server】修复 文件管理偶发无法查看发片下载地址

------

### 2.10.44.4-beta (2023-09-02)

### 🐞 解决BUG、优化功能

1. 【server】修复 资产管理共享仓库新建归属到工作空间问题（感谢@沈钊）
2. 【server】升级 springboot 、oshi、docker-java、jgit

------

### 2.10.44.3-beta (2023-08-31)

### 🐞 解决BUG、优化功能

1. 【server】升级 mwiede.jsch 版本

------

### 2.10.44.2-beta (2023-08-30)

### 🐞 解决BUG、优化功能

1. 【server】优化 构建回滚创建新的构建记录（感谢[@Smith](https://gitee.com/autools) [Gitee issues I7VEJA](https://gitee.com/dromara/Jpom/issues/I7VEJA)）

------

### 2.10.44.1-beta (2023-08-29)

### 🐣 新增功能

1. 【server】新增 支持 git submodules
   （感谢 [@Croce](https://gitee.com/Croce) [Gitee pr 195](https://gitee.com/dromara/Jpom/pulls/195) ）

### 🐞 解决BUG、优化功能

1. 【server】修复 新增资产无法正常监控问题（感谢@乔、@MichelleChung、@Pluto）
2. 【server】优化 编辑集群地址不验证，调整到心跳检测验证（感谢@黄纲）
3. 【server】优化 构建新增环境变量：BUILD_ORIGINAL_RESULT_DIR_FILE、BUILD_RESULT_DIR_FILE（发布流程）(感谢@黄纲)

------

### 2.10.42.6-beta (2023-08-23)

### 🐞 解决BUG、优化功能

1. 【server】修复 未配置集群地址时无法切换工作空间（感谢@ccx2480）

------

### 2.10.42.5-beta (2023-08-23)

### 🐣 新增功能

1. 【server】新增
   集群化管理工作空间（感谢@定格、[@paobu](https://gitee.com/iniushi) [Gitee issues I7UG5V](https://gitee.com/dromara/Jpom/issues/I7UG5V)）

### 🐞 解决BUG、优化功能

1. 【server】修复 资产管理 SSH 管理系统名称显示未知问题（感谢@勤思·）
2. 【server】优化 资产管理 Docker 管理支持配置分组
3. 【server】优化 仓库管理支持配置分组
4. 【server】优化 SSH 文件夹支持前端排序（感谢@勤思·）

------

### 2.10.42.4-beta (2023-08-19)

### 🐣 新增功能

1. 【server】优化 ssh 相关功能支持 openssh8+
   （感谢 [@孤城落寞](https://gitee.com/gclm) [Gitee pr 193](https://gitee.com/dromara/Jpom/pulls/193) ）

### 🐞 解决BUG、优化功能

1. 【server】优化 仓库账号、 SSH 证书密码支持选择环境变量
2. 【all】升级 commons-compress、fastjson、hutool 版本
3. 【server】优化 maven 依赖冲突
4. 【server】优化 文件发布-节点发布文件名使用真实名称（感谢@勤思·）
5. 【server】优化 文件发布-ssh发布新增变量：FILE_NAME、FILE_EXT_NAME

------

### 2.10.42.3-beta (2023-08-04)

### 🐣 新增功能

1. 【server】新增 SSH
   文件管理修改文件权限功能（感谢 [@MichelleChung](https://gitee.com/michelle1028) [Gitee issues I6VDXS](https://gitee.com/dromara/Jpom/issues/I6VDXS) ）

### 🐞 解决BUG、优化功能

1. 【server】升级 h2、SpringBoot 版本
2. 【server】使用系统git时，无法克隆tag问题优化 （感谢@唐明）
3. 【server】优化 SSH 和 代码仓库的密码从工作空间变量中读取

------

### 2.10.42.2-beta (2023-07-04)

### 🐣 新增功能

1. 【server】新增 Docker 容器重建功能，即删除原有的容器，重新创建一个新的容器

### 🐞 解决BUG、优化功能

1. 【server】优化
   删除工作空间前预检查关联数据存在情况（感谢 [@陈旭](https://gitee.com/chenxu8989) [Gitee issues I7F0ZN](https://gitee.com/dromara/Jpom/issues/I7F0ZN) ）
2. 【server】优化
   退出登录支持彻底退出、切换账号退出（感谢 [@wangfubiao](https://gitee.com/wangfubiao) [Gitee issues I7GA5Q](https://gitee.com/dromara/Jpom/issues/I7GA5Q) ）
3. 【server】优化 IP 白名单验证忽略 IPV6 情况
4. 【server】优化 服务端缓存管理支持查看黑名单 IP 详细信息（感谢@酱总）

------

### 2.10.42.1-beta (2023-06-26)

### 🐣 新增功能

1. 【server】新增 Docker 管理增加 SSH 连接

### 🐞 解决BUG、优化功能

1. 【server】修复 SSH
   编辑输入框出现部分关键词时保持报错（感谢 [@一只羊](https://gitee.com/hjdyzy) [Gitee issues I7E3UG](https://gitee.com/dromara/Jpom/issues/I7E3UG) ）
2. 【server】优化 日志组件支持显示 \t 制表符、清空缓冲区滚动到顶部
3. 【server】修复 彻底删除节点分发时未自动删除关联日志（感谢@ccx2480）
4. 【server】修复
   节点管理中脚本模板翻页无效（感谢 [@wangfubiao](https://gitee.com/wangfubiao) [Gitee issues I7F0RS](https://gitee.com/dromara/Jpom/issues/I7F0RS) ）
5. 【server】优化
   工作空间配置页面中新增节点分发白名单配置入口（感谢 [@陈旭](https://gitee.com/chenxu8989) [Gitee issues I7F0W0](https://gitee.com/dromara/Jpom/issues/I7F0W0) ）
6. 【server】优化 构建附加环境变量支持解析 URL 参数格式
   （感谢 [@爱琳琳真是太好了](https://gitee.com/qiqi513_admin) [Gitee issues I7FROG](https://gitee.com/dromara/Jpom/issues/I7FROG) ）
7. 【server】优化 构建支持单个配置保留天数和保留个数
   （感谢 [@阿超](https://gitee.com/VampireAchao) [Gitee issues I7FOG2](https://gitee.com/dromara/Jpom/issues/I7FOG2) ）

------

### 2.10.41.3-beta (2023-06-12)

### 🐣 新增功能

1. 【server】新增 SSH 列表支持显示 docker 版本信息
2. 【server】优化 Docker 镜像增加批量删除（已经被容器使用的镜像不会删除）
3. 【server】优化 启用 Jpom 新版专属 logo

### 🐞 解决BUG、优化功能

1. 【server】修复 查看 docker 容器日志 web socket 线程被阻塞问题
2. 【server】优化 日志组件显示高亮、滚动条样式优化
3. 【server】优化 web socket 会话关闭显示分类
4. 【server】优化 页面滚动条样式
5. 【server】优化 编辑关联分发，选择项目下拉框不能显示项目全名称（tooltip）（感谢@LYY）
6. 【server】优化 监听页面关闭事件，主动关闭 websocket
7. 【server】修复
   批量构建触发器无法正常使用（感谢 [@botboy](https://github.com/cheakin) [Github issues 48](https://github.com/dromara/Jpom/issues/48) ）

### ⚠️ 注意

1. 如果自定义过 SSH 监控脚本需要自行同步获取 docker 信息脚本

------

### 2.10.41.2-beta (2023-06-09)

### 🐣 新增功能

1. 【server】新增 工作空间新增分组字段（存在多个分组时页面切换将使用二级选择）（感谢@酱总）

### 🐞 解决BUG、优化功能

1. 【server】修复 页面关闭 docker 终端未主动关闭后台终端进程问题
2. 【server】优化 docker 终端页面缓冲区大小自动适应
3. 【server】优化 项目列表可以查看项目日志（避免控制台卡顿无法操作下载日志）(感谢@阿超)
4. 【server】优化 日志组件采用虚拟滚动渲染，避免日志过多浏览器卡死

------

### 2.10.41.1-beta (2023-05-15)

### 🐣 新增功能

1. 【server】新增 仓库支持导入导出
2. 【server】新增 镜像创建容器支持配置 hostname、集群服务支持配置 hostname（感谢@心光）

### 🐞 解决BUG、优化功能

1. 【server】优化 资产管理支持管理共享仓库
2. 【server】优化 增大验证码检测功能异常捕捉范围
3. 【server】修复
   令牌导入仓库令牌长度不足问题（感谢 [@Sherman Chu](https://github.com/yeliulee) [Github issues 45](https://github.com/dromara/Jpom/issues/45) ）
4. 【server】修复
   分发列表配置功能无法使用（感谢 [@Free](https://gitee.com/fjlyy321) [Gitee issues I716UI](https://gitee.com/dromara/Jpom/issues/I716UI) ）
5. 【server】修复 构建卡片布局、构建详情中构建方式显示不正确（感谢@A）

------

### 2.10.40.8-beta (2023-04-19)

### 🐞 解决BUG、优化功能

1. 【server】修复
   仓库编辑清除密码按钮弹窗层级问题（感谢 [@轩辕豆豆](https://gitee.com/xuanyuandoudou) [Gitee issues I6VSCR](https://gitee.com/dromara/Jpom/issues/I6VSCR) ）
2. 【server】修复
   优化构建列表卡片布局存在未构建数据布局错乱问题（感谢 [@lin_yeqi](https://gitee.com/lin_yeqi) [Gitee issues I6VUB7](https://gitee.com/dromara/Jpom/issues/I6VUB7) ）

------

### 2.10.40.7-beta (2023-04-14)

### 🐞 解决BUG、优化功能

1. 【server】修复 在线更新无法正常使用

------

### 2.10.40.6-beta (2023-04-14)

### 🐣 新增功能

1. 【server】新增 容器构建中对 gradle
   插件的支持（感谢 [@xiaozhi](https://gitee.com/XiaoZhiGongChengShi) [Gitee pr 188](https://gitee.com/dromara/Jpom/pulls/188) ）

### 🐞 解决BUG、优化功能

1. 【server】优化 构建 SSH 发布命令支持 `SSH_RELEASE_PATH` 环境变量（感谢@定格）
2. 【server】修复 全屏终端无法打开文件管理（感谢@Pluto）
3. 【server】优化 自动探测服务端登录验证码是否可用
4. 【all】优化 文件编辑后缀识别支持配置文件名或者正则表达式（感谢@MichelleChung）
5. 【server】优化 支持自动执行触发器清理
6. 【server】优化 重新登录未加载管理员菜单（@A）
7. 【server】修复 第三方登录跳转测试丢失

------

### 2.10.40.5-beta (2023-04-12)

### 🐞 解决BUG、优化功能

1. 【server】修复 清空浏览器缓存未跳转到登录页面
2. 【server】优化 构建拉取 git 仓库支持使用服务器中的 git 插件，实现配置克隆深度参数
3. 【server】修复
   删除节点脚本报错（感谢 [@xiaozhi](https://gitee.com/XiaoZhiGongChengShi) [Gitee issues I6USMY](https://gitee.com/dromara/Jpom/issues/I6USMY) ）

------

### 2.10.40.4-beta (2023-04-07)

### 🐞 解决BUG、优化功能

1. 【agent】修复 获取项目状态部分情况出现 NPE （感谢@酱总）

------

### 2.10.40.3-beta (2023-04-06)

### 🐞 解决BUG、优化功能

1. 【server】优化 编辑构建无法重置已经选择的事件脚本 （感谢@左手生活，右手浪漫）
2. 【server】优化 登录页面切换验证码自动清空验证码输入框（感谢@TrouBles）
3. 【server】修复 docker 集群日志查看后未自动关闭造成日志文件继续增长的问题（@无味。）
4. 【server】优化 服务端缓存项目信息的创建时间和修改时间同步为节点中的数据创建、修改时间
5. 【server】优化 文件管理支持批量删除（感谢@左手生活，右手浪漫）
6. 【agent】优化 取消 hutool-cache 包依赖
7. 【server】优化 JustAuth fastjson 依赖配置为 fastjson2

------

### 2.10.40.2-beta (2023-04-04)

### 🐞 解决BUG、优化功能

1. 【server】修复 项目文件跟踪控制台无法正常使用（感谢@左手生活，右手浪漫）
2. 【server】修复 插件端日志无法正常差异
3. 【server】修复 docker 拉取镜像不能识别私有仓库地址（@章强）

------

### 2.10.40.1-beta (2023-04-04)

### 🐞 解决BUG、优化功能

1. 【server】修复 日志搜索控制台无法正常使用（感谢@左手生活，右手浪漫）

------

### 2.10.39.4-beta (2023-04-03)

### 🐞 解决BUG、优化功能

1. 【server】修复 查看文件发布详情节点名称未显示
2. 【server】优化 发布记录重建不能选中节点

------

### 2.10.39.3-beta (2023-04-03)

### 🐞 解决BUG、优化功能

1. 【server】修复 资产管理机器管理单个分配工作空间无法正常使用（感谢@咻咻咻秀啊）
2. 【server】修复 资产管理相关权限、操作日志无法记录问题（感谢@咻咻咻秀啊）
3. 【server】修复 docker 控制台 、日志无法正常使用
4. 【server】优化 docker 控制台页面布局优化，支持单独查看 docker-compose
5. 【server】优化 docker 实时查看日志支持配置是否显示时间戳

------

### 2.10.39.2-beta (2023-03-31)

### 🐞 解决BUG、优化功能

1. 【server】修复 构建同步到文件管理中心失败（感谢@破冰）

------

### 2.10.39.1-beta (2023-03-31)

### 🐞 解决BUG、优化功能

1. 【server】优化 登录成功主动刷新菜单缓存、切换账号登录工作空间无权限页面白屏（感谢@A、@零壹）
2. 【all】更名 变更包名为 `org.dromara.jpom`
3. 【server】修复 编辑 docker 导入证书弹窗无法正常显示问题（感谢@左手生活，右手浪漫）
4. 【server】修复 工作空间中资产管理相关页面搜索无数据时出现操作引导提示（感谢@酱总）

------

### 2.10.38.11-beta (2023-03-31)

### 🐣 新增功能

1. 【server】新增 oauth2 新增 gitee、github 平台（感谢@A）

### 🐞 解决BUG、优化功能

1. 【server】优化 规范 oauth2 登录接口和回调地址

------

### 2.10.38.10-beta (2023-03-30)

### 🐞 解决BUG、优化功能

1. 【server】修复 节点脚本支持全局共享同步节点、节点管理查看脚本重复问题（感谢@奇奇）
2. 【server】修复 创建构建选择命令模板无法修改（感谢@定格）
3. 【server】优化 构建新增配置是否发布隐藏文件属性（感谢@简单）

------

### 2.10.38.9-beta (2023-03-30)

### 🐣 新增功能

1. 【all】新增 节点脚本支持全局共享（感谢@奇奇）
2. 【server】新增 构建状态新增`队列等待`，用于标记当前构建存于线程排队中（感谢@酱总）

### 🐞 解决BUG、优化功能

1. 【server】修复 SSH 终端无法正常使用的问题（感谢@左手生活，右手浪漫）
2. 【server】修复 缓存中的工作空间 ID 和路由工作空间 ID 不一致问题
3. 【server】修复 超出构建队列数取消任务不生效问题（感谢@酱总）

------

### 2.10.38.8-beta (2023-03-30)

### 🐞 解决BUG、优化功能

1. 【server】修复 节点脚本、项目控制台无法正常使用的问题（感谢@奇奇）

------

### 2.10.38.7-beta (2023-03-29)

### 🐣 新增功能

1. 【server】新增 构建新增配置排除发布目录表达式（感谢@毛毛虫）

### 🐞 解决BUG、优化功能

1. 【server】修复 修改节点未填写密码不能保存
2. 【server】修复 服务端脚本非共享脚本不能执行问题
3. 【server】优化 构建命令支持引用脚本模板内容（便于复杂构建命令管理）（感谢@毛毛虫）

------

### 2.10.38.6-beta (2023-03-29)

### 🐞 解决BUG、优化功能

1. 【server】修复 SSH 分组无法正常搜索、排序异常（感谢@A）
2. 【server】修复 编辑构建分组、保存并构建按钮无效（感谢@酱总）
3. 【server】修复 全局共享脚本日志无法查看问题（感谢@酱总）

------

### 2.10.38.5-beta (2023-03-29)

### 🐞 解决BUG、优化功能

1. 【server】优化 证书管理支持使用文件管理部署
2. 【server】优化 节点管理中项目管理和脚本管理菜单合并为一个菜单
3. 【server】修复 节点分发构建确认弹窗筛选项目不生效问题
4. 【server】修复 无法使用添加构建功能
5. 【server】修复 资产管理 ssh 分组不生效问题（感谢@A）
6. 【server】优化 构建详情页面布局（构建触发器、查看构建历史）
7. 【server】优化 新增构建状态描述来记录构建异常信息
8. 【server】优化 构建页面新增卡片布局方式

### ⚠️ 注意

构建触发器变动，发生异常时 type 为 error,并且新增：statusMsg 字段

------

### 2.10.38.4-beta (2023-03-28)

### 🐣 新增功能

1. 【all】新增 文件管理发布支持发布到节点指定目录

### 🐞 解决BUG、优化功能

1. 【server】修复 无法正常使用下载功能
2. 【all】升级 springboot、hutool、fastjson2、svnkit 版本

------

### 2.10.38.3-beta (2023-03-27)

### 🐞 解决BUG、优化功能

1. 【server】优化 删除管理脚本中的 `-XX:+AggressiveOpts` 参数
   （感谢 [@牛孝祖](https://gitee.com/niuxiaozu) [Gitee issues I6PUNM](https://gitee.com/dromara/Jpom/issues/I6PUNM) ）
2. 【server】修复 兼容 oauth2 登录没有 state 场景（感谢@酱总）

------

### 2.10.38.2-beta (2023-03-27)

### 🐣 新增功能

1. 【server】新增 支持 oauth2 登录
   （感谢 [@MaxKeyTop](https://gitee.com/maxkeytop_admin) [Gitee pr 183](https://gitee.com/dromara/Jpom/pulls/183) ）

### 🐞 解决BUG、优化功能

1. 【server】优化 在线构建仓库支持全局共享（避免同一个仓库频繁创建）（感谢@酱总）
2. 【server】修复 开启 beta 计划快速安装命令不是 beta 版本（感谢@酱总）
3. 【server】修复 批量构建任务等待中没有日志输出问题（感谢@酱总）
4. 【server】优化 构建支持批量取消（感谢@酱总）
5. 【server】优化 取消构建时候主动删除构建容器
6. 【server】优化 构建触发器新增微队列，避免同一时间提交构建任务被拒绝（服务重启队列任务将丢失）（感谢@轩辕豆豆）
7. 【server】优化 环境管理页面支持查看间隔任务统计信息
8. 【server】优化 令牌导入仓库模块统一调整为模板配置（部分方式不支持搜索）（感谢@魏宏斌）
9. 【agent】优化 DSL 项目报警内容添加状态消息（感谢@核桃）
10. 【server】优化 服务端脚本支持配置全局共享（感谢@酱总）

------

### 2.10.38.1-beta (2023-03-23)

### 🐣 新增功能

1. 【server】新增 证书管理全部迁移到服务端统一导入 （感谢@.）
2. 【server】新增 节点项目支持导入，导出（感谢@酱总）

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

### ⚠️ 注意

1. 如果节点已经配置过项目文件下载远程地址白名单需要统一配置到服务端的工作空间的白名单。
2. 已经配置节点项目远程下载白名单将保留只读，不做实际判断

### ❌ 不兼容功能

1. 【agent】取消 节点管理证书管理取消上传编辑功能（保留查询删除功能）
2. 【agent】取消 节点白名单配置取消 ssl 证书路径配置
3. 【agent】取消 节点项目文件下载远程文件白名单统一调整到服务端白名单配置

------
