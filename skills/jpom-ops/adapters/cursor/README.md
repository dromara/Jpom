# Cursor 适配

推荐用 **MCP 方式**（`jpom-mcp-server` 0.1.0 起可用）；也可退回 Skill 方式（`SKILL.md` + REST 脚本）。

## 方式一：MCP（推荐）

1. 确认本机 Node.js ≥ 18。
2. 在项目内创建 `.cursor/mcp.json`（或全局 `~/.cursor/mcp.json`），参考 `mcp.example.json`：

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

3. 打开 Cursor 设置 → MCP，确认 `jpom` 已连接、工具列表出现 `jpom_*`。
4. 在 Agent/Chat 中用自然语言提问，例如「列出 Jpom 节点状态」「最近一次构建失败原因」。

安全约定：默认 `JPOM_READ_ONLY=true`；token 只放本地（建议文件权限 600），不要提交到仓库。写操作需显式改 `JPOM_READ_ONLY=false` 并在对话中确认目标。

自检：`npx -y jpom-mcp-server --version` 与 `... doctor`。

## 方式二：Skill（REST 脚本）

将 `SKILL.md` 加入项目 Rules/Instructions；用终端执行 `scripts/jpom_api.py`（Linux/macOS 可用 `jpom-api.sh` 包装）（只读）。适合只需要巡检、或 MCP 不可用的环境。

> 先用 `python scripts/jpom_api.py capabilities`（Linux/macOS 可用 `./scripts/jpom-api.sh capabilities`）查看完整只读/写命令清单（默认只读）。
