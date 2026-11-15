# jpom-mcp-server

Jpom 的 [Model Context Protocol](https://modelcontextprotocol.io/) (MCP) 服务器。它是一个独立 sidecar：
通过 **stdio** 与 MCP 客户端通信，通过 Jpom Server 的 REST API 工作，**不修改也不嵌入 Jpom 主程序**
（Jpom 主程序为 Java 8 / Spring Boot 2.7，MCP 侧独立发版）。

- 包名：`jpom-mcp-server`
- 版本：`0.1.1`（首个公开版本 0.1.0 的 CLI bin 修复版，见 [CHANGELOG.md](https://gitee.com/dromara/Jpom/blob/dev/mcp-server/CHANGELOG.md)）
- 运行环境：Node.js >= 18，ESM，Linux / macOS / Windows
- 协议：MCP stdio（stdio transport），兼容 Claude Desktop、Cursor、VS Code、DeepSeek Harness 等客户端
- 许可证：Mulan PSL v2

## 能力概览

把 Jpom 的日常运维面暴露为 MCP 工具，按域分组（完整清单见 [工具清单](#工具清单)）：

| 域 | 能力 | 默认 |
| --- | --- | --- |
| 节点 | 节点/节点分组、监控数据、进程列表、机器信息 | 只读 |
| 项目 | 节点上的项目列表与项目详情（运行模式、路径、状态等） | 只读 |
| 构建 | 构建配置、构建详情、构建历史、构建分组、仓库与分支 | 只读 |
| 脚本 | 服务端脚本/执行日志、节点脚本/执行日志 | 只读 |
| 资产 | 机器、SSH、FTP、Docker 资产 | 只读 |
| 监控 | 项目监控配置 | 只读 |
| 分发 | 分发任务记录与执行日志 | 只读 |
| 系统 | 系统信息、版本检查、备份、集群、受管日志文件 | 只读 |
| 用户审计 | 用户、权限组、登录日志、操作日志、最近操作 | 只读 |
| 日志 | 系统/节点日志快照与有界 live 窗口、构建/脚本日志行游标 | 只读 |
| 写操作闸门 | 触发/取消/重新发布构建、项目启停重启、结束进程、在线升级 | **默认禁用** |
| 通用 REST | `jpom_api_call`（只读方法白名单 + 写操作二次确认） | 只读优先 |

设计要点：

- **默认只读**：`JPOM_READ_ONLY=true` 时写工具直接拒绝，且不会发出任何请求；需要写操作时显式改为 `false`。
- **有界结果**：每个工具结果按 `JPOM_MAX_RESULT_CHARS`（默认 50000 字符）截断；列表类工具分页（默认 20，最大 200）。
- **日志不灌满上下文**：日志按"快照 + 行游标 + 有界 live 窗口"读取，而不是无限流。
- **错误可排查**：Jpom 的 `{code,msg,data}` 业务错误以 `isError` 结果返回，并附带 800/999 等错误码提示。
- **安全边界**：token 只通过请求头传递，不进入 URL、不写入工具结果；通用 REST 工具禁止访问绕过权限的 `/api/` 路径。

## 安装

### 使用 npx（推荐，无需克隆仓库）

```bash
npx -y jpom-mcp-server --version   # 打印版本
npx -y jpom-mcp-server --help      # 打印用法与环境变量
```

### 全局安装

```bash
npm install -g jpom-mcp-server
jpom-mcp-server --version
```

### 本地源码方式

```bash
cd jpom-parent/mcp-server
npm ci
npm run build
node dist/index.js --version
```

本地源码方式下，MCP 客户端配置里的 `command` 用 `node`、`args` 指向构建后的
`dist/index.js`（见下方示例）。发布前 `prepack` 会自动执行 `npm run build`，因此 npm 包内已包含 `dist/`。

### 连接自检：doctor

```bash
JPOM_BASE_URL=http://127.0.0.1:2122 JPOM_TOKEN_FILE=/path/to/jpom-longterm-token npx -y jpom-mcp-server doctor
```

成功时输出 JSON，包含 `ok`、`baseUrl`、`workspace`、`readOnly`、当前用户可访问的 `workspaces`
与节点总数 `nodeTotal`。建议在配置 MCP 客户端之前先跑通 `doctor`。

## MCP 客户端配置

所有客户端都需要通过配置里的 `env` 注入 `JPOM_*` 环境变量（客户端启动 server 进程时不会继承你终端里的全部变量）。
把 `JPOM_TOKEN` 换成自己的 Jpom 长期 token，或改用 `JPOM_TOKEN_FILE` 指向权限 600 的本地文件。

### Claude Desktop

配置文件：macOS `~/Library/Application Support/Claude/claude_desktop_config.json`；
Windows `%APPDATA%\Claude\claude_desktop_config.json`。修改后**完全退出并重启** Claude Desktop。

```json
{
  "mcpServers": {
    "jpom": {
      "command": "npx",
      "args": ["-y", "jpom-mcp-server"],
      "env": {
        "JPOM_BASE_URL": "http://127.0.0.1:2122",
        "JPOM_TOKEN": "<YOUR_JPOM_LONG_TERM_TOKEN>",
        "JPOM_WORKSPACE": "DEFAULT",
        "JPOM_READ_ONLY": "true"
      }
    }
  }
}
```

### Cursor

项目级 `.cursor/mcp.json` 或全局 `~/.cursor/mcp.json`：

```json
{
  "mcpServers": {
    "jpom": {
      "command": "npx",
      "args": ["-y", "jpom-mcp-server"],
      "env": {
        "JPOM_BASE_URL": "http://127.0.0.1:2122",
        "JPOM_TOKEN": "<YOUR_JPOM_LONG_TERM_TOKEN>",
        "JPOM_WORKSPACE": "DEFAULT",
        "JPOM_READ_ONLY": "true"
      }
    }
  }
}
```

配置后打开 Cursor 设置 → MCP，确认 `jpom` 已连接、工具列表出现 `jpom_*`。

### VS Code

工作区级 `.vscode/mcp.json`（字段是 `servers`，stdio 需显式 `type`）：

```json
{
  "servers": {
    "jpom": {
      "type": "stdio",
      "command": "npx",
      "args": ["-y", "jpom-mcp-server"],
      "env": {
        "JPOM_BASE_URL": "http://127.0.0.1:2122",
        "JPOM_TOKEN": "<YOUR_JPOM_LONG_TERM_TOKEN>",
        "JPOM_WORKSPACE": "DEFAULT",
        "JPOM_READ_ONLY": "true"
      }
    }
  }
}
```

### DeepSeek Harness（DSH）

DSH 的 MCP 配置是插件配置树（YAML），不是 `mcpServers` JSON。在 profile patch 或
`$DSH_HOME/cordis.patch.yml` 中追加一条 `dsh-mcp-client` entry：

```yaml
- id: mcp-jpom
  name: '@deepseek-ai/dsh-mcp-client'
  config:
    serverName: jpom
    transport: stdio
    command: npx
    args: ['-y', 'jpom-mcp-server']
    env:
      JPOM_BASE_URL: http://127.0.0.1:2122
      JPOM_TOKEN: '<YOUR_JPOM_LONG_TERM_TOKEN>'
      JPOM_WORKSPACE: DEFAULT
      JPOM_READ_ONLY: 'true'
    toolCallTimeoutMs: 120000
```

说明：DSH 中工具名显示为 `mcp__jpom__<tool>`（例如 `mcp__jpom__jpom_nodes`）；token 建议用
`!!js process.env.JPOM_TOKEN` 之类方式引用，不要把真实值写进配置文件。DSH 的 JSON 配置文件不直接支持
`mcpServers`，请以 DSH 的插件配置为准。

### 本地源码方式（任意客户端）

```json
{
  "mcpServers": {
    "jpom": {
      "command": "node",
      "args": ["/absolute/path/to/Jpom/jpom-parent/mcp-server/dist/index.js"],
      "env": {
        "JPOM_BASE_URL": "http://127.0.0.1:2122",
        "JPOM_TOKEN_FILE": "/absolute/path/to/jpom-longterm-token",
        "JPOM_WORKSPACE": "DEFAULT",
        "JPOM_READ_ONLY": "true"
      }
    }
  }
}
```

> 使用源码方式前先执行 `npm ci && npm run build`；`dist/index.js` 不存在时客户端会启动失败。

## 环境变量

| 变量 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- |
| `JPOM_BASE_URL` | 是 | — | Jpom Server 地址，必须以 `http://` 或 `https://` 开头，例如 `http://127.0.0.1:2122`；末尾斜杠会被去掉。若 Jpom 配置了 context-path 需一并带上。 |
| `JPOM_TOKEN` | 二选一 | — | Jpom 长期用户 token（请求头 `JPOM-USER-TOKEN`）。建议为 AI 单独创建最小权限 token。 |
| `JPOM_TOKEN_FILE` | 二选一 | — | 存放 token 的文件路径；读取后会去掉所有空白字符。建议 `chmod 600`，不要把 token 提交到仓库。 |
| `JPOM_WORKSPACE` | 否 | `DEFAULT` | 操作的工作空间 id；会作为 `workspaceId` 请求头发送，影响所有工具可见的数据范围。 |
| `JPOM_READ_ONLY` | 否 | `true` | `true`（默认）只启用只读工具；仅当值为 `false` / `0` / `no` / `off` 时才启用写工具，其它值（含 `true`/`1`/`yes`/`on`）均表示只读。 |
| `JPOM_TIMEOUT_MS` | 否 | `60000` | 单次 HTTP 请求超时（毫秒），范围 1000 ~ 600000。 |
| `JPOM_MAX_RESULT_CHARS` | 否 | `50000` | 单个工具结果最大字符数，范围 2000 ~ 500000，超出部分截断并标注。 |
| `JPOM_VERIFY_SSL` | 否 | `true` | 是否校验证书。仅在自签名测试环境设为 `false`；关闭时会在 stderr 打印告警。生产环境请保持开启。 |

## 工具清单

共 54 个工具：47 个只读工具、6 个受控写工具、1 个通用 REST 工具。工具通过 MCP `tools/list` 暴露，
只读工具标记 `readOnlyHint: true`；写工具与 `jpom_api_call` 标记 `readOnlyHint: false`，写工具另有 `destructiveHint: true`。

### 工作空间（只读）

| 工具 | 说明 | 关键参数 |
| --- | --- | --- |
| `jpom_workspaces` | 当前 token 用户可访问的工作空间 | — |
| `jpom_workspace_list` | 全部工作空间（需系统级权限） | — |
| `jpom_workspace_env` | 工作空间环境变量 | `page`、`limit` |

### 节点（只读）

| 工具 | 说明 | 关键参数 |
| --- | --- | --- |
| `jpom_nodes` | 当前工作空间的节点列表（分页） | `page`、`limit` |
| `jpom_node_groups` | 节点分组名称 | — |
| `jpom_node_monitor` | 节点监控采样（CPU/内存/磁盘/网络，最多最近 500 条） | `nodeId` |
| `jpom_node_processes` | 节点上可见的进程（pid、命令、CPU/内存） | `nodeId` |
| `jpom_node_machine_info` | 节点机器信息（OS、架构、CPU、内存、agent 版本） | `nodeId` |

### 项目（只读）

| 工具 | 说明 | 关键参数 |
| --- | --- | --- |
| `jpom_projects` | 节点上的项目列表（分页） | `nodeId`、`page`、`limit` |
| `jpom_project_info` | 项目详情；不带 `id` 时列出节点全部项目 | `nodeId`、`id?`、`page`、`limit` |

### 构建（只读）

| 工具 | 说明 | 关键参数 |
| --- | --- | --- |
| `jpom_builds` | 在线构建配置列表（分页） | `page`、`limit` |
| `jpom_build_get` | 单个构建配置详情 | `id` |
| `jpom_build_history` | 构建历史记录（状态、耗时、触发方式、分支，分页） | `page`、`limit` |
| `jpom_build_groups` | 构建分组名称 | — |
| `jpom_repositories` | 构建仓库列表（分页） | `page`、`limit` |
| `jpom_repository_groups` | 仓库分组名称 | — |
| `jpom_build_branches` | 仓库可用分支 | `repositoryId` |

### 脚本（只读）

| 工具 | 说明 | 关键参数 |
| --- | --- | --- |
| `jpom_scripts` | 服务端脚本列表（分页） | `page`、`limit` |
| `jpom_scripts_all` | 服务端脚本全量列表（不分页） | — |
| `jpom_script_logs` | 服务端脚本执行日志（分页） | `page`、`limit` |
| `jpom_node_scripts` | 节点脚本模板列表（分页） | `nodeId`、`page`、`limit` |
| `jpom_node_script_logs` | 节点脚本执行日志（分页） | `nodeId`、`page`、`limit` |

### 资产（只读）

| 工具 | 说明 | 关键参数 |
| --- | --- | --- |
| `jpom_assets` | 按类型列资产：machine / ssh / ftp / docker | `type`、`page`、`limit` |
| `jpom_machines` | 机器资产 | `page`、`limit` |
| `jpom_ssh_assets` | SSH 资产 | `page`、`limit` |
| `jpom_ftp_assets` | FTP 资产 | `page`、`limit` |
| `jpom_docker_assets` | Docker 资产 | `page`、`limit` |

### 监控与分发（只读）

| 工具 | 说明 | 关键参数 |
| --- | --- | --- |
| `jpom_monitors` | 项目监控配置（分页） | `page`、`limit` |
| `jpom_dispatch` | 分发任务记录（分页） | `page`、`limit` |
| `jpom_dispatch_logs` | 分发执行日志（分页） | `page`、`limit` |

### 系统（只读）

| 工具 | 说明 | 关键参数 |
| --- | --- | --- |
| `jpom_system_info` | 系统信息（版本、JVM/OS、运行时长、授权等） | — |
| `jpom_check_version` | 检查是否有新版本 | — |
| `jpom_system_backups` | 系统备份文件（分页） | `page`、`limit` |
| `jpom_clusters` | 服务端集群（分页） | `page`、`limit` |
| `jpom_log_read_list` | 受管日志文件清单（分页） | `page`、`limit` |

### 用户与审计（只读）

| 工具 | 说明 | 关键参数 |
| --- | --- | --- |
| `jpom_users` | 用户列表（分页，需系统级权限） | `page`、`limit` |
| `jpom_permission_groups` | 权限组列表（分页，需系统级权限） | `page`、`limit` |
| `jpom_login_logs` | 登录日志（分页） | `page`、`limit` |
| `jpom_operate_logs` | 操作/审计日志（分页） | `page`、`limit` |
| `jpom_recent_logs` | 最近操作日志（`limit` 1~200，默认 20） | `limit` |

### 日志（只读）

| 工具 | 说明 | 关键参数 |
| --- | --- | --- |
| `jpom_system_log_files` | 系统日志文件树；传 `machineId` 时列出插件端日志目录 | `machineId?` |
| `jpom_system_log_read` | 读取系统/节点日志尾部或按 `grep` 过滤；默认最多处理 5 MiB | `path`、`machineId?`、`tailLines`、`maxBytes`、`grep`、`ignoreCase`、`maxMatches` |
| `jpom_system_log_follow` | 短时 WebSocket 跟随系统/节点日志（有界窗口） | `path`、`machineId?`、`durationMs`、`maxLines` |
| `jpom_project_log_follow` | 短时跟随项目日志/日志文件；`keyword` 走日志检索模式（需 `fileName`） | `nodeId`、`id`、`projectId`、`fileName?`、`keyword?`、`durationMs`、`maxLines` |
| `jpom_build_log` | 按行游标读构建日志，返回 `nextLine` | `id`、`buildId`、`line`、`maxLines` |
| `jpom_script_log` | 按行游标读服务端脚本执行日志 | `id`、`executeId`、`line`、`maxLines` |
| `jpom_node_script_log` | 按行游标读节点脚本执行日志 | `nodeId`、`id`、`executeId`、`line`、`maxLines` |

### 写操作（默认禁用，需 `JPOM_READ_ONLY=false`）

| 工具 | 说明 | 关键参数 |
| --- | --- | --- |
| `jpom_build_start` | 触发构建，可指定分支与备注 | `id`、`branchName?`、`buildRemark?` |
| `jpom_build_cancel` | 取消运行中的构建 | `id` |
| `jpom_build_rerelease` | 按历史记录重新发布 | `logId` |
| `jpom_project_operate` | 项目启停重启（`opt`：start / stop / restart） | `nodeId`、`id`、`opt` |
| `jpom_node_kill_pid` | 结束节点/机器上的进程（高危） | `nodeId`、`pid`、`machineId?` |
| `jpom_system_upgrade` | 触发在线升级（最高风险，可能重启 Server） | `machineId?` |

### 通用 REST（只读优先）

| 工具 | 说明 | 关键参数 |
| --- | --- | --- |
| `jpom_api_call` | 直接调用 Jpom REST API 的逃生通道。GET 始终允许；POST 仅允许只读白名单（如 `/system/info`、`/build/list`）；其余 POST 在只读模式下拒绝，开启写后仍需 `confirm:true`。禁止 `/api/` 前缀与完整 URL。 | `method`、`path`、`query?`、`form?`、`json?`、`confirm?` |

> 优先使用类型化工具；`jpom_api_call` 只是补充手段，且无法绕过 Jpom 自身的权限校验。

## 日志使用建议

AI/LLM 的上下文有限，MCP 客户端对单次工具结果也有上限。日志类工具的设计目标是"可续读的快照"，
不是把整份日志塞给模型：

1. **先列文件、再取快照**：用 `jpom_system_log_files` 找到日志路径，再用 `jpom_system_log_read`
   取 `tailLines`（默认 200，最大 5000）；大文件用 `grep` + `maxMatches` 缩小范围。
2. **构建/脚本日志用行游标**：`jpom_build_history` / `jpom_script_logs` 找到记录后，
   调 `jpom_build_log` 或 `jpom_script_log`（`line=1`、`maxLines=200`），
   用返回的 `nextLine` 作为下一次的 `line` 继续读，直到 `nextLine` 不再前进。节点脚本用
   `jpom_node_script_log`。
3. **follow 是有界窗口，不是无限流**：`jpom_system_log_follow` / `jpom_project_log_follow`
   只收集 `durationMs`（默认 3000，最大 15000）或 `maxLines`（默认 200，最大 2000）以内的新行；
   Jpom 订阅时只回放少量最近行，**历史请用 read/游标工具补齐**，不要用 follow 回填。
4. **控制单次上下文**：优先 `maxLines` 小步读取、用 `grep`/`keyword` 过滤；结果超限时按
   `JPOM_MAX_RESULT_CHARS` 截断并提示继续。需要完整日志时让用户下载或分段读，不要让模型反复拉全量。
5. **注意行号语义**：`line` 是 1 基行号；日志轮转后行号可能变化，跨轮转时以重新读取为准。

## 安全

- **最小权限 token**：在 Jpom 中为 AI 单独创建长期 token，只授予需要的工作空间与权限组；不要复用管理员 token。
- **默认只读**：`JPOM_READ_ONLY` 默认 `true`；只有明确需要变更时才设为 `false`，并在会话结束后改回。
- **写操作二次确认**：类型化写工具在只读模式下直接拒绝；开启写之后，执行前应回显「环境 / 工作空间 / 对象 / 动作」，
  得到用户明确同意再执行，优先选择最小、可回滚的动作。`jpom_api_call` 对非白名单 POST 还要求 `confirm:true`。
- **token 不进 URL、不进仓库**：token 仅通过 `JPOM-USER-TOKEN` 请求头发送；不要写进命令行参数、URL、
  客户端配置提交记录或截图。客户端配置建议用 `JPOM_TOKEN_FILE`（`chmod 600`）或本地环境变量注入。
- **TLS**：`JPOM_VERIFY_SSL` 默认校验；仅在自签名测试环境临时关闭，并知悉中间人风险。
- **审计**：所有操作可通过 `jpom_operate_logs` / `jpom_login_logs` 回溯；写操作请保留操作记录。
- **拒绝绕过**：`jpom_api_call` 禁止 `/api/` 前缀（Jpom 开放 API 绕过用户权限拦截器），也无法绕过 Jpom 权限。

## 本地开发、测试与打包

```bash
cd jpom-parent/mcp-server
npm ci                 # 按 package-lock.json 安装依赖
npm run typecheck      # tsc --noEmit
npm run build          # 编译到 dist/
npm test               # vitest（单测 + 集成测试）
npm run pack:check     # npm pack --dry-run，检查发布内容
node dist/index.js doctor   # 用真实 Jpom 自检连通性（需 JPOM_* 环境变量）
```

其他可用命令：

| 命令 | 作用 |
| --- | --- |
| `npm run dev` | 用 tsx 直接运行 TypeScript 源码 |
| `npm run start` | 运行构建后的 `dist/index.js` |
| `npm run test:unit` | 只跑单测（`test/unit`） |
| `npm run test:integration` | 只跑集成测试（`test/integration`） |
| `npm run e2e:dev` | 对 dev 环境执行端到端脚本，输出 JSON 报告 |

要求 Node.js >= 18；项目为 ESM（`"type": "module"`），源码使用 NodeNext 模块解析。
发布前 `prepack` 会自动构建，请确保 `npm run build` 与 `npm test` 均通过。

## 故障排查

| 现象 | 可能原因与处理 |
| --- | --- |
| Jpom API 返回 `code=800` | token 无效或已过期（修改密码/重新生成长期 token 后旧 token 失效）。重新在 Jpom「用户 → 长期 token」生成，更新 `JPOM_TOKEN` / `JPOM_TOKEN_FILE`；`doctor` 可快速验证。 |
| Jpom API 返回 `code=999` | 权限不足。检查 token 所属用户的工作空间与权限组、`JPOM_WORKSPACE` 是否指向正确工作空间，以及对应功能是否有权限；先用 `jpom_workspaces` 确认可访问范围。 |
| 连接失败 / `ECONNREFUSED` / `ENOTFOUND` / 超时 | 检查 `JPOM_BASE_URL`（协议、主机、端口，默认 2122）、Jpom 是否启动、context-path 是否一致、本机网络/代理/防火墙；`JPOM_TIMEOUT_MS` 过小也会导致超时。 |
| TLS 证书错误 | 自签名或证书链不完整。测试环境可临时 `JPOM_VERIFY_SSL=false`；生产环境应修复证书，不要关闭校验。 |
| 返回非 JSON / HTML 登录页 | `JPOM_BASE_URL` 少了或多了 context-path，或请求被反向代理重定向到登录页；确认地址能直接访问 REST 接口。 |
| 客户端看不到 `jpom_*` 工具 | 查看客户端 MCP 日志（server 的 stderr）；确认 `command/args` 可执行、Node ≥ 18、`env` 已注入、改配置后已重启客户端；先在终端用 `doctor` 验证。 |
| `npx` 启动慢或失败 | 首次运行需下载包；检查网络/registry 镜像与 Node 版本。内网环境可改用全局安装或本地源码方式。 |
| 结果被截断 | 触达 `JPOM_MAX_RESULT_CHARS` 上限；缩小 `limit`/`maxLines`、用 `grep`/`keyword` 过滤或分段读取。 |

## 兼容性与限制

- 面向 Jpom 2.11.x 的 REST API；Jpom 主程序升级后若接口变化，以实际返回为准。
- 0.1.x 仅支持 **stdio** 传输，暂不提供远程 HTTP/SSE transport；一个客户端进程对应一个 Jpom 连接配置。
- 不提供交互式终端、文件上传、脚本在线执行等 WebSocket/高风险管理面；这类操作请使用 Jpom Web UI。
- 集群/多实例部署下，日志 tail 与部分实时数据依赖具体实例的内存状态，与 Jpom Web 端行为一致。

## 相关链接

- Jpom 项目：<https://gitee.com/dromara/Jpom>
- 文档：<https://jpom.top/>
- AI 运维接入（Skill / MCP）：<https://jpom.top/pages/ai-ops-skill-mcp/>
- 变更记录：[CHANGELOG.md](https://gitee.com/dromara/Jpom/blob/dev/mcp-server/CHANGELOG.md)

## License

Mulan PSL v2，见 [LICENSE](https://gitee.com/dromara/Jpom/blob/dev/mcp-server/LICENSE)。
