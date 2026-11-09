# testdata — Java Agent 黄金响应（已脱敏）

本目录是 Go 插件端**结构回归**用的脱敏样本：比对「键集合 + 每个键的 JSON 类型」，不比较值
（动态值已替换为占位符）。文件与端点映射见 `_endpoints.json`。

## 生成/更新

采集、归一化、结构比对工具由维护者在内部工作区维护，**不随开源仓发布**；本目录只保留
**已脱敏**的样本。若需重新生成（Java 版本升级、新增端点等），请使用内部工具，并确保：

1. 抓取/生成过程不把含真实主机名、路径、账号、业务数据的原始响应提交进仓库；
2. 归一化后核对 `_endpoints.json` 的 slug → endpoint 映射；
3. 结构变更在 PR/变更说明中写清差异。

## 脱敏规则（内部归一化工具）

| 类别 | 处理 |
| --- | --- |
| 动态数字 | 整数/浮点 -> 0 / 0.0（保留 JSON 类型）；数字字符串 -> "0" |
| 动态标识 | randomId/installId -> <uuid>；32hex -> <hex32>；40hex -> <sha1>；id/nodeId/workspaceId 等 -> <id>/<node-id>/<workspace-id> |
| 时间 | yyyy-MM-dd HH:mm:ss -> 2024-01-01 00:00:00；日期目录名 -> <date>；纯数字长串 -> "0" |
| 路径 | /home/<user>/** -> /home/example/**；路径内版本号 -> <version> |
| 主机 | hostName -> <host-name>；IPv4（仅 IP 语义字段/点分字符串）-> 10.0.0.1；MAC -> 02:00:00:00:00:00 |
| 人员/业务名 | 用户字段 -> <user>；name -> <name>；group/description -> 占位 |
| 文本 | changelog/content/context/script/defArgs -> <changelog>/<file-content>/<script-content>/<def-args>；>2000 字符 -> <long-text:N> |
| 集合 | envVarKeys -> ["<env>"]；数组最多保留 3 个元素（结构比对只看首元素） |
| 保留 | code（200/405/900）、布尔、枚举名（runMode/type/state）、字段名与类型 |

## Go-only 额外字段（结构比对允许）

| 路径 | 说明 |
| --- | --- |
| $.data.manifest.engine | Go 插件端新增 engine=go（Server 识别 Go 引擎；Java/旧 Server 忽略未知字段）。内部归一化工具的 ALLOWED_EXTRA_PREFIXES 已放行，结构比对仍应为 0 failure |

## 约束

- **禁止**把 raw/ 或含真实值的响应提交进仓库；只提交本目录（testdata）。
- 新增端点：归一化后把 slug → endpoint 映射补进 `_endpoints.json`。
- 若某端点响应结构变更（Java 版本升级），重新生成 testdata 并在变更说明中写明差异。

