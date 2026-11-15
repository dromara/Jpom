# VS Code / MCP 客户端适配

推荐用 **MCP 方式**（`jpom-mcp-server` 0.1.0 起可用）。VS Code 使用 `.vscode/mcp.json`（工作区级）或用户设置里的 `mcp.servers`，字段结构与 Claude/Cursor 略有不同：

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

步骤：
1. 确认本机 Node.js ≥ 18。
2. 在工作区根目录创建 `.vscode/mcp.json`，内容参考 `mcp.example.json`（把 token 换成自己的值）。
3. 打开 VS Code 的 MCP 视图/命令面板，启动 `jpom` server；在 Copilot Chat 的 Agent 模式中即可调用 `jpom_*` 工具。
4. 若团队使用用户级配置，把同样的 server 条目放到用户 `settings.json` 的 `mcp.servers` 下。

安全约定：默认 `JPOM_READ_ONLY=true`；token 不要提交到仓库（`.vscode/mcp.json` 建议加入 `.gitignore` 或改用 `JPOM_TOKEN_FILE`）。写操作需显式关闭只读并确认目标。

自检：`npx -y jpom-mcp-server --version`；连通性 `doctor`。
