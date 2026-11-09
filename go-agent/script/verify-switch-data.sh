#!/usr/bin/env bash
# verify-switch-data.sh — Java Agent <-> Go Agent 平滑切换的数据兼容性检查（切换前/后各跑一次）
#
# 用法：
#   切换前快照：  ./verify-switch-data.sh --mode snapshot --data <data-dir> --out /tmp/switch-before.json
#   切换后比对：  ./verify-switch-data.sh --mode compare  --data <data-dir> --baseline /tmp/switch-before.json
#   两目录对比：  ./verify-switch-data.sh --mode diff --before <java-data> --after <go-data> [--expect-install-id ID] [--json out.json]
#
# 检查项：必需文件/可解析性、id 集合、实体字段集合、字段 JSON 类型、whitelist、INSTALL installId、
#         remote_version 字段、目录结构、紧凑 JSON 格式、agent_authorize 权限。
# 退出码：0 = 通过（可能有 warning）；1 = 有 error（不建议继续切换）。
set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" >/dev/null 2>&1 && pwd)"
PY="$SCRIPT_DIR/verify-switch-data.py"

MODE=""
DATA=""
OUT=""
BASELINE=""
BEFORE=""
AFTER=""
EXPECT_INSTALL=""

usage() { sed -n '2,14p' "$0" | sed 's/^# \{0,1\}//'; }

while [ $# -gt 0 ]; do
  case "$1" in
    --mode) MODE="$2"; shift 2 ;;
    --data) DATA="$2"; shift 2 ;;
    --out) OUT="$2"; shift 2 ;;
    --baseline) BASELINE="$2"; shift 2 ;;
    --before) BEFORE="$2"; shift 2 ;;
    --after) AFTER="$2"; shift 2 ;;
    --expect-install-id) EXPECT_INSTALL="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "未知参数: $1" >&2; exit 2 ;;
  esac
done

[ -f "$PY" ] || { echo "缺少 $PY" >&2; exit 2; }

snapshot() {
  local data="$1" out="$2"
  [ -d "$data" ] || { echo "数据目录不存在: $data" >&2; exit 2; }
  python3 - "$data" "$out" <<'PY'
import hashlib, json, os, sys
data, out = sys.argv[1], sys.argv[2]

def load(p):
    try:
        return json.load(open(p, encoding="utf-8"))
    except Exception:
        return None

def shape(o):
    if isinstance(o, dict):
        return {k: shape(v) for k, v in o.items()}
    if isinstance(o, list):
        return ["<list>"]
    return type(o).__name__

snap = {"files": {}, "shapes": {}, "sha1": {}}
for name in sorted(os.listdir(data)):
    p = os.path.join(data, name)
    if not os.path.isfile(p):
        continue
    snap["files"][name] = os.path.getsize(p)
    snap["sha1"][name] = hashlib.sha1(open(p, "rb").read()).hexdigest()
    if name.endswith(".json"):
        d = load(p)
        snap["shapes"][name] = shape(d) if d is not None else {"__error__": True}
json.dump(snap, open(out, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
print("snapshot ->", out, "files:", len(snap["files"]))
PY
}

compare_baseline() {
  local data="$1" baseline="$2"
  python3 - "$data" "$baseline" <<'PY'
import json, os, sys
data, baseline = sys.argv[1], sys.argv[2]
base = json.load(open(baseline, encoding="utf-8"))
errors = 0

def load(p):
    try:
        return json.load(open(p, encoding="utf-8"))
    except Exception:
        return None

def shape(o):
    if isinstance(o, dict):
        return {k: shape(v) for k, v in o.items()}
    if isinstance(o, list):
        return ["<list>"]
    return type(o).__name__

def walk_cmp(a, b, path=""):
    global errors
    if isinstance(a, dict) and isinstance(b, dict):
        for k in a:
            if k not in b:
                print("[ERROR] 字段丢失 %s.%s" % (path, k)); errors += 1
            else:
                walk_cmp(a[k], b[k], path + "." + k)
    elif isinstance(a, list) and isinstance(b, list):
        pass
    elif type(a) != type(b):
        print("[ERROR] 类型变化 %s: %s -> %s" % (path, type(a).__name__, type(b).__name__)); errors += 1

for name, shp in base.get("shapes", {}).items():
    p = os.path.join(data, name)
    if not os.path.isfile(p):
        print("[ERROR] 文件丢失 %s" % name); errors += 1
        continue
    now = shape(load(p))
    walk_cmp(shp, now, name)
    print("[PASS] %s 结构" % name)
for name in base.get("files", {}):
    if not os.path.isfile(os.path.join(data, name)):
        print("[ERROR] 文件丢失 %s" % name); errors += 1
print("\n== summary: %d error(s) ==" % errors)
sys.exit(1 if errors else 0)
PY
}

case "$MODE" in
  snapshot)
    [ -n "$DATA" ] && [ -n "$OUT" ] || { echo "--mode snapshot 需要 --data 与 --out" >&2; exit 2; }
    snapshot "$DATA" "$OUT"
    ;;
  compare)
    [ -n "$DATA" ] && [ -f "$BASELINE" ] || { echo "--mode compare 需要 --data 与 --baseline" >&2; exit 2; }
    compare_baseline "$DATA" "$BASELINE"
    ;;
  diff)
    [ -n "$BEFORE" ] && [ -n "$AFTER" ] || { echo "--mode diff 需要 --before 与 --after" >&2; exit 2; }
    ARGS=(--before "$BEFORE" --after "$AFTER")
    [ -n "$EXPECT_INSTALL" ] && ARGS+=(--expect-install-id "$EXPECT_INSTALL")
    [ -n "$OUT" ] && ARGS+=(--json "$OUT")
    python3 "$PY" ${ARGS[@]}
    ;;
  *) usage; exit 2 ;;
esac

