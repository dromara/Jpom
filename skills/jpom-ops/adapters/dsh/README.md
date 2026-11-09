# DeepSeek Harness 适配

DSH 支持两种方式：**MCP**（推荐）与 **Skill**（REST 脚本）。两者可同时安装。

## 方式一：MCP（推荐）

DSH 的 MCP 配置是插件配置树（YAML），不是 Claude/Cursor 的 `mcpServers` JSON。在 DSH 的 profile patch 或 `$DSH_HOME/cordis.patch.yml` 中追加一条 `@deepseek-ai/dsh-mcp-client` entry：

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

说明：
- 工具在 DSH 中显示为 `mcp__jpom__<tool>`（例如 `mcp__jpom__jpom_nodes`）。
- token 建议通过环境变量引用（如 `!!js process.env.JPOM_TOKEN`），不要把真实值写进配置文件。
- 修改配置后重载 DSH，确认工具列表出现 `mcp__jpom__*`。
- 若 DSH 环境没有 npm/npx，可改为本地源码方式：`command: node`、`args: ['/path/to/Jpom/jpom-parent/mcp-server/dist/index.js']`。

## 方式二：Skill（REST 脚本）

1. 将 `skills/jpom-ops/` 安装到 DSH 可发现的 skills 目录（项目级或用户级，按 DSH 部署约定）。
2. 仅本地配置：`JPOM_BASE_URL`、`JPOM_TOKEN` 或 `JPOM_TOKEN_FILE`（chmod 600）、`JPOM_WORKSPACE`。
3. 触发：`查看 Jpom 节点状态`、`最近构建失败`、`查操作日志`。默认只读。

> 先用 `python scripts/jpom_api.py capabilities`（Linux/macOS 可用 `./scripts/jpom-api.sh capabilities`）查看完整只读/写命令清单（默认只读）。
