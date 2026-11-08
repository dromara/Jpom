#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""jpom-ops: Jpom REST API 客户端（跨平台，仅标准库）

兼容：Jpom 2.11.x（dev 2.11.13 已逐条验证）
运行环境：Python 3.8+（Windows / Linux / macOS 通用，无需 bash/curl）

用法示例：
    set JPOM_BASE_URL=http://127.0.0.1:2122      # Windows cmd
    set JPOM_TOKEN_FILE=C:\\path\\jpom-token
    python jpom_api.py workspaces

    export JPOM_BASE_URL=http://127.0.0.1:2122   # Linux/macOS
    export JPOM_TOKEN_FILE=/path/jpom-token
    python3 scripts/jpom_api.py nodes --limit 5

安全：
- token 只从环境变量或本地文件读取，不出现在命令行参数；
- 默认只读；写操作需 JPOM_OPS_ALLOW_WRITE=true 且 --confirm；
- 不创建临时文件，凭据仅存在于进程内存。
"""
import argparse
import json
import os
import sys
from urllib.parse import urlencode
from urllib.request import Request, build_opener, HTTPSHandler
from urllib.error import HTTPError, URLError
import ssl

# Windows 控制台默认不是 UTF-8，强制输出编码，避免中文提示乱码
for _stream in (sys.stdout, sys.stderr):
    try:
        _stream.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

READONLY_POST = {
    "/user/recent-log-data",
    "/node/list_data.json",
    "/node/list_group_all.json",
    "/node/project_list",
    "/node/node_monitor_data.json",
    "/node/processList",
    "/node/manage/get_project_info",
    "/node/script/list_all",
    "/node/script_log/list",
    "/build/list",
    "/build/history/history_list.json",
    "/build/manage/get-now-log",
    "/build/repository/list",
    "/build/branch-list",
    "/script/list",
    "/script_log/list",
    "/outgiving/dispatch-list",
    "/outgiving/log_list_data.json",
    "/system/assets/machine/list-data",
    "/system/assets/ssh/list-data",
    "/system/assets/ftp/list-data",
    "/system/assets/docker/list-data",
    "/system/backup/list",
    "/system/log_data.json",
    "/system/info",
    "/system/check_version.json",
    "/monitor/list_data.json",
    "/cluster/list",
    "/user/get_user_list",
    "/user-permission-group/get-list",
    "/system/workspace_env/list",
    "/user/login-log/list-data",
    "/user/list-operate-log-data",
    "/log-read/list",
}

CAPABILITIES = """jpom-ops 能力覆盖（Jpom 2.11.x REST）：
  已覆盖：工作空间、节点(含监控/进程/分组/机器信息)、项目(列表/详情)、
          构建(配置/详情/历史/日志/分组/仓库/分支)、服务端与节点脚本(列表/日志)、
          资产(机器/SSH/FTP/Docker)、项目监控、分发、系统备份、集群、
          用户/权限组/登录日志/操作日志、工作空间环境变量、托管日志、系统信息与版本检查。
  写操作（默认禁用，需 JPOM_OPS_ALLOW_WRITE=true + --confirm）：
          触发构建/取消/重建、项目启停重启、杀进程、在线升级。
  不在范围（WebSocket/交互式通道，无法用 REST 安全封装）：
          终端(SSH/Docker CLI/节点控制台)、脚本在线执行、文件浏览与上传、Docker 命令下发。
"""


class ApiError(Exception):
    pass


def usage_exit(msg: str = "", code: int = 2):
    if msg:
        print(msg, file=sys.stderr)
    print(
        "usage: python jpom_api.py <command> [--flags]\n"
        "      运行 capabilities 查看完整命令与能力清单",
        file=sys.stderr,
    )
    sys.exit(code)


def env_get(name: str, default: str = "") -> str:
    return os.environ.get(name, default).strip()


def load_token() -> str:
    tok = env_get("JPOM_TOKEN")
    if tok:
        return tok
    path = env_get("JPOM_TOKEN_FILE")
    if path:
        if not os.path.isfile(path):
            usage_exit(f"JPOM_TOKEN_FILE 不存在: {path}")
        with open(path, "r", encoding="utf-8", errors="ignore") as f:
            tok = "".join(f.read().split())
        if not tok:
            usage_exit(f"token 文件为空: {path}")
        return tok
    usage_exit("缺少 JPOM_BASE_URL 或 JPOM_TOKEN/JPOM_TOKEN_FILE")


class Client:
    def __init__(self):
        self.base = env_get("JPOM_BASE_URL").rstrip("/")
        self.workspace = env_get("JPOM_WORKSPACE", "DEFAULT") or "DEFAULT"
        self.pretty = env_get("JPOM_PRETTY", "1") != "0"
        self.strict = env_get("JPOM_STRICT_CODE", "0") == "1"
        self.allow_write = env_get("JPOM_OPS_ALLOW_WRITE", "false").lower() == "true"
        self.confirm = False
        self.token = None

    def require_env(self):
        if not self.base:
            usage_exit("缺少 JPOM_BASE_URL 或 JPOM_TOKEN/JPOM_TOKEN_FILE")
        if self.token is None:
            tok = load_token()
            self.token = tok

    def _readonly_ok(self, method: str, path: str) -> bool:
        if method in ("GET", "HEAD"):
            return True
        return path in READONLY_POST

    def request(self, method, path, params=None, json_body=None, write=False):
        if write:
            if not self.allow_write:
                print(
                    f"blocked: 写操作默认禁用（{path}）。如确需执行：设置 JPOM_OPS_ALLOW_WRITE=true 并加 --confirm",
                    file=sys.stderr,
                )
                sys.exit(3)
            if not self.confirm:
                print(
                    f"blocked: 写操作需要显式 --confirm（{path}，工作空间={self.workspace}）",
                    file=sys.stderr,
                )
                sys.exit(4)
        elif not self._readonly_ok(method, path):
            print(
                f"blocked in read-only mode: {method} {path}（如需写操作请用写命令并显式授权）",
                file=sys.stderr,
            )
            sys.exit(3)

        self.require_env()
        url = self.base + path
        body = None
        headers = {
            "JPOM-USER-TOKEN": self.token,
            "workspaceId": self.workspace,
            "Accept": "application/json",
        }
        if json_body is not None:
            body = json_body.encode("utf-8")
            headers["Content-Type"] = "application/json"
        elif method in ("POST", "PUT", "PATCH", "DELETE"):
            body = urlencode(params or {}, doseq=True).encode("utf-8")
            headers["Content-Type"] = "application/x-www-form-urlencoded"
        elif params:
            url = url + ("&" if "?" in url else "?") + urlencode(params, doseq=True)

        opener = build_opener()
        if url.startswith("https://") and env_get("JPOM_VERIFY_SSL", "1") == "0":
            ctx = ssl.create_default_context()
            ctx.check_hostname = False
            ctx.verify_mode = ssl.CERT_NONE
            opener = build_opener(HTTPSHandler(context=ctx))

        req = Request(url, data=body, headers=headers, method=method)
        try:
            with opener.open(req, timeout=90) as resp:
                raw = resp.read().decode("utf-8", errors="replace")
        except HTTPError as e:
            raw = e.read().decode("utf-8", errors="replace")
        except URLError as e:
            print(f"请求失败: {method} {url} ({e.reason})", file=sys.stderr)
            sys.exit(1)

        if self.pretty:
            try:
                print(json.dumps(json.loads(raw), ensure_ascii=False, indent=2))
            except Exception:
                print(raw)
        else:
            print(raw)

        if self.strict:
            try:
                code = json.loads(raw).get("code")
            except Exception:
                code = None
            if code is not None and code != 200:
                print(f"api error: code={code} ({method} {path})", file=sys.stderr)
                sys.exit(1)


def build_parser():
    p = argparse.ArgumentParser(add_help=False)
    p.add_argument("command", nargs="?")
    p.add_argument("--page", type=int, default=1)
    p.add_argument("--limit", type=int, default=20)
    p.add_argument("--node-id", default="")
    p.add_argument("--id", default="")
    p.add_argument("--pid", default="")
    p.add_argument("--opt", default="")
    p.add_argument("--branch-name", default="")
    p.add_argument("--remark", default="")
    p.add_argument("--build-id", default="")
    p.add_argument("--line", default="1")
    p.add_argument("--log-id", default="")
    p.add_argument("--repository-id", default="")
    p.add_argument("--machine-id", default="")
    p.add_argument("--confirm", action="store_true")
    p.add_argument("--form", action="append", default=[])
    p.add_argument("--json", dest="json_body", default="")
    p.add_argument("raw_args", nargs="*")
    p.add_argument("-h", "--help", action="store_true")
    return p


def require(cond, msg):
    if not cond:
        print(msg, file=sys.stderr)
        sys.exit(2)


def main():
    if len(sys.argv) < 2:
        usage_exit()
    args = build_parser().parse_args(sys.argv[1:])
    if args.help or args.command in (None, "-h", "--help"):
        usage_exit()

    c = Client()
    c.confirm = args.confirm
    cmd = args.command
    page, limit = args.page, args.limit

    def paging():
        return {"page": page, "limit": limit}

    if cmd == "capabilities":
        print(CAPABILITIES)
        return

    # ---------- 只读 ----------
    reads = {
        "workspaces": ("GET", "/user/my-workspace", None),
        "workspace-list": ("GET", "/system/workspace/list_all", None),
        "nodes": ("POST", "/node/list_data.json", paging()),
        "node-groups": ("GET", "/node/list_group_all.json", None),
        "builds": ("POST", "/build/list", paging()),
        "build-history": ("POST", "/build/history/history_list.json", paging()),
        "build-groups": ("GET", "/build/list_group_all", None),
        "repositories": ("POST", "/build/repository/list", paging()),
        "repository-groups": ("GET", "/build/repository/list-group", None),
        "scripts": ("POST", "/script/list", paging()),
        "scripts-all": ("GET", "/script/list-all", None),
        "script-logs": ("POST", "/script_log/list", paging()),
        "machines": ("POST", "/system/assets/machine/list-data", paging()),
        "ssh-assets": ("POST", "/system/assets/ssh/list-data", paging()),
        "ftp-assets": ("POST", "/system/assets/ftp/list-data", paging()),
        "docker-assets": ("POST", "/system/assets/docker/list-data", paging()),
        "monitors": ("POST", "/monitor/list_data.json", paging()),
        "dispatch": ("POST", "/outgiving/dispatch-list", paging()),
        "dispatch-logs": ("POST", "/outgiving/log_list_data.json", paging()),
        "backups": ("POST", "/system/backup/list", paging()),
        "clusters": ("POST", "/cluster/list", paging()),
        "users": ("POST", "/user/get_user_list", paging()),
        "permission-groups": ("POST", "/user-permission-group/get-list", paging()),
        "workspace-env": ("POST", "/system/workspace_env/list", paging()),
        "login-logs": ("POST", "/user/login-log/list-data", paging()),
        "operate-logs": ("POST", "/user/list-operate-log-data", paging()),
        "recent-logs": ("POST", "/user/recent-log-data", {"limit": limit}),
        "log-read": ("POST", "/log-read/list", paging()),
        "system-info": ("POST", "/system/info", {}),
        "check-version": ("POST", "/system/check_version.json", {}),
        "server-logs": ("POST", "/system/log_data.json", {}),
    }
    if cmd in reads:
        method, path, params = reads[cmd]
        c.request(method, path, params)
        return

    if cmd == "node-monitor":
        require(args.node_id, "缺少必填参数 --node-id（可用 capabilities 查看命令用法）")
        c.request("POST", "/node/node_monitor_data.json", {"nodeId": args.node_id})
        return
    if cmd == "node-process":
        require(args.node_id, "缺少必填参数 --node-id（可用 capabilities 查看命令用法）")
        c.request("POST", "/node/processList", {"nodeId": args.node_id})
        return
    if cmd == "node-machine-info":
        require(args.node_id, "缺少必填参数 --node-id（可用 capabilities 查看命令用法）")
        c.request("GET", "/node/machine-info", {"nodeId": args.node_id})
        return
    if cmd == "projects":
        require(args.node_id, "缺少必填参数 --node-id（可用 capabilities 查看命令用法）")
        c.request("POST", "/node/project_list", {"nodeId": args.node_id, **paging()})
        return
    if cmd == "project-info":
        require(args.node_id, "缺少必填参数 --node-id（可用 capabilities 查看命令用法）")
        params = {"nodeId": args.node_id, **paging()}
        if args.id:
            params["id"] = args.id
        c.request("POST", "/node/manage/get_project_info", params)
        return
    if cmd == "build-get":
        require(args.id, "缺少必填参数 --id（可用 capabilities 查看命令用法）")
        c.request("GET", "/build/get", {"id": args.id})
        return
    if cmd == "build-log":
        require(args.id, "缺少必填参数 --id（可用 capabilities 查看命令用法）")
        require(args.build_id, "缺少必填参数 --build-id（可用 capabilities 查看命令用法）")
        require(args.build_id.isdigit(), "--build-id 必须为正整数")
        require(str(args.line).isdigit(), "--line 必须为正整数")
        c.request(
            "POST",
            "/build/manage/get-now-log",
            {"id": args.id, "buildId": args.build_id, "line": args.line},
        )
        return
    if cmd == "branches":
        require(args.repository_id, "缺少必填参数 --repository-id（可用 capabilities 查看命令用法）")
        c.request("POST", "/build/branch-list", {"repositoryId": args.repository_id})
        return
    if cmd == "node-scripts":
        require(args.node_id, "缺少必填参数 --node-id（可用 capabilities 查看命令用法）")
        c.request("POST", "/node/script/list_all", {"nodeId": args.node_id, **paging()})
        return
    if cmd == "node-script-logs":
        require(args.node_id, "缺少必填参数 --node-id（可用 capabilities 查看命令用法）")
        c.request("POST", "/node/script_log/list", {"nodeId": args.node_id, **paging()})
        return

    # ---------- 写操作 ----------
    if cmd == "build-start":
        require(args.id, "缺少必填参数 --id（可用 capabilities 查看命令用法）")
        params = {"id": args.id}
        if args.branch_name:
            params["branchName"] = args.branch_name
        if args.remark:
            params["buildRemark"] = args.remark
        c.request("POST", "/build/manage/start", params, write=True)
        return
    if cmd == "build-cancel":
        require(args.id, "缺少必填参数 --id（可用 capabilities 查看命令用法）")
        c.request("POST", "/build/manage/cancel", {"id": args.id}, write=True)
        return
    if cmd == "build-rerelease":
        require(args.log_id, "缺少必填参数 --log-id（可用 capabilities 查看命令用法）")
        c.request("POST", "/build/manage/reRelease", {"logId": args.log_id}, write=True)
        return
    if cmd == "project-operate":
        require(args.node_id, "缺少必填参数 --node-id（可用 capabilities 查看命令用法）")
        require(args.id, "缺少必填参数 --id（可用 capabilities 查看命令用法）")
        require(args.opt, "缺少必填参数 --opt（可用 capabilities 查看命令用法）")
        require(args.opt in ("start", "stop", "restart"), "--opt 仅支持 start|stop|restart")
        c.request(
            "POST",
            "/node/manage/operate",
            {"nodeId": args.node_id, "id": args.id, "opt": args.opt},
            write=True,
        )
        return
    if cmd == "node-kill-pid":
        require(args.node_id, "缺少必填参数 --node-id（可用 capabilities 查看命令用法）")
        require(args.pid, "缺少必填参数 --pid（可用 capabilities 查看命令用法）")
        params = {"nodeId": args.node_id, "pid": args.pid}
        if args.machine_id:
            params["machineId"] = args.machine_id
        c.request("POST", "/node/kill.json", params, write=True)
        return
    if cmd == "system-upgrade":
        params = {}
        if args.machine_id:
            params["machineId"] = args.machine_id
        c.request("GET", "/system/remote_upgrade.json", params, write=True)
        return

    if cmd == "raw":
        if len(args.raw_args) < 2:
            usage_exit("raw 用法: raw <METHOD> <PATH> [--form k=v]... [--json '{}']")
        method, path = args.raw_args[0].upper(), args.raw_args[1]
        params = {}
        for item in args.form:
            require("=" in item, f"raw --form 需要 k=v 格式: {item}")
            k, v = item.split("=", 1)
            params[k] = v
        c.request(method, path, params, json_body=args.json_body or None)
        return

    print(f"未知命令: {cmd}", file=sys.stderr)
    usage_exit()


if __name__ == "__main__":
    main()
