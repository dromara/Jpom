#!/usr/bin/env bash
# capture-golden.sh — 从 Java Agent（或 Go Agent）抓取 HTTP 黄金响应，归一化+脱敏后产出可入库 testdata
#
# 用途：
#   1. 抓取：向 live Agent 发送 POST 请求，原始响应默认写 /tmp/jpom-golden-raw（仓库外，不入库）
#   2. 归一化：动态字段（pid/时间戳/uuid/路径/主机名/大小…）替换为占位符，保留 JSON 结构与字段类型
#   3. 脱敏：真实路径/主机名/IP/用户名/项目名/业务文本替换为 example 占位值
#   4. 比对：--check 用归一化后的结构（键集合 + JSON 类型）与 testdata 比对，忽略值，用于 Go Agent 回归
#
# 安全红线：
#   - 不打印、不落盘 agent 密码/鉴权哈希（鉴权值只在内存/管道中传给 curl）
#   - 原始响应默认写仓库外（/tmp/jpom-golden-raw），禁止把含真实值的产物提交进仓库
#   - 输出默认强制脱敏；--allow-raw 仅用于本地排查
#
# 用法：
#   ./capture-golden.sh --capture info get-stat-info disk-info
#   ./capture-golden.sh --capture --endpoint-file endpoints.txt
#   ./capture-golden.sh --normalize raw/info.json --out testdata/info.json
#   ./capture-golden.sh --normalize-dir raw --out testdata
#   ./capture-golden.sh --check testdata --agent-url http://127.0.0.1:2124
#
# 鉴权（二选一）：
#   JPOM_AGENT_AUTHORIZE_FILE  agent_authorize.json 路径（默认 /home/user/jpom/agent/data/agent_authorize.json）
#   JPOM_AGENT_AUTHORIZE       直接给 sha1 值（不要写进仓库/脚本）
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" >/dev/null 2>&1 && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

AGENT_URL=${JPOM_AGENT_URL:-http://127.0.0.1:2123}
WORKSPACE=${JPOM_WORKSPACE:-DEFAULT}
LANG_HDR=${JPOM_LANG_HDR:-zh-CN}
SERVER_USER=${JPOM_SERVER_USER_NAME:-example-user}
AUTHORIZE_FILE=${JPOM_AGENT_AUTHORIZE_FILE:-/home/user/jpom/agent/data/agent_authorize.json}
AUTHORIZE=${JPOM_AGENT_AUTHORIZE:-}
SSH_TARGET=${JPOM_SSH_TARGET:-}
RAW_DIR=${RAW_DIR:-/tmp/jpom-golden-raw}
TESTDATA_DIR=${TESTDATA_DIR:-$ROOT_DIR/testdata}
TIMEOUT=${JPOM_TIMEOUT:-30}
NORMALIZER="$SCRIPT_DIR/normalize-golden.py"
MODE=""
ALLOW_RAW=0
ENDPOINTS=()
ENDPOINT_FILE=""
NORMALIZE_IN=""
CHECK_DIR=""

log() { printf '[capture-golden] %s\n' "$*" >&2; }
die() { printf '[capture-golden][ERROR] %s\n' "$*" >&2; exit 1; }

usage() {
  sed -n '2,30p' "$0" | sed 's/^# \{0,1\}//'
  exit 0
}

while [ $# -gt 0 ]; do
  case "$1" in
    --capture) MODE="capture"; shift ;;
    --normalize) MODE="normalize"; NORMALIZE_IN="$2"; shift 2 ;;
    --normalize-dir) MODE="normalize-dir"; NORMALIZE_IN="$2"; shift 2 ;;
    --check) MODE="check"; CHECK_DIR="$2"; shift 2 ;;
    --agent-url) AGENT_URL="$2"; shift 2 ;;
    --workspace) WORKSPACE="$2"; shift 2 ;;
    --lang) LANG_HDR="$2"; shift 2 ;;
    --user-name) SERVER_USER="$2"; shift 2 ;;
    --authorize-file) AUTHORIZE_FILE="$2"; shift 2 ;;
    --ssh) SSH_TARGET="$2"; shift 2 ;;
    --raw-dir) RAW_DIR="$2"; shift 2 ;;
    --out) TESTDATA_DIR="$2"; shift 2 ;;
    --timeout) TIMEOUT="$2"; shift 2 ;;
    --endpoint-file) ENDPOINT_FILE="$2"; shift 2 ;;
    --allow-raw) ALLOW_RAW=1; shift ;;
    -h|--help) usage ;;
    -*) die "未知参数：$1（--help 查看用法）" ;;
    *) ENDPOINTS+=("$1"); shift ;;
  esac
done

[ -n "$MODE" ] || usage

# ---------- 鉴权：只从文件/环境变量取，绝不打印 ----------
load_authorize() {
  if [ -n "$AUTHORIZE" ]; then
    return 0
  fi
  if [ -n "$SSH_TARGET" ]; then
    AUTHORIZE="$(ssh -F /dev/null -o StrictHostKeyChecking=accept-new -o UserKnownHostsFile=/tmp/dev_kh "$SSH_TARGET" \
      "python3 -c 'import json,hashlib;d=json.load(open(\"$AUTHORIZE_FILE\"));print(hashlib.sha1((d[\"agentName\"]+\"@\"+d[\"agentPwd\"]).encode()).hexdigest())'" 2>/dev/null)"
  else
    [ -f "$AUTHORIZE_FILE" ] || die "找不到鉴权文件 $AUTHORIZE_FILE（可用 JPOM_AGENT_AUTHORIZE 直接给 sha1）"
    AUTHORIZE="$(python3 -c 'import json,hashlib,sys;d=json.load(open(sys.argv[1]));print(hashlib.sha1((d["agentName"]+"@"+d["agentPwd"]).encode()).hexdigest())' "$AUTHORIZE_FILE")"
  fi
  [ -n "$AUTHORIZE" ] || die "鉴权值计算失败"
}

# ---------- 抓取 ----------
capture_one() {
  local path="$1"
  local slug
  slug="$(printf '%s' "$path" | sed 's|^/||; s|[/.]|_|g')"
  local out="$RAW_DIR/$slug.json"
  mkdir -p "$RAW_DIR"
  log "capture $path -> $out"
  if [ -n "$SSH_TARGET" ]; then
    ssh -F /dev/null -o StrictHostKeyChecking=accept-new -o UserKnownHostsFile=/tmp/dev_kh "$SSH_TARGET" \
      "curl -sS --max-time $TIMEOUT -X POST -H 'Jpom-Agent-Authorize: $AUTHORIZE' -H 'workspaceId: $WORKSPACE' -H 'Jpom-Server-UserName: $SERVER_USER' -H 'Accept-Language: $LANG_HDR' -H 'transport-encryption: 0' -d '' 'http://127.0.0.1:2123$path'" > "$out"
  else
    curl -sS --max-time "$TIMEOUT" -X POST \
      -H "Jpom-Agent-Authorize: $AUTHORIZE" \
      -H "workspaceId: $WORKSPACE" \
      -H "Jpom-Server-UserName: $SERVER_USER" \
      -H "Accept-Language: $LANG_HDR" \
      -H "transport-encryption: 0" \
      -d '' "$AGENT_URL$path" > "$out"
  fi
  [ -s "$out" ] || die "响应为空：$path（检查 agent/端口/鉴权）"
}


# ---------- 归一化（调用 normalize-golden.py） ----------
do_normalize_one() {
  python3 "$NORMALIZER" --in "$1" --out "$2"
}

do_normalize_dir() {
  local src_dir="$1" out_dir="$2"
  mkdir -p "$out_dir"
  local f base
  for f in "$src_dir"/*.json; do
    [ -f "$f" ] || continue
    base="$(basename "$f")"
    case "$base" in _*) continue ;; esac
    do_normalize_one "$f" "$out_dir/$base" || return 1
    log "normalized $f -> $out_dir/$base"
  done
  python3 - "$out_dir" <<'PY_MAP'
import json, os, sys
out_dir = sys.argv[1]
known = {
    "info": "/info", "get_stat_info": "/get-stat-info", "disk_info": "/disk-info",
    "hw_disk__info": "/hw-disk--info", "network_interfaces": "/network-interfaces",
    "processList": "/processList", "script_list_json": "/script/list.json",
    "manage_getProjectInfo": "/manage/getProjectInfo",
    "manage_getProjectItem": "/manage/getProjectItem",
    "manage_getProjectStatus": "/manage/getProjectStatus",
    "manage_log_logSize": "/manage/log/logSize",
    "system_cache": "/system/cache",
    "system_getConfig_json": "/system/getConfig.json",
    "system_log_data_json": "/system/log_data.json",
    "system_check_version_json": "/system/check_version.json",
    "system_whitelistDirectory_data": "/system/whitelistDirectory_data",
    "script_library_list": "/script-library/list",
}
mapping = {}
for fn in sorted(os.listdir(out_dir)):
    if not fn.endswith(".json") or fn.startswith("_"):
        continue
    slug = fn[:-5]
    mapping[slug] = known.get(slug, "/" + slug.replace("_", "/"))
with open(os.path.join(out_dir, "_endpoints.json"), "w", encoding="utf-8") as f:
    json.dump(mapping, f, ensure_ascii=False, indent=2)
    f.write("\n")
PY_MAP
}

capture_to() {
  local path="$1" out="$2" slug
  slug="$(printf '%s' "$path" | sed 's|^/||; s|[/.]|_|g')"
  if [ -n "$SSH_TARGET" ]; then
    ssh -F /dev/null -o StrictHostKeyChecking=accept-new -o UserKnownHostsFile=/tmp/dev_kh "$SSH_TARGET" \
      "curl -sS --max-time $TIMEOUT -X POST -H 'Jpom-Agent-Authorize: $AUTHORIZE' -H 'workspaceId: $WORKSPACE' -H 'Jpom-Server-UserName: $SERVER_USER' -H 'Accept-Language: $LANG_HDR' -d '' 'http://127.0.0.1:2123$path'" > "$out"
  else
    curl -sS --max-time "$TIMEOUT" -X POST \
      -H "Jpom-Agent-Authorize: $AUTHORIZE" \
      -H "workspaceId: $WORKSPACE" \
      -H "Jpom-Server-UserName: $SERVER_USER" \
      -H "Accept-Language: $LANG_HDR" \
      -d '' "$AGENT_URL$path" > "$out"
  fi
}

# ---------- 主流程 ----------
mkdir -p "$RAW_DIR"

case "$MODE" in
  capture)
    load_authorize
    if [ -n "$ENDPOINT_FILE" ]; then
      while IFS= read -r line; do
        [ -n "$line" ] || continue
        case "$line" in \#*) continue ;; esac
        ENDPOINTS+=("$line")
      done < "$ENDPOINT_FILE"
    fi
    [ ${#ENDPOINTS[@]} -gt 0 ] || die "没有指定端点（示例：--capture info get-stat-info）"
    for ep in ${ENDPOINTS[@]}; do
      capture_one "$ep"
    done
    log "抓取完成：$RAW_DIR（未脱敏，禁止入库）"
    if [ "$ALLOW_RAW" -eq 0 ]; then
      do_normalize_dir "$RAW_DIR" "$TESTDATA_DIR" || die "归一化失败"
      log "已生成脱敏 testdata：$TESTDATA_DIR（可入库）"
    else
      log "--allow-raw：跳过归一化（产物含真实值，不要入库）"
    fi
    ;;

  normalize)
    [ -n "$NORMALIZE_IN" ] || die "--normalize 需要输入文件"
    do_normalize_one "$NORMALIZE_IN" "$TESTDATA_DIR"
    log "normalized -> $TESTDATA_DIR"
    ;;

  normalize-dir)
    [ -d "$NORMALIZE_IN" ] || die "--normalize-dir 目录不存在：$NORMALIZE_IN"
    do_normalize_dir "$NORMALIZE_IN" "$TESTDATA_DIR" || die "归一化失败"
    log "normalized dir -> $TESTDATA_DIR"
    ;;

  check)
    [ -d "$CHECK_DIR" ] || die "--check 目录不存在：$CHECK_DIR"
    load_authorize
    TMP_RAW="$(mktemp -d)"
    trap 'rm -rf "$TMP_RAW"' EXIT
    python3 - "$CHECK_DIR" "$TMP_RAW" <<'PY_LIST'
import json, os, sys
check_dir, tmp_raw = sys.argv[1], sys.argv[2]
mapping_file = os.path.join(check_dir, "_endpoints.json")
if os.path.isfile(mapping_file):
    mapping = json.load(open(mapping_file, encoding="utf-8"))
else:
    mapping = {}
    for fn in os.listdir(check_dir):
        if fn.endswith(".json") and not fn.startswith("_"):
            mapping[fn[:-5]] = "/" + fn[:-5].replace("_", "/")
lines = []
for slug, ep in sorted(mapping.items()):
    lines.append("%s\t%s" % (ep, os.path.join(tmp_raw, slug + ".json")))
open(os.path.join(tmp_raw, "list.tsv"), "w", encoding="utf-8").write("\n".join(lines) + "\n")
PY_LIST
    while IFS=$'\t' read -r ep out; do
      [ -n "$ep" ] || continue
      log "check fetch $ep"
      capture_to "$ep" "$out" || log "抓取失败：$ep（跳过）"
    done < "$TMP_RAW/list.tsv"
    python3 "$NORMALIZER" --check "$CHECK_DIR" --in-dir "$TMP_RAW"
    rc=$?
    log "结构比对完成，退出码 $rc（0=一致）"
    exit $rc
    ;;

  *) usage ;;
esac

