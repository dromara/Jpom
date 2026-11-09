# 该分支提供项目的下载链接

`jdk/<版本>/<架构>` 文件内容为清华 TUNA 上 Adoptium 的 JDK tar.gz 直链，供 Docker 构建的 `uses: java` 插件读取。

## 更新清单

清单由 **GitHub Actions 定时更新**（workflow 文件在 `master` 分支：`.github/workflows/update-jdk.yml`）：

- 定时：每天 UTC 00:00（北京时间 08:00）
- 手动：仓库 Actions 页面 → `Update JDK manifest` → Run workflow
- 流程：从 Gitee 检出本分支 → `python3 update_manifest.py` → 有变化才提交 `update jdk` 并 push 回 Gitee

本地手动执行（Python 3，无需第三方依赖）：

```bash
python3 update_manifest.py --dry-run   # 只打印差异，不提交
python3 update_manifest.py             # 有变化时提交并 push
```

说明：

- 数据源为 Adoptium API 的最新 GA 文件名 + TUNA 直链，写入前对每个 URL 做 HEAD 校验；失败时保留旧值，不会写入 404 链接。
- 只维护清单中已存在的“版本/架构”组合，不新增/删除文件。
- 旧的 Gitee Go 定时流水线（`.workflow/ruby-jdk.yml` + `jdk.rb`）因 Gitee Go 构建额度受限已停用，需要时可从 git 历史恢复。
