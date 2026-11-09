#!/usr/bin/env python3
# Copyright (c) 2019 Of Him Code Technology Studio
# Jpom is licensed under Mulan PSL v2.
#
# e2e-full-driver.py — Java 插件端 vs Go 插件端全端点（含破坏性）对照驱动
#
# 由 e2e-full.sh 调用；环境变量：
#   E2E_JAVA_BASE / E2E_GO_BASE        两个实例的安装目录
#   E2E_APP_JAVA / E2E_APP_GO          两个实例的测试项目目录
#   E2E_JAVA_URL / E2E_GO_URL          两个实例的 base URL（如 http://127.0.0.1:2127）
#   E2E_RESULT_DIR                     结果输出目录
#   E2E_LANG                           请求语言（默认 zh-CN）
#   E2E_ONLY                           只跑匹配的用例名（正则，可选）
#   E2E_VERBOSE=1                      打印每个用例
#   E2E_FIXTURE_URL                    远程下载夹具服务地址（python -m http.server）
#   E2E_BACKUP_COUNT                   实例配置里的项目文件备份保留个数（断言用）
#
# 结果：<RESULT_DIR>/result.json（机器可读）、summary.md（人读摘要）、raw/*（DIFF 证据，已脱敏）
#
# 说明：
#   - 两个实例使用同一份基线 data，测试项目使用各自独立目录（互相隔离、不触碰生产）；
#   - 动态值（时间/pid/installId/uuid/实例绝对路径/备份 id 等）在比较前归一化；
#   - 用例顺序固定，破坏性操作在每个实例上按同样顺序执行，便于落盘数据对照。
import hashlib
import json
import os
import re
import shutil
import socket
import subprocess
import sys
import time
import traceback
import urllib.error
import urllib.parse
import urllib.request

LANG = os.environ.get("E2E_LANG", "zh-CN")
GO_ONLY = os.environ.get("E2E_GO_ONLY", "0") == "1"
APP_ROOT_JAVA = os.environ.get("E2E_APP_ROOT_JAVA", "")
APP_ROOT_GO = os.environ.get("E2E_APP_ROOT_GO", "")
RESULT_DIR = os.environ.get("E2E_RESULT_DIR", "./results")
ONLY = os.environ.get("E2E_ONLY", "")
VERBOSE = os.environ.get("E2E_VERBOSE", "0") == "1"
FIXTURE_URL = os.environ.get("E2E_FIXTURE_URL", "http://127.0.0.1:8999")
BACKUP_COUNT = int(os.environ.get("E2E_BACKUP_COUNT", "3"))
HOSTNAME = socket.gethostname()

TIMEOUT = 60
RESULTS = []
CTX = {}
T0 = time.time()


# ---------------------------------------------------------------- HTTP 基础
class Inst(object):
    def __init__(self, name, base, app, url, project_id, demo_jar):
        self.name = name
        self.base = os.path.realpath(base)
        self.app = os.path.realpath(app)
        self.url = url.rstrip("/")
        self.project_id = project_id
        self.demo_jar = demo_jar
        self.auth = self._load_auth()
        self.vars = {}

    def _load_auth(self):
        path = os.path.join(self.base, "data", "agent_authorize.json")
        try:
            with open(path, encoding="utf-8") as fp:
                d = json.load(fp)
            return hashlib.sha1((d["agentName"] + "@" + d["agentPwd"]).encode()).hexdigest()
        except Exception:
            return ""

    def path(self, *parts):
        return os.path.join(self.base, *parts)

    def data_path(self, *parts):
        return os.path.join(self.base, "data", *parts)


JAVA = Inst("java", os.environ.get("E2E_JAVA_BASE", ""), os.environ.get("E2E_APP_JAVA", ""),
            os.environ.get("E2E_JAVA_URL", ""), "E2EJAVA", "e2eapp-java.jar")
GO = Inst("go", os.environ.get("E2E_GO_BASE", ""), os.environ.get("E2E_APP_GO", ""),
          os.environ.get("E2E_GO_URL", ""), "E2EGO", "e2eapp-go.jar")
INSTS = [JAVA, GO]
INST = {"java": JAVA, "go": GO}

_opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))


def multipart_body(fields, files):
    """fields: dict[str,str]; files: list[(field, filename, bytes, ctype)]"""
    boundary = "----jpomE2E" + hashlib.md5(os.urandom(8)).hexdigest()
    out = []
    for k, v in (fields or {}).items():
        out.append(("--%s\r\nContent-Disposition: form-data; name=\"%s\"\r\n\r\n%s\r\n"
                    % (boundary, k, v)).encode())
    for field, filename, content, ctype in files or []:
        out.append(("--%s\r\nContent-Disposition: form-data; name=\"%s\"; filename=\"%s\"\r\n"
                    "Content-Type: %s\r\n\r\n" % (boundary, field, filename, ctype)).encode())
        out.append(content)
        out.append(b"\r\n")
    out.append(("--%s--\r\n" % boundary).encode())
    return b"".join(out), "multipart/form-data; boundary=" + boundary


def resolve_vars(inst, value):
    """把 "@KEY@" 替换为实例私有变量（脚本 id/执行 id/备份 id 等）"""
    if isinstance(value, str):
        def repl(m):
            key = m.group(1)
            if key not in inst.vars:
                raise KeyError("未捕获变量: " + key)
            return str(inst.vars[key])
        return re.sub(r"@([A-Z0-9_]+)@", repl, value)
    if isinstance(value, dict):
        return {k: resolve_vars(inst, v) for k, v in value.items()}
    if isinstance(value, list):
        return [resolve_vars(inst, v) for v in value]
    return value


def req(inst, method, path, form=None, json_body=None, files=None, headers=None,
        no_auth=False, timeout=None):
    headers = dict(headers or {})
    if not no_auth:
        headers.setdefault("Jpom-Agent-Authorize", inst.auth)
    headers.setdefault("Accept-Language", LANG)
    headers.setdefault("Jpom-Server-UserName", "e2e-user")
    headers.setdefault("workspaceId", "DEFAULT")
    headers.setdefault("transport-encryption", "0")
    data = None
    url = inst.url + path
    form = resolve_vars(inst, form or {})
    if files is not None:
        fields = {k: str(v) for k, v in form.items()}
        data, ctype = multipart_body(fields, files)
        headers.setdefault("Content-Type", ctype)
    elif json_body is not None:
        json_body = resolve_vars(inst, json_body)
        data = json.dumps(json_body, ensure_ascii=False).encode()
        headers.setdefault("Content-Type", "application/json")
    elif method == "GET":
        if form:
            url += "?" + urllib.parse.urlencode(form)
    else:
        data = urllib.parse.urlencode(form).encode()
        headers.setdefault("Content-Type", "application/x-www-form-urlencoded")
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    started = time.time()
    try:
        with _opener.open(request, timeout=timeout or TIMEOUT) as resp:
            body = resp.read()
            status = resp.status
            ctype = resp.headers.get("Content-Type")
    except urllib.error.HTTPError as e:
        body = e.read()
        status = e.code
        ctype = e.headers.get("Content-Type") if e.headers else None
    except Exception as e:  # 连接失败/超时
        return {"status": None, "ctype": None, "body": ("ERR:" + repr(e)).encode(),
                "json": None, "error": repr(e), "elapsed_ms": int((time.time() - started) * 1000)}
    parsed = None
    try:
        parsed = json.loads(body.decode("utf-8"))
    except Exception:
        parsed = None
    return {"status": status, "ctype": ctype, "body": body, "json": parsed,
            "elapsed_ms": int((time.time() - started) * 1000)}


def norm_str(inst, text):
    if text is None:
        return None
    s = str(text)
    for tok, repl in (
            (inst.base, "<BASE>"),
            (inst.app, "<APP>"),
            (os.path.dirname(inst.app), "<E2E_ROOT>"),
            (exec_user_home(), "<HOME>"),
            (HOSTNAME, "<HOST>"),
            (inst.project_id, "<PID>"),
            (inst.demo_jar, "<DEMO_JAR>"),
    ):
        if tok:
            s = s.replace(tok, repl)
    s = re.sub(r"[0-9a-fA-F]{32}", "<UUID>", s)
    s = re.sub(r"\b\d{17}\b", "<BACKUP_ID>", s)
    s = re.sub(r"\b\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}:\d{2}(?:\.\d+)?\b", "<TIME>", s)
    s = re.sub(r"\b\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?(?:Z|[+-]\d{2}:\d{2})", "<TIME>", s)
    s = re.sub(r"(?m)^(\s*port:\s*)\d+", r"\1<PORT>", s)
    s = re.sub(r"127\.0\.0\.1:\d+", "<LOCAL_URL>", s)
    return s


def exec_user_home():
    try:
        return os.path.expanduser("~")
    except Exception:
        return ""


IGNORE_VALUE_KEYS = {
    "pid", "pids", "installId", "installTime", "createTime", "modifyTime", "dateTime",
    "time", "timestamp", "startTime", "endTime", "endTimestamp", "startTimestamp",
    "lastModified", "lastModifiedTime", "modified", "fileSize", "dataSize", "oldJarsSize",
    "size", "length", "totalSize", "readFileOnLineCount", "scriptExecLogSize", "pidPort",
    "upTime", "jvmStartTime", "uploadTime", "expireTime", "freeSpace", "totalSpace",
    "usedSpace", "available", "free", "used", "total", "rxBytes", "txBytes", "rx", "tx",
    "rxPackets", "txPackets", "cpuLoad", "loadAverage", "dateTimeStr", "loggerSize",
    "lastTime", "firstTime", "refreshTime", "uploadTime", "downloadTime",
}


def norm_obj(inst, obj, key=None):
    """把对象里的动态值归一化，供跨实例比较"""
    if isinstance(obj, dict):
        out = {}
        for k, v in obj.items():
            nk = norm_str(inst, k) if isinstance(k, str) else k
            out[nk] = norm_obj(inst, v, k)
        return out
    if isinstance(obj, list):
        return [norm_obj(inst, v, key) for v in obj]
    if key in IGNORE_VALUE_KEYS:
        if isinstance(obj, bool):
            return "<DYNAMIC_BOOL>"
        if isinstance(obj, (int, float)):
            return "<DYNAMIC_NUM>"
        if obj is None:
            return None
        return "<DYNAMIC_STR>"
    if isinstance(obj, str):
        return norm_str(inst, obj)
    return obj


def json_shape(obj, prefix="$"):
    out = []
    if isinstance(obj, dict):
        out.append(prefix + ":object")
        for k in sorted(obj):
            out.extend(json_shape(obj[k], prefix + "." + k))
    elif isinstance(obj, list):
        out.append(prefix + ":array")
        if obj:
            out.extend(json_shape(obj[0], prefix + "[]"))
    elif isinstance(obj, bool):
        out.append(prefix + ":boolean")
    elif isinstance(obj, (int, float)):
        out.append(prefix + ":number")
    elif obj is None:
        out.append(prefix + ":null")
    else:
        out.append(prefix + ":string")
    return out


def short(value, limit=500):
    if isinstance(value, str):
        return value[:limit]
    try:
        text = json.dumps(value, ensure_ascii=False, sort_keys=True)
    except Exception:
        text = repr(value)
    return text[:limit]


def sanitize_summary(inst, resp):
    """生成用于报告/落盘的脱敏响应摘要"""
    out = {"status": resp.get("status"), "contentType": resp.get("ctype"),
           "elapsedMs": resp.get("elapsed_ms")}
    if resp.get("error"):
        out["error"] = norm_str(inst, resp["error"])
    body = resp.get("body") or b""
    parsed = resp.get("json")
    if isinstance(parsed, dict):
        out["code"] = parsed.get("code")
        out["msg"] = norm_str(inst, parsed.get("msg"))
        if "data" in parsed:
            data = parsed["data"]
            out["dataKeys"] = sorted(data.keys()) if isinstance(data, dict) else (
                "array[%d]" % len(data) if isinstance(data, list) else type(data).__name__)
            out["data"] = norm_str(inst, short(norm_obj(inst, data), 700))
    else:
        out["contentLength"] = len(body)
        try:
            text = body.decode("utf-8")
            out["bodyPreview"] = norm_str(inst, text[:300])
        except Exception:
            out["bodySha1"] = hashlib.sha1(body).hexdigest()
    return out


# ---------------------------------------------------------------- 比较
class Cmp(object):
    """单用例比较结果"""

    def __init__(self, name):
        self.name = name
        self.diffs = []
        self.notes = []
        self.fail = None

    def add(self, kind, path, java, go):
        self.diffs.append({"type": kind, "path": path, "java": java, "go": go})

    def note(self, text):
        self.notes.append(text)


def compare_json(inst_j, inst_g, jv, gv, cmp, path="", ignore=()):
    if path in ignore:
        return
    if type(jv) is not type(gv) and not (isinstance(jv, (int, float)) and isinstance(gv, (int, float))):
        cmp.add("type", path or "$", type(jv).__name__, type(gv).__name__)
        return
    if isinstance(jv, dict):
        jk, gk = set(jv), set(gv)
        if jk - gk:
            cmp.add("data-missing", path or "$", sorted(jk - gk)[:12], "<absent>")
        if gk - jk:
            cmp.add("data-extra", path or "$", "<absent>", sorted(gk - jk)[:12])
        for k in sorted(jk & gk):
            compare_json(inst_j, inst_g, jv[k], gv[k], cmp, path + "." + k if path else k, ignore)
    elif isinstance(jv, list):
        if len(jv) != len(gv):
            cmp.add("array-length", path or "$", len(jv), len(gv))
        for i, (a, b) in enumerate(zip(jv, gv)):
            compare_json(inst_j, inst_g, a, b, cmp, "%s[%d]" % (path or "$", i), ignore)
    else:
        if isinstance(jv, str):
            nj, ng = norm_str(inst_j, jv), norm_str(inst_g, gv)
        else:
            nj, ng = jv, gv
        if nj != ng:
            cmp.add("value", path or "$", short(nj), short(ng))


def apply_expect(cmp, expect):
    """把“设计上允许的差异”从 diffs 移到 notes（如 Go 独有的 manifest.engine、实例路径/端口等）"""
    if not expect:
        return cmp
    remain = []
    for d in cmp.diffs:
        blob = str(d.get("path", "")) + str(d.get("java", "")) + str(d.get("go", ""))
        if any(tok in blob for tok in expect):
            cmp.note("expected-diff %s %s: java=%s go=%s" % (d["type"], d["path"],
                                                             short(d["java"], 120), short(d["go"], 120)))
        else:
            remain.append(d)
    cmp.diffs = remain
    return cmp


def compare_responses(name, jresp, gresp, method, path, volatile=False, ignore=("data.content",),
                      msg_prefix_ok=False, strict_bin=False, cmp=None, expect=None):
    cmp = cmp or Cmp(name)
    if jresp.get("status") is None or gresp.get("status") is None:
        cmp.fail = "请求异常: java=%s go=%s" % (jresp.get("error") or jresp.get("status"),
                                               gresp.get("error") or gresp.get("status"))
        return cmp
    if jresp["status"] != gresp["status"]:
        cmp.add("http-status", "status", jresp["status"], gresp["status"])
    jc = (jresp.get("ctype") or "").split(";")[0].strip()
    gc = (gresp.get("ctype") or "").split(";")[0].strip()
    if jc != gc:
        cmp.note("content-type: java=%s go=%s" % (jc, gc))
    jj, gj = jresp.get("json"), gresp.get("json")
    if isinstance(jj, dict) and isinstance(gj, dict):
        if jj.get("code") != gj.get("code"):
            cmp.add("code", "code", jj.get("code"), gj.get("code"))
        jm, gm = jj.get("msg"), gj.get("msg")
        if norm_str(JAVA, jm) != norm_str(GO, gm):
            if msg_prefix_ok and jm and gm and norm_str(JAVA, jm).split("：")[0] == norm_str(GO, gm).split("：")[0]:
                cmp.note("msg 明细不同（前缀一致，允许）: java=%s go=%s" % (short(norm_str(JAVA, jm), 180),
                                                                    short(norm_str(GO, gm), 180)))
            else:
                cmp.add("msg", "msg", norm_str(JAVA, jm), norm_str(GO, gm))
        sj, sg = set(json_shape(jj.get("data"))), set(json_shape(gj.get("data")))
        if sj - sg:
            cmp.add("data-missing", "data", sorted(sj - sg)[:16], "<absent>")
        if sg - sj:
            cmp.add("data-extra", "data", "<absent>", sorted(sg - sj)[:16])
        if not volatile:
            compare_json(JAVA, GO, norm_obj(JAVA, jj.get("data")), norm_obj(GO, gj.get("data")),
                         cmp, "data", ignore)
    elif jj is None and gj is None:
        jb, gb = jresp.get("body") or b"", gresp.get("body") or b""
        if strict_bin:
            if hashlib.sha1(jb).hexdigest() != hashlib.sha1(gb).hexdigest():
                cmp.add("binary", "body", "sha1:" + hashlib.sha1(jb).hexdigest()[:12],
                        "sha1:" + hashlib.sha1(gb).hexdigest()[:12])
        else:
            cmp.note("binary body: java=%d bytes go=%d bytes" % (len(jb), len(gb)))
    else:
        cmp.add("body-kind", "body", "json" if jj is not None else "raw",
                "json" if gj is not None else "raw")
    return apply_expect(cmp, expect)


def record_cmp(name, group, method, path, cmp, jresp, gresp, note="", tags=None):
    if cmp is None:
        cmp = Cmp(name)
    result = "PASS"
    if cmp.fail:
        result = "FAIL"
    elif cmp.diffs:
        result = "DIFF"
    entry = {
        "name": name, "group": group, "method": method, "path": path,
        "result": result, "diffs": cmp.diffs, "notes": cmp.notes, "note": note,
        "tags": tags or [],
        "java": sanitize_summary(JAVA, jresp), "go": sanitize_summary(GO, gresp),
    }
    if entry["note"]:
        entry["notes"].append(entry["note"])
    RESULTS.append(entry)
    if VERBOSE or result != "PASS":
        print("[%s] %-46s %s %s" % (result, name, method, path), file=sys.stderr)
        for d in cmp.diffs[:6]:
            print("        %s %s: java=%s go=%s" % (d["type"], d["path"], d["java"], d["go"]), file=sys.stderr)
    return entry


def skip_case(name, group, path, reason):
    entry = {"name": name, "group": group, "method": "-", "path": path, "result": "SKIP",
             "diffs": [], "notes": [reason], "note": reason, "tags": ["skip"],
             "java": {}, "go": {}}
    RESULTS.append(entry)
    if VERBOSE:
        print("[SKIP] %-46s %s" % (name, reason), file=sys.stderr)
    return entry


def run_case(name, group, method, path, form=None, json_body=None, files=None, headers=None,
             no_auth=False, volatile=False, ignore=("data.content",), msg_prefix_ok=False,
             strict_bin=False, note="", tags=None, post=None, custom=None, timeout=None,
             expect=None):
    """在两个实例上执行同一请求并比较；custom 可完全接管比较"""
    if ONLY and not re.search(ONLY, name):
        return None
    if custom is not None:
        jresp, gresp, cmp = custom()
        return record_cmp(name, group, method, path, cmp, jresp, gresp, note, tags)
    jresp = req(JAVA, method, path, form=form, json_body=json_body, files=files,
                headers=headers, no_auth=no_auth, timeout=timeout)
    gresp = req(GO, method, path, form=form, json_body=json_body, files=files,
                headers=headers, no_auth=no_auth, timeout=timeout)
    if post:
        post(jresp, gresp)
    cmp = compare_responses(name, jresp, gresp, method, path, volatile=volatile, ignore=ignore,
                            msg_prefix_ok=msg_prefix_ok, strict_bin=strict_bin, expect=expect)
    return record_cmp(name, group, method, path, cmp, jresp, gresp, note, tags)


def run_state(name, group, fn, note="", ignore=("data.content",), volatile=False):
    if ONLY and not re.search(ONLY, name):
        return None
    try:
        jv = norm_obj(JAVA, fn(JAVA))
        gv = norm_obj(GO, fn(GO))
    except Exception as e:
        cmp = Cmp(name)
        cmp.fail = "快照/读取失败: %r" % (e,)
        traceback.print_exc()
        return record_cmp(name, group, "STATE", "-", cmp, {}, {})
    cmp = Cmp(name)
    sj, sg = set(json_shape(jv, "$")), set(json_shape(gv, "$"))
    if sj - sg:
        cmp.add("state-missing", "$", sorted(sj - sg)[:16], "<absent>")
    if sg - sj:
        cmp.add("state-extra", "$", "<absent>", sorted(sg - sj)[:16])
    if not volatile:
        compare_json(JAVA, GO, jv, gv, cmp, "$", ignore)
    jresp = {"status": 200, "ctype": "application/json", "json": {"code": 200, "msg": "", "data": jv}, "body": b""}
    gresp = {"status": 200, "ctype": "application/json", "json": {"code": 200, "msg": "", "data": gv}, "body": b""}
    return record_cmp(name, group, "STATE", "-", cmp, jresp, gresp, note)


# ---------------------------------------------------------------- 落盘快照
def load_json(path):
    with open(path, encoding="utf-8") as fp:
        return json.load(fp)


def snapshot_data(inst):
    out = {}
    for fname in ("project.json", "script.json", "script_log.json",
                  "workspace_env_var.json", "whitelistDirectory.json"):
        path = inst.data_path(fname)
        out[fname] = load_json(path) if os.path.isfile(path) else None
    install = inst.data_path("INSTALL.json")
    out["INSTALL.json"] = {"keys": sorted(load_json(install).keys())} if os.path.isfile(install) else None
    gs = {}
    gs_dir = inst.data_path("global-script")
    if os.path.isdir(gs_dir):
        for fname in sorted(os.listdir(gs_dir)):
            p = os.path.join(gs_dir, fname)
            if os.path.isfile(p):
                gs[fname] = load_json(p)
    out["global-script"] = gs
    data_dir = inst.data_path()
    out["data-top-level"] = sorted(os.listdir(data_dir)) if os.path.isdir(data_dir) else []
    out["remote_version.present"] = os.path.isfile(inst.data_path("remote_version.json"))
    return out


def tree_snapshot(root, inst, normalize_names=True):
    """返回 {相对路径: 'dir'|sha1}；名字里的实例特征做归一化"""
    result = {}
    if not os.path.isdir(root):
        return {"__absent__": True}
    for cur, dirs, files in os.walk(root):
        dirs.sort()
        rel_dir = os.path.relpath(cur, root)
        if rel_dir != ".":
            name = rel_dir if not normalize_names else norm_str(inst, rel_dir)
            name = name.replace(inst.demo_jar, "<DEMO_JAR>")
            result[name.rstrip("/") + "/"] = "dir"
        for f in sorted(files):
            full = os.path.join(cur, f)
            rel = os.path.relpath(full, root)
            name = norm_str(inst, rel).replace(inst.demo_jar, "<DEMO_JAR>")
            if name.lower().endswith((".zip", ".tar", ".gz", ".7z", ".rar", ".log")):
                result[name] = "archive"
                continue
            try:
                with open(full, "rb") as fp:
                    result[name] = hashlib.sha1(fp.read()).hexdigest()
            except Exception:
                result[name] = "unreadable"
    return result


def snapshot_app(inst):
    return tree_snapshot(inst.app, inst)


def snapshot_backup(inst):
    return tree_snapshot(inst.data_path("project_file_backup"), inst)


def snapshot_logs(inst):
    return tree_snapshot(inst.path("logs"), inst, normalize_names=False)


def snapshot_project_logs(inst):
    pid = inst.vars.get("project_id", inst.project_id)
    root = inst.path("project-log")
    return tree_snapshot(root, inst, normalize_names=True)


def wait_file(path, timeout=25):
    end = time.time() + timeout
    while time.time() < end:
        if os.path.exists(path) and os.path.getsize(path) >= 0:
            if os.path.isfile(path) and os.path.getsize(path) > 0:
                return True
        time.sleep(0.2)
    return False


def append_padding(inst, project_id, size=120 * 1024):
    """给项目控制台日志追加固定内容，超过 Java 的 100KB 备份阈值"""
    log_dir = inst.path("project-log", project_id)
    os.makedirs(log_dir, exist_ok=True)
    log_file = os.path.join(log_dir, project_id + ".log")
    line = "e2e-padding-line-0123456789-abcdefghijklmnopqrstuvwxyz\n"
    need = max(0, size - (os.path.getsize(log_file) if os.path.exists(log_file) else 0))
    with open(log_file, "a", encoding="utf-8") as fp:
        fp.write(line * (need // len(line) + 1))


def spawn_sleep():
    p = subprocess.Popen(["sleep", "300"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    return p


# ---------------------------------------------------------------- 组合用例辅助
def both(method, path, **kw):
    return req(JAVA, method, path, **kw), req(GO, method, path, **kw)


def run_custom(name, group, method, path, fn, note=""):
    if ONLY and not re.search(ONLY, name):
        return None
    try:
        result = fn()
    except Exception as e:
        traceback.print_exc()
        cmp = Cmp(name)
        cmp.fail = "自定义用例异常: %r" % (e,)
        return record_cmp(name, group, method, path, cmp, {}, {}, note)
    jresp, gresp, cmp = result
    if cmp is None:
        cmp = Cmp(name)
    return record_cmp(name, group, method, path, cmp, jresp, gresp, note)


def get_json(resp):
    j = resp.get("json")
    return j if isinstance(j, dict) else {}


# ================================================================ 只读组
def group_read():
    # ---- 索引 / 系统信息
    run_case("GET /(index)", "只读", "GET", "/", strict_bin=True, note="纯文本索引页")
    run_case("GET /index.html", "只读", "GET", "/index.html", strict_bin=True)

    INFO_EXPECT = ["engine", "lastTime", "manifest.port", "manifest.jarFile", "manifest.version"]

    def info_both():
        jr, gr = both("POST", "/info")
        try:
            mj = get_json(jr).get("data", {}).get("manifest", {})
            mg = get_json(gr).get("data", {}).get("manifest", {})
            CTX["manifest_java"] = {k: mj.get(k) for k in ("version", "buildTime", "debug", "engine")}
            CTX["manifest_go"] = {k: mg.get(k) for k in ("version", "buildTime", "debug", "engine")}
        except Exception:
            pass
        cmp = compare_responses("info", jr, gr, "POST", "/info", ignore=(),
                                expect=INFO_EXPECT)
        return jr, gr, cmp
    run_custom("info", "只读", "POST", "/info", info_both,
               note="installId/pid/端口/路径为设计内差异；pluginSize 为已知差异（G8）")

    run_case("get-stat-info", "只读", "POST", "/get-stat-info", volatile=True,
             expect=["engine"], note="系统负载实时变化；Go manifest 多 engine 字段（Server 侧设计需要）")
    run_case("processList", "只读", "POST", "/processList",
             form={"processName": "java", "count": "5"}, volatile=True, note="进程列表实时变化")
    run_case("disk-info", "只读", "POST", "/disk-info", volatile=True)
    run_case("hw-disk--info", "只读", "POST", "/hw-disk--info", volatile=True)
    run_case("network-interfaces", "只读", "POST", "/network-interfaces", volatile=True)

    # ---- 白名单 / 系统配置 / 日志 / 缓存 / 版本
    run_case("whitelistDirectory_data", "只读", "POST", "/system/whitelistDirectory_data")
    run_case("getConfig.json", "只读", "POST", "/system/getConfig.json",
             ignore=("data.content", "data.file"), note="配置内容按实例归一化后比较")
    run_case("log_data.json", "只读", "POST", "/system/log_data.json", volatile=True)
    run_case("cache", "只读", "POST", "/system/cache", volatile=True)
    run_case("check_version.json", "只读", "POST", "/system/check_version.json")
    run_case("change_log", "只读", "POST", "/system/change_log")

    # ---- 基线项目数据（只读）
    run_case("getProjectInfo", "只读", "POST", "/manage/getProjectInfo")
    run_case("getProjectItem", "只读", "POST", "/manage/getProjectItem", form={"id": "6KA03M"})
    run_case("getProjectItem-miss", "只读", "POST", "/manage/getProjectItem", form={"id": "NOT_EXIST"})
    run_case("getProjectStatus", "只读", "POST", "/manage/getProjectStatus", form={"id": "6KA03M"})
    run_case("getProjectPort", "只读", "POST", "/manage/getProjectPort",
             form={"ids": "[\"6KA03M\",\"NED151\"]"})
    run_case("logSize", "只读", "POST", "/manage/log/logSize", form={"id": "6KA03M"}, volatile=True)
    run_case("logBack", "只读", "POST", "/manage/log/logBack", form={"id": "6KA03M"}, volatile=True)
    run_case("file/getFileList", "只读", "POST", "/manage/file/getFileList",
             form={"id": "6KA03M", "path": "/java"}, volatile=True)
    run_case("file/list-backup", "只读", "POST", "/manage/file/list-backup", form={"id": "6KA03M"}, volatile=True)
    run_case("file/diff_file(empty)", "只读", "POST", "/manage/file/diff_file",
             json_body={"id": "6KA03M", "data": [], "dir": "/"})
    run_case("file/read_file-miss", "只读", "POST", "/manage/file/read_file",
             form={"id": "6KA03M", "filePath": "/", "filename": "no-such-file.txt"})

    # ---- 脚本 / 脚本库（只读）
    run_case("script/list.json", "只读", "POST", "/script/list.json")
    run_case("script/item.json", "只读", "POST", "/script/item.json",
             form={"id": "e00c6b77175c4e37988bc947892f8f61"})
    run_case("script/log-miss", "只读", "POST", "/script/log",
             form={"id": "e00c6b77175c4e37988bc947892f8f61", "executeId": "not-exist", "line": "1"})
    run_case("script/pull_exec_log", "只读", "POST", "/script/pull_exec_log", form={"pullCount": "5"})
    run_case("script-library/list", "只读", "POST", "/script-library/list")
    run_case("script-library/get", "只读", "POST", "/script-library/get", form={"id": "test"})

    # ---- 错误分支
    run_case("unknown-path", "只读", "POST", "/no-such-endpoint")
    run_case("unauthorized", "只读", "POST", "/info", no_auth=True, note="缺少鉴权头")
    run_case("wrong-method GET /info", "只读", "GET", "/info")
    run_case("not-implemented /exportTop", "只读", "POST", "/exportTop")
    run_case("not-implemented /manage/recover/list_data", "只读", "POST", "/manage/recover/list_data")


# ================================================================ 项目/文件/备份/日志组
def project_state(inst):
    return {
        "project.json": load_json(inst.data_path("project.json")),
        "backup-tree": snapshot_backup(inst),
        "script.json": load_json(inst.data_path("script.json")),
    }


def group_project():
    baseline_whitelist = load_json(JAVA.data_path("whitelistDirectory.json"))
    suffix_json = json.dumps(baseline_whitelist.get("allowEditSuffix", []), ensure_ascii=False)

    # ---- 白名单：授权独立测试项目目录
    run_case("whitelistDirectory_submit", "项目", "POST", "/system/whitelistDirectory_submit",
             form={"project": "[\"@APP_PATH@\"]", "allowEditSuffix": suffix_json},
             note="两实例各自授权自己的独立测试目录")
    run_case("whitelistDirectory_data(after)", "项目", "POST", "/system/whitelistDirectory_data")
    run_case("whitelist-directory-submit-invalid", "项目", "POST", "/system/whitelistDirectory_submit",
             form={"project": "[\"@APP_PATH@\"]", "allowEditSuffix": ""},
             note="可编辑后缀为空应报错")

    # ---- 保存项目（预检查 + 正式保存）
    project_form = {
        "id": "@PROJECT_ID@", "name": "e2eapp", "runMode": "Jar",
        "whitelistDirectory": "@APP_PATH@", "lib": "", "logPath": "",
        "autoStart": "false", "args": "e2e-arg", "mainClass": "",
    }
    preview_form = dict(project_form)
    preview_form["previewData"] = "true"
    run_case("saveProject(preview)", "项目", "POST", "/manage/saveProject", form=preview_form,
             note="预检查不落库")
    run_case("getProjectItem(preview 后)", "项目", "POST", "/manage/getProjectItem",
             form={"id": "@PROJECT_ID@"})
    run_case("saveProject", "项目", "POST", "/manage/saveProject", form=project_form)
    run_case("getProjectItem(new)", "项目", "POST", "/manage/getProjectItem",
             form={"id": "@PROJECT_ID@"})
    run_case("getProjectInfo(with new)", "项目", "POST", "/manage/getProjectInfo")
    run_state("state:data-after-saveProject", "状态", snapshot_data)

    # ---- 项目文件：读 / 写 / 复制 / 新建 / 重命名 / 压缩 / 远程下载 / 分片上传
    run_case("file/getFileList(new)", "文件", "POST", "/manage/file/getFileList",
             form={"id": "@PROJECT_ID@", "path": "/"})
    run_case("file/getFileList(sub)", "文件", "POST", "/manage/file/getFileList",
             form={"id": "@PROJECT_ID@", "path": "/sub"})
    run_case("file/read_file", "文件", "POST", "/manage/file/read_file",
             form={"id": "@PROJECT_ID@", "filePath": "/", "filename": "config.txt"})
    run_case("file/read_file(disallowed suffix)", "文件", "POST", "/manage/file/read_file",
             form={"id": "@PROJECT_ID@", "filePath": "/", "filename": "@DEMO_JAR@"},
             note="jar 不在 allowEditSuffix，应 405")
    run_case("file/update_config_file", "文件", "POST", "/manage/file/update_config_file",
             form={"id": "@PROJECT_ID@", "filePath": "/", "filename": "config.txt",
                   "fileText": "hello e2e updated\n"})
    run_case("file/read_file(updated)", "文件", "POST", "/manage/file/read_file",
             form={"id": "@PROJECT_ID@", "filePath": "/", "filename": "config.txt"})
    run_case("file/copy", "文件", "POST", "/manage/file/copy",
             form={"id": "@PROJECT_ID@", "filePath": "/", "filename": "config.txt"})
    run_case("file/getFileList(after copy)", "文件", "POST", "/manage/file/getFileList",
             form={"id": "@PROJECT_ID@", "path": "/"})
    run_case("file/new_file_folder.json", "文件", "POST", "/manage/file/new_file_folder.json",
             form={"id": "@PROJECT_ID@", "levelName": "", "filename": "e2e-folder"})
    run_case("file/new_file_folder.json(exists)", "文件", "POST", "/manage/file/new_file_folder.json",
             form={"id": "@PROJECT_ID@", "levelName": "", "filename": "e2e-folder"},
             note="重复创建应报错")
    run_case("file/rename.json", "文件", "POST", "/manage/file/rename.json",
             form={"id": "@PROJECT_ID@", "levelName": "", "filename": "e2e-folder",
                   "newname": "e2e-folder-renamed"})
    run_case("file/rename.json(miss)", "文件", "POST", "/manage/file/rename.json",
             form={"id": "@PROJECT_ID@", "levelName": "", "filename": "no-such",
                   "newname": "x"})
    run_case("file/compress", "文件", "POST", "/manage/file/compress",
             form={"id": "@PROJECT_ID@", "filePath": "/", "filename": "sub", "type": "zip"})
    run_case("file/compress(invalid type)", "文件", "POST", "/manage/file/compress",
             form={"id": "@PROJECT_ID@", "filePath": "/", "filename": "sub", "type": "rar"})
    run_case("file/getFileList(after rename/compress)", "文件", "POST", "/manage/file/getFileList",
             form={"id": "@PROJECT_ID@", "path": "/"})
    run_case("file/remote_download", "文件", "POST", "/manage/file/remote_download",
             form={"id": "@PROJECT_ID@", "url": FIXTURE_URL + "/remote.txt", "levelName": ""},
             note="从本地夹具 HTTP 下载，不依赖外网")
    run_case("file/read_file(remote)", "文件", "POST", "/manage/file/read_file",
             form={"id": "@PROJECT_ID@", "filePath": "/", "filename": "remote.txt"})

    upload_bytes = b"upload fixture content for e2e\n"
    upload_md5 = hashlib.md5(upload_bytes).hexdigest()
    half = len(upload_bytes) // 2
    slices = [upload_bytes[:half], upload_bytes[half:]]
    slice_id = "e2e-slice-1"

    def upload_step():
        jr = gr = None
        for inst_idx, inst in enumerate(INSTS):
            for now, chunk in enumerate(slices):
                r = req(inst, "POST", "/manage/file2/upload-sharding",
                        form={"sliceId": slice_id, "totalSlice": str(len(slices)),
                              "nowSlice": str(now), "fileSumMd5": upload_md5},
                        files=[("file", "e2e-upload.txt.%d" % now, chunk, "application/octet-stream")])
                if inst_idx == 0:
                    jr = r
                else:
                    gr = r
        return jr, gr, None
    run_custom("file2/upload-sharding", "文件", "POST", "/manage/file2/upload-sharding", upload_step)

    run_case("file2/sharding-merge", "文件", "POST", "/manage/file2/sharding-merge",
             form={"type": "file", "path": "@APP_PATH@", "sliceId": slice_id,
                   "totalSlice": str(len(slices)), "fileSumMd5": upload_md5})
    run_case("file/getFileList(after upload)", "文件", "POST", "/manage/file/getFileList",
             form={"id": "@PROJECT_ID@", "path": "/"})
    run_case("file2/sharding-merge(bad md5)", "文件", "POST", "/manage/file2/sharding-merge",
             form={"type": "file", "path": "@APP_PATH@", "sliceId": "no-such-slice",
                   "totalSlice": "1", "fileSumMd5": "00000000000000000000000000000000"})

    run_case("file/deleteFile", "文件", "POST", "/manage/file/deleteFile",
             form={"id": "@PROJECT_ID@", "filename": "remote.txt"})
    run_case("file/deleteFile(miss)", "文件", "POST", "/manage/file/deleteFile",
             form={"id": "@PROJECT_ID@", "filename": "remote.txt"})
    run_case("file/batch_delete", "文件", "POST", "/manage/file/batch_delete",
             json_body={"id": "@PROJECT_ID@", "dir": "/",
                        "data": [{"name": "config(1).txt"}, {"name": "upload.txt"}]})
    run_case("file/batch_delete(empty)", "文件", "POST", "/manage/file/batch_delete",
             json_body={"id": "@PROJECT_ID@", "dir": "/", "data": []})
    run_case("file/diff_file", "文件", "POST", "/manage/file/diff_file",
             json_body={"id": "@PROJECT_ID@", "dir": "/",
                        "data": [{"name": "config.txt", "sha1": "deadbeef"}]})
    run_state("state:app-tree-after-fileops", "状态", snapshot_app)
    time.sleep(1.5)
    run_state("state:data-after-fileops", "状态", lambda inst: {
        "project.json": load_json(inst.data_path("project.json")),
        "backup-tree": snapshot_backup(inst),
    })

    # ---- 文件备份：create（由文件写操作自动产生）/ list / item / download / recover / delete
    def list_backups():
        jr, gr = both("POST", "/manage/file/list-backup", form={"id": "@PROJECT_ID@"})
        for inst, r in ((JAVA, jr), (GO, gr)):
            data = get_json(r).get("data") or {}
            items = data.get("list") or []
            backup_id = None
            for item in items:
                for key in ("name", "filename", "id", "backupId", "path"):
                    v = item.get(key)
                    if isinstance(v, str) and re.fullmatch(r"\d{17}", v.strip("/")):
                        backup_id = v.strip("/")
                        break
                if backup_id:
                    break
            if backup_id is None and items:
                for key in ("name", "filename", "id"):
                    v = items[0].get(key)
                    if isinstance(v, str):
                        backup_id = v
                        break
            inst.vars["BACKUP_ID"] = backup_id or "no-backup-id"
        return jr, gr, None
    run_custom("file/list-backup", "备份", "POST", "/manage/file/list-backup", list_backups,
               note="文件写操作已自动创建备份（file-backup-count 已设为 3）")
    run_case("file/backup-item-files", "备份", "POST", "/manage/file/backup-item-files",
             form={"id": "@PROJECT_ID@", "backupId": "@BACKUP_ID@", "path": "/"})
    run_case("file/backup-download", "备份", "GET", "/manage/file/backup-download",
             form={"id": "@PROJECT_ID@", "backupId": "@BACKUP_ID@", "filename": "config.txt"},
             strict_bin=True, note="备份内 config.txt 内容应完全一致")
    run_case("file/backup-recover", "备份", "POST", "/manage/file/backup-recover",
             form={"id": "@PROJECT_ID@", "backupId": "@BACKUP_ID@", "filename": "config.txt"})
    run_case("file/read_file(recovered)", "备份", "POST", "/manage/file/read_file",
             form={"id": "@PROJECT_ID@", "filePath": "/", "filename": "config.txt"})
    run_case("file/backup-delete", "备份", "POST", "/manage/file/backup-delete",
             form={"id": "@PROJECT_ID@", "backupId": "@BACKUP_ID@", "filename": "config.txt"})
    run_case("file/backup-item-files(after delete)", "备份", "POST", "/manage/file/backup-item-files",
             form={"id": "@PROJECT_ID@", "backupId": "@BACKUP_ID@", "path": "/"})
    run_case("file/backup-recover(miss)", "备份", "POST", "/manage/file/backup-recover",
             form={"id": "@PROJECT_ID@", "backupId": "no-such-backup", "filename": "x"})
    run_case("file/backup-download(miss)", "备份", "GET", "/manage/file/backup-download",
             form={"id": "@PROJECT_ID@", "backupId": "no-such-backup", "filename": "x"})

    # ---- 项目启停 + 控制台日志
    run_case("operate(start)", "项目日志", "POST", "/manage/operate",
             form={"id": "@PROJECT_ID@", "opt": "start"}, timeout=90)
    run_case("getProjectStatus(running)", "项目日志", "POST", "/manage/getProjectStatus",
             form={"id": "@PROJECT_ID@"})
    run_case("logSize(running)", "项目日志", "POST", "/manage/log/logSize",
             form={"id": "@PROJECT_ID@"}, volatile=True)

    def pad_step():
        for inst in INSTS:
            append_padding(inst, inst.vars["PROJECT_ID"])
            time.sleep(0.3)
        jr, gr = both("POST", "/manage/log/logBack", form={"id": "@PROJECT_ID@"})
        return jr, gr, None
    run_custom("logBack(before reset)", "项目日志", "POST", "/manage/log/logBack", pad_step)

    def reset_and_capture():
        jr, gr = both("POST", "/manage/log/resetLog", form={"id": "@PROJECT_ID@"})
        time.sleep(0.5)
        lj, lg = both("POST", "/manage/log/logBack", form={"id": "@PROJECT_ID@"})
        for inst, r in ((JAVA, lj), (GO, lg)):
            arr = ((get_json(r).get("data") or {}).get("array")) or []
            names = sorted([it.get("filename") or it.get("name") or "" for it in arr if isinstance(it, dict)])
            inst.vars["LOGBACK_KEY"] = names[-1] if names and names[-1] else "no-logback-file"
        cmp = compare_responses("logBack(after reset)", lj, lg, "POST", "/manage/log/logBack", volatile=True)
        record_cmp("logBack(after reset)", "项目日志", "POST", "/manage/log/logBack", cmp, lj, lg)
        return jr, gr, None
    run_custom("resetLog", "项目日志", "POST", "/manage/log/resetLog", reset_and_capture,
               note="先向控制台日志追加超过 100KB 内容再重置（触发日志备份）")

    run_case("logBack_download", "项目日志", "GET", "/manage/log/logBack_download",
             form={"id": "@PROJECT_ID@", "key": "@LOGBACK_KEY@"}, volatile=True,
             note="备份日志内容含运行输出，仅比较状态/类型/大小级别")
    run_case("export", "项目日志", "GET", "/manage/log/export", form={"id": "@PROJECT_ID@"},
             volatile=True)
    run_case("logBack_delete", "项目日志", "POST", "/manage/log/logBack_delete",
             form={"id": "@PROJECT_ID@", "name": "@LOGBACK_KEY@"})
    run_case("logBack_delete(miss)", "项目日志", "POST", "/manage/log/logBack_delete",
             form={"id": "@PROJECT_ID@", "name": "@LOGBACK_KEY@"})
    run_case("operate(stop)", "项目日志", "POST", "/manage/operate",
             form={"id": "@PROJECT_ID@", "opt": "stop"}, timeout=90)
    run_case("getProjectStatus(stopped)", "项目日志", "POST", "/manage/getProjectStatus",
             form={"id": "@PROJECT_ID@"})
    run_case("operate(invalid opt)", "项目日志", "POST", "/manage/operate",
             form={"id": "@PROJECT_ID@", "opt": "no-such-opt"})
    run_case("operate(miss project)", "项目日志", "POST", "/manage/operate",
             form={"id": "NOT_EXIST", "opt": "start"})

    # ---- 删除项目（含彻底删除文件）
    run_state("state:data-before-deleteProject", "状态", project_state)
    run_case("deleteProject(thorough)", "项目", "POST", "/manage/deleteProject",
             form={"id": "@PROJECT_ID@", "thorough": "true"})
    run_case("getProjectItem(after delete)", "项目", "POST", "/manage/getProjectItem",
             form={"id": "@PROJECT_ID@"})
    run_case("deleteProject(already gone)", "项目", "POST", "/manage/deleteProject",
             form={"id": "@PROJECT_ID@"}, note="重复删除按 Java 语义返回成功")
    run_state("state:app-tree-after-delete", "状态", snapshot_app)
    run_state("state:data-after-deleteProject", "状态", snapshot_data)


# ================================================================ 脚本组
def script_ids(inst):
    r = req(inst, "POST", "/script/list.json")
    data = get_json(r).get("data") or []
    return set(item.get("id") for item in data if isinstance(item, dict))


def group_script():
    before_java = script_ids(JAVA)
    before_go = script_ids(GO)

    def save_add():
        jr, gr = both("POST", "/script/save.json",
                      form={"name": "e2e-script", "context": "echo e2e-hello", "type": "add"})
        after_java = script_ids(JAVA)
        after_go = script_ids(GO)
        new_j = sorted(after_java - before_java)
        new_g = sorted(after_go - before_go)
        JAVA.vars["SCRIPT_ID"] = new_j[0] if new_j else "no-script-id"
        GO.vars["SCRIPT_ID"] = new_g[0] if new_g else "no-script-id"
        return jr, gr, None
    run_custom("script/save.json(add)", "脚本", "POST", "/script/save.json", save_add,
               note="add 不返回 id，通过 list 差集捕获")
    run_case("script/list.json(after add)", "脚本", "POST", "/script/list.json")
    run_case("script/item.json(new)", "脚本", "POST", "/script/item.json", form={"id": "@SCRIPT_ID@"})

    def exec_script():
        jr, gr = both("POST", "/script/exec", form={"id": "@SCRIPT_ID@", "args": "", "params": ""})
        for inst, r in ((JAVA, jr), (GO, gr)):
            eid = get_json(r).get("data")
            inst.vars["EXEC_ID"] = eid if isinstance(eid, str) and eid else "no-exec-id"
        return jr, gr, None
    run_custom("script/exec", "脚本", "POST", "/script/exec", exec_script,
               note="返回 executeId（uuid，比较时归一化）")
    time.sleep(3)
    run_case("script/log", "脚本", "POST", "/script/log",
             form={"id": "@SCRIPT_ID@", "executeId": "@EXEC_ID@", "line": "1"},
             note="执行输出（echo e2e-hello）")
    run_case("script/pull_exec_log(has entry)", "脚本", "POST", "/script/pull_exec_log",
             form={"pullCount": "10"})
    run_case("script/del_log", "脚本", "POST", "/script/del_log",
             form={"id": "@SCRIPT_ID@", "executeId": "@EXEC_ID@"})
    run_case("script/log(after del_log)", "脚本", "POST", "/script/log",
             form={"id": "@SCRIPT_ID@", "executeId": "@EXEC_ID@", "line": "1"})
    run_case("script/del_exec_log", "脚本", "POST", "/script/del_exec_log",
             json_body={"ids": ["@EXEC_ID@"]})
    run_case("script/pull_exec_log(after del)", "脚本", "POST", "/script/pull_exec_log",
             form={"pullCount": "10"})
    run_case("script/change-workspace-id", "脚本", "POST", "/script/change-workspace-id",
             form={"id": "@SCRIPT_ID@", "newWorkspaceId": "E2E_WS", "newNodeId": "E2E_NODE"})
    run_case("script/item.json(after change-ws)", "脚本", "POST", "/script/item.json",
             form={"id": "@SCRIPT_ID@"})
    run_case("script/change-workspace-id(invalid)", "脚本", "POST", "/script/change-workspace-id",
             form={"id": "@SCRIPT_ID@", "newNodeId": "E2E_NODE"})
    run_case("script/save.json(update)", "脚本", "POST", "/script/save.json",
             form={"id": "@SCRIPT_ID@", "name": "e2e-script-updated", "context": "echo e2e-updated"})
    run_case("script/item.json(after update)", "脚本", "POST", "/script/item.json",
             form={"id": "@SCRIPT_ID@"})
    run_case("script/save.json(add duplicate)", "脚本", "POST", "/script/save.json",
             form={"id": "@SCRIPT_ID@", "name": "dup", "context": "x", "type": "add"})
    run_case("script/save.json(empty context)", "脚本", "POST", "/script/save.json",
             form={"name": "x", "context": "", "type": "add"})
    run_case("script/del.json", "脚本", "POST", "/script/del.json", form={"id": "@SCRIPT_ID@"})
    run_case("script/item.json(after del)", "脚本", "POST", "/script/item.json",
             form={"id": "@SCRIPT_ID@"})
    run_case("script/del.json(again)", "脚本", "POST", "/script/del.json", form={"id": "@SCRIPT_ID@"})

    # ---- 脚本库
    run_case("script-library/save", "脚本库", "POST", "/script-library/save",
             form={"id": "e2e-lib", "script": "echo lib-e2e", "description": "e2e lib", "version": "1.0"})
    run_case("script-library/get", "脚本库", "POST", "/script-library/get", form={"id": "e2e-lib"})
    run_case("script-library/list(after save)", "脚本库", "POST", "/script-library/list")
    run_case("script-library/save(overwrite)", "脚本库", "POST", "/script-library/save",
             form={"id": "e2e-lib", "script": "echo lib-e2e-v2", "description": "e2e lib v2", "version": "2.0"})
    run_case("script-library/get(after overwrite)", "脚本库", "POST", "/script-library/get",
             form={"id": "e2e-lib"})
    run_case("script-library/save(empty script)", "脚本库", "POST", "/script-library/save",
             form={"id": "e2e-lib", "script": ""})
    run_case("script-library/del", "脚本库", "POST", "/script-library/del", form={"id": "e2e-lib"})
    run_case("script-library/get(after del)", "脚本库", "POST", "/script-library/get", form={"id": "e2e-lib"})
    run_case("script-library/list(after del)", "脚本库", "POST", "/script-library/list")
    run_state("state:data-after-script", "状态", snapshot_data)


# ================================================================ 环境变量组
def group_env():
    run_case("workspace_env/update", "环境变量", "POST", "/system/workspace_env/update",
             form={"name": "E2E_VAR", "value": "e2e-value", "description": "e2e desc"})
    run_case("workspace_env/update(overwrite)", "环境变量", "POST", "/system/workspace_env/update",
             form={"name": "E2E_VAR", "value": "e2e-value-2", "description": "e2e desc 2"})
    run_case("workspace_env/update(no name)", "环境变量", "POST", "/system/workspace_env/update",
             form={"value": "x", "description": "y"})

    def cache_env():
        jr, gr = both("POST", "/system/cache")
        cmp = compare_responses("cache(after env)", jr, gr, "POST", "/system/cache", volatile=True)
        for label, r in (("java", jr), ("go", gr)):
            keys = (get_json(r).get("data") or {}).get("envVarKeys") or []
            if "E2E_VAR" not in keys:
                cmp.add("value", "data.envVarKeys", ["E2E_VAR"], "%s:%s" % (label, keys[:20]))
        return jr, gr, cmp
    run_custom("cache(after env)", "环境变量", "POST", "/system/cache", cache_env)

    run_case("workspace_env/delete", "环境变量", "POST", "/system/workspace_env/delete",
             form={"name": "E2E_VAR"})
    run_case("workspace_env/delete(no name)", "环境变量", "POST", "/system/workspace_env/delete", form={})
    run_case("workspace_env/delete(again)", "环境变量", "POST", "/system/workspace_env/delete",
             form={"name": "E2E_VAR"})
    run_state("state:data-after-env", "状态", snapshot_data)


# ================================================================ 系统域组
def group_system():
    run_case("system/clearCache(fileSize)", "系统", "POST", "/system/clearCache",
             form={"type": "fileSize"})
    run_case("system/clearCache(pidPort)", "系统", "POST", "/system/clearCache",
             form={"type": "pidPort"})
    run_case("system/clearCache(oldJarsSize)", "系统", "POST", "/system/clearCache",
             form={"type": "oldJarsSize"})
    run_case("system/clearCache(invalid type)", "系统", "POST", "/system/clearCache",
             form={"type": "no-such-type"})
    run_case("system/log_data.json", "系统", "POST", "/system/log_data.json", volatile=True)
    run_case("system/log_del(old)", "系统", "POST", "/system/log_del.json",
             form={"path": "e2e-old.log"}, note="夹具日志时间被改为 2 天前，可删除")
    run_case("system/log_del(new)", "系统", "POST", "/system/log_del.json",
             form={"path": "e2e-new.log"}, note="最近日志不可删除")
    run_case("system/log_del(missing path)", "系统", "POST", "/system/log_del.json", form={})
    run_case("system/log_download", "系统", "GET", "/system/log_download",
             form={"path": "e2e-new.log"}, strict_bin=True, note="夹具日志内容应完全一致")
    run_case("system/log_download(miss)", "系统", "GET", "/system/log_download",
             form={"path": "no-such.log"})
    run_case("system/log_data.json(after del)", "系统", "POST", "/system/log_data.json", volatile=True)

    # kill.json：真实杀掉一个 sleep 进程
    def kill_sleep():
        procs = []
        resps = []
        for inst in INSTS:
            p = spawn_sleep()
            procs.append(p)
            r = req(inst, "POST", "/kill.json", form={"pid": str(p.pid)})
            resps.append(r)
            time.sleep(0.3)
        for p in procs:
            try:
                p.kill()
            except Exception:
                pass
        return resps[0], resps[1], None
    run_custom("kill.json(sleep pid)", "系统", "POST", "/kill.json", kill_sleep,
               note="每个实例各杀一个独立 sleep 进程")

    run_case("kill.json(agent self)", "系统", "POST", "/kill.json",
             form={"pid": "@AGENT_PID@"}, note="禁止杀自身进程")
    run_case("kill.json(no such pid)", "系统", "POST", "/kill.json", form={"pid": "999999"})
    run_case("kill.json(no pid)", "系统", "POST", "/kill.json", form={})

    # save_config：写入当前配置 + 注释（restart=false，不重启实例）
    def save_config_step():
        jr, gr = both("POST", "/system/getConfig.json")
        jc = (get_json(jr).get("data") or {}).get("content", "")
        gc = (get_json(gr).get("data") or {}).get("content", "")
        new_j = jc.rstrip("\n") + "\n# e2e save_config test\n"
        new_g = gc.rstrip("\n") + "\n# e2e save_config test\n"
        rj = req(JAVA, "POST", "/system/save_config.json",
                 form={"content": new_j, "restart": "false"})
        rg = req(GO, "POST", "/system/save_config.json",
                 form={"content": new_g, "restart": "false"})
        return rj, rg, compare_responses("system/save_config.json", rj, rg, "POST",
                                        "/system/save_config.json")
    run_custom("system/save_config.json", "系统", "POST", "/system/save_config.json", save_config_step,
               note="写入原配置+注释，restart=false")
    run_case("system/getConfig.json(after save)", "系统", "POST", "/system/getConfig.json",
             ignore=("data.file",), note="配置内容应已包含刚写入的注释")
    run_case("system/save_config.json(invalid yaml)", "系统", "POST", "/system/save_config.json",
             form={"content": "jpom: [\n  bad", "restart": "false"}, msg_prefix_ok=True,
             note="非法 YAML 应报错，异常明细允许不同")
    run_case("system/getConfig.json(after invalid)", "系统", "POST", "/system/getConfig.json",
             ignore=("data.file",))

    # 升级相关：只测参数校验错误分支，不做真实替换/重启（见报告说明）
    run_case("system/upload-jar-sharding(no file)", "升级", "POST", "/system/upload-jar-sharding",
             form={"sliceId": "e2e-up", "totalSlice": "1", "nowSlice": "0",
                   "fileSumMd5": "00000000000000000000000000000000"},
             note="缺少上传文件，仅验证错误分支")
    run_case("system/upload-jar-sharding-merge(bad)", "升级", "POST",
             "/system/upload-jar-sharding-merge",
             form={"sliceId": "no-such-slice", "totalSlice": "1",
                   "fileSumMd5": "00000000000000000000000000000000"})
    skip_case("system/remote_upgrade.json", "升级", "/system/remote_upgrade.json",
              "跳过：会下载并替换插件端二进制/重启，属真实升级场景；见报告“未覆盖项”")
    skip_case("system/node_update WS restart", "升级", "/node_update",
              "跳过：WebSocket 升级流程，VERIFY-REPORT 第一轮已做帧级审计")

    run_case("info(after all)", "系统", "POST", "/info", ignore=(),
             expect=["engine", "lastTime", "manifest.port", "manifest.jarFile", "manifest.version"])
    run_state("state:conf-final", "状态", lambda inst: {
        "application.yml": open(inst.path("conf", "application.yml"), encoding="utf-8").read(),
        "logback.xml": (open(inst.path("conf", "logback.xml"), encoding="utf-8").read()
                        if os.path.isfile(inst.path("conf", "logback.xml")) else "<absent>"),
    })
    run_state("state:data-final", "状态", snapshot_data)
    run_state("state:backup-final", "状态", snapshot_backup)
    run_state("state:logs-final", "状态", lambda inst: {
        "e2e-old.log 存在": os.path.isfile(inst.path("logs", "e2e-old.log")),
        "e2e-new.log 存在": os.path.isfile(inst.path("logs", "e2e-new.log")),
        "logs 目录存在": os.path.isdir(inst.path("logs")),
    })


# ================================================================ go-only：契约/i18n 预期值断言
def _go_record(name, group, method, path, resp, expect, diffs, notes):
    result = "PASS"
    if diffs:
        result = "DIFF"
    if resp.get("status") is None:
        result = "FAIL"
    entry = {
        "name": name, "group": group, "method": method, "path": path, "result": result,
        "diffs": diffs, "notes": notes, "note": "", "tags": ["go-only"],
        "java": {"mode": "contract-expected", "expect": expect},
        "go": sanitize_summary(GO, resp),
    }
    RESULTS.append(entry)
    if VERBOSE or result != "PASS":
        print("[%s] %-44s %s %s" % (result, name, method, path), file=sys.stderr)
        for d in diffs[:4]:
            print("         %s %s: expect=%s actual=%s" % (d["type"], d["path"], d["java"], d["go"]),
                  file=sys.stderr)
    return entry


def go_check(name, group, method, path, form=None, json_body=None, files=None,
             code=None, msg=None, msg_contains=None, msg_prefix=None, http_status=200,
             data_check=None, note=""):
    """对 Go 实例发起请求，按 Java 契约预期（code/msg/i18n 文案/data 断言）判定 PASS/DIFF。"""
    if ONLY and not re.search(ONLY, name):
        return None
    resp = req(GO, method, path, form=form, json_body=json_body, files=files)
    diffs, notes = [], []
    if note:
        notes.append(note)
    if resp.get("status") is None:
        diffs.append({"type": "request", "path": "-", "java": "expected-success",
                      "go": resp.get("error")})
    else:
        if http_status is not None and resp["status"] != http_status:
            diffs.append({"type": "http-status", "path": "status",
                          "java": http_status, "go": resp["status"]})
        j = resp.get("json")
        if isinstance(j, dict):
            if code is not None:
                accepts = code if isinstance(code, (list, tuple, set)) else [code]
                if j.get("code") not in accepts:
                    diffs.append({"type": "code", "path": "code",
                                  "java": str(code), "go": str(j.get("code"))})
            jm = norm_str(GO, j.get("msg")) or ""
            if msg is not None:
                accepts = msg if isinstance(msg, (list, tuple)) else [msg]
                if not any(jm == norm_str(GO, m) for m in accepts):
                    diffs.append({"type": "msg", "path": "msg",
                                  "java": " | ".join(str(m) for m in accepts), "go": jm})
            if msg_contains is not None and norm_str(GO, msg_contains) not in jm:
                diffs.append({"type": "msg-contains", "path": "msg",
                              "java": msg_contains, "go": jm})
            if msg_prefix is not None and not jm.startswith(norm_str(GO, msg_prefix)):
                diffs.append({"type": "msg-prefix", "path": "msg",
                              "java": msg_prefix, "go": jm})
            if data_check is not None:
                try:
                    problems = data_check(j.get("data")) or []
                except Exception as e:
                    problems = ["data_check 异常: %r" % (e,)]
                for p in problems:
                    diffs.append({"type": "data", "path": "data", "java": "expected",
                                  "go": p})
        elif code is not None:
            diffs.append({"type": "body", "path": "body", "java": "json", "go": "raw"})
    expect = {"httpStatus": http_status, "code": code,
              "msg": msg if isinstance(msg, (list, tuple)) else ([msg] if msg else None),
              "msgPrefix": msg_prefix, "msgContains": msg_contains}
    return _go_record(name, group, method, path, resp, expect, diffs, notes)


def go_observe(name, group, fn, expected, note=""):
    """对实例落盘状态做断言（文件存在/内容/data 变化），expected 为 Java 契约预期值。"""
    if ONLY and not re.search(ONLY, name):
        return None
    try:
        actual = fn()
    except Exception as e:
        actual = "ERROR: %r" % (e,)
    diffs, notes = [], []
    if note:
        notes.append(note)
    if actual != expected:
        diffs.append({"type": "observe", "path": "-", "java": expected, "go": actual})
    resp = {"status": 200, "ctype": "application/json", "body": b"",
            "json": {"code": 200, "msg": "", "data": actual}}
    return _go_record(name, group, "OBSERVE", "-", resp,
                      {"expected": expected}, diffs, notes)


def go_read_file(inst, rel):
    try:
        with open(os.path.join(inst.app, rel), encoding="utf-8") as fp:
            return fp.read()
    except Exception as e:
        return "ERROR: %r" % (e,)


def go_only_suite():
    pid = GO.project_id
    app_root = app_root_of(GO)
    baseline_whitelist = load_json(GO.data_path("whitelistDirectory.json"))
    suffix = "\n".join(baseline_whitelist.get("allowEditSuffix", []))
    project_form = {
        "id": pid, "name": "e2eapp", "runMode": "Jar",
        "whitelistDirectory": app_root, "lib": "app", "logPath": "",
        "autoStart": "false", "args": "e2e-arg", "mainClass": "",
    }

    # ---- 白名单
    go_check("whitelistDirectory_submit(invalid empty)", "只读-g", "POST",
             "/system/whitelistDirectory_submit", form={"project": "", "allowEditSuffix": suffix},
             code=405, msg="项目路径授权不能为空")
    go_check("whitelistDirectory_submit", "只读-g", "POST", "/system/whitelistDirectory_submit",
             form={"project": app_root, "allowEditSuffix": suffix}, code=200, msg="保存成功")
    go_check("whitelistDirectory_data", "只读-g", "POST", "/system/whitelistDirectory_data",
             code=200, data_check=lambda d: [] if (isinstance(d, dict) and d.get("project") == [app_root]) else ["whitelist.project != [%s]: %s" % (app_root, d)])

    # ---- 项目保存
    preview = dict(project_form); preview["previewData"] = "true"
    go_check("saveProject(preview)", "项目-g", "POST", "/manage/saveProject", form=preview,
             code=200, msg="检查通过")
    go_observe("saveProject(preview) 不落库", "项目-g",
               lambda: pid in (load_json(GO.data_path("project.json")) or {}), False)
    go_check("saveProject", "项目-g", "POST", "/manage/saveProject", form=project_form,
             code=200, msg="新增成功！")
    go_observe("saveProject 落库", "项目-g",
               lambda: pid in (load_json(GO.data_path("project.json")) or {}), True)
    go_check("getProjectItem", "项目-g", "POST", "/manage/getProjectItem", form={"id": pid},
             code=200, data_check=lambda d: [] if isinstance(d, dict) and d.get("id") == pid else ["data.id != %s" % pid])

    # ---- 项目文件
    def file_list_has(*names):
        def check(data):
            if not isinstance(data, list):
                return ["data 不是数组"]
            got = set(x.get("filename") or x.get("name") for x in data if isinstance(x, dict))
            missing = [n for n in names if n not in got]
            return ["缺少文件: %s（实际: %s）" % (missing, sorted(got))] if missing else []
        return check
    go_check("file/getFileList", "文件-g", "POST", "/manage/file/getFileList",
             form={"id": pid, "path": "/"}, code=200, msg="查询成功",
             data_check=file_list_has("config.txt", "run.sh", "sub", "e2eapp.jar"))
    go_check("file/read_file", "文件-g", "POST", "/manage/file/read_file",
             form={"id": pid, "filePath": "/", "filename": "config.txt"}, code=200,
             data_check=lambda d: [] if d == "hello e2e\n" else ["文件内容不符: %r" % (d,)])
    go_check("file/read_file(disallowed suffix)", "文件-g", "POST", "/manage/file/read_file",
             form={"id": pid, "filePath": "/", "filename": "e2eapp.jar"},
             code=405, msg="不允许编辑的文件后缀")
    go_check("file/update_config_file", "文件-g", "POST", "/manage/file/update_config_file",
             form={"id": pid, "filePath": "/", "filename": "config.txt",
                   "fileText": "hello e2e updated\n"}, code=200, msg="文件写入成功")
    go_observe("update_config_file 落盘", "文件-g",
               lambda: go_read_file(GO, "config.txt"), "hello e2e updated\n")
    go_check("file/copy", "文件-g", "POST", "/manage/file/copy",
             form={"id": pid, "filePath": "/", "filename": "config.txt"}, code=200, msg="复制成功")
    go_observe("copy 生成 config(1).txt", "文件-g",
               lambda: os.path.isfile(os.path.join(GO.app, "config(1).txt")), True)
    go_check("file/new_file_folder.json", "文件-g", "POST", "/manage/file/new_file_folder.json",
             form={"id": pid, "levelName": "", "filename": "e2e-folder"}, code=200, msg="操作成功")
    go_observe("new_file_folder 创建目录", "文件-g",
               lambda: os.path.isdir(os.path.join(GO.app, "e2e-folder")), True)
    go_check("file/new_file_folder.json(exists)", "文件-g", "POST", "/manage/file/new_file_folder.json",
             form={"id": pid, "levelName": "", "filename": "e2e-folder"},
             code=405, msg="文件夹或者文件已存在")
    go_check("file/rename.json", "文件-g", "POST", "/manage/file/rename.json",
             form={"id": pid, "levelName": "", "filename": "e2e-folder",
                   "newname": "e2e-folder-renamed"}, code=200, msg="操作成功")
    go_observe("rename 生效", "文件-g",
               lambda: os.path.isdir(os.path.join(GO.app, "e2e-folder-renamed")), True)
    go_check("file/rename.json(miss)", "文件-g", "POST", "/manage/file/rename.json",
             form={"id": pid, "levelName": "", "filename": "no-such", "newname": "x"},
             code=405, msg="文件不存在")
    go_check("file/compress", "文件-g", "POST", "/manage/file/compress",
             form={"id": pid, "filePath": "/", "filename": "sub", "type": "zip"},
             code=200, msg="压缩成功")
    go_observe("compress 生成 sub.zip", "文件-g",
               lambda: os.path.isfile(os.path.join(GO.app, "sub.zip")), True)
    go_check("file/compress(invalid type)", "文件-g", "POST", "/manage/file/compress",
             form={"id": pid, "filePath": "/", "filename": "sub", "type": "rar"},
             code=(405, 500), msg_prefix="不支持的压缩类型,")
    go_check("file/remote_download", "文件-g", "POST", "/manage/file/remote_download",
             form={"id": pid, "url": FIXTURE_URL + "/remote.txt", "levelName": ""},
             code=200, msg_prefix="下载成功文件大小：")
    go_observe("remote_download 落盘", "文件-g", lambda: go_read_file(GO, "remote.txt"),
               "remote fixture file for e2e\n")

    upload_bytes = b"upload fixture content for e2e\n"
    upload_md5 = hashlib.md5(upload_bytes).hexdigest()
    half = len(upload_bytes) // 2
    slices = [upload_bytes[:half], upload_bytes[half:]]
    slice_id = "e2e-slice-go"
    for now, chunk in enumerate(slices):
        go_check("file2/upload-sharding#%d" % now, "文件-g", "POST", "/manage/file2/upload-sharding",
                 form={"sliceId": slice_id, "totalSlice": str(len(slices)),
                       "nowSlice": str(now), "fileSumMd5": upload_md5},
                 files=[("file", "e2e-upload.txt.%d" % now, chunk, "application/octet-stream")],
                 code=200, msg="上传成功")
    go_check("file2/sharding-merge", "文件-g", "POST", "/manage/file2/sharding-merge",
             form={"type": "file", "path": GO.app, "sliceId": slice_id,
                   "totalSlice": str(len(slices)), "fileSumMd5": upload_md5},
             code=200, msg="上传成功")
    go_observe("分片上传落盘", "文件-g", lambda: go_read_file(GO, "e2e-upload.txt"),
               upload_bytes.decode())
    go_check("file2/sharding-merge(bad md5)", "文件-g", "POST", "/manage/file2/sharding-merge",
             form={"type": "file", "path": GO.app, "sliceId": "no-such-slice",
                   "totalSlice": "1", "fileSumMd5": "0" * 32},
             code=405, msg_prefix="文件上传失败,存在分片丢失的情况")
    go_check("file/deleteFile", "文件-g", "POST", "/manage/file/deleteFile",
             form={"id": pid, "filename": "remote.txt"}, code=200, msg="删除成功")
    go_observe("deleteFile 生效", "文件-g",
               lambda: os.path.exists(os.path.join(GO.app, "remote.txt")), False)
    go_check("file/batch_delete", "文件-g", "POST", "/manage/file/batch_delete",
             json_body={"id": pid, "dir": "/",
                        "data": [{"name": "config(1).txt"}, {"name": "e2e-upload.txt"}]},
             code=200, msg="删除成功")
    go_check("file/batch_delete(empty)", "文件-g", "POST", "/manage/file/batch_delete",
             json_body={"id": pid, "dir": "/", "data": []}, code=405,
             msg="没有要对比的数据")
    go_check("file/diff_file", "文件-g", "POST", "/manage/file/diff_file",
             json_body={"id": pid, "dir": "/", "data": [{"name": "config.txt", "sha1": "deadbeef"}]},
             code=200)

    # ---- 文件备份
    backups = {}
    def capture_backup(data):
        items = (data or {}).get("list") or []
        if not items:
            return ["备份列表为空（file-backup-count 已设为 %d）" % BACKUP_COUNT]
        candidates = []
        for item in items:
            for key in ("name", "filename", "id"):
                v = item.get(key)
                if isinstance(v, str) and re.fullmatch(r"\d{17}", v.strip("/")):
                    candidates.append(v.strip("/"))
                    break
        if candidates:
            backups["id"] = max(candidates)  # 取最新快照（17 位时间 id）
        else:
            backups["id"] = items[0].get("name") or items[0].get("filename") or "x"
        backups["count"] = len(items)
        return []
    go_check("file/list-backup", "备份-g", "POST", "/manage/file/list-backup",
             form={"id": pid}, code=200, msg="查询成功", data_check=capture_backup,
             note="备份由 saveProject/update_config_file/copy/compress/deleteFile 等写操作自动创建")
    bid = backups.get("id", "no-backup-id")
    backup_files = {}
    def capture_item_files(d):
        names = [x.get("filename") or x.get("name") for x in (d or []) if isinstance(x, dict)]
        names = sorted(n for n in names if n)
        backup_files["names"] = names
        if not names:
            return ["备份文件列表为空"]
        return []
    go_check("file/backup-item-files", "备份-g", "POST", "/manage/file/backup-item-files",
             form={"id": pid, "backupId": bid, "path": "/"}, code=200, msg="查询成功",
             data_check=capture_item_files)
    # diff-backup（Java 默认开启）会从快照剔除“当前目录仍存在”的未变化文件，
    # 因此最新快照里保留的是被删除的文件（config(1).txt / e2e-upload.txt）。
    names = backup_files.get("names") or []
    pick = next((n for n in ("e2e-upload.txt", "config(1).txt", "config.txt") if n in names),
                names[0] if names else "no-file")
    go_observe("最新快照保留被删除文件（diff-backup 语义）", "备份-g",
               lambda: any(n in (backup_files.get("names") or []) for n in ("e2e-upload.txt", "config(1).txt")), True)
    go_check("file/backup-download", "备份-g", "GET", "/manage/file/backup-download",
             form={"id": pid, "backupId": bid, "filename": pick},
             http_status=200, data_check=None,
             note="下载备份内 %s（HTTP 200 + 非空 body）" % pick)
    go_check("file/backup-recover", "备份-g", "POST", "/manage/file/backup-recover",
             form={"id": pid, "backupId": bid, "filename": pick}, code=200, msg="还原成功")
    go_observe("backup-recover 还原文件落盘", "备份-g",
               lambda: os.path.isfile(os.path.join(GO.app, pick)), True)
    go_check("file/backup-delete", "备份-g", "POST", "/manage/file/backup-delete",
             form={"id": pid, "backupId": bid, "filename": pick}, code=200, msg="删除成功")
    go_observe("备份目录非空", "备份-g",
               lambda: os.path.isdir(GO.data_path("project_file_backup", pid)), True)

    # ---- 项目启停与控制台日志
    go_check("operate(start)", "项目日志-g", "POST", "/manage/operate",
             form={"id": pid, "opt": "start"}, code=200, msg="操作成功",
             data_check=lambda d: [] if isinstance(d, dict) and d.get("success") else ["data.success 不为 true: %s" % (d,)])
    go_check("getProjectStatus(running)", "项目日志-g", "POST", "/manage/getProjectStatus",
             form={"id": pid}, code=200,
             data_check=lambda d: [] if isinstance(d, dict) and str(d.get("statusMsg", "")).startswith("running") else ["statusMsg 不以 running 开头: %s" % (d,)])
    go_check("logSize(running)", "项目日志-g", "POST", "/manage/log/logSize", form={"id": pid},
             code=200, data_check=lambda d: [] if isinstance(d, dict) and d.get("logSize") else ["logSize 为空"])
    for inst in [GO]:
        append_padding(inst, pid)
    go_check("resetLog", "项目日志-g", "POST", "/manage/log/resetLog", form={"id": pid},
             code=200, msg="重置成功", note="控制台日志超过 100KB 才触发备份")
    logback = {}
    def capture_logback(data):
        arr = (data or {}).get("array") or []
        names = sorted([x.get("filename") or x.get("name") or "" for x in arr if isinstance(x, dict)])
        if not names:
            return ["日志备份列表为空（resetLog 后应有备份文件）"]
        logback["key"] = names[-1]
        return []
    go_check("logBack(after reset)", "项目日志-g", "POST", "/manage/log/logBack", form={"id": pid},
             code=200, data_check=capture_logback)
    key = logback.get("key", "no-logback-file")
    go_check("logBack_download", "项目日志-g", "GET", "/manage/log/logBack_download",
             form={"id": pid, "key": key}, http_status=200)
    go_check("export", "项目日志-g", "GET", "/manage/log/export", form={"id": pid}, http_status=200)
    go_check("logBack_delete", "项目日志-g", "POST", "/manage/log/logBack_delete",
             form={"id": pid, "name": key}, code=200, msg="删除成功")
    go_check("operate(stop)", "项目日志-g", "POST", "/manage/operate",
             form={"id": pid, "opt": "stop"}, code=200, msg="操作成功")
    go_check("getProjectStatus(stopped)", "项目日志-g", "POST", "/manage/getProjectStatus",
             form={"id": pid}, code=200,
             data_check=lambda d: [] if isinstance(d, dict) and d.get("statusMsg") == "stopped" else ["statusMsg != stopped: %s" % (d,)])
    go_check("operate(invalid opt)", "项目日志-g", "POST", "/manage/operate",
             form={"id": pid, "opt": "no-such-opt"}, code=405, msg="请选择操作类型")

    # ---- 删除项目
    go_check("deleteProject(thorough)", "项目-g", "POST", "/manage/deleteProject",
             form={"id": pid, "thorough": "true"}, code=200, msg="删除成功！")
    go_observe("deleteProject 删除项目目录", "项目-g",
               lambda: os.path.exists(GO.app), False)
    go_observe("project.json 已移除该项目", "项目-g",
               lambda: pid in (load_json(GO.data_path("project.json")) or {}), False)
    go_check("getProjectItem(after delete)", "项目-g", "POST", "/manage/getProjectItem",
             form={"id": pid}, code=200,
             data_check=lambda d: [] if d is None else ["data 应为 null: %r" % (d,)])

    # ---- 脚本
    def script_ids():
        r = req(GO, "POST", "/script/list.json")
        return set(x.get("id") for x in (get_json(r).get("data") or []) if isinstance(x, dict))
    before = script_ids()
    go_check("script/save.json(add)", "脚本-g", "POST", "/script/save.json",
             form={"name": "e2e-script", "context": "echo e2e-hello", "type": "add"},
             code=200, msg="添加成功")
    added = sorted(script_ids() - before)
    sid = added[0] if added else "no-script-id"
    go_observe("script 已落库", "脚本-g", lambda: sid != "no-script-id", True)
    def item_new_check(d):
        if not isinstance(d, dict) or d.get("id") != sid:
            return ["data.id 不符"]
        problems = []
        if "autoExecCron" in d:
            problems.append("新增脚本不应输出 autoExecCron（Java NON_NULL）: %r" % (d.get("autoExecCron"),))
        if "nodeId" in d:
            problems.append("新增脚本不应输出 nodeId（Java NON_NULL）: %r" % (d.get("nodeId"),))
        if d.get("lastRunUser") != "-":
            problems.append("lastRunUser 应为 -: %r" % (d.get("lastRunUser"),))
        return problems
    go_check("script/item.json(new)", "脚本-g", "POST", "/script/item.json", form={"id": sid},
             code=200, data_check=item_new_check)
    exec_r = req(GO, "POST", "/script/exec", form={"id": sid, "args": "", "params": ""})
    eid = get_json(exec_r).get("data") if isinstance(get_json(exec_r).get("data"), str) else ""
    _go_record("script/exec", "脚本-g", "POST", "/script/exec", exec_r,
               {"code": 200, "msg": "开始执行"}, [] if (get_json(exec_r).get("code") == 200 and eid) else
               [{"type": "data", "path": "data", "java": "executeId", "go": str(get_json(exec_r).get("data"))}],
               [])
    time.sleep(3)
    go_check("script/log", "脚本-g", "POST", "/script/log",
             form={"id": sid, "executeId": eid, "line": "1"}, code=200,
             data_check=lambda d: [] if (isinstance(d, dict) and d.get("run") is False
                                         and any("e2e-hello" in (x or "") for x in (d.get("dataLines") or [])))
             else ["执行日志中未找到 e2e-hello 或 run != false: %s" % (short(d, 300),)])
    go_check("script/pull_exec_log", "脚本-g", "POST", "/script/pull_exec_log",
             form={"pullCount": "10"}, code=200,
             data_check=lambda d: [] if any(isinstance(x, dict) and (x.get("id") == eid or x.get("executeId") == eid) for x in (d or []))
             else ["pull_exec_log 未包含本次执行记录"])
    go_check("script/log(line=0)", "脚本-g", "POST", "/script/log",
             form={"id": sid, "executeId": eid, "line": "0"}, code=200,
             data_check=lambda d: [] if isinstance(d, dict) and d.get("getLine") == 0
             else ["line=0 时 getLine 应为 0: %r" % ((d or {}).get("getLine"),)])
    go_check("script/log(missing line)", "脚本-g", "POST", "/script/log",
             form={"id": sid, "executeId": eid}, code=400, msg="行号不正确")
    go_check("script/del_log", "脚本-g", "POST", "/script/del_log",
             form={"id": sid, "executeId": eid}, code=200, msg="删除成功")
    go_check("script/del_exec_log", "脚本-g", "POST", "/script/del_exec_log",
             json_body={"ids": [eid]}, code=200, msg="删除成功")
    go_check("script/save.json(update)", "脚本-g", "POST", "/script/save.json",
             form={"id": sid, "name": "e2e-script-updated", "context": "echo e2e-updated"},
             code=200, msg="修改成功")
    def item_update_check(d):
        if not isinstance(d, dict):
            return ["data 不是对象"]
        problems = []
        if d.get("defArgs") != "[]":
            problems.append("更新后 defArgs 应为 []: %r" % (d.get("defArgs"),))
        if d.get("autoExecCron") != "":
            problems.append("更新后 autoExecCron 应为空串: %r" % (d.get("autoExecCron"),))
        return problems
    go_check("script/item.json(after update)", "脚本-g", "POST", "/script/item.json",
             form={"id": sid}, code=200, data_check=item_update_check)
    go_check("script/save.json(empty context)", "脚本-g", "POST", "/script/save.json",
             form={"name": "x", "context": "", "type": "add"}, code=405, msg="内容为空")
    go_check("script/del.json", "脚本-g", "POST", "/script/del.json", form={"id": sid},
             code=200, msg="删除成功")
    go_observe("script.json 已移除该脚本", "脚本-g",
               lambda: sid not in (load_json(GO.data_path("script.json")) or {}), True)

    # ---- 脚本库
    go_check("script-library/save", "脚本库-g", "POST", "/script-library/save",
             form={"id": "e2e-lib", "script": "echo lib-e2e",
                   "description": "e2e lib", "version": "1.0"}, code=200, msg="保存成功")
    go_observe("global-script/e2e-lib.json 已生成", "脚本库-g",
               lambda: os.path.isfile(GO.data_path("global-script", "e2e-lib.json")), True)
    go_check("script-library/get", "脚本库-g", "POST", "/script-library/get", form={"id": "e2e-lib"},
             code=200, data_check=lambda d: [] if isinstance(d, dict) and d.get("version") == "1.0" else ["version 不符"])
    go_check("script-library/list", "脚本库-g", "POST", "/script-library/list", code=200,
             data_check=lambda d: [] if any(isinstance(x, dict) and x.get("id") == "e2e-lib" for x in (d or []))
             else ["列表中未找到 e2e-lib"])
    go_observe("script-library/list 顺序为目录遍历序（test 先于新建 e2e-lib，与 Java A 轮观测一致）", "脚本库-g",
               lambda: [x.get("id") for x in (get_json(req(GO, "POST", "/script-library/list")).get("data") or []) if isinstance(x, dict)],
               ["test", "e2e-lib"])
    go_check("script-library/save(empty script)", "脚本库-g", "POST", "/script-library/save",
             form={"id": "e2e-lib", "script": ""}, code=400, msg="脚本内容不能为空")
    go_check("script-library/del", "脚本库-g", "POST", "/script-library/del", form={"id": "e2e-lib"},
             code=200, msg="删除成功")
    go_observe("global-script/e2e-lib.json 已删除", "脚本库-g",
               lambda: os.path.exists(GO.data_path("global-script", "e2e-lib.json")), False)

    # ---- 工作空间环境变量
    go_check("workspace_env/update", "环境变量-g", "POST", "/system/workspace_env/update",
             form={"name": "E2E_VAR", "value": "e2e-value", "description": "e2e desc"},
             code=200, msg="更新成功")
    go_check("workspace_env/delete", "环境变量-g", "POST", "/system/workspace_env/delete",
             form={"name": "E2E_VAR"}, code=200, msg="删除成功")
    go_observe("workspace_env_var 已恢复", "环境变量-g",
               lambda: "E2E_VAR" in (((load_json(GO.data_path("workspace_env_var.json")) or {}).get("DEFAULT") or {}).get("varData") or {}), False)

    # ---- 系统域
    for t in ("fileSize", "pidPort", "oldJarsSize"):
        go_check("system/clearCache(%s)" % t, "系统-g", "POST", "/system/clearCache",
                 form={"type": t}, code=200, msg="清空成功")
    go_check("system/clearCache(invalid)", "系统-g", "POST", "/system/clearCache",
             form={"type": "no-such-type"}, code=405, msg_prefix="没有对应类型：")
    go_observe("clearCache(fileSize) 后 temp 目录保留", "系统-g",
               lambda: os.path.isdir(GO.data_path("temp")), True)
    go_check("system/log_data.json", "系统-g", "POST", "/system/log_data.json", code=200)
    go_check("system/log_del(old)", "系统-g", "POST", "/system/log_del.json",
             form={"path": "e2e-old.log"}, code=200, msg="删除成功")
    go_observe("旧系统日志已删除", "系统-g",
               lambda: os.path.exists(GO.path("logs", "e2e-old.log")), False)
    go_check("system/log_del(new)", "系统-g", "POST", "/system/log_del.json",
             form={"path": "e2e-new.log"}, code=405, msg="不能删除近一天相关的日志(文件修改时间)")
    go_check("system/log_download", "系统-g", "GET", "/system/log_download",
             form={"path": "e2e-new.log"}, http_status=200)
    go_observe("系统日志下载内容一致", "系统-g",
               lambda: os.path.isfile(GO.path("logs", "e2e-new.log")), True)

    def kill_sleep():
        p = spawn_sleep()
        r = req(GO, "POST", "/kill.json", form={"pid": str(p.pid)})
        time.sleep(0.3)
        try:
            p.kill()
        except Exception:
            pass
        return r
    r = kill_sleep()
    _go_record("kill.json(sleep pid)", "系统-g", "POST", "/kill.json", r,
               {"code": 200, "msg": "成功kill"},
               [] if (get_json(r).get("code") == 200) else
               [{"type": "code", "path": "code", "java": "200", "go": str(get_json(r).get("code"))}], [])
    go_check("kill.json(agent self)", "系统-g", "POST", "/kill.json",
             form={"pid": GO.vars.get("AGENT_PID", "0")}, code=405, msg="不支持在线关闭 Agent 进程")

    # save_config：写原内容+注释，不重启
    go_check("system/getConfig.json", "系统-g", "POST", "/system/getConfig.json", code=200)
    cfg = (get_json(req(GO, "POST", "/system/getConfig.json")).get("data") or {}).get("content", "")
    go_check("system/save_config.json", "系统-g", "POST", "/system/save_config.json",
             form={"content": cfg.rstrip("\n") + "\n# e2e save_config test\n", "restart": "false"},
             code=200, msg="修改成功")
    go_check("system/getConfig.json(after save)", "系统-g", "POST", "/system/getConfig.json",
             code=200, data_check=lambda d: [] if isinstance(d, dict) and "# e2e save_config test" in (d.get("content") or "")
             else ["配置内容未包含测试注释"])
    go_check("system/save_config.json(invalid yaml)", "系统-g", "POST", "/system/save_config.json",
             form={"content": "jpom: [\n  bad", "restart": "false"}, code=500,
             msg_prefix="内容格式错误，请检查修正:")

    # ---- 最终状态
    go_observe("data/project.json 无测试项目", "状态-g",
               lambda: pid in (load_json(GO.data_path("project.json")) or {}), False)
    go_observe("data/script.json 无测试脚本", "状态-g",
               lambda: sid in (load_json(GO.data_path("script.json")) or {}), False)
    go_observe("data/whitelistDirectory.json 指向测试授权目录", "状态-g",
               lambda: (load_json(GO.data_path("whitelistDirectory.json")) or {}).get("project"), [app_root])
    go_observe("data/INSTALL.json installId 已重新生成", "状态-g",
               lambda: len((load_json(GO.data_path("INSTALL.json")) or {}).get("installId", "")), 32)
    def capture_manifest():
        r = req(GO, "POST", "/info")
        m = (get_json(r).get("data") or {}).get("manifest") or {}
        CTX["manifest_go"] = {k: m.get(k) for k in ("version", "buildTime", "debug", "engine")}
        return r
    _go_record("info(after all)", "状态-g", "POST", "/info", capture_manifest(),
               {"code": 200}, [] if get_json(capture_manifest()).get("code") == 200 else
               [{"type": "code", "path": "code", "java": "200", "go": str(get_json(capture_manifest()).get("code"))}], [])


def app_root_of(inst):
    return os.path.dirname(inst.app) if inst.app else ""


# ================================================================ 报告
GROUP_ORDER = ["只读", "项目", "文件", "备份", "项目日志", "脚本", "脚本库", "环境变量", "系统", "升级", "状态",
               "只读-g", "项目-g", "文件-g", "备份-g", "项目日志-g", "脚本-g", "脚本库-g", "环境变量-g", "系统-g", "状态-g"]


def write_reports():
    os.makedirs(RESULT_DIR, exist_ok=True)
    raw_dir = os.path.join(RESULT_DIR, "raw")
    os.makedirs(raw_dir, exist_ok=True)
    counts = {}
    for r in RESULTS:
        counts[r["result"]] = counts.get(r["result"], 0) + 1
    for r in RESULTS:
        if r["result"] in ("DIFF", "FAIL"):
            safe = re.sub(r"[^A-Za-z0-9_.-]+", "_", r["name"])
            with open(os.path.join(raw_dir, safe + ".java.json"), "w", encoding="utf-8") as fp:
                json.dump(r["java"], fp, ensure_ascii=False, indent=2)
            with open(os.path.join(raw_dir, safe + ".go.json"), "w", encoding="utf-8") as fp:
                json.dump(r["go"], fp, ensure_ascii=False, indent=2)
    payload = {
        "mode": "go-only" if GO_ONLY else "dual",
        "generatedAt": time.strftime("%Y-%m-%d %H:%M:%S"),
        "elapsedSeconds": int(time.time() - T0),
        "host": HOSTNAME,
        "java": {"base": JAVA.base, "url": JAVA.url, "app": JAVA.app,
                 "manifest": CTX.get("manifest_java")},
        "go": {"base": GO.base, "url": GO.url, "app": GO.app,
               "manifest": CTX.get("manifest_go")},
        "language": LANG, "backupCount": BACKUP_COUNT,
        "counts": counts, "total": len(RESULTS),
        "results": RESULTS,
    }
    with open(os.path.join(RESULT_DIR, "result.json"), "w", encoding="utf-8") as fp:
        json.dump(payload, fp, ensure_ascii=False, indent=2)

    lines = []
    lines.append("# Java vs Go 插件端全端点（含破坏性）对照结果（自动生成）\n")
    lines.append("- 生成时间：%s（耗时 %ds）" % (payload["generatedAt"], payload["elapsedSeconds"]))
    lines.append("- 测试主机：%s" % HOSTNAME)
    if GO_ONLY:
        lines.append("- 模式：go-only（只启动 Go 隔离实例，Java 期望值来自 CONTRACT-HTTP + Java i18n 文案）")
    else:
        lines.append("- Java 实例：%s（%s），manifest=%s" % (JAVA.base, JAVA.url, CTX.get("manifest_java")))
    lines.append("- Go 实例：%s（%s），manifest=%s" % (GO.base, GO.url, CTX.get("manifest_go")))
    lines.append("- 请求语言：%s，项目文件备份保留数：%d" % (LANG, BACKUP_COUNT))
    lines.append("- 结果统计：共 %d 项 — PASS %d / DIFF %d / FAIL %d / SKIP %d / UNVERIFIED %d\n" % (
        payload["total"], counts.get("PASS", 0), counts.get("DIFF", 0),
        counts.get("FAIL", 0), counts.get("SKIP", 0), counts.get("UNVERIFIED", 0)))
    lines.append("## 分组统计\n")
    lines.append("| 分组 | 总数 | PASS | DIFF | FAIL | SKIP | UNVERIFIED |")
    lines.append("| --- | --- | --- | --- | --- | --- | --- |")
    for g in GROUP_ORDER:
        items = [r for r in RESULTS if r["group"] == g]
        if not items:
            continue
        lines.append("| %s | %d | %d | %d | %d | %d | %d |" % (
            g, len(items),
            sum(1 for r in items if r["result"] == "PASS"),
            sum(1 for r in items if r["result"] == "DIFF"),
            sum(1 for r in items if r["result"] == "FAIL"),
            sum(1 for r in items if r["result"] == "SKIP"),
            sum(1 for r in items if r["result"] == "UNVERIFIED")))
    lines.append("\n## 逐项结果\n")
    lines.append("| # | 分组 | 用例 | 方法 | 路径 | 结果 | 差异摘要 |")
    lines.append("| --- | --- | --- | --- | --- | --- | --- |")
    for i, r in enumerate(RESULTS, 1):
        summary = "; ".join("%s %s: java=%s go=%s" % (d["type"], d["path"], short(d["java"], 80),
                                                     short(d["go"], 80)) for d in r["diffs"][:4])
        if not summary and r["notes"]:
            summary = short("; ".join(r["notes"]), 160)
        lines.append("| %d | %s | %s | %s | %s | %s | %s |" % (
            i, r["group"], r["name"], r["method"], r.get("path", ""), r["result"],
            summary.replace("|", "\\|")))
    bad = [r for r in RESULTS if r["result"] in ("DIFF", "FAIL")]
    if bad:
        lines.append("\n## DIFF / FAIL 明细（脱敏）\n")
        for r in bad:
            lines.append("### [%s] %s（%s %s）" % (r["result"], r["name"], r["method"], r.get("path", "")))
            if r.get("note"):
                lines.append("- 说明：%s" % r["note"])
            for d in r["diffs"][:12]:
                lines.append("- 差异 %s %s：java=%s / go=%s" % (
                    d["type"], d["path"], short(d["java"], 300), short(d["go"], 300)))
            for n in r["notes"]:
                lines.append("- 备注：%s" % n)
            lines.append("- Java 摘要：%s" % short(json.dumps(r["java"], ensure_ascii=False), 700))
            lines.append("- Go 摘要：%s" % short(json.dumps(r["go"], ensure_ascii=False), 700))
            lines.append("")
    skips = [r for r in RESULTS if r["result"] == "SKIP"]
    if skips:
        lines.append("\n## 跳过项\n")
        for r in skips:
            lines.append("- %s（%s）：%s" % (r["name"], r["path"], r["note"]))
    with open(os.path.join(RESULT_DIR, "summary.md"), "w", encoding="utf-8") as fp:
        fp.write("\n".join(lines) + "\n")

    print("\n================ E2E 结果 ================", file=sys.stderr)
    print("总数 %d | PASS %d | DIFF %d | FAIL %d | SKIP %d | UNVERIFIED %d" % (
        payload["total"], counts.get("PASS", 0), counts.get("DIFF", 0),
        counts.get("FAIL", 0), counts.get("SKIP", 0), counts.get("UNVERIFIED", 0)), file=sys.stderr)
    for r in RESULTS:
        if r["result"] in ("DIFF", "FAIL"):
            print("  [%s] %-44s %s" % (r["result"], r["name"],
                                       "; ".join(d["type"] + ":" + d["path"] for d in r["diffs"][:3])),
                  file=sys.stderr)
    print("摘要: %s" % os.path.join(RESULT_DIR, "summary.md"), file=sys.stderr)
    print("详细: %s" % os.path.join(RESULT_DIR, "result.json"), file=sys.stderr)
    print("=========================================", file=sys.stderr)


def main():
    for inst in INSTS:
        inst.vars["APP_PATH"] = inst.app
        inst.vars["PROJECT_ID"] = inst.project_id
        inst.vars["DEMO_JAR"] = inst.demo_jar
        try:
            with open(inst.path("bin", "agent.pid"), encoding="utf-8") as fp:
                inst.vars["AGENT_PID"] = fp.read().strip()
        except Exception:
            inst.vars["AGENT_PID"] = "0"
    os.makedirs(RESULT_DIR, exist_ok=True)
    if GO_ONLY:
        try:
            go_only_suite()
        except Exception:
            traceback.print_exc()
            print("go-only 驱动异常中断，仍输出已有结果", file=sys.stderr)
        finally:
            write_reports()
        return 0
    try:
        group_read()
        group_project()
        group_script()
        group_env()
        group_system()
    except Exception:
        traceback.print_exc()
        print("驱动异常中断，仍输出已有结果", file=sys.stderr)
    finally:
        write_reports()
    return 0


if __name__ == "__main__":
    sys.exit(main())
