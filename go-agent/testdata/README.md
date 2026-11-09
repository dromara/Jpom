# testdata — Java Agent 黄金响应（已脱敏）

本目录由 script/capture-golden.sh 生成，用于 Go Agent 的**结构回归**：
比对「键集合 + 每个键的 JSON 类型」，不比较值（动态值已替换为占位符）。

文件与端点映射见 _endpoints.json。

## 生成/更新

```bash
# 1) 从 live Java Agent 抓取（默认 127.0.0.1:2123，或 --ssh user@host 走远端）
#    鉴权：JPOM_AGENT_AUTHORIZE_FILE=.../agent_authorize.json 或 JPOM_AGENT_AUTHORIZE=<sha1>
cd go-agent
./script/capture-golden.sh --capture /info /get-stat-info /disk-info

# 2) 只对已有 raw/ 重新归一化
./script/capture-golden.sh --normalize-dir raw --out testdata

# 3) 对 Go Agent（例如 127.0.0.1:2124）做结构比对，退出码 0=一致
./script/capture-golden.sh --check testdata --agent-url http://127.0.0.1:2124
```

## 脱敏规则（脚本 normalize-golden.py）

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
| $.data.manifest.engine | Go 插件端新增 engine=go（Server 识别 Go 引擎；Java/旧 Server 忽略未知字段）。normalize-golden.py 的 ALLOWED_EXTRA_PREFIXES 已放行，check 仍应为 0 failure |

## 约束

- **禁止**把 raw/ 或含真实值的响应提交进仓库；只提交本目录（testdata）。
- 新增端点：先用 capture-golden.sh 抓取并归一化，再把 slug->endpoint 映射补进 _endpoints.json。
- 若某端点响应结构变更（Java 版本升级），重新生成 testdata 并在 PR 中说明差异。

