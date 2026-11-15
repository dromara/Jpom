# jpom-ops Skill（Jpom AI 运维，REST 全能力 / 只读默认）

通过 Jpom 现有 REST API + 长期 token（`JPOM-USER-TOKEN`）完成日常巡检与受控操作，**不修改 Jpom 主程序**。

- 默认**只读**；写操作（触发构建、启停项目、杀进程、在线升级）需 `JPOM_OPS_ALLOW_WRITE=true` **且** `--confirm`。
- 两种接入方式：
  - **MCP Server（推荐）**：`jpom-mcp-server`（0.1.0 起可用），在 Claude Desktop / Cursor / VS Code / DeepSeek Harness 中通过 stdio 加载，见下方「MCP Server」与 `adapters/`。
  - **Skill（REST 脚本）**：本文档的 `SKILL.md` + `scripts/jpom_api.py`，适合无法加载 MCP 或只需要只读巡检的环境。
- 官方文档：《AI 运维接入（Skill / MCP）》<https://jpom.top/pages/ai-ops-skill-mcp/>

## MCP Server（推荐）

`jpom-mcp-server` 是独立 sidecar（Node.js ≥ 18，stdio），通过 Jpom REST API 工作，不修改 Jpom 主程序。无需克隆仓库即可运行：

```bash
npx -y jpom-mcp-server --version   # 查看版本
npx -y jpom-mcp-server --help      # 查看参数/环境变量
```

MCP 客户端配置（Claude Desktop / Cursor 示例，VS Code 见 `adapters/vscode-mcp/`，DSH 用 YAML 插件配置见 `adapters/dsh/`）：

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

关键环境变量：`JPOM_BASE_URL`（必填）、`JPOM_TOKEN` 或 `JPOM_TOKEN_FILE`（二选一）、`JPOM_WORKSPACE`（默认 `DEFAULT`）、`JPOM_READ_ONLY`（默认 `true`，写工具需显式改为 `false`）。完整说明见 npm 包 README。

连接自检（在配置 MCP 客户端前先验证 token 与地址）：

```bash
JPOM_BASE_URL=http://127.0.0.1:2122 JPOM_TOKEN_FILE=/path/to/jpom-longterm-token \
  npx -y jpom-mcp-server doctor
```

安全约定同 Skill：token 只放环境变量/600 权限文件，默认只读，写操作先回显「环境 / 工作空间 / 对象 / 动作」并获确认。

## 快速开始

**Linux / macOS**（需 Python 3.8+；`jpom-api.sh` 是薄包装）：

```bash
export JPOM_BASE_URL=http://127.0.0.1:2122
export JPOM_TOKEN_FILE=/path/to/jpom-longterm-token   # chmod 600；或 JPOM_TOKEN=...
export JPOM_WORKSPACE=DEFAULT

./scripts/jpom-api.sh capabilities
./scripts/jpom-api.sh workspaces
./scripts/jpom-api.sh nodes
./scripts/jpom-api.sh project-info --node-id <节点ID>
./scripts/jpom-api.sh build-history
./scripts/jpom-api.sh system-info
```

**Windows（cmd / PowerShell，推荐直接用 Python）**：

```bat
set JPOM_BASE_URL=http://127.0.0.1:2122
set JPOM_TOKEN_FILE=C:\path\jpom-token
set JPOM_WORKSPACE=DEFAULT
python scripts\jpom_api.py capabilities
python scripts\jpom_api.py nodes --limit 5
```

> 同一份 `scripts/jpom_api.py` 在 Windows / Linux / macOS 行为一致；无需 bash/curl。

## 能力覆盖

| 域 | 命令 |
|---|---|
| 工作空间 | `workspaces` `workspace-list` `workspace-env` |
| 节点 | `nodes` `node-groups` `node-machine-info` `node-monitor` `node-process` |
| 项目 | `projects` `project-info` |
| 构建 | `builds` `build-get` `build-history` `build-log` `build-groups` `repositories` `repository-groups` `branches` |
| 脚本 | `scripts` `scripts-all` `script-logs` `node-scripts` `node-script-logs` |
| 资产 | `machines` `ssh-assets` `ftp-assets` `docker-assets` |
| 监控/分发/系统 | `monitors` `dispatch` `dispatch-logs` `system-info` `check-version` `server-logs` `backups` `clusters` |
| 用户与审计 | `users` `permission-groups` `login-logs` `operate-logs` `recent-logs` `log-read` |

写命令（默认禁用）：`build-start` `build-cancel` `build-rerelease` `project-operate` `node-kill-pid` `system-upgrade`。

**不在范围**（WebSocket/交互式/高风险管理面）：终端、脚本在线执行、文件浏览上传、用户/权限/系统配置写操作。

## 安全约定

- token 只从环境变量或 `chmod 600` 本地文件读取，脚本用 0600 临时 curl 配置传递，不出现在命令行。
- 写操作必须回显「环境 / 工作空间 / 对象 / 动作」并获得明确同意。
- `JPOM_STRICT_CODE=1` 可让鉴权失败等业务错误以非 0 退出。
