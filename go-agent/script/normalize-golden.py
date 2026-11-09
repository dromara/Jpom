#!/usr/bin/env python3
# normalize-golden.py — 黄金响应归一化/脱敏 + 结构比对（v2）
#
# 原则：
#   1. 只改值和动态 key 名，不改结构（层级 + JSON 类型），产物用于 Go 端「结构回归」。
#   2. 业务码/枚举/布尔等稳定值保留（code 必须保留 200/405/900）。
#   3. 脱敏规则集中可审查；不读取任何凭据。
#
# 用法：
#   normalize-golden.py --in raw/info.json --out testdata/info.json
#   normalize-golden.py --in-dir raw --out-dir testdata
#   normalize-golden.py --check testdata --in-dir raw
import argparse
import json
import os
import re
import sys

# 稳定值 key：不做数值清零
KEEP_INT_KEYS = {"code", "cpuNum", "osCpuCores", "priority", "major", "minor", "ifType",
                 "index", "privacy", "triggerExecType", "totalSlice", "nowSlice",
                 "executeCount", "failedCount", "succeedCount", "pluginSize",
                 "scriptExecLogSize", "projectCount", "scriptCount", "javaVirtualCount",
                 "systemSleep", "pId", "line", "getLine", "pullCount", "stripComponents"}
# 数值型动态值（整型）
INT_ZERO_KEYS = {"port", "pid", "processId", "pidPort", "readFileOnLineCount", "start",
                 "end", "size", "completeSize", "fileSize", "dataSize", "oldJarsSize"}
# 数值型动态值（浮点）
FLOAT_ZERO_KEYS = {"cpu", "memory", "swapMemory", "virtualMemory", "disk", "used", "toTal",
                   "sys", "user", "wait", "free", "processCpuLoadCumulative",
                   "osLoadAverage", "subnetMasks", "prefixLengths"}
# 字符串型动态值
STRING_DYNAMIC_KEYS = {
    "randomId": "<uuid>", "installId": "<uuid>", "id": "<id>", "nodeId": "<node-id>",
    "workspaceId": "<workspace-id>", "scriptId": "<script-id>", "executeId": "<execute-id>",
    "linkId": "<link-id>", "machineId": "<machine-id>", "key": "<key>",
    "version": "<version>", "agentName": "<agent-name>", "agentPwd": "<redacted>",
    "tagName": "<tag-name>", "token": "<redacted>", "downloadSource": "<download-source>",
}
# 路径类（保留 example 目录风格）
PATH_KEYS = {"dataPath", "jarFile", "savePath", "file", "logPath", "logBackPath",
             "whitelistDirectory", "lib", "runPath", "tempPath", "path", "logFile",
             "levelName", "newJar", "beforeJar", "fileName"}
# 主机/系统类
HOST_KEYS = {"hostName": "<host-name>", "osName": "<os-name>", "osVersion": "<os-version>",
             "hardwareVersion": "<hardware>", "cpuModel": "<cpu-model>",
             "osCpuIdentifierName": "<cpu-model>", "commandLine": "<command-line>",
             "runCommand": "<run-command>", "jvm": "<jvm-args>", "args": "<program-args>"}
# 人员/业务名
USER_KEYS = {"user": "<user>", "modifyUser": "<user>", "createUser": "<user>",
             "lastRunUser": "<user>", "optUser": "<user>", "name": "<name>",
             "group": "<group>", "containerId": "<container-id>", "description": "<description>"}
# 文本内容类
TEXT_KEYS = {"changelog": "<changelog>", "content": "<file-content>", "context": "<script-content>",
             "script": "<script-content>", "dslContent": "<dsl-content>", "dslEnv": "<dsl-env>",
             "defArgs": "<def-args>", "msg": None, "error": "<error>", "statusMsg": "<status-msg>"}
# 列表/映射类动态内容
LIST_DYNAMIC = {"envVarKeys": ["<env>"], "hostIpv4s": ["10.0.0.1"], "hostIpv6s": [],
                "ipv4addr": ["10.0.0.1"], "ipv6addr": [], "osLoadAverage": [0.0]}

IPV4_RE = re.compile(r"(?<![0-9A-Za-z._/-])(?:\d{1,3}\.){3}\d{1,3}(?![0-9A-Za-z._-])")
MAC_RE = re.compile(r"(?<![0-9a-fA-F])[0-9a-fA-F]{2}(?::[0-9a-fA-F]{2}){5}(?![0-9a-fA-F-])")
UUID_RE = re.compile(r"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
HEX32_RE = re.compile(r"(?<![0-9a-f])[0-9a-f]{32}(?![0-9a-f])")
HEX40_RE = re.compile(r"(?<![0-9a-f])[0-9a-f]{40}(?![0-9a-f])")
HOME_RE = re.compile(r"/home/[^/\s\"']+")
DATETIME_RE = re.compile(r"\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}(?::\d{2})?")
PROJECT_ID_RE = re.compile(r"^[A-Z0-9]{6}$")
PURE_NUM_RE = re.compile(r"^\d+(\.\d+)?$")
VERSION_IN_TEXT_RE = re.compile(r"\d+\.\d+\.\d+(?:\.\d+)?")
PATH_SEGMENT_MAP = {"/home/example/jpom-demo": "/home/example/deploy"}
DATE_ONLY_RE = re.compile(r"^\d{4}-\d{2}-\d{2}$")

# IP 替换白名单 key（避免把 2.11.13.1 版本号当 IP）
IP_SAFE_KEYS = {"hostIpv4s", "ipv4addr", "hostIpv6s", "ipv6addr", "hostName", "address",
                "bindAddress", "ip", "clientIp"}


def sanitize_string(s, key=None):
    if not isinstance(s, str):
        return s
    if len(s) > 2000:
        return "<long-text:%d>" % len(s)
    if key in ("version", "osVersion", "hardwareVersion", "changelogUrl", "agentUrl",
               "serverUrl", "runCommand", "commandLine"):
        return s
    s = HEX40_RE.sub("<sha1>", s)
    s = HEX32_RE.sub("<hex32>", s)
    s = UUID_RE.sub("<uuid>", s)
    s = MAC_RE.sub("02:00:00:00:00:00", s)
    if key in IP_SAFE_KEYS or "." in s:
        s = IPV4_RE.sub("10.0.0.1", s)
    s = HOME_RE.sub("/home/example", s)
    for a, b in PATH_SEGMENT_MAP.items():
        s = s.replace(a, b)
    s = VERSION_IN_TEXT_RE.sub("<version>", s)
    s = DATETIME_RE.sub("2024-01-01 00:00:00", s)
    if PURE_NUM_RE.match(s) and key not in ("code", "version"):
        return "0"
    if re.match(r"^\d{4}-\d{2}-\d{2}$", s):
        return "<date>"
    return s


def normalize(obj, key=None):
    if isinstance(obj, dict):
        out = {}
        for k, v in obj.items():
            nk = k
            if k in ("DEFAULT", "GLOBAL", "system"):
                nk = k
            elif HEX32_RE.fullmatch(k) or UUID_RE.fullmatch(k):
                nk = "<map-id>"
            elif PROJECT_ID_RE.match(k) and k != "DEFAULT":
                nk = "<map-project-id>"
            elif DATE_ONLY_RE.match(k):
                nk = "<date>"
            elif k in ("stdout.log", "agent.log"):
                nk = k
            elif k not in ("id", "name", "code", "msg", "data") and len(k) > 20:
                nk = "<map-key>"
            out[nk] = normalize(v, k)
        return out
    if isinstance(obj, list):
        if key in LIST_DYNAMIC:
            return LIST_DYNAMIC[key]
        items = [normalize(v, key) for v in obj]
        # 只为回归保留形状：数组最多保留 3 个元素，避免 testdata 过大（结构比对只看首元素）
        return items[:3]
    if isinstance(obj, bool) or obj is None:
        return obj
    if isinstance(obj, int):
        if key in KEEP_INT_KEYS:
            return obj
        if key in INT_ZERO_KEYS:
            return 0
        return 0 if obj != 0 else 0
    if isinstance(obj, float):
        return 0.0
    if isinstance(obj, str):
        if key in STRING_DYNAMIC_KEYS:
            return STRING_DYNAMIC_KEYS[key]
        if key in HOST_KEYS:
            return HOST_KEYS[key]
        if key in USER_KEYS:
            return USER_KEYS[key]
        if key in TEXT_KEYS:
            return TEXT_KEYS[key] if TEXT_KEYS[key] is not None else obj
        if key in PATH_KEYS:
            return sanitize_string(obj, key)
        if key in ("dateTime",):
            return "<datetime>"
        if key in ("timeZoneId",):
            return "<timezone>"
        if key in ("changelogUrl", "agentUrl", "serverUrl", "downloadSource"):
            return "<url>"
        if key in ("auth",):
            return obj
        return sanitize_string(obj, key)
    return obj


SHAPE_TYPE = {dict: "object", list: "array", str: "string", bool: "boolean",
              int: "number", float: "number"}


def shape(obj, path="$"):
    out = []
    if isinstance(obj, dict):
        out.append((path, "object"))
        for k in sorted(obj.keys()):
            out.extend(shape(obj[k], path + "." + k))
    elif isinstance(obj, list):
        out.append((path, "array"))
        if obj:
            out.extend(shape(obj[0], path + "[]"))
    else:
        out.append((path, SHAPE_TYPE.get(type(obj), "unknown")))
    return out


def load_json(path):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def write_json(path, obj):
    d = os.path.dirname(path)
    if d:
        os.makedirs(d, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write("\n")


def normalize_file(src, dst):
    norm = normalize(load_json(src))
    write_json(dst, norm)
    return norm


def compare(norm_live, norm_expect, name):
    a, b = set(shape(norm_live)), set(shape(norm_expect))
    missing, extra = sorted(b - a), sorted(a - b)
    if not missing and not extra:
        return True
    print("[MISMATCH] %s" % name, file=sys.stderr)
    for p, t in missing:
        print("  - missing: %s (%s)" % (p, t), file=sys.stderr)
    for p, t in extra:
        print("  + extra:   %s (%s)" % (p, t), file=sys.stderr)
    return False


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--in", dest="inp")
    ap.add_argument("--out", dest="out")
    ap.add_argument("--in-dir", dest="in_dir")
    ap.add_argument("--out-dir", dest="out_dir")
    ap.add_argument("--check", dest="check_dir")
    args = ap.parse_args()

    if args.check_dir:
        if not args.in_dir:
            print("--check 需要配合 --in-dir", file=sys.stderr)
            return 2
        mf = os.path.join(args.check_dir, "_endpoints.json")
        mapping = load_json(mf) if os.path.isfile(mf) else {}
        failures = 0
        for slug, ep in sorted(mapping.items()):
            raw = os.path.join(args.in_dir, slug + ".json")
            exp = os.path.join(args.check_dir, slug + ".json")
            if not os.path.isfile(raw) or not os.path.isfile(exp):
                print("[SKIP] %s (%s)" % (slug, ep), file=sys.stderr)
                continue
            if not compare(normalize(load_json(raw)), load_json(exp), slug):
                failures += 1
        print("check done: %d failure(s)" % failures, file=sys.stderr)
        return 1 if failures else 0

    if args.in_dir:
        out_dir = args.out_dir or "testdata"
        os.makedirs(out_dir, exist_ok=True)
        for fn in sorted(os.listdir(args.in_dir)):
            if fn.endswith(".json"):
                normalize_file(os.path.join(args.in_dir, fn), os.path.join(out_dir, fn))
                print("normalized %s" % fn, file=sys.stderr)
        return 0

    if args.inp and args.out:
        normalize_file(args.inp, args.out)
        return 0

    ap.print_help()
    return 2


if __name__ == "__main__":
    sys.exit(main())

