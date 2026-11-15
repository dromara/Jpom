# Claude Desktop 适配

推荐用 **MCP 方式**（`jpom-mcp-server` 0.1.0 起可用）；也可退回 Skill 方式（直接执行 REST 脚本）。

## 方式一：MCP（推荐）

1. 确认本机 Node.js ≥ 18（`node -v`）。
2. 编辑 Claude Desktop 配置：
   - macOS：`~/Library/Application Support/Claude/claude_desktop_config.json`
   - Windows：`%APPDATA%\Claude\claude_desktop_config.json`
3. 参考 `claude_desktop_config.example.json` 增加 `mcpServers.jpom`，把 `JPOM_BASE_URL`、token 换成自己的值。
4. 完全退出并重启 Claude Desktop；在输入框的工具（锤子）图标里应能看到 `jpom_*` 工具。

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

安全约定：
- `JPOM_READ_ONLY` 保持 `"true"`，只开放查询类工具；确需写操作时再改为 `"false"`，并先在 Jpom 中确认目标对象。
- token 只放本地配置文件（文件权限 600），**不要提交到仓库、不要截图传播**。也可以改用 `JPOM_TOKEN_FILE` 指向只读文件。

自检：`npx -y jpom-mcp-server --version`；连通性 `JPOM_BASE_URL=... JPOM_TOKEN=... npx -y jpom-mcp-server doctor`。

## 方式二：Skill（REST 脚本）

将 `SKILL.md` 作为项目指令/上下文，本机配置 `JPOM_*` 环境变量并允许执行 `scripts/jpom_api.py`（Linux/macOS 也可用 `jpom-api.sh` 包装）。适合无法加载 MCP、只需要只读巡检的场景。

> 先用 `python scripts/jpom_api.py capabilities`（Linux/macOS 可用 `./scripts/jpom-api.sh capabilities`）查看完整只读/写命令清单（默认只读）。
