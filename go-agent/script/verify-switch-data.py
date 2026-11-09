#!/usr/bin/env python3
# verify-switch-data.py — Java Agent <-> Go Agent 数据目录兼容性检查（切换前后使用）
#
# 检查内容：
#   1. data/ 必需文件是否存在、是否可解析为 JSON
#   2. 以 id 为 key 的业务文件（project.json/script.json/script_log.json/workspace_env_var.json）：
#      id 集合、每个实体的字段集合、字段 JSON 类型是否一致（以 Java 侧为基准）
#   3. whitelistDirectory.json 字段与类型
#   4. INSTALL.json installId 是否稳定（可用 --expect-install-id 指定期望值）
#   5. remote_version.json 缓存字段（允许 Go 多出字段，但要报告）
#   6. 目录结构：script/<id>/log、global-script、temp、script_run_cache
#   7. 文件格式：紧凑 JSON 检查（Java 写紧凑；Go 也应紧凑，仅告警不失败）
#   8. 文件权限：agent_authorize.json 应为 600（告警）
#
# 用法：
#   verify-switch-data.py --before <java-data-dir> --after <go-data-dir> [--expect-install-id ID] [--json out.json]
#   退出码：0 全部通过（可能有 warning）；1 存在 error
import argparse
import json
import os
import stat
import sys

REQUIRED = ["INSTALL.json", "agent_authorize.json", "project.json", "script.json",
            "whitelistDirectory.json", "workspace_env_var.json"]
OPTIONAL = ["script_log.json", "remote_version.json"]


class Report:
    def __init__(self):
        self.rows = []
        self.errors = 0
        self.warns = 0

    def add(self, level, name, detail=""):
        self.rows.append({"level": level, "name": name, "detail": detail})
        if level == "ERROR":
            self.errors += 1
        elif level == "WARN":
            self.warns += 1
        print("[%s] %s %s" % (level, name, detail))

    def dump(self, path):
        with open(path, "w", encoding="utf-8") as f:
            json.dump({"errors": self.errors, "warns": self.warns, "rows": self.rows},
                      f, ensure_ascii=False, indent=2)


def load_json(path):
    try:
        with open(path, "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception as e:
        return {"__error__": str(e)}


def entity_shape(obj):
    """返回 (field -> json type) 映射；用于比较实体字段集合与类型"""
    types = {}
    if not isinstance(obj, dict):
        return types
    for k, v in obj.items():
        if isinstance(v, bool):
            types[k] = "boolean"
        elif isinstance(v, (int, float)):
            types[k] = "number"
        elif v is None:
            types[k] = "null"
        elif isinstance(v, dict):
            types[k] = "object"
        elif isinstance(v, list):
            types[k] = "array"
        else:
            types[k] = "string"
    return types


def compare_idmap(rep, name, before, after):
    if not isinstance(before, dict) or "__error__" in before:
        rep.add("ERROR", name, "基准文件无法解析")
        return
    if not isinstance(after, dict) or "__error__" in after:
        rep.add("ERROR", name, "目标文件无法解析")
        return
    miss_ids = sorted(set(before) - set(after))
    extra_ids = sorted(set(after) - set(before))
    if miss_ids:
        rep.add("ERROR", name, "丢失 id: %s" % ",".join(miss_ids[:8]))
    if extra_ids:
        rep.add("WARN", name, "新增 id: %s" % ",".join(extra_ids[:8]))
    field_errors = 0
    for pid in sorted(set(before) & set(after)):
        sb, sa = entity_shape(before[pid]), entity_shape(after[pid])
        missing = sorted(set(sb) - set(sa))
        extra = sorted(set(sa) - set(sb))
        type_diff = sorted(k for k in set(sb) & set(sa) if sb[k] != sa[k])
        if missing:
            rep.add("ERROR", "%s[%s]" % (name, pid), "缺少字段: %s" % ",".join(missing[:10]))
            field_errors += 1
        if extra:
            rep.add("WARN", "%s[%s]" % (name, pid), "新增字段: %s" % ",".join(extra[:10]))
        if type_diff:
            rep.add("ERROR", "%s[%s]" % (name, pid),
                    "字段类型变化: %s" % ",".join("%s(%s->%s)" % (k, sb[k], sa[k]) for k in type_diff[:10]))
            field_errors += 1
        if field_errors > 20:
            break
    if not miss_ids and not field_errors:
        rep.add("PASS", name, "id/字段/类型一致（%d 个实体）" % len(before))


def compare_whitelist(rep, before, after):
    for key in ("project", "allowEditSuffix"):
        vb = before.get(key)
        va = after.get(key)
        if type(vb) != type(va):
            rep.add("ERROR", "whitelistDirectory.%s" % key, "类型变化: %s -> %s" % (type(vb), type(va)))
        elif isinstance(vb, list) and sorted(vb) != sorted(va):
            rep.add("INFO", "whitelistDirectory.%s" % key, "内容变化（可能正常）: %s -> %s" % (vb, va))
        else:
            rep.add("PASS", "whitelistDirectory.%s" % key, "")


def check_format(rep, name, path):
    try:
        raw = open(path, "rb").read()
    except Exception as e:
        rep.add("ERROR", name, "无法读取: %s" % e)
        return
    if raw.startswith(b"\xef\xbb\xbf"):
        rep.add("WARN", name, "存在 UTF-8 BOM（Java 不写 BOM）")
    elif b"\n" in raw.strip():
        rep.add("WARN", name, "非紧凑 JSON（含换行；Java 写紧凑格式）")
    else:
        rep.add("PASS", name, "紧凑 JSON")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--before", required=True, help="基准数据目录（切换前，通常是 Java 的 data）")
    ap.add_argument("--after", required=True, help="目标数据目录（切换后，通常是 Go 的 data）")
    ap.add_argument("--expect-install-id", default="")
    ap.add_argument("--json", default="")
    args = ap.parse_args()
    rep = Report()

    b, a = args.before, args.after
    if not os.path.isdir(b):
        rep.add("ERROR", "before", "目录不存在: %s" % b)
    if not os.path.isdir(a):
        rep.add("ERROR", "after", "目录不存在: %s" % a)
    if rep.errors:
        return 1

    for name in REQUIRED:
        pb, pa = os.path.join(b, name), os.path.join(a, name)
        if not os.path.isfile(pa):
            rep.add("ERROR", name, "目标缺失")
        elif not os.path.isfile(pb):
            rep.add("WARN", name, "基准缺失（跳过比较）")
        else:
            rep.add("PASS", name, "存在")
    for name in OPTIONAL:
        if os.path.isfile(os.path.join(a, name)):
            rep.add("PASS", name, "存在")

    # INSTALL.json
    ib = load_json(os.path.join(b, "INSTALL.json"))
    ia = load_json(os.path.join(a, "INSTALL.json"))
    if "installId" not in ia or not ia.get("installId"):
        rep.add("ERROR", "INSTALL.json", "installId 缺失（Server 侧节点绑定会失效）")
    else:
        if args.expect_install_id and ia["installId"] != args.expect_install_id:
            rep.add("ERROR", "INSTALL.json", "installId 期望 %s 实际 %s" % (args.expect_install_id, ia["installId"]))
        elif ib.get("installId") and ib["installId"] != ia["installId"]:
            rep.add("WARN", "INSTALL.json", "installId 变化 %s -> %s" % (ib.get("installId"), ia["installId"]))
        else:
            rep.add("PASS", "INSTALL.json", "installId=%s" % ia["installId"])

    # 授权文件权限
    auth = os.path.join(a, "agent_authorize.json")
    if os.path.isfile(auth):
        mode = stat.S_IMODE(os.stat(auth).st_mode)
        if mode & 0o077:
            rep.add("WARN", "agent_authorize.json", "权限过宽 %o（建议 600）" % mode)
        else:
            rep.add("PASS", "agent_authorize.json", "权限 %o" % mode)
        d = load_json(auth)
        if "agentName" not in d or "agentPwd" not in d:
            rep.add("ERROR", "agent_authorize.json", "字段缺失")

    # 业务 id-map 文件
    for name in ("project.json", "script.json", "workspace_env_var.json"):
        pb = os.path.join(b, name)
        pa = os.path.join(a, name)
        if os.path.isfile(pb) and os.path.isfile(pa):
            compare_idmap(rep, name, load_json(pb), load_json(pa))
    if os.path.isfile(os.path.join(a, "script_log.json")):
        pb, pa = os.path.join(b, "script_log.json"), os.path.join(a, "script_log.json")
        if os.path.isfile(pb):
            compare_idmap(rep, "script_log.json", load_json(pb), load_json(pa))
        else:
            rep.add("INFO", "script_log.json", "基准无该文件（忽略）")

    # whitelist
    pb, pa = os.path.join(b, "whitelistDirectory.json"), os.path.join(a, "whitelistDirectory.json")
    if os.path.isfile(pb) and os.path.isfile(pa):
        compare_whitelist(rep, load_json(pb), load_json(pa))

    # remote_version 缓存字段
    rv_b, rv_a = os.path.join(b, "remote_version.json"), os.path.join(a, "remote_version.json")
    if os.path.isfile(rv_a):
        da, db = load_json(rv_a), load_json(rv_b) if os.path.isfile(rv_b) else {}
        extra = sorted(set(da) - set(db))
        missing = sorted(set(db) - set(da))
        if missing:
            rep.add("ERROR", "remote_version.json", "缺少字段: %s" % ",".join(missing))
        elif extra:
            rep.add("WARN", "remote_version.json", "多出字段（Java 可忽略，但建议对齐）: %s" % ",".join(extra))
        else:
            rep.add("PASS", "remote_version.json", "字段一致")

    # 目录结构
    for d in ("script", "global-script", "temp", "script_run_cache"):
        p = os.path.join(a, d)
        if os.path.isdir(p):
            rep.add("PASS", "dir:%s" % d, "")
        else:
            rep.add("WARN", "dir:%s" % d, "目录不存在（首次启动后可自动创建）")

    # 格式检查（仅告警）
    for name in ("project.json", "script.json", "workspace_env_var.json", "whitelistDirectory.json"):
        p = os.path.join(a, name)
        if os.path.isfile(p):
            check_format(rep, name, p)

    print("\n== summary: %d error(s), %d warning(s) ==" % (rep.errors, rep.warns))
    if args.json:
        rep.dump(args.json)
    return 1 if rep.errors else 0


if __name__ == "__main__":
    sys.exit(main())

