# 🚀 版本日志

## 2.11.14.6-beta

------

## 2.11.14.5-beta (2026-11-23)

### 🐞 解决BUG、优化功能

1. 【server】修复 **非超级管理员「新增项目」被误判为越权**（提示「没有当前数据权限,需要管理员或者数据创建人才操作该数据」，2026-11-21 重启计划群反馈，2.11.13.1 起存在）：新增项目时表单必填的项目 ID 还没入库，节点项目缓存查不到，权限拦截器的 fail-closed（缓存未命中即拒绝）把它当成越权，只有 `sys` 账号能建。现在 `@NodeDataPermission` 新增 `allowMissingData` 声明，项目保存显式允许「数据尚未入库」并继续走方法内的归属校验（`checkProjectPermission` 仍会拒绝其它工作空间的项目 ID）；未声明的方法行为不变，其余接口仍是 fail-closed
2. 【server】修复 **服务端读不到 IP 授权配置时被伪装成「IP 拒绝」**（页面跳 `/prohibit-access`、返回 `{"code":999,"msg":"Prohibition of access"}`，2026-11-21 重启计划群反馈：构建卡住后重新构建全是 999）：`IpInterceptor` 在读取/解析系统参数异常时一律回 999，用户在 IP 白名单页查不出任何问题。现在区分两种拒绝——IP 命中黑名单/不在白名单仍是 999（`Prohibition of access`），而服务端读取或解析配置失败改为 500 +「IP授权配置读取失败（服务端数据库或系统参数异常），这不是IP限制，请查看服务端日志后重试」，前端不再跳 `/prohibit-access`；两种情况的拒绝语义与日志均保留

------

## 2.11.14.4-beta (2026-11-21)

### 🐣 新增功能

1. 【server】【web-vue】新增 **SSH 项目**：不安装插件端，只靠一台 SSH 机器 + 一个工作目录 + 一段生命周期脚本（`run.start/status/stop/restart/reload` + `config.log`）管理项目，配置沿用 DSL 的 YAML 语义（新增可选字段 `script` 内联脚本与 `timeout` 执行超时，均可被项目内脚本文件或服务端脚本库 `G@` 引用替代）。内置 9 个模板（Spring Boot Jar、通用 Java classpath、Docker Compose、systemd、Node.js、Python、Go/二进制、Nginx 静态站、纯文件）按字段表单渲染成可读可改的 YAML，保存后与模板解耦；提供启动/停止/重启/重载、状态（脚本自证 + PID 文件回退）与端口探测，执行统一带超时、输出落控制台日志文件；菜单入口在「节点&项目 → SSH 项目」，独立表 `SSH_PROJECT_INFO`、独立接口与列表，不与现有项目列表混搜；SSH 项目不做进程守护（需要守护请用 systemd 模板），控制台（WS 实时日志 + 应用日志切换 + 启停按钮）、自动启动（每分钟扫描，多实例靠脚本幂等兜底）、执行审计（`SSH_PROJECT_EXEC_LOG`：谁/哪台机/哪个钩子/退出码/耗时）一并提供；项目文件管理（直接复用 SSH 资产文件管理实现，按项目目录限定授权路径）、日志备份/清空/下载一并提供；触发器（`/api/ssh-project/{id}/{token}?action=start|stop|restart|reload|status`，无需登录、校验项目 token；token 由触发器地址接口生成/重置，与项目、脚本触发器同一套生成、调用统计与「系统 → 触发器」统一管理，重置后旧地址立即失效）一并提供；在线构建「发布方式」新增「SSH 项目」（产物直接上传到项目目录，clearOld 时先清空，完成后按项目生命周期脚本重启；被构建引用的项目禁止删除）

### 🐞 解决BUG、优化功能

1. 【server】【web-vue】修复 SSH 项目「机器 SSH」列与编辑弹窗显示为空、下拉没有 placeholder：列表改为展示机器/资产名称（tooltip 保留 id 便于排查），编辑回显按 `machineSshId` 反查工作空间记录、匹配不到时给出可读兜底项；同时修正所有 `a-select` 初始值为空字符串导致 placeholder 不显示的问题（Ant Design Vue 只有值为 undefined 时才渲染 placeholder），并给相关输入补上占位文案
2. 【server】【agent-java】【agent-go】修复 项目 DSL 配置里包含中文时解析失败（`special characters are not allowed`）：YAML 解析改为字符流（`Reader`），不再依赖 JVM 默认编码——默认编码为 ASCII/POSIX 的机器上，含中文的 DSL 配置此前无法保存
3. 【server】【agent-go】修复 反向隧道下 Go 插件端的 WebSocket 通道（在线升级、项目控制台、脚本执行）建连后立即断开；Java 插件端不受影响
4. 【server】修复 在线升级「所有节点」页的引擎、平台、实时版本号、打包时间、运行时间刷新页面或换标签页后全部为空（Go 节点必现）；现在通道异常时会给出提示
5. 【agent-go】修复 Go 节点「运行时间」显示被缩小 1000 倍、机器列表「JDK 版本」列显示成 Go 版本；Java 插件端不受影响
6. 【server】【agent-java】【web-vue】新增 在线升级「所有节点」页的「平台」列展示 Java 插件端运行平台（此前只有 Go 插件端有值），与 Go 端同口径（如 linux/amd64）；旧版 Java 插件端显示「未上报」
7. 【web-vue】修复 资产管理「FTP 管理」列表没有表格边框（行列之间没有分隔线），与 SSH、Docker、机器等列表不一致；补齐后样式统一，功能与数据不变
8. 【server】【web-vue】优化 机器管理新增「插件端类型」列并把插件端引擎落库（保存资产与心跳读取 /info manifest）：Java 插件端上「可作为构建节点」开关直接禁用并提示需升级为 Go 插件端，不再出现「能打开、保存时才被拒绝」；在线升级的「引擎」列统一更名为「插件端类型」，取值统一显示为 Go 插件端 / Java 插件端
9. 【server】【web-vue】新增 机器资产「通讯方式」（自动 / 仅反向隧道 / 仅正向拉取）：选「仅反向隧道」时由插件端主动连接服务端，节点地址、账号、密码、协议、代理、超时不再参与通信（界面禁用、保存不再强制填写，且不会静默回退正向）；默认为「自动」，隧道在线走隧道、离线回退正向，老数据行为不变；机器列表展示通讯方式与隧道在线状态
10. 【server】【agent-go】修复 节点构建拉取产物时「重试成功后仍判定失败」：原先重试循环用同一个异常变量记录失败，后一次重试已经拉全产物仍会抛出上一次的旧异常（构建日志里只剩一条旧的大小不一致，看不到任何传输错误）；同时把拉取改造成**断点续传 + 字节对账**：每次尝试都会在构建日志记录「起点 / 本次收到 / 累计 / 耗时」，响应被中途截断时明确提示「传输被中断（声明 X 实际 Y）」而不是笼统的大小不一致；重试 3 次改为 5 次并退避等待，节点支持 `Range` 时从断点继续（老版本节点返回 200 全量时自动从头重下），产物大小与 sha1 校验保持原语义
11. 【web-vue】修复 机器资产「可作为构建节点」的说明文字用内联 `font-size:12px` + 硬编码 `rgba(0,0,0,.45)` 的 div 硬写，同一句话又重复挂了一个 tooltip：浅色下字号比同表单其它项小一号、颜色绕过主题，**深色模式下更是几乎看不见（实测对比度 1.15:1，文字比背景还暗）**。改为走 `a-form-item` 的 `help` 槽（与「节点地址」「构建并发上限」等同字号 14px、同主题色、与控件列左对齐），颜色随主题切换（深色下 `rgba(255,255,255,.45)`、对比度 4.40:1），去掉重复 tooltip，文案与功能不变；同时新增浏览器 E2E 契约 `machine-form-help`（浅色 + 深色两轮比对同弹窗内各项 help 的计算样式与对比度），防止同类跑偏再次出现
12. 【server】【web-vue】修复 **SSH 项目控制台打开后不可用**（日志区只有「未匹配到合适的权限不足」「SSH 项目不存在」）：控制台 WebSocket 握手一直没带 `nodeId` 参数，被节点权限校验当成「没有该节点」直接拒绝；现在 SSH 项目不再要求 `nodeId`（与节点无关），前端也按脚本控制台口径补上该参数。同时把「控制台」改成与「项目控制台」一致的抽屉内打开（不再新开标签页），`/ssh-project/console` 独立路由保留用于深链
13. 【server】修复 SSH 项目控制台看不到日志（只有「欢迎加入」一行）：日志跟随命令是 `tail -n N -F '$HOME/.jpom/logs/{id}.log'`，单引号里 `$HOME` 不展开，tail 每次都报文件不存在，而 stderr 被丢弃，于是控制台永远只剩欢迎语；现在先把 HOME 展开成远端绝对路径再跟随，并在日志文件不存在/为空时给出明确提示（项目还没通过 Jpom 启动过 / 日志当前为空），不再让人误以为日志功能坏了
14. 【server】【web-vue】新增 **SSH 项目版本快照与快速回滚**（与插件端项目的版本快照同语义）：构建配置打开「版本快照」后，发布到 SSH 项目时先在目标机器登记基线、发布成功再把项目目录登记成本次构建的版本（`$HOME/.jpom/versions/<项目ID>/`，含来源、构建号、文件数与内容指纹，按「版本保留个数」自动清理旧版本）；「构建历史 → 快速回滚」对 SSH 项目按构建号列出该次构建产生的版本，可直接切回（先把当前目录做成安全快照，再覆盖版本内容并做内容指纹校验，校验失败自动回到安全快照，可选切换后重启），不再需要重新传输产物；SSH 项目列表「更多 → 版本」提供版本列表、人工创建快照、切换与删除（当前运行版本不可删除），与节点项目的「版本」页交互一致
15. 【server】【web-vue】修复 **SSH 项目「文件」抽屉打开后一直「暂无数据」**：文件授权的项目目录带结尾斜杠（`…/probe/`），与 SSH 资产的目录写法不一致，目录树节点也显示成带斜杠的怪路径；现在统一不带结尾斜杠，并让文件管理在只有一个授权目录（即项目目录）时自动进入并列出文件，不再需要先点目录树
16. 【server】【web-vue】修复 **SSH 项目触发器自成一套规则**（手工填「访问令牌」、地址前缀 `/openapi`、不参与统一触发器管理）：改为与项目、脚本、SSH 脚本等既有触发器完全一致——字段统一为 `triggerToken`，「更多 → 触发器」里查看/复制单条与批量地址、需要时「重置」，token 由服务端生成并登记到「系统 → 触发器」（记录生成人、调用次数，可在统一列表删除），openapi 地址统一为 `/api/ssh-project/{id}/{token}?action=...`（带代理 contextPath），调用时按 token 绑定人记账；编辑弹窗不再提供手工填写 token，保存时也不会写入客户端传入的 token
17. 【web-vue】优化 **列表「操作」列统一固定到最右侧**：调度日志详情、项目文件「版本」、SSH 项目「版本」、仓库导入、工作空间「环境变量」、用户「权限组」6 个列表的操作列此前没写 `fixed: 'right'`，列一多就得横向滚动才找得到按钮，现在与其余列表统一固定到右侧；仓库导入弹窗同时补上横向滚动（没有 `scroll.x` 时 Ant Design Vue 会直接忽略 `fixed`，只写 fixed 仍不生效）。另把这条约定做成机械门禁 `dev-scripts/check-table-action-column.py`（漏 `fixed`、漏 `scroll.x`、操作列不是最后一列直接阻断提交）与浏览器契约场景 `table-action`（缩窄视口把表格滚到尽头，量操作列是否仍贴右），新增列表页不再靠人记
18. 【web-vue】优化 **列表「操作」列宽度改为按内容自适应**（全系统 60 处统一）：此前操作列都写死宽度（80px～400px 共 24 种，甚至按工作空间取 `this.global ? '240px' : '180px'`），按钮少的页面留一大片白、按钮多的页面又把按钮挤换行；典型如 SSH 项目列表写 400px 而实际只占 280px。现在统一改为 `width: '1%'` + 全局 `white-space: nowrap`，列宽正好等于按钮组宽度（实测 SSH 项目 400px → 297px、节点列表 200px → 193px、权限组 120px → 121px，富余仅剩单元格内边距），并随「个性化 → 紧凑模式」自动收缩（同一列 297px → 262px），不需要任何前端判断分支；`preflight-check.sh --ui` 门禁同步升级（写死宽度、缺 nowrap 规则都会阻断提交），浏览器契约 `table-action` 增加「列宽 = 内容宽 + 内边距」并在常规/紧凑两种模式下各校验一轮
19. 【server】修复 反向隧道离线时接口只返回通用「服务异常」：`TransportAgentException` 与 `AgentException` 是两套异常却共用一个参数类型为 `AgentException` 的处理方法，Spring 解析参数失败后退化成兜底文案；现在拆成独立处理器，用户能直接看到「节点未通过反向隧道连接（当前通讯方式为『仅反向隧道』）」这类真实原因（`dev` 回归发现）
20. 【server】【agent-java】【agent-go】新增 **反向隧道自动注册的节点自动绑定通讯凭据**：插件端在 `HELLO` 里额外上报自己的授权账号与摘要（`authUser`/`authToken`，不含明文密码），服务端落库到机器资产新增列 `authorizeToken` 后节点可直接使用，不必再手工补账号密码；已存在但缺授权的老节点会在握手时自动补齐（不覆盖人工配置）；协议字段只增，旧服务端忽略、旧插件端不发送，行为不变
21. 【server】新增 **反向隧道自动注册可直接分配到工作空间**：新增配置 `jpom.agent.tunnel.auto-register-workspace`（默认空＝只建机器资产，保持原行为），配置后握手自动注册会同时生成节点，实现「插件端配好 `server-url`/`key`/`machine-id` 即可用」，不需要人工到机器资产页再点「分配」
22. 【server】修复 机器资产列表带 `workspaceId` 表单参数时 500（`Column "WORKSPACEID" not found`）：机器资产不属于工作空间，列表查询会把客户端参数直接当列名拼进 SQL；现在对「实体没有 workspaceId 字段」的列表接口自动剔除该参数，工作空间实体的强制隔离逻辑不变（`dev` 回归发现）
23. 【agent-go】修复 Go 插件端 `allowEditSuffix` 多值塞成一条时报 `Unsupported charset`（如 `.conf@UTF-8,.txt@UTF-8`）：与 Java 端对齐为「后缀取第一个 `@` 之前、编码取最后一个 `@` 之后」，非法编码回退默认编码而不是报错；Java 插件端不受影响（`dev` 回归发现）
24. 【agent-java】【web-vue】修复 插件端没有外置 `conf/application.yml`（直接 `java -jar` 启动）时「系统配置」页 500（`class path resource ... cannot be resolved to absolute file path`）：现在按只读返回配置内容并提示原因，前端同步禁用保存按钮；按 `bin/Agent.sh` 启动（有外置配置）时行为不变
25. 【server】【agent-java】【agent-go】修复 插件端刚上线 30-60s 内节点被误判「长时间未主动上报状态」：首帧推送要等指标库首次采集；现在服务端在「推送超龄但反向隧道仍在线」时保持上一状态（离线交由心跳判定），插件端启动即预热状态采集并立即执行首轮推送
------

## 2.11.14.3-beta (2026-11-20)

### 🐣 新增功能

1. 【server】【agent-go】新增 **构建节点**：构建配置可指定由某个 Go 插件端承担「拉取代码 + 执行构建命令 + 收集产物」，服务端只做编排、排队、日志落盘与产物发布。构建日志、历史记录、产物下载、节点分发/SSH/项目发布链路与本地构建完全一致，前端「构建日志」页无需任何改动
2. 【server】新增 构建节点能力校验与选择接口：只有 Go 插件端（`/info` 声明 `capabilities: ["build"]`）且机器资产开启「可作为构建节点」的节点才能被选中；保存与运行两处都会校验节点在线、引擎与能力，容器构建、SVN 仓库在节点上直接拒绝并给出明确提示
3. 【server】新增 构建节点异常收敛：节点并发已满自动排队等待、日志流断开自动查节点状态对账、节点重启后任务按「丢失」处理，避免出现永久「构建中」
4. 【server】新增 构建产物回收与校验：产物 zip 由服务端主动拉取，校验大小与 sha1 后解压到原有结果目录结构（含 ant 通配的结果目录改写语义），并做 zip-slip 路径校验
5. 【server】【web-vue】新增 构建方式「节点构建」：构建方式从两种（本地构建、容器构建）扩展为三种，选「节点构建」后可指定由哪台 Go 插件端承担「拉取代码 + 执行构建命令 + 收集产物」，脚本写法与本地构建一致；机器资产新增「可作为构建节点」开关与并发上限，构建列表/历史/详情展示执行节点
6. 【server】【agent】【agent-go】【web-vue】新增 DSL 项目控制台查看运行日志：DSL 配置新增 `config.log` 声明项目自己的日志文件（`name`/`path`/`charset`，路径支持相对项目路径、绝对路径与 `${PROJECT_PATH}`、`${PROJECT_ID}`、`${PROJECT_NAME}` 变量），项目控制台新增日志文件下拉并实时跟踪，不再只能看 DSL 脚本输出；未配置时控制台行为与之前完全一致，客户端只回传日志标识、真实路径由插件端解析（相对路径限制在项目目录内）

### 🐞 解决BUG、优化功能

1. 【server】修复 docker 资产控制台越权（CVSS 8.8）：`/system/assets/docker/images`、`container`、`volumes`、`networks`、`swarm-service`、`swarm` 六个资产控制台控制器补上系统管理权限校验。此前只有某个工作空间执行权限的用户，可以用未分配给任何工作空间的 docker 资产 id 直接创建并启动挂载宿主目录的特权容器（2026-11-19 外部安全报告；此前的跨工作空间隔离只覆盖了工作空间的 docker 控制器，没有触及这六个资产控制台控制器）
2. 【server】修复 docker 资产列表与集群列表接口回显仓库密码：资产模型不再序列化 `registryPassword`，`/system/assets/docker/swarm/list-all` 也不再对非系统管理用户开放；编辑 docker 资产时密码框留空表示沿用原密码，不会因为保存一次资产把仓库凭据清空
3. 【server】修复工作空间 docker「拉取镜像」把资产存储的仓库账号密码发送到调用者指定的第三方镜像仓库（CVSS 6.5）：只有镜像与资产配置的镜像仓库同域时才附加资产仓库凭据，其它情况按匿名拉取，并在拉取日志中说明已跳过资产仓库凭据（2026-11-19 外部安全报告）
4. 【web-vue】修复 新建构建时发布方式选「项目」但还没有选择节点/项目，点「保存」「保存并构建」完全没有任何反应（无请求、无提示、输入白填）的问题：`releaseMethodDataIdList` 未初始化导致保存流程里读 `.length` 抛错，而这个异常被表单校验的 catch 静默吞掉；现在该字段初始化为数组并给出「请选择节点项目」提示，保存过程中的任何异常都会弹出可见错误并输出到浏览器控制台
5. 【web-vue】修复 产品内提示文案已经过时的菜单路径（项目表单/文件管理/分发表单/脚本模版/释放分发确认）：把「节点管理中的【插件端配置】=>【授权配置】」改为「【系统管理】-> 【资产管理】-> 【机器管理】中打开该节点的【授权配置】」，把「【节点分发】=>【分发授权配置】」改为「【节点&项目】-> 【授权配置】」，其余提示补上「节点&项目 -> 项目列表、逻辑节点 -> 脚本管理」等真实路径；四条语言包（`zh_cn`/`zh_hk`/`zh_tw`/`en_us`）同步修改，只改文案值不动 key
6. 【server】【agent】【agent-go】【web-vue】修复 构建配置只打开「版本快照」开关、保留个数留空（默认 0）时版本快照其实从未生效：0 被原样透传给插件端，插件端据此判定「未启用」，发布日志却打印「已登记版本快照」，于是构建历史「快速回滚」点进去永远没有记录（2026-11-19 用户群反馈）。现在保留个数 0/空统一按默认 3 处理（前端打开开关自动补 3），服务端改为同时校验插件端返回的 `enabled`（不再只看 success），未生效时在发布日志明确告警；快速回滚切换版本时显式带上保留个数，避免插件端按「未启用」把历史版本清理到只剩当前版本（Java/Go 双端一致）
7. 【server】修复 文件发布任务可跨工作空间指定节点/SSH 目标、发布二级目录可穿越到授权目录之外（CVSS 8.8）：创建文件发布任务时目标 id 只做存在性校验、没有工作空间条件，工作空间 A 仅有执行/上传权限的用户只要知道工作空间 B 的 node/SSH 主机 id，就能让发布前/后脚本在 B 的主机上执行、文件落到 B 的发布目录（A 的节点列表里看不到该主机，`/node/ssh_command/batch` 对同一 id 会拒绝）；同一入口的 `releasePathSecondary` 只做字符串拼装 + `normalize`，`..` 可以把文件写到分发授权目录之外。现在目标按请求工作空间解析（查不到直接剔除，全部非法时拒绝创建、不启动任务），发布二级目录必须落在已授权的父目录内（越界直接拒绝），文件发布与证书发布共用同一处解析，任务执行时再按任务所属工作空间校验一次目标（2026-11-20 外部安全报告 #6）
8. 【server】修复 **反向隧道下所有下载类接口失效**（项目文件下载 405「请选择文件」、备份下载「备份id缺失」、日志回溯下载「对应文件必须填写」，用户点下载无反应）：隧道转发把参数一律编码成 GET 请求体，而插件端下载接口是 GET 映射、不解析请求体，参数整包丢失。现在下载转发按与表单同源的加密与 URL 编码规则生成 query 拼到路径上、body 传空，只改服务端，正向直连与 Java/Go 插件端行为不变
9. 【server】修复 文件发布任务创建报 500「空指针」（任务记录已落库但无法执行）：save2Template（保存为模板）是可选参数，未选择时为 null，模板服务直接 switch(type) 触发 NPE；现在为空时直接跳过模板保存。顺带修复同类「数据不存在」提示：构建回滚/构建编辑/备份删除与恢复/项目触发 URL/日志搜索保存等 13 处数据存在性校验从 Objects.requireNonNull 改为 405 友好提示，不再统一回「Server程序错误，空指针」
10. 【server】【web-vue】修复 权限组编辑弹窗打不开（浏览器报 Unexpected end of JSON input，整个权限组功能不可用）：服务端把空的「允许/禁止执行时段」存成空字符串，前端 JSON.parse("") 直接抛错；现在服务端空配置统一存 []、前端同时加兜底（历史数据无需迁移）
11. 【server】修复 镜像导入只校验扩展名：上传任意非 tar 内容也返回「导入成功」；现在按 tar 归档结构校验，非法内容回 405「不是合法的镜像包」。另修复未知路由返回 code=500（与真实服务端异常无法区分），统一回 code=404
12. 【server】修复 操作监控规则不选报警人时返回 500「空指针」（应为 405「请选择报警联系人」）；项目触发 URL 传插件端项目 id 时报「空指针」，现在提示「项目不存在」
13. 【server】修复 「节点&项目 → 同步节点项目」会把**节点分发项目**从项目列表里删掉（同步一次就消失，只能重新创建分发项目）：同步语义原本是清空该节点在当前工作空间的全部项目缓存再按节点实际项目重建，而分发项目是服务端虚拟项目、节点上并不存在，删了就同步不回来；现在只清理节点侧真实存在的项目缓存，分发项目保留
14. 【server】修复 脚本模板可重复添加同名脚本：重名会插入新 id，旧 id 变孤儿，DSL 项目里引用的启动/停止脚本 id 悄悄失效（表现为「启动脚本不生效」）；现在新增时按「名称 + 工作空间」查重，重名直接提示「标记已存在」
15. 【server】修复 构建「已取消」状态被覆盖成「失败」：取消要先杀进程/关闭节点会话再落库，被杀进程的退出码、节点回报的 `cancelled` 都会被构建线程写成「执行命令退出码非0」，把 `已取消` 盖成 `失败`（构建历史里的取消记录因此失真）。现在用一个进程内的「取消意图」标记，取消后到达的任何终态都归一为「已取消」，下一次构建开始时清除标记（本地构建与节点构建都覆盖）
16. 【server】修复 X.509 证书包只放一张自签名证书（最常见的用法）时恒报「公钥或者私钥不存在」，PKCS#8/PKCS#1 私钥、pem/crt 扩展名都一样失败：解析逻辑固定去取「另一张证书」的公钥来和私钥比对，单证书时取到 null；现在单证书按自签名处理（公钥取自己、跳过证书链校验），带证书链的包行为不变
17. 【agent】插件端控制台支持「指令不带 projectId」：连接时已确定的 projectId 会缓存，指令缺省时回退使用，避免第三方客户端/老版本前端出现「连接成功但每条指令都提示没有对应项目」（Go 插件端本就是这个行为，本次 Java 端对齐）
18. 【server】修复 **证书列表明文回显私钥口令**：`/certificate/list`、`/certificate/list-all` 把 `certPassword` 原样返回，任何拥有「证书列表」权限的账号（含只读用户）都能拿到 JKS/PKCS12 私钥口令。现在列表响应一律不回显口令，口令只保留在服务端（编辑证书也不需要前端回传口令）
19. 【server】修复 用户空间绑定查询恒为空：`/user/workspace_list` 只按用户 id 查绑定表，而绑定实际挂在权限组上；现在按「用户 id + 所属权限组」聚合，超级管理员直接返回全部工作空间
20. 【server】修复 项目保存接口静默忽略 `outGivingProject`：带该参数提交时返回「修改成功」但标记不会生效（分发标记只由节点分发流程维护）；现在明确回 405 并提示「分发项目请通过节点分发创建」
21. 【server】【agent】修复 容器构建的报错不可诊断：缺基础镜像时只回「当前还不支持：<runsOn>」，用户既不知道 `runsOn` 是**镜像后缀**（插件端拼 `jpomdocker/runs_<runsOn>`），也不知道该 `docker pull` 什么。现在提示里带上缺失镜像名、`runsOn` 语义与两种处置方式；同时在开发文档补了 `DockerYmlDsl` 字段表、最小样例与报错对照表
22. 【server】修复 SSH 交互终端失败信息不可诊断：建连失败一律只回「SSH console connection timed out」，真实原因（例如资产未分配/参数不对导致的「No corresponding SSH information」）只写在服务端日志；现在会把真实异常信息回显到终端
23. 【server】【agent】【agent-go】【web-vue】修复 构建按「保留个数默认 3」生成版本后，项目文件页「版本」仍提示「版本快照未开启」、看不到已有版本也无法切换/删除的问题：插件端版本列表与能力探测新增 hasVersions，只要磁盘上已有版本即视为可用；版本切换在客户端未指定保留个数时按默认 3 保留，避免插件端按「未启用」把历史版本清理到只剩当前版本；前端在有版本时直接渲染列表（配置为 0 时保留个数显示为 —）
24. 【server】修复 接口直传「节点构建 + 容器构建专属参数」（runsOn/dockerFile/fromTag/contextDir）被静默接受的问题：保存时直接拒绝并提示「节点构建与容器构建互斥」
25. 【web-vue】修复 构建列表/卡片对「节点构建」显示为「本地构建」（构建方式列与卡片只区分容器/非容器），现在三种构建方式分别显示

### ⚠️ 升级说明

1. **资产控制台 docker 操作要求系统管理权限**：`/system/assets/docker/*` 六个控制台接口（镜像/容器/卷/网络/集群服务/集群信息）现在与 docker 资产的管理接口一致，只允许系统管理员（系统管理权限）访问。普通工作空间用户仍可在自己的工作空间「docker」菜单里操作已分配给自己工作空间的 docker 资产，行为不变
2. **仓库密码不再回显**：docker 资产列表、集群列表、工作空间 docker 列表都不再返回 `registryPassword`；编辑资产时密码框为空即沿用已保存的密码，只有填写新密码才会覆盖。已发布的 2.11.12–2.11.14.2 受影响，升级到本版本即修复
3. **拉取镜像的仓库凭据**：工作空间「拉取镜像」只有在镜像地址与资产配置的「仓库地址」同域时才使用资产里保存的仓库账号密码；从其它仓库拉取时改为匿名拉取（公有镜像不受影响，私有镜像需要在资产里配置对应仓库地址）
4. **版本快照保留个数 0 的语义变更**：构建配置「版本保留个数」留空或填 0 以前表示「跟随插件端 `jpom.project.version-count`」（默认 0 = 关闭），现在统一按默认 3 个保留。只影响显式打开「版本快照」开关的构建配置，未开启的构建行为完全不变；已开启的构建升级后第一次发布会真正开始生成版本快照（每个项目最多 3 份完整目录快照，磁盘占用需留意，可通过保留个数或项目 DSL `file.versionCount` 调整）
5. **新增构建方式「节点构建」，默认不使用**：升级后既有构建配置仍为「本地构建」，行为与产物路径完全不变；机器资产的「可作为构建节点」默认关闭。要使用该功能需先把目标机器上的插件端升级到本版本（Java 插件端不支持承担构建），并在「资产管理 -> 机器管理」中打开开关，再把构建方式改为「节点构建」并选择节点。节点构建暂不支持容器构建（二者是不同的构建方式）与 SVN 仓库，保存时即会提示
6. **文件发布目标与发布目录校验收紧**：文件发布任务（含证书发布）指定的节点/SSH 必须属于当前工作空间，跨工作空间的目标会被剔除、全部无效时拒绝创建任务；发布二级目录必须位于所选的发布父目录内，`..` 等越界写法直接报错。同工作空间的正常发布、已保存的发布模板和「重新发布」（使用父任务落库的目录）不受影响。已发布的 2.11.12–2.11.14.2 受影响，升级到本版本即修复

------

## 2.11.14.2-beta (2026-11-19)

### 🐣 新增功能

1. 【agent】新增插件端主动连接服务端的反向连接隧道：服务端配置一个连接密钥即可，插件端只要能出站访问服务端，不再要求服务端能直连插件端端口，适合插件端在 NAT/内网、不对外开放端口的场景
2. 【server】新增反向隧道在线状态查询接口（`/tunnel/online`、`/tunnel/status`），可按机器节点判断是否通过隧道在线
3. 【server】未注册节点默认拒绝接入反向隧道；开启 `jpom.agent.tunnel.allow-auto-register` 后可按连接上报的节点标识自动创建机器节点资产
4. 【server】【agent】【agent-go】新增 插件端主动推送节点状态（反向隧道 `PUSH` 帧）：服务端首次收到推送后把该机器切换为推送模式（`transportMode=1`），不再按心跳周期轮询 `/get-stat-info`；推送数据超过 `jpom.agent.tunnel.push-max-age-seconds`（默认 90 秒）未更新按无法连接处理，连续 3 个心跳周期没有推送会自动回退拉取。服务端需先升级：握手时声明推送能力，未声明的老服务端下插件端保持拉取、不发送推送帧
5. 【server】【agent】【agent-go】新增 构建发布版本快照与快速回滚：构建配置可开启「保留版本快照」，每次发布成功后把完整版本保留在节点本地，回滚时只切换节点本地版本并重启项目，不再重新传输产物；构建历史新增「快速回滚」入口，原「重新发布」（重新传输产物）保留为兜底
6. 【web-vue】新增 项目文件页「版本」入口：可直接查看节点上的版本快照（首次基线/构建发布/切换前快照）、切换回任意版本并选择是否重启项目、删除非当前版本，无需再回到构建历史
7. 【server】【agent】【agent-go】新增 版本快照的资源开关：插件端 `jpom.project.version-count`（默认 0 关闭）与 `jpom.project.version-path` 控制是否开启及存放位置，项目 dsl 的 `file.versionCount`/`file.versionPath` 可按项目覆盖；未开启的项目发布行为与升级前完全一致

### 🐞 解决BUG、优化功能
1. 【web-vue】修复 新增构建弹窗底部「保存/保存并构建」点击后没有任何反应（无请求、无提示）的问题：改为显式方法调用，编辑器未就绪时给出可见提示
2. 【server】修复 反向代理改写 Host 时在线升级页 WebSocket 被误判为跨站、提示“您没有对应权限”且无法更新节点的问题；新增配置 `jpom.web.allowed-origins` 可指定对外域名放行
3. 【server】优化 WebSocket 来源校验失败的提示：明确告知需配置 `jpom.web.allowed-origins` 或让反向代理透传 `Host`/`X-Forwarded-Host`
4. 【web-vue】修复 在线升级页 WebSocket 断开后点击“更新/批量更新”没有任何反馈的问题，现在会提示检查 ws 代理
5. 【server】修复 节点/资产状态可能永久显示“正常”的问题：多服务判定改用活跃心跳、资产分组为空时改为监控全部分组；超过约 90 秒未刷新检测结果的节点改为显示“检测超时”
6. 【server】修复 SSH/FTP 文件管理的路径穿越问题：客户端构造的文件名不再能读取、覆盖或删除授权目录之外的文件
7. 【all】修复 未登录、权限拦截、构建记录不存在等接口提示中文显示为问号的问题：服务端与 Java 插件端现在统一按 UTF-8 输出响应（Go 插件端原本就是 UTF-8，不受影响）
8. 【agent】修正 反向隧道未启用时日志提示的配置键前缀（实际配置项为 `jpom.tunnel.server-url`/`key`/`machine-id`）
9. 【server】修复 docker 构建 DSL 的 `fromTag` SQL 注入（VL-001）：按标签查询 docker 节点改为参数绑定，并限制 `fromTag` 只能包含镜像名/标签常见字符；具备工作空间构建权限的用户不再能通过标签绕过工作空间条件读取业务库
10. 【server】修复 `/oauth2-{provide}` 未授权反射型 XSS（VL-002）：平台标识只接受已启用平台的字符白名单值，非法值回退为空；同时校验 `Jpom-ProxyPath` 请求头（二级代理路径），不再把请求头原样写入首页 HTML
11. 【server】修复 开启反向隧道后，在节点上编辑项目文件/脚本时内容里含 `&`（如 shell 的 `&&`、URL、HTML 属性）会被截断保存的问题：隧道转发的表单参数改为 URL 编码，不再把值拆成多个参数（候选验证发现，影响 2.11.14.2 自身引入的隧道；未开启隧道不受影响）
12. 【server】修复 编辑构建仓库时把其他工作空间仓库凭据发到调用者地址的问题：仓库编辑改为先按「当前工作空间 + GLOBAL」确认仓库归属、再用未脱敏的 scoped 查询回填密码/私钥，仓库 id 不属于当前空间时直接拒绝，不再用调用者填写的仓库地址执行 `git ls-remote`（2026-11-19 外部安全报告；此前非隔离查询会把其他工作空间仓库的 git 密码以 HTTP Basic 发到外部）

### ⚠️ 升级说明

1. **反向隧道默认关闭**：服务端需配置 `jpom.agent.tunnel.key`（并重启）才会启用 `/agent/tunnel` 端点；未配置时行为与升级前完全一致（服务端主动拉取插件端）。插件端需在 `conf/application.yml` 同时配置 `jpom.tunnel.server-url`（支持 `ws`/`wss`）、`key`、`machine-id`，三项齐全才启用；Java 与 Go 插件端都支持
2. **反向代理部署**：若升级后在线升级页仍提示“您没有对应权限”，请把对外访问域名配置到 `jpom.web.allowed-origins`（支持完整 origin 或主机名，逗号分隔）后重启服务端，或让反向代理透传 `Host`/`X-Forwarded-Host`
3. **节点状态新增“检测超时”**：超过约 90 秒（3 个心跳周期）未刷新检测结果的节点不再显示“正常”（橙色）；该状态为只读折算，不写库、无需配置；多服务部署中某台服务掉线后，其余服务会接管全部分组的资产监控
4. **SSH/FTP 文件管理路径校验收紧**：文件名、重命名目标与子目录统一校验，`..` 等越界写法会被拒绝并提示路径越界；正常浏览、创建、上传、下载、重命名与修改权限不受影响。2.11.14 只修复了新建文件/文件夹入口，其余入口需升级到本版本
5. **版本快照默认关闭**：插件端 `jpom.project.version-count` 默认 `0`、构建配置的「保留版本快照」默认关闭，升级后不产生任何额外磁盘占用。开启后每个保留版本会占用约等于发布产物大小的磁盘，按保留个数滚动清理（永远保留当前运行版本）；快速回滚仅对开启后发布过的构建生效，更早的构建仍使用「重新发布」。切换版本会把项目文件目录覆盖为该版本内容（`logs`、`temp` 目录保留），切换前会自动把当前目录快照为安全版本，失败时自动切回
6. **主动推送默认开启（先升级服务端、再升级插件端）**：反向隧道启用且插件端升级到本版本后，服务端会在首次收到推送时把该机器切换为推送模式并停止轮询；插件端可用 `jpom.tunnel.push-enabled: false` 关闭推送、用 `jpom.tunnel.push-interval`（默认 30 秒）调整上报间隔。**升级顺序：服务端在前**——服务端握手时会声明是否支持推送，未声明的老服务端下插件端保持拉取、不发送推送帧（避免老服务端把未知帧当非法帧断开隧道）。老版本插件端不推送，继续按原方式拉取，无需改动；**多服务端部署仍保持拉取模式**（各实例只持有自己那条隧道的推送数据，切换会误判离线）。推送中断连续 3 个心跳周期会自动回退拉取，不需要手工改库
7. **`fromTag` 与 `/oauth2-{provide}` 校验收紧**：Docker 构建配置的 `fromTag` 只允许字母、数字、`.`、`:`、`/`、`_`、`-`（原有合法镜像名/标签不受影响）；`/oauth2-{provide}` 的平台标识必须是当前已启用的 OAuth2 平台，否则首页不再注入该值（登录页改用其他方式解析回调，OAuth2 登录流程不变）；`Jpom-ProxyPath` 请求头只接受以 `/` 开头的路径安全字符，非法值回退为空（前端按根路径处理，与未配置二级路径的行为一致）。已发布的 2.11.12–2.11.14 受影响，升级到本版本即修复
8. **仓库编辑校验收紧**：`/build/repository/edit` 在仓库 id 不存在或不属于当前工作空间时直接提示「无效的仓库信息」，不再尝试连接请求里的仓库地址（此前会先把该 id 的密码/私钥回填后发起连接）。编辑当前工作空间的仓库与 GLOBAL 仓库行为不变，已保存的账号密码/私钥照常回填；修改密码时仍以请求里的新值为准

------


## 2.11.14.1-beta (2026-11-16)

### 🐞 解决BUG、优化功能

1. 【server】修复 2.11.13 起构建「获取分支」时仓库凭证被脱敏置空导致认证失败的问题：分支列表接口改为在工作空间校验后使用不脱敏的仓库查询，HTTP 账号密码与 SSH 私钥重新可用，构建可以正常选择分支（[Gitee issues IKVPL6](https://gitee.com/dromara/Jpom/issues/IKVPL6)）

### ⚠️ 升级说明

1. **影响 2.11.13 / 2.11.14**：这两个版本在构建配置「选择分支」时会提示认证失败（HTTP 为账号密码不正确、SSH 为 Auth fail for methods 'publickey'），无法获取分支列表。升级到 2.11.14.1 后恢复正常，已保存的仓库地址、账号密码与私钥无需重新填写。

------


## 2.11.13.3-beta

### 🐣 新增功能

1. 【mcp】新增 npm 包 jpom-mcp-server（MCP stdio Server，npm 独立发版，当前 0.1.1）：配置 Jpom 地址与长期 token 后，Claude Desktop/Cursor/VS Code/DeepSeek Harness 等 MCP 客户端可查询节点、项目、构建、脚本、资产、监控、分发、系统与审计日志，并支持日志读取/有界实时跟随；默认只读，写操作需显式开启。
2. 【server】发布包默认改为 tar.gz 格式（server/agent 各自 tar.gz + sha1），过渡窗口内同时提供 zip 供旧版本客户端在线升级；Go 插件端 5 平台安装包统一 tar.gz（Windows 也是 tar.gz，内含 `bin/agent.exe`），升级容器不变。

### 🐞 解决BUG、优化功能

1. 【server】修复 SSH 文件管理新建文件/文件夹（`new_file_folder.json`）的命令注入风险：创建动作改为 SFTP 调用（`ChannelSftp.put`/`mkdir`），不再把文件名/路径拼接进远程 shell 命令；文件名同时校验：不能包含 `/`、`\` 与 CR/LF/NUL，且不能为 `.`、`..`。
2. 【mcp】修复 jpom-mcp-server 0.1.0 的 CLI bin 缺少 shebang 导致 `npx jpom-mcp-server` 无法执行的问题（0.1.1 已发布，0.1.0 已在 npm 标记废弃，请升级到 0.1.1+）。

### ⚠️ 升级说明

1. **SSH 文件管理名称校验**：新建文件/文件夹名称不能包含 `/`、`\`、CR/LF/NUL，且不能是 `.` 或 `..`（含点号的普通名称如 `.hidden`、`...` 仍可创建）；包含 `;`、`|`、`&`、`$()` 等字符的名称会按普通文件名创建，不再被 shell 解释。正常的文件创建、上传、重命名不受影响。
2. **发布包格式变化**：server/agent 安装包默认 tar.gz；旧版本（≤ 2.11.13.1）在线升级依赖过渡窗口内的 zip（`legacy_zip` 关闭后仅产 tar.gz）。不再提供合并包 `jpom-<version>.tar.gz`，请分别下载 server/agent 安装包。
3. **Go 升级容器兼容别名下线**：不再生成 `agent-go-<version>-upgrade.jar`（2.11.13.2-beta 升级说明第 4 条所述 linux-amd64 手工上传别名失效），5 平台容器 `agent-go-<version>-upgrade-<os>-<arch>.jar` 已覆盖全部平台；旧版 Server 手工上传请直接选择与节点平台一致的容器。
4. **jpom-mcp-server 为 npm 独立发版**：不随 Jpom 安装包分发，通过 `npx -y jpom-mcp-server` 使用；0.1.0 用户请升级到 0.1.1+。

------


## 2.11.13.2-beta (2026-11-09)

### 🐣 新增功能

1. 【agent-go】新增 Go 语言插件端：单二进制、低内存、免 JDK，兼容 Java 插件端的配置、数据和协议，可在 Java/Go 之间平滑切换。
2. 【server】在线升级支持 Go 插件端：自动识别节点类型与运行平台并下发匹配的升级包，Java 节点升级方式不变。
3. 【agent-go】提供多平台安装包与在线升级包（Linux/macOS 的 amd64、arm64 与 Windows amd64），升级容器按节点平台精确匹配。
4. 【agent-go】支持国际化、Windows/macOS 系统监控、7z 解压、GBK 编码、cron 年份/分组，以及升级失败自动回滚。

### 🐞 解决BUG、优化功能

1. 【all】修复 项目/节点脚本控制台、系统日志等日志监听在频繁打开关闭时，WebSocket 会话与文件监听 Tailer 线程可能未被释放、持续占用内存的问题：会话集合改为并发安全并按会话 ID 去重，watcher 的 add/close/restart 原子化且关闭幂等，关闭后不再接收新会话；发送失败立即移除并关闭会话；切换日志文件时先离线上一个监听；服务端 WebSocket 装饰器缓存与传输层代理缓冲在关闭/发送失败时即时释放
2. 【agent】优化 控制台 WebSocket 会话增加空闲超时（依赖前端 5s 心跳），异常断开（未收到 close 帧）的会话由容器回收；脚本执行会话与执行中缓存增加异常路径兜底清理，避免长期运行后堆内存持续增长导致进程假死/OOM
3. 【server】修复 重装/迁移服务端后“同步节点项目（脚本）”只提示拉取到 N 个、实际更新 0 个的问题：插件端项目/脚本仍带已不存在的旧工作空间时，现在会回退到节点当前工作空间再同步；工作空间仍然存在但不同（共享机器多空间）与 GLOBAL 数据保持原逻辑（[Gitee issues ICH1VK](https://gitee.com/dromara/Jpom/issues/ICH1VK)）
4. 【server】优化 严格构建下 sh 脚本在环境模板之后、用户命令之前注入 `set -e`（bash 下同时 `set -o pipefail`），中间命令或管道失败立即中断构建，不再继续执行后续命令；非严格构建与 Windows 批处理语义保持不变（[Gitee issues ICW9KZ](https://gitee.com/dromara/Jpom/issues/ICW9KZ)）
5. 【server】修复 Gitea/Gogs 的 GET webhook 把 push JSON 作为 `payload` 查询串携带、超过 Tomcat 默认请求行/请求头上限（8KB）导致接口直接返回 400 的问题：放宽 `server.max-http-header-size` 到 64KB（[Gitee issues IDG4PT](https://gitee.com/dromara/Jpom/issues/IDG4PT)）
6. 【web-vue】优化 逻辑节点、项目列表、分发列表的自动刷新：默认关闭（开关仍按用户+表格持久化在浏览器本地），项目列表/分发列表刷新间隔由 5s 调整为 30s，浏览器标签页不可见时暂停自动刷新；项目列表按节点查询运行状态增加防重入与 15s 超时，避免节点离线时请求长期挂起并随刷新叠加，占满同源连接导致同页/同标签其它列表请求 20s 超时（[Gitee issues ICXOGI](https://gitee.com/dromara/Jpom/issues/ICXOGI)）
7. 【server】修复 重置超级管理员密码（`--rest:super_user_pwd`）后新密码仅写入 `logs/server.log`、Windows 无控制台导致用户误以为重置失败的问题：命令执行结果同时输出到标准输出（`logs/stdout.log`/控制台）；超级管理员查询统一为 `systemUser=1`（保留 `parent=sys` 回退兼容旧数据），避免历史数据下静默失败（[Gitee issues IJNJZJ](https://gitee.com/dromara/Jpom/issues/IJNZJZ)）
8. 【agent】优化 远程下载文件保存为 URL 原始文件名（此前保存为随机文件名），Java/Go 插件端行为一致。

### ⚠️ 升级说明

1. **严格构建语义变化**：开启【严格构建】后，sh 构建脚本会在环境模板之后、用户命令之前注入 set -e（bash 同时 set -o pipefail），中间命令或管道中任一命令失败会立即中断，不再继续执行后续命令（此前仅在脚本整体退出码非 0 时才判定失败）。依赖“某条命令失败后继续执行”的严格构建脚本，请在脚本开头显式 set +e 自行管理；非严格构建（推荐用于允许中间命令失败的场景）与 Windows 批处理不受影响。
2. **Gitea/Gogs GET webhook 请求头上限**：新安装包默认 server.max-http-header-size: 65536（64KB）。已安装实例的外部配置文件 conf/application.yml 不会随升级自动覆盖，如仍遇到 Gitea/Gogs GET webhook 400，请在该文件 server: 下增加 max-http-header-size: 65536（或在【系统管理 → 外部配置文件】中重置为默认配置后按需调整），再重启服务端。
3. **列表自动刷新默认值变化**：逻辑节点、项目列表、分发列表的自动刷新默认关闭（此前默认开启），仍可在页面右上角手动开启，并按“用户+表格”持久化在浏览器本地；项目列表/分发列表刷新间隔调整为 30 秒，浏览器标签页不可见时暂停自动刷新。此前已手动开启自动刷新的用户不受影响（本地配置优先）。
4. **Go 插件端（可选新组件）**：安装包与在线升级包按平台区分（linux/darwin 的 amd64、arm64，windows-amd64），升级容器必须与节点平台一致；旧版 Server 手工上传时 linux-amd64 可用 agent-go-<version>-upgrade.jar 兼容别名。Windows 请使用包内 AgentGo.bat，服务托管只保留一种方式（nssm/sc 或手工启动）。
5. **Go 插件端配置提示**：JPOM_SERVICE 与 Java 的 -D 启动参数对 Go 插件端无效；AES 密钥请用环境变量 JPOM_ENCRYPT_AES_KEY（与服务端一致），升级后拉起由 systemd/nssm 配置负责。
6. **Java 插件端远程下载文件名变化**：远程下载的文件现在按 URL 原始文件名保存（此前为随机文件名）；如同名文件已存在会使用随机名回退。
------


## 2.11.13.1-beta (2026-11-09)

### 🐣 新增功能


### 🐞 解决BUG、优化功能

1. 【server】修复 文件中心下载接口未认证 SQL 注入风险：Hutool 会把查询参数值解析为 SQL 条件表达式（如 `token=is not null` 绕过别名+token 校验），`sort` 参数可注入原生 ORDER BY，且数据库异常会把完整 SQL 回显给客户端；现在查询 Bean 值统一使用占位符绑定，`sort`/`filter_*` 增加字段白名单，token 增加格式校验，并修复别名下载可跨文件读取的越权问题
2. 【server】修复 通用分页查询的 `:in` 过滤字段（如 `workspaceId:in`）在字段白名单校验前未剥离后缀而被静默丢弃的问题，避免工作空间隔离条件失效导致项目列表、节点脚本列表、文件中心（含触发器 token）、证书、仓库、环境变量（明文值）等数据跨工作空间串显/泄露
3. 【server】修复 权限拦截器在处理器方法缺少 `@Feature` 时静默放行的问题：属于权限管理模块的控制器改为 fail-closed，并补齐证书导出等接口的方法级权限声明（证书导出需要 `DOWNLOAD` 权限），避免未授权用户直达敏感业务接口
4. 【server】修复 WebSocket 鉴权降级：握手改用登录会话（HttpOnly Cookie）认证，不再仅凭 URL 中的密码摘要 `userId` 通过校验；补充禁用账号校验、IP 白名单与 Origin 同源校验，避免绕过 HTTP 侧拦截器；同时修复 `system_log` 及项目日志 WebSocket 日志文件名路径穿越（含符号链接逃逸），服务端与插件端同步修复
5. 【server】加固 工作空间强制过滤与导出范围：客户端伪造 `workspaceId`/`workspaceId:in`/`%workspaceId%` 等同字段参数不再能覆盖服务端强制的工作空间条件；项目导出、仓库导出改为仅导出“当前工作空间 + GLOBAL”，仓库分组下拉与 SSH 终端执行日志列表按工作空间过滤；非时间字段的 `xxx[0]`/`xxx[1]` 过滤参数不再导致 500
6. 【web-vue】修复 触发器/构建说明文案中的 JSON 大括号被前端国际化组件误判为插值占位符、打开相关弹窗时控制台报 `SyntaxError: Invalid token in placeholder` 的问题（文案改为转义输出大括号，显示内容不变）
7. 【all】修复 数据库异常响应回显完整 SQL/列名/表名的信息暴露问题：数据库类异常统一返回通用错误文案，完整异常堆栈仅记录在服务端日志（正常业务/参数/权限异常提示不受影响）


### ⚠️ 升级说明

1. **权限收紧（需要管理员确认）**：以下接口现在要求对应工作空间的方法权限（此前仅有登录/节点数据权限即可访问），升级后如提示“没有对应管理权限”，请到【用户管理 → 权限组】为相关用户的工作空间勾选对应权限；超级管理员不受影响：
   - 证书管理 → 证书导出：需要 `DOWNLOAD`（下载）权限；
   - 节点项目文件备份：下载需要 `DOWNLOAD`，删除需要 `DEL`，创建/恢复备份需要 `EDIT`。
2. **列表数据范围恢复**：项目列表、节点脚本列表、文件中心、静态文件存储、证书、仓库、脚本列表/执行记录、环境变量等“全局/工作空间共享”列表恢复为仅展示**当前工作空间 + 全局（GLOBAL）**数据；此前因工作空间过滤条件失效会显示其他工作空间数据（本次为安全修复，非故障）。
3. **触发器 token 格式校验**：文件下载、构建/脚本/SSH/项目/环境变量等触发器 token 仅接受**字母数字 6~128 位**；标准 32 位十六进制（当前版本）和历史 10 位字母数字 token 兼容。若仍在使用更早期非标准格式 token 且升级后提示无效，请重置该触发器 token。
4. **文件中心下载接口参数语义**：
   - `sort` 仅接受文件记录的真实字段（如 `createTimeMillis:desc`），非法字段会被忽略；
   - `filter_*` 仅接受真实字段，非法字段/非法值会被忽略（不再返回 500）；
   - 按别名下载时按“别名 + token”精确匹配；同一别名对应多个文件时，以 token 实际绑定的记录为准。
5. **错误提示变化**：非法或无权限 token 会提前返回“token 错误或已失效”；按别名下载且过滤条件无匹配时，当前也会提示“token 错误或已失效”（已记录为后续体验优化项，不影响功能）。
6. **无数据库表结构/字段变更**，无需手工迁移；升级前仍建议按惯例备份 `data`/`conf`。
7. **WebSocket 鉴权与日志安全收紧**：
   - WebSocket 改用登录会话（HttpOnly Cookie）认证，不再支持在 URL 中传 `userId=密码摘要`；前端已同步适配，升级后建议刷新页面；**自定义 WebSocket 客户端**需先登录或调用任意接口获取会话 Cookie，并按与页面同源的 `Origin` 连接；非浏览器客户端也可改用 `Authorization`（JWT）或 `JPOM-USER-TOKEN`（长期 token）请求头认证。
   - WebSocket 现在同样受 IP 访问限制；**禁用账号无法建立新的 WebSocket 连接**；已经建立的连接不会因账号随后被禁用而立即断开（重连时会校验）。
   - 系统日志、插件端日志、项目日志的 WebSocket 日志文件名限制在日志/项目目录内，防止路径穿越与符号链接逃逸；若自定义日志文件不在默认日志/项目目录，可能无法通过 WebSocket 实时查看（安全收紧）。
   - 反向代理需保留 `Host`（或设置 `X-Forwarded-Host`）并允许 WebSocket Upgrade；否则同源校验可能拒绝连接（浏览器直连/常规 Nginx 反代无需额外配置）。
8. **导出与空间过滤范围变化（需要管理员确认）**：
   - 【节点&项目 → 项目列表】导出、【在线构建 → 仓库信息】导出：仅导出**当前工作空间 + GLOBAL**数据（此前不受当前空间限制）；需要其它空间数据时请切换工作空间后分别导出；
   - 仓库分组下拉、SSH 终端执行日志列表现在按当前工作空间过滤（系统管理员同样受当前空间约束）；
   - 节点数据权限校验在服务端缓存未同步/数据不存在时由“放行”改为“拒绝”（fail-closed）：刚创建或同步数据后立即操作如提示无对应权限，请稍后重试或刷新页面；
   - 非时间字段带 `[0]`/`[1]` 的过滤参数将被忽略（不再返回 500）。
9. **数据库异常提示变化**：数据库类异常统一返回通用错误提示，不再在接口响应中展示 SQL/列名/表名等细节；排查时请查看服务端日志（完整堆栈仍会记录）。正常业务/参数/权限异常提示不变。


------

## 2.11.12.3-beta (2026-11-07)

### 🐣 新增功能

1. 【server】新增 容器构建 java 插件支持 JDK 21、25（#114）
2. 【server】新增 仓库导入支持 GitLab 私人令牌方式，可直接使用 token 拉取项目（[Gitee issues I9AZSS](https://gitee.com/dromara/Jpom/issues/I9AZSS)）

### 🐞 解决BUG、优化功能

1. 【all】升级 fastjson2 及 fastjson1 兼容包到 2.0.66，修复 fastjson2 AutoType 哈希碰撞绕过导致的远程代码执行风险（影响 fastjson2 ≤2.0.62，QVD-2026-45876；openapi 批量接口在鉴权前会解析请求体）
2. 【all】修复 后端参数校验失败时直接返回 i18n key 未翻译的问题（例如节点注册接口 `workspaceId` 为空时会提示 `i18n.workspace_id_required.c967`），现在按请求语言返回提示文案
3. 【all】优化 补全后端 i18n 遗漏：构建差异检查、JWT 密钥、可信代理、数据库初始化/备份/导入、达梦列 SQL 长度、节点注册错误等 31 处提示与日志支持多语言（新增/补登记 19 个 key，同步 4 种语言），并修正登录验证码提示 key 未登记 words.json、后缀不符合规范的问题
4. 【all】修复 管理脚本通过 `sh xxx.sh` 调用时报语法错误的问题，脚本自动切换到 bash 执行（[Gitee issues IC20RL](https://gitee.com/dromara/Jpom/issues/IC20RL)）
5. 【server】修复 终端在空命令行按退格、方向键等按键时数组越界的问题（#109）
6. 【server】优化 容器构建 runsOn 镜像构建失败时输出明确错误并中断构建，避免后续流程只报 `404 No such image`（#103）
7. 【server】修复 构建产物目录配置为绝对路径时构建列表接口 500 的问题（[Gitee issues IA6VM8](https://gitee.com/dromara/Jpom/issues/IA6VM8)）
8. 【server】优化 构建记录仓库提交说明字段长度加宽至 1000，避免较长提交信息写入失败（[Gitee issues ID35HQ](https://gitee.com/dromara/Jpom/issues/ID35HQ)）
9. 【server】修复 构建配置别名缺少规则校验提示的问题（[Gitee issues IBI5AF](https://gitee.com/dromara/Jpom/issues/IBI5AF)）
10. 【server】修复 权限组工作空间穿梭窗勾选计数错误的问题（[Gitee issues IBHK0I](https://gitee.com/dromara/Jpom/issues/IBHK0I)）

------

## 2.11.12.2-beta (2026-11-02)

### 🐣 新增功能

1. 【server】新增 差异构建支持配置监听路径、排除路径（Ant 风格通配），仅当变更文件匹配规则时才触发构建（感谢@凉拌西瓜 [Gitee pr 295](https://gitee.com/dromara/Jpom/pulls/295) ）
2. 【server】新增 项目文件列表支持分页展示，可切换每页条数（感谢@Justubborn [Gitee pr 294](https://gitee.com/dromara/Jpom/pulls/294) ）

### 🐞 解决BUG、优化功能

1. 【server】修复 跨工作空间越权访问问题：Docker、SSH 命令、构建仓库、SSH/FTP 文件、监控、节点项目与节点分发等接口未校验资产所属工作空间（#115 #119 #120，CVE-2026-91993、CVE-2026-111605）
2. 【server】修复 用户身份校验（checkUser）将入参解析为查询条件，可能被利用匹配任意用户的越权风险（#121，CVE-2026-120201）
3. 【server】修复 系统初始化接口缺少用户名、密码参数校验，可能被绕过初始化的问题
4. 【server】修复 数据库查询条件注入风险：主键条件以及分页过滤、排序字段统一使用占位符绑定和字段名白名单校验
5. 【all】升级 Tomcat 9.0.123、JGit、BouncyCastle、commons-lang3 等依赖，修复已知安全漏洞（#118；#116 已评估为不受影响）
6. 【all】优化 下载中心域名由 `d.jpom.download` 切换为 `d2.jpom.download`
7. 【server】优化 节点监控异常通知内容不再包含完整异常堆栈，仅保留错误信息，减少正式环境无效日志（感谢@Robot [Gitee pr 293](https://gitee.com/dromara/Jpom/pulls/293) ）
8. 【server】修复 JWT 签名密钥未配置时回退到源码公开固定值的问题：现在首次启动自动生成随机密钥并持久化到 `<data>/.jwt-key`（权限 600），空白配置也视为未配置；建议显式配置至少 32 位随机 `jpom.user.token-jwt-key`
9. 【server】修复 客户端可通过伪造 `X-Forwarded-For` / `X-Real-IP` 绕过 IP 白名单和登录 IP 失败锁定的问题：新增可信代理配置 `jpom.web.trusted-proxies`（默认仅信任本机回环），统一 IP 解析并在存在 IPv4 白名单时拒绝非 IPv4 客户端
10. 【agent】修复 Agent 未配置授权密码时把自动生成的明文密码写入日志的问题：日志不再输出密码，`data/agent_authorize.json` 权限收紧为 600
11. 【server】修复 `/tools/net-ping`、`/tools/net-telnet`、`/tools/ip-list` 缺少功能权限、任意登录用户可做服务端网络探测和网卡信息读取的问题：现在要求系统管理员权限，并限制端口/超时、增加操作日志；普通用户仍可使用 Cron 工具
12. 【server】修复 登录验证码可被空串绕过、账号不存在与密码错误返回码不同导致账号枚举的问题：验证码校验前移并改为一次性、5 分钟有效，账号不存在与密码错误统一返回 501

### ⚠️ 升级说明

1. 未配置 `jpom.user.token-jwt-key` 的部署升级后会自动生成 `<data>/.jwt-key`，旧登录 token 将失效，用户需要重新登录；集群部署请为所有节点显式配置相同的 key。
2. 反向代理不在本机时，请将代理地址配置到 `jpom.web.trusted-proxies`，否则 IP 白名单和登录 IP 锁定将看到代理 IP（这是安全收紧，不是故障）。
3. `/tools/net-ping`、`/tools/net-telnet`、`/tools/ip-list` 现在仅系统管理员可访问，普通用户菜单会隐藏；验证码失败或过期后需要重新获取。

------

## 2.11.12.1-beta

### 🐣 新增功能

1. 【server】新增 数据库支持使用达梦数据库（感谢[@wxyShine](https://gitee.com/wxyShine) ）
2. 【server】新增 FTP 管理，文件查看和构建发布方式新增 FTP 支持

### 🐞 解决BUG、优化功能

1. 【server】优化 数据库表支持配置前缀 `jpom.db.table-prefix` （感谢@ccx2480）
2. 【server】修复 终端输入命令，按Backspace 会退出终端（感谢[@dgs](https://gitee.com/dgs0924) [Gitee issues ICA57K](https://gitee.com/dromara/Jpom/issues/ICA57K) ）
3. 【server】优化 脚本日志和 SSH 命令日志支持批量删除（感谢[@lin_yeqi](https://gitee.com/lin_yeqi) [Gitee issues IBIM6W](https://gitee.com/dromara/Jpom/issues/IBIM6W) ）
4. 【server】修复 表格自定义列在部分字段不生效情况
5. 【server】优化 构建发布方式中 Docker 镜像支持配置网络模式（感谢@酱总）

------

## 2.11.11.4-beta (2025-05-06)

### 🐣 新增功能

1. 【server】新增 远程安装时的授权码支持（感谢@奇奇）

### 🐞 解决BUG、优化功能

1. 【server】修复 SSH控制台输入Control + R后无法记录日志的问题
2. 【server】优化 容器构建镜像新增 [毫秒镜像](https://1ms.run/) 临时仓库
3. 【all】优化 修改 tomcat版本到9.0.99安全版本 （感谢@[龙猫](https://gitee.com/woyeshimao) [Gitee pr 292](https://gitee.com/dromara/Jpom/pulls/292)）

------

## 2.11.11.3-beta (2025-01-10)

### 🐞 解决BUG、优化功能

1. 【server】优化 构建页面保存按钮使用 loading （感谢[@hhh](https://gitee.com/who9264) [Gitee issues IBD8WF](https://gitee.com/dromara/Jpom/issues/IBD8WF) ）
2. 【server】优化 SSH 文件列表支持跟随目录排序
3. 【server】优化 用户操作日志记录表中用户 id 长度不够照成登录失败问题（感谢[@Halo](https://gitee.com/halo-lmf) [Gitee issues IBFNKE](https://gitee.com/dromara/Jpom/issues/IBFNKE) ）
4. 【agent】优化 DSL 项目备份支持配置开启或者关闭差异备份（默认使用差异备份：仅备份变动过的文件）（感谢[@ercats](https://gitee.com/ercats) [Gitee issues IBF73J](https://gitee.com/dromara/Jpom/issues/IBF73J) ）
5. 【server】优化 构建 webhook 新增 git 的提交id和提交信息参数（感谢[@皮儿卡啦没有丘](https://gitee.com/Aimeia) [Gitee issues IBDEU0](https://gitee.com/dromara/Jpom/issues/IBDEU0) ）
6. 【server】优化 文件中心发布文件支持保存为模板方便后续使用（感谢[@lin_yeqi](https://gitee.com/lin_yeqi) [Gitee issues IB2FRF](https://gitee.com/dromara/Jpom/issues/IB2FRF) ）

------

## 2.11.11.2-beta (2024-12-26)

### 🐣 新增功能

1. 【all】升级 hutool 版本
2. 【server】新增 服务端在线升级页面支持配置下载授权码，方便国内用户使用全速 CDN

### 🐞 解决BUG、优化功能

1. 【server】修复 ssh文件管理显示错位（感谢[@lin_yeqi](https://gitee.com/lin_yeqi) [Gitee issues IB98AU](https://gitee.com/dromara/Jpom/issues/IB98AU) ）

------

## 2.11.11.1-beta (2024-12-03)

### 🐣 新增功能

1. 【server】新增 支持使用 TOPIAM 企业数字身份管控平台登录（oauth2）

------

## 2.11.10.2-beta (2024-12-02)

### 🐞 解决BUG、优化功能

1. 【server】修复 容器构建自定义插件，环境变量不匹配问题（感谢[@杨晓龙](https://gitee.com/pubresyang) [Gitee pr 285](https://gitee.com/dromara/Jpom/pulls/285) ）
2. 【agent】优化 项目日志文件编码格式支持独立配置（感谢[@不渡](https://gitee.com/zhangjxing) [Gitee issues IB3DCP](https://gitee.com/dromara/Jpom/issues/IB3DCP) ）
3. 【all】优化 使用默认语言时增加判断所在时区来识别中文环境
4. 【all】升级 fastjson 依赖版本

------

## 2.11.10.1-beta (2024-11-21)

### 🐞 解决BUG、优化功能

1. 【server】升级 docker-java 版本到 3.4.0
2. 【server】优化 数据库备份支持外部调用触发器来实现自定义备份周期（感谢@刘志远）
3. 【server】优化 SSH、项目 文件管理树加载 loading 避免网络卡顿影响树渲染错误（感谢@Bo）
4. 【server】优化 SSH、项目 文件管理树支持横向滚动条
5. 【server】优化 构建记录存储最后 commitId commitMsg 信息（感谢@program）
6. 【server】优化 引用脚本库时出现 ${xxxx} 出现引用失败：No group with name {xxxx} （感谢[@foot_print](https://gitee.com/zhangyf35) [Gitee issues IAW413](https://gitee.com/dromara/Jpom/issues/IAW413) ）
7. 【server】优化 构建环境变量自动识别系统变量密文相关关键词：pwd、pass、password、token 为隐私变量（感谢[@lin_yeqi](https://gitee.com/lin_yeqi) [Gitee issues IB34L9](https://gitee.com/dromara/Jpom/issues/IB34L9) ）
8. 【agent】优化 项目状态触发器 result 参数返回 json 类型，DSL 项目重启成功返回 pid（感谢@新）
9. 【server】优化 日志查看器中行号增加无法选中样式（感谢[@ning](https://gitee.com/mrning001) [Gitee pr 284](https://gitee.com/dromara/Jpom/pulls/284) ）

------

## 2.11.9.3-beta (2024-09-02)

### 🐞 解决BUG、优化功能

1. 【server】优化 构建 webhook 携带工作空间和集群id参数（workspaceId、clusterInfoId、workspaceName）（感谢@🦁子）
2. 【server】修复 构建命令引用脚本时相关脚本引用脚本库未生效问题（感谢@木迷榖）
3. 【server】优化 构建命令、本地发布命令支持直接引起脚本库（`G@("xx")` xx 为脚本标记）
4. 【server】修复 构建时日志记录器异常关闭未提示并且构建状态未修改（感谢@陈晨🍊）
5. 【server】优化 构建日志弹窗支持查看可用环境变量
6. 【server】优化 构建支持查看可用环境变量（感谢[@慌i](https://gitee.com/L_FaN) [Gitee issues IALAUW](https://gitee.com/dromara/Jpom/issues/IALAUW) ）
7. 【server】升级 mysql 驱动版本到 8.2.0
8. 【server】优化 ssh docker 独立配置是否需要 sudo 执行
9. 【server】修复 ssh docker 选型固定只有最新 10 条调整为搜索下拉（@山上雪）

------

## 2.11.9.2-beta (2024-08-22)

### 🐞 解决BUG、优化功能

1. 【server】修复 git仓库账号/密码包含特殊符号导致系统Git工具无法成功认证的问题（感谢@🇩）
2. 【server】修复 SSH 分片上传极大概率上传失败问题（感谢@勤思·、@张飞鸿）
3. 【all】升级 hutool、logback、tomcat 版本
4. 【server】优化 时长格式化工具支持 i18n 国际化
5. 【server】修复 终端日志处理字节'7'导致的日志补全记录不全 （感谢@[adao](https://github.com/YazaiHu) [Github issues 91](https://github.com/dromara/Jpom/issues/91) ）
6. 【server】升级 spring 包到 5.3.39 修复漏洞 CVE-2024-38808，CVE-2024-38809（感谢@[龙猫](https://gitee.com/woyeshimao)）[Gitee Pr 281](https://gitee.com/dromara/Jpom/pulls/281)

------

## 2.11.9.1-beta (2024-08-08)

### 🐣 新增功能

1. 【server】新增 项目监控新增沉默时间配置，避免报警后不再触发报警

### 🐞 解决BUG、优化功能

1. 【all】修复 多会话使用文件跟踪器可能出现多个线程未关闭问题（感谢[@沐剑屏](https://gitee.com/MuJianPing) [Gitee issues IAI0I1](https://gitee.com/dromara/Jpom/issues/IAI0I1) ）
2. 【all】修复 windows 管理脚本在 JDK 1.8 无法正常启动（感谢@流浪的墨染青衫、@LL）
3. 【server】修复 多个工作空间分组后子工作空间层级异常造成不能正常使用
4. 【server】修复 监控日志标题字段长度太短在英语模式下字段异常（感谢@Deer）
5. 【server】优化 监控新增使用语言来实现通知消息国际化

------

## 2.11.8.5-beta (2024-08-02)

### 🐞 解决BUG、优化功能

1. 【server】优化 更多组件的页面层级问题，避免层级错乱无法正常使用页面相关功能
2. 【server】修复 容器构建 DSL 配置示例错乱

------

## 2.11.8.4-beta (2024-07-25)

### 🐞 解决BUG、优化功能

1. 【server】优化 登录验证码去除易混淆字母（oO0、lL1、q9Q、pP）
2. 【server】修复 批量构建引起环境变量丢失问题（感谢@[xieliangza](https://github.com/xieliangza) [Github issues 88](https://github.com/dromara/Jpom/issues/88) ）
3. 【all】优化 解析 HTTP `Accept-Language` 请求头支持多语言最高优先级
4. 【server】修复 页面未刷新情况下打开弹窗次数过多不能提示窗口层级太低（感谢[@lin_yeqi](https://gitee.com/lin_yeqi) [Gitee issues IAEBUZ](https://gitee.com/dromara/Jpom/issues/IAEBUZ) ）
5. 【server】优化 分发日志显示关联数据信息（感谢[@pumpkinor](https://gitee.com/pumpkinor) [Gitee issues IAF7IV](https://gitee.com/dromara/Jpom/issues/IAF7IV) ）
6. 【server】优化 分发文件使用文件中心或者静态文件上传至节点使用实际文件名（感谢[@pumpkinor](https://gitee.com/pumpkinor) [Gitee issues IAF7GD](https://gitee.com/dromara/Jpom/issues/IAF7GD) ）
7. 【server】优化 SSH 文件管理支持浏览器分片上传（感谢[@超人那个超i](https://gitee.com/chao_a) [Gitee issues IAD9W4](https://gitee.com/dromara/Jpom/issues/IAD9W4) ）
8. 【all】优化 管理脚本判断 JDK9+ 自动添加解除限制反射参数：--add-opens=xxxx

------

## 2.11.8.3-beta (2024-07-05)

### 🐞 解决BUG、优化功能

1. 【server】修复 淘汰 javax.security.cert.X509Certificate（感谢[@Jie](https://github.com/index-jie) [Github issues 84](https://github.com/dromara/Jpom/issues/86) ）
2. 【all】优化 启动控制台日志完善 i18n
3. 【server】修复 证书删除后弹窗未正常关闭

------

## 2.11.8.2-beta (2024-07-04)

### 🐞 解决BUG、优化功能

1. 【server】修复 非管理员用户无法使用脚本库数据（感谢[@丁炳坤](https://gitee.com/dbk12138) [Gitee issues IA8ZKP](https://gitee.com/dromara/Jpom/issues/IA8ZKP) ）
2. 【server】修复 构建编辑页面未输入构建命令提示错误（感谢[@yt](https://gitee.com/a1127583020) [Gitee issues IAAI8D](https://gitee.com/dromara/Jpom/issues/IAAI8D) ）
3. 【server】优化 构建容器镜像支持推送到远程仓库后自动删除本地镜像（感谢 [@爱琳琳真是太好了](https://gitee.com/qiqi513_admin) [Gitee issues IA6UBO](https://gitee.com/dromara/Jpom/issues/IA6UBO) ）

------

## 2.11.8.1-beta (2024-07-03)

### 🐞 解决BUG、优化功能

1. 【server】修复 静态文件扫描到空时更新数据库异常（感谢[@blackcat](https://gitee.com/kylin_lawliet) [Gitee issues IA6UO0](https://gitee.com/dromara/Jpom/issues/IA6UO0) ）
2. 【server】修复 SSH命令行操作日志支持更全展示（感谢[@beens](https://gitee.com/beens) [Gitee issues IA6P3J](https://gitee.com/dromara/Jpom/issues/IA6P3J) ）
3. 【all】优化 升级 hutool、oshi、mwiede-jsch、fastjson2 版本

------

## 2.11.7.1-beta (2024-06-20)

### 🐞 解决BUG、优化功能

1. 【server】修复 操作确认交互弹窗层级过低无法正常显示使用（感谢@ccx2480）

------

## 2.11.6.6-beta (2024-06-19)

### 🐣 新增功能

1. 【all】新增 国际化语言支持：繁體中文（中國香港）、繁體中文（中國臺灣）

### ⚠️ 注意

- 前端国际化翻译程度：98%
- 后端已翻译语言可以度：95%（部分异步执行日志等目前未支持）

------

## 2.11.6.5-beta (2024-06-17)

### 🐣 新增功能

1. 【server】新增 服务端脚本、SSH 脚本支持引用全局脚本库（`G@("xx")` xx 为脚本标记）
2. 【agent】新增 节点脚本支持引用全局脚本库（`G@("xx")` xx 为脚本标记）

### 🐞 解决BUG、优化功能

1. 【agent】修复 不同工作空间下同一个机器节点相同的项目ID的项目数据被覆盖（感谢@小朱）
2. 【agent】优化 DSL 项目支持引用脚本库中的脚本（G@xxxx）xxxx 为脚本标记
3. 【all】优化 新增系统语言配置 `jpom.system.lang`
4. 【server】优化 前端紧凑模式在浅色模式下也生效

### ⚠️ 注意

- 前端国际化翻译程度：90%
- 后端已翻译语言可以度：80%（部分异步执行日志等目前未支持）

后端日志国际化需要新增或者修改 `jpom.system.lang` 配置项

------

## 2.11.6.4-beta (2024-06-14)

### 🐣 新增功能

1. 【all】新增 支持 i18n 语言国际化（zh-CN、en-US）

### ⚠️ 注意

#### 翻译进度

- 前端国际化翻译程度：80%
- 后端国际化翻译程度：90%

#### 使用程度

- 前端已翻译语言可以度：100%
- 后端已翻译语言可以度：60%（后端日志、异步执行日志等目前未支持）

### 🤝致谢

感谢 [@a20070322](https://gitee.com/a20070322) / Controllers 大佬贡献 Jpom 前端 i18n 工具。

------

## 2.11.6.3-beta (2024-06-14)

### 🐣 新增功能

1. 【agent】新增 全局脚本库（DSL 项目可引用）

### 🐞 解决BUG、优化功能

1. 【server】修复 gogs 仓库令牌导入异常（感谢@张飞鸿）
2. 【server】修复 自定义仓库令牌导入后页面异常（感谢@张飞鸿）

------

## 2.11.6.2-beta (2024-06-06)

### 🐞 解决BUG、优化功能

1. 【server】修复 没有端口的容器重建页面异常（感谢@冰淇淋还是冰激凌）
2. 【server】修复 工作空间菜单配置错误（感谢@Again... .）
3. 【server】优化 ssh 管理独立 tab 页面使用默认的字符串排序
4. 【server】修复 服务端脚本无法执行、参数值描述不对应（感谢@冰淇淋还是冰激凌）

------

## 2.11.6.1-beta (2024-06-04)

### 🐣 新增功能

1. 【agent】新增 项目文件支持快捷复制到当前文件夹
2. 【agent】新增 项目文件夹支持快捷压缩（感谢[@yiziyu](https://gitee.com/yiziyu) [Gitee issues I9737L](https://gitee.com/dromara/Jpom/issues/I9737L) ）

### 🐞 解决BUG、优化功能

1. 【server】优化 部分参数、环境变量配置交互优化取消文本输入框采用标签模式（感谢@湘江夜色）
2. 【server】修复 部分页面中文描述未正常显示
3. 【server】优化 文件发布支持选择脚本模板（感谢[@linCodeTest](https://gitee.com/linWorld) [Gitee issues I9P0EU](https://gitee.com/dromara/Jpom/issues/I9P0EU) ）
4. 【server】优化 升级 postgresql 版本（感谢@ʟᴊx💎💎）
5. 【server】修复 机器相关授权配置文件后缀输入框未正常显示（@感谢@ccx2480）

------

## 2.11.5.2-beta (2024-05-30)

### 🐞 解决BUG、优化功能

1. 【server】优化 自动续签采用无感模式（感谢@湘江夜色）
2. 【server】优化 容器构建执行配置自定义 host 参数（感谢@冰淇淋还是冰激凌）
3. 【all】升级 tomcat、yaml 版本（感谢@佳驰）
4. 【all】升级 bcprov-jdk18on 版本

------

## 2.11.5.1-beta (2024-04-30)

### 🐣 新增功能

1. 【agent】新增 项目支持配置禁止扫描目录避免大目录页面超时（感谢@我）

------

## 2.11.5.0-beta (2024-04-29)

### 🐣 新增功能

1. 【all】新增 自由脚本方便调试机器节点

### 🐞 解决BUG、优化功能

1. 【server】修复 资产管理 SSH 配置禁用命令无法回显（感谢@zhangw）
2. 【server】修复 资产管理 SSH 未配置授权目录时 NPE （感谢[@Anley](https://gitee.com/MrAnley) [Gitee issues I9J17G](https://gitee.com/dromara/Jpom/issues/I9J17G) ）
3. 【agent】优化 监控机器网络流程支持配置排除网卡或者仅统计对应的网卡
4. 【server】修复 退出登录时页面会提示需要登录相关信息
5. 【server】优化 页面检测新版本判断是否加入 beta
6. 【agent】优化 添加数据记录修改人（感谢[@陈旭](https://gitee.com/chenxu8989) [Gitee issues I9JSY7](https://gitee.com/dromara/Jpom/issues/I9JSY7) ）
7. 【server】优化 插件端注册到服务端，网络测试支持 ping + telnet （感谢@泊凉青川）

------

## 2.11.4.2-beta (2024-04-22)

### 🐣 新增功能

1. 【server】新增 发布系统公告

### 🐞 解决BUG、优化功能

1. 【server】升级 前端组件版本
2. 【all】优化 管理脚本删除 `-XX:-UseBiasedLocking` 使其能在高版本 jdk 运行
3. 【server】修复 构建列表卡片模式按钮文字错乱
4. 【server】修复 项目列表和逻辑节点卡片视图冲突
5. 【server】修复 docker管理新增docker选择证书界面权重异常 （感谢[@伤感的风铃草](https://gitee.com/bwy-flc) [Gitee issues I9GYVA](https://gitee.com/dromara/Jpom/issues/I9GYVA) ）
6. 【server】修复 系统管理中用户管理中登录日志无法筛选
7. 【server】优化 用户登录记录操作日志（保证操作监控能记录）
8. 【server】修复 系统管理中用户登录日志无法分页
9. 【server】优化 Oauth2 支持配置创建账号配置权限组
10. 【server】修复 文件发布权限为执行权限、文件发布记录删除无记录日志 （感谢@蓝枫）

------

## 2.11.4.1-beta (2024-04-12)

### 🐞 解决BUG、优化功能

1. 【server】优化 构建相关页面无法正常打开
2. 【server】优化 Git 仓库地址不正确相关提示更准确（感谢@易自玉）

------

## 2.11.4.0-beta (2024-04-12)

### 🐣 新增功能

1. 【server】新增 Oauth2 新增【飞书账号】、【自建 Gitlab】登录（感谢[@鸡皮蒜毛与鸡毛蒜皮](https://gitee.com/cuia) [Gitee issues I9ELGS](https://gitee.com/dromara/Jpom/issues/I9ELGS) ）
2. 【server】新增 Oauth2 新增企业微信登录

### 🐞 解决BUG、优化功能

1. 【server】优化 oauth2 第三方平台登录解析用户名将依次尝试：平台用户名、邮箱、uuid
2. 【server】修复 无法查询到分组信息（页面下拉框）（感谢[@Robot](https://gitee.com/robot1937) [Gitee issues I9FN9U](https://gitee.com/dromara/Jpom/issues/I9FN9U) ）
3. 【all】升级 hutool 版本
4. 【server】修复 修复孤独数据描述错别字（感谢[@cuiyongsheng](https://github.com/Cuiys1458) [Github issues 77](https://github.com/dromara/Jpom/issues/77) ）
5. 【server】修复 前端地址栏输入二级路径 404 页面卡死问题

------

## 2.11.3.2-beta (2024-04-07)

### 🐣 新增功能

1. 【server】新增 Oauth2 新增钉钉扫码登录

### 🐞 解决BUG、优化功能

1. 【agent】优化 DSL 项目支持配置在特定目录执行脚本（run.execPath）
2. 【agent】优化 管理脚本 -Xss 默认值修改为 512k（感谢@Again... ）
3. 【server】优化 管理脚本 -Xss 默认值修改为 1024k（感谢@Again... ）
4. 【server】优化 声明使用开源软件列表、增加本软件开源协议声明

------

## 2.11.3.1-beta (2024-04-02)

### 🐣 新增功能

1. 【server】新增 数据库支持 *mariadb*

### 🐞 解决BUG、优化功能

1. 【server】修复 无法查询到分组信息（页面下拉框）（感谢@猫猫向钱跑）
2. 【server】修复 【项目文件管理远程下载】、【镜像创建容器】确认按钮无法使用（感谢@猫猫向钱跑）
3. 【server】修改 资产管理机器管理删除按钮无法正常使用（感谢@🇩）
4. 【server】修复 SSH 面板文件管理无法正常切换（感谢@勤思·）
5. 【server】优化 部分页面在火狐浏览器无法正常打开（感谢[@sparkarvin](https://gitee.com/arvinlovegood_admin) [Gitee issues I96IOA](https://gitee.com/dromara/Jpom/issues/I96IOA) ）
   （感谢[@a20070322](https://gitee.com/a20070322) [Gitee Pr 221](https://gitee.com/dromara/Jpom/pulls/221)  ）

------

## 2.11.3.0-beta (2024-03-21)

### 🐣 新增功能

1. 【server】新增 数据库支持 *postgresql* （感谢[@王先生](https://gitee.com/whz_gmg1)）[Gitee Pr 223](https://gitee.com/dromara/Jpom/pulls/223)

### 🐞 解决BUG、优化功能

1. 【all】优化 新增 `jpom.system.command-use-sudo` 配置属性控制是否使用 sudo 执行部分系统命令
2. 【server】优化 前端页面 keep-alive 可能导致的内存泄漏问题（感谢[@a20070322](https://gitee.com/a20070322) [Gitee issues I9510M](https://gitee.com/dromara/Jpom/issues/I9510M) ）
3. 【server】修复 部分弹窗不生效问题（感谢[@a20070322](https://gitee.com/a20070322)  [Gitee Pr 215](https://gitee.com/dromara/Jpom/pulls/215) ）
4. 【server】优化 前端 ES lint 配置规范前端代码（感谢[@a20070322](https://gitee.com/a20070322) [Gitee Pr 214](https://gitee.com/dromara/Jpom/pulls/214) / [Gitee Pr 215](https://gitee.com/dromara/Jpom/pulls/215) / [Gitee Pr 217](https://gitee.com/dromara/Jpom/pulls/217) ）
5. 【server】修复 docker 控制台网络选项卡页面空白（感谢@破冰）
6. 【server】修复 节点历史监控统计图表时间查询不生效（感谢@九問）
7. 【server】优化 SSH 脚本触发器支持传入参数当环境变量（感谢@小朱）
8. 【server】修复 h2迁移其它数据库时部分数据丢失（感谢[@王先生](https://gitee.com/whz_gmg1)）[Gitee issues I9977K](https://gitee.com/dromara/Jpom/issues/I9977K)
9. 【server】优化 逐步引入新版表格（构建、项目、节点、资产机器）（感谢[@a20070322](https://gitee.com/a20070322) [Gitee Pr 218](https://gitee.com/dromara/Jpom/pulls/218) / [Gitee Pr 220](https://gitee.com/dromara/Jpom/pulls/220) / [Gitee Pr 222](https://gitee.com/dromara/Jpom/pulls/222) ）
10. 【server】优化 工作空间概括构建日志支持快速查看详情（感谢@Roger.cao）

------

## 2.11.2.3 (2024-02-29)

### 🐞 解决BUG、优化功能

1. 【server】优化 系统文件占用空间统计周期调整为每天2次（感谢[@singlethread](https://gitee.com/zengwei_joni) [Gitee issues I9302U](https://gitee.com/dromara/Jpom/issues/I9302U) ）
2. 【server】优化 支持配置前端所有参数编码来规避部分安全规则检查（感谢[@zhaozxc2010](https://gitee.com/zhaozxc2010) [Gitee issues I8Z1VJ](https://gitee.com/dromara/Jpom/issues/I8Z1VJ) ）
3. 【server】优化 上传文件空文件提示文件路径（感谢[@SchuckBate](https://gitee.com/skBate) [Gitee issues I93FI6](https://gitee.com/dromara/Jpom/issues/I93FI6) ）
4. 【server】优化 监听日志文件消息发送失败后自动移除会话（感谢[@singlethread](https://gitee.com/zengwei_joni) [Gitee issues I93ZFX](https://gitee.com/dromara/Jpom/issues/I93ZFX) ）
5. 【server】优化 容器构建产物为文件时保存路径层级错误（感谢[@vfhky](https://github.com/vfhky) [Github Pr 71](https://github.com/dromara/Jpom/pull/71) ）

------

## 2.11.2.2 (2024-02-22)

### 🐞 解决BUG、优化功能

1. 【server】优化 升级 docker-java、jgit 版本
2. 【all】优化 升级 commons-compress 版本
3. 【agent】优化 升级 oshi 版本
4. 【server】优化 新增配置节点 websocket 通讯消息大小限制（jpom.node.web-socket-message-size-limit）（感谢@长弘）

------

## 2.11.2.1 (2024-02-19)

### 🐞 解决BUG、优化功能

1. 【server】优化 构建代码未变动流程打断触发器未传入原因（statusMsg）（感谢@烛孩）
2. 【server】修复 项目控制台日志删除弹窗未能正常关闭（感谢@%）
3. 【server】修复 脚本日志时间筛选不生效（感谢[@zhaozxc2010](https://gitee.com/zhaozxc2010) [Gitee issues I8ZNKL](https://gitee.com/dromara/Jpom/issues/I8ZNKL) ）
4. 【server】优化 页面左侧菜单固定悬浮不跟随屏幕滚动条滚动（感谢[@a20070322](https://gitee.com/a20070322) [Gitee issues I8ZOOB](https://gitee.com/dromara/Jpom/issues/I8ZOOB) / [Gitee Pr 201](https://gitee.com/dromara/Jpom/pulls/201) ）
5. 【server】优化 新增机器节点提示未选择协议（感谢[@a20070322](https://gitee.com/a20070322) [Gitee issues I8ZDZT](https://gitee.com/dromara/Jpom/issues/I8ZDZT) / [Gitee Pr 202](https://gitee.com/dromara/Jpom/pulls/202) ）
6. 【server】修复 SSH 资产硬盘信息显示错误（感谢[@a20070322](https://gitee.com/a20070322) [Gitee issues I8ZY7K](https://gitee.com/dromara/Jpom/issues/I8ZY7K) ）
7. 【server】优化 表格搜索区域小屏幕适配 （感谢[@a20070322](https://gitee.com/a20070322) [Gitee issues I8ZY0B](https://gitee.com/dromara/Jpom/issues/I8ZY0B) ）
8. 【server】优化 SSH 文件管理树操作优化 （感谢[@a20070322](https://gitee.com/a20070322) [Gitee issues I9054L](https://gitee.com/dromara/Jpom/issues/I9054L) / [Gitee issues I5DMKG](https://gitee.com/dromara/Jpom/issues/I5DMKG) ）
9. 【server】优化 整体页面顶部菜单吸顶效果（感谢[@a20070322](https://gitee.com/a20070322) [Gitee issues I907Y8](https://gitee.com/dromara/Jpom/issues/I907Y8) ）
10. 【server】优化 资产监控线程池独立管理（感谢[@singlethread](https://gitee.com/zengwei_joni) [Gitee issues I918AB](https://gitee.com/dromara/Jpom/issues/I918AB) ）
11. 【server】优化 构建回滚使用构建独立线程池
12. 【all】优化 升级 hutool 版本（主要解决版本号排序异常）（感谢 [@Tom Xin](https://gitee.com/meiMingle) [Gitee issues I8Z3TI](https://gitee.com/dromara/Jpom/issues/I8Z3TI) / [Hutool issues I8Z3VE](https://gitee.com/dromara/hutool/issues/I8Z3VE)）
13. 【all】优化 升级 fastjson 版本
14. 【server】优化 页面整体滚动条兼容高版本浏览器（感谢@Controllers）

### 🤝致谢

感谢 [@a20070322](https://gitee.com/a20070322) / Controllers 大佬帮助优化 Jpom 前端部分老大难问题。

------

## 2.11.2.0 (2024-01-22)

### 🐞 解决BUG、优化功能

1. 【agent】修复 修改项目日志路径如果文件夹不存在报错（感谢@长弘）
2. 【server】修复 节点机器日志无法下载（感谢@Again...）
3. 【all】升级 hutool 版本
4. 【agent】升级 oshi 版本
5. 【server】升级 mwiede、apache-sshd 版本（感谢@*斌）
6. 【server】优化 项目列表 file 类型正常排序（不再排序到最后）（感谢[@pal865](https://gitee.com/pal865) [Gitee issues I8XU32](https://gitee.com/dromara/Jpom/issues/I8XU32) ）
7. 【all】修复 windows 环境保存配置并重启失败（感谢[@Robot](https://gitee.com/robot1937) [Gitee issues I8Y01T](https://gitee.com/dromara/Jpom/issues/I8Y01T) ）
8. 【server】修复 新版本页面部分分页切换失效（构建详情、资产机器、逻辑节点）（感谢@zac）

------

## 2.11.1.5 (2024-01-18)

### 🐞 解决BUG、优化功能

1. 【agent】修复 插件端非默认工作空间项目重启后变为孤独数据（感谢@ccx2480）
2. 【server】修复 新增节点分发项目数据为孤独数据

------

## 2.11.1.3/4-beta (2024-01-17)

### 🐞 解决BUG、优化功能

1. 【server】优化 修复页面打包页面底部白屏

------

## 2.11.1.2-beta (2024-01-17)

### 🐣 新增功能

1. 【server】新增 触发器调用次数统计、触发器统一管理
2. 【server】新增 本地构建命令执行支持配置多线程方式（多线程接收输出流，避免极端情况卡死）

### 🐞 解决BUG、优化功能

1. 【all】优化 机器状态新增：资源监控异常（资源监控异常不影响功能使用）
2. 【server】优化 取消登录页动态背景图
3. 【server】修复 节点分发文件中心、静态文件后文件自动被删除（感谢@九問）
4. 【server】优化 容器构建支持配置容器资源（HostConfig）（感谢@珂儿）

------

## 2.11.1.1-beta (2024-01-16)

### 🐞 解决BUG、优化功能

1. 【server】修复 静态文件名太短（100个字符）（感谢@*斌）
2. 【server】修复 还原数据库弹窗内容提示为空（感谢@伤感的风铃草🌿）
3. 【server】优化 echarts 支持跟随深色模式
4. 【server】修复 编辑节点分发服务端脚本弹窗被挡住（感谢@🇩）
5. 【server】优化 前端打包（缩减首屏加载时间）（感谢@曾梦想仗剑走天涯）

------

## 2.11.1.0-beta (2024-01-15)

### 🐞 解决BUG、优化功能

1. 【server】修复 docker TLS 证书无法查看全部、证书无法编辑（新版遗漏）
2. 【server】优化 docker 资产监控支持自定义配置 cron `jpom.assets.docker.monitor-cron`
3. 【server】修复 容器终端、容器日志无法正常使用
4. 【server】修复 新版本页面多处无法正常使用相关问题（优化部分提示说明）

------

## 2.11.0.13-beta (2024-01-12)

### 🐞 解决BUG、优化功能

1. 【server】修复 部分 mysql 存储创建索引异常
2. 【agent】优化 插件端自由脚本（分发文件脚本）大小限制调整为 5M（感谢@九問）
3. 【server】修复 新版本 UI 部分错别字、日志阅读查看无法正常使用等
4. 【server】优化 编辑器样式、DSL 配置样式

------

## 2.11.0.12-beta (2024-01-12)

### 🐣 新增功能

1. 【server】新增 资产总览统计

### 🐞 解决BUG、优化功能

1. 【server】修复 mysql 存储使用游标查询报错（不使用游标）（感谢@🇩）
2. 【server】修复 新版 UI 下拉框搜索不生效
3. 【server】优化 **添加**关键词替换为**新增**
4. 【server】优化 部分页面无数据提示
5. 【server】修复 节点分发构建产物选择构建历史为匹配对应构建
6. 【server】修复 部分操作构建环境变量丢失（保存并构建、后台构建、直接构建）

------

## 2.11.0.11-beta (2024-01-11)

### 🐞 解决BUG、优化功能
