#!/usr/bin/env bash
# jpom-ops: Jpom REST API 封装（只读默认 + 显式授权写操作）
# 兼容：Jpom 2.11.x（dev 环境 2.11.13 逐条验证）
# 依赖：bash、curl；python3 可选（JSON 美化 / 严格错误码判定）
#
# 覆盖范围：工作空间、节点、项目、构建、脚本、资产、监控、分发、备份、
#          集群、用户与日志、系统信息。WebSocket 专属能力（终端、脚本执行、
#          Docker CLI、节点控制台）不在本工具范围，详见 capabilities。
set -euo pipefail

BASE_URL="${JPOM_BASE_URL:-}"
WORKSPACE="${JPOM_WORKSPACE:-DEFAULT}"
TOKEN="${JPOM_TOKEN:-}"
PRETTY="${JPOM_PRETTY:-1}"

if [[ -z "$TOKEN" && -n "${JPOM_TOKEN_FILE:-}" ]]; then
  if [[ -f "$JPOM_TOKEN_FILE" ]]; then
    TOKEN="$(tr -d '[:space:]' < "$JPOM_TOKEN_FILE")"
  else
    echo "JPOM_TOKEN_FILE 不存在: $JPOM_TOKEN_FILE" >&2
    exit 2
  fi
fi

usage() {
  cat >&2 <<'EOF'
usage:
  JPOM_BASE_URL=... JPOM_TOKEN_FILE=... jpom-api.sh <command> [--flags]

read-only commands:
  capabilities                          # 能力清单与本工具覆盖范围
  workspaces                            # 当前 token 可访问工作空间
  workspace-list                        # 全部工作空间
  nodes [--page --limit]                # 节点列表
  node-groups                           # 节点分组
  node-monitor --node-id <id>           # 节点监控数据
  node-process --node-id <id>           # 节点进程列表
  node-machine-info --node-id <id>      # 节点机器信息
  projects --node-id <id>               # 节点下项目
  project-info --node-id <id> [--id]    # 项目详情
  builds [--limit]                      # 构建配置
  build-get --id <id>                   # 构建详情
  build-history [--limit]               # 构建历史
  build-log --id <id> --build-id <n> [--line 100]   # 构建日志
  build-groups                          # 构建分组
  repositories [--limit]                # 仓库列表
  repository-groups                     # 仓库分组
  branches --repository-id <id>         # 仓库分支（会访问远程仓库）
  scripts [--limit]                     # 服务端脚本
  scripts-all                           # 服务端脚本全量
  script-logs [--limit]                 # 服务端脚本日志
  node-scripts --node-id <id>           # 节点脚本
  node-script-logs --node-id <id>       # 节点脚本日志
  machines [--limit]                    # 资产-机器
  ssh-assets [--limit]                  # 资产-SSH
  ftp-assets [--limit]                  # 资产-FTP
  docker-assets [--limit]               # 资产-Docker
  monitors [--limit]                    # 项目监控
  dispatch [--limit]                    # 分发/发布项目
  dispatch-logs [--limit]               # 分发日志
  backups [--limit]                     # 系统备份
  clusters [--limit]                    # 集群
  users [--limit]                       # 用户列表
  permission-groups [--limit]           # 用户权限组
  workspace-env [--limit]               # 工作空间环境变量
  login-logs [--limit]                  # 登录日志
  operate-logs [--limit]                # 操作日志
  recent-logs [--limit]                 # 最近操作日志
  log-read [--limit]                    # 托管日志文件
  system-info                           # 系统信息（含版本）
  check-version                         # 新版本检查
  server-logs                           # 服务端日志文件列表
  raw <METHOD> <PATH> [--form k=v]... [--json '{}']

write commands（需 JPOM_OPS_ALLOW_WRITE=true 且显式 --confirm）:
  build-start --id <id> [--branch-name x] [--remark x]
  build-cancel --id <id>
  build-rerelease --log-id <logId>
  project-operate --node-id <nid> --id <pid> --opt start|stop|restart
  node-kill-pid --node-id <nid> --pid <pid>
  system-upgrade [--machine-id <id>]

env:
  JPOM_BASE_URL / JPOM_TOKEN 或 JPOM_TOKEN_FILE / JPOM_WORKSPACE / JPOM_PRETTY=0
  JPOM_STRICT_CODE=1   接口 code 非 200 时以非 0 退出
  JPOM_OPS_ALLOW_WRITE=true  解锁写命令（默认只读；仍需 --confirm）
EOF
  exit 2
}

need_env() {
  [[ -n "$BASE_URL" && -n "$TOKEN" ]] || {
    echo "缺少 JPOM_BASE_URL 或 JPOM_TOKEN/JPOM_TOKEN_FILE" >&2
    exit 2
  }
}

need_val() {
  [[ $# -ge 2 && -n "${2:-}" ]] || { echo "参数 $1 缺少取值" >&2; exit 2; }
}

require_flag() {
  [[ -n "${2:-}" ]] || { echo "缺少必填参数 $1（可用 capabilities 查看命令用法）" >&2; exit 2; }
}

CURL_CFG="$(mktemp "${TMPDIR:-/tmp}/jpom-api.XXXXXX")"
trap 'rm -f "$CURL_CFG"' EXIT

write_cfg() {
  ( umask 077
    printf 'header = "JPOM-USER-TOKEN: %s"\n' "$TOKEN" > "$CURL_CFG"
    printf 'header = "workspaceId: %s"\n' "$WORKSPACE" >> "$CURL_CFG"
  )
}

# 只读白名单：GET/HEAD 一律允许；POST 仅放行下列只读查询接口
is_readonly_path() {
  case "$2" in
    *"/user/recent-log-data"|*"/node/list_data.json"|*"/node/list_group_all.json"|\
    *"/node/project_list"|*"/node/node_monitor_data.json"|*"/node/processList"|\
    *"/node/manage/get_project_info"|*"/node/script/list_all"|*"/node/script_log/list"|\
    *"/build/list"|*"/build/history/history_list.json"|*"/build/manage/get-now-log"|\
    *"/build/repository/list"|*"/build/branch-list"|\
    *"/script/list"|*"/script_log/list"|*"/outgiving/dispatch-list"|\
    *"/outgiving/log_list_data.json"|*"/system/assets/machine/list-data"|\
    *"/system/assets/ssh/list-data"|*"/system/assets/ftp/list-data"|\
    *"/system/assets/docker/list-data"|*"/system/backup/list"|*"/system/log_data.json"|\
    *"/system/info"|*"/system/check_version.json"|\
    *"/monitor/list_data.json"|*"/cluster/list"|*"/user/get_user_list"|\
    *"/user/login-log/list-data"|*"/user/list-operate-log-data"|*"/log-read/list"|    *"/user-permission-group/get-list"|*"/system/workspace_env/list")
      return 0 ;;
  esac
  return 1
}

do_call() {
  local method="$1" path="$2"; shift 2
  need_env
  write_cfg
  local url="${BASE_URL%/}${path}"
  local out
  out="$(curl -sS --config "$CURL_CFG" -X "$method" "$@" -- "$url")" || {
    echo "请求失败: $method $url" >&2
    return 1
  }
  if [[ "$PRETTY" == "1" ]] && command -v python3 >/dev/null 2>&1; then
    printf '%s' "$out" | python3 -m json.tool 2>/dev/null || printf '%s\n' "$out"
  else
    printf '%s\n' "$out"
  fi
  if [[ "${JPOM_STRICT_CODE:-0}" == "1" ]] && command -v python3 >/dev/null 2>&1; then
    local code
    code="$(printf '%s' "$out" | python3 -c 'import sys,json;print(json.load(sys.stdin).get("code",""))' 2>/dev/null || true)"
    if [[ -n "$code" && "$code" != "200" ]]; then
      echo "api error: code=$code ($method $path)" >&2
      return 1
    fi
  fi
}

read_call() {
  local method="$1" path="$2"; shift 2
  if [[ "$method" != "GET" && "$method" != "HEAD" ]] && ! is_readonly_path "$method" "$path"; then
    echo "blocked in read-only mode: $method $path（如需写操作请用写命令并显式授权）" >&2
    exit 3
  fi
  do_call "$method" "$path" "$@"
}

write_guard() {
  local action="$1"
  if [[ "${JPOM_OPS_ALLOW_WRITE:-false}" != "true" ]]; then
    echo "blocked: 写操作默认禁用（$action）。如确需执行：设置 JPOM_OPS_ALLOW_WRITE=true 并加 --confirm" >&2
    exit 3
  fi
  if [[ "${CONFIRM:-false}" != "true" ]]; then
    echo "blocked: 写操作需要显式 --confirm（$action，环境=$BASE_URL，工作空间=$WORKSPACE）" >&2
    exit 4
  fi
}

# ---------------- 参数解析 ----------------
page=1
limit=20
node_id=""
id=""
pid=""
opt=""
branch_name=""
remark=""
build_id_raw=""
line=100
log_id=""
repository_id=""
machine_id=""
CONFIRM=false

cmd="${1:-}"
shift || true

# raw 自带位置参数（METHOD PATH），必须在通用 flag 解析之前处理
if [[ "$cmd" == "raw" ]]; then
  m="${1:-}"
  p="${2:-}"
  [[ -n "$m" && -n "$p" ]] || usage
  shift 2 || true
  args=()
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --form) need_val "$1" "${2:-}"; args+=(-H 'Content-Type: application/x-www-form-urlencoded' --data-urlencode "$2"); shift 2 ;;
      --json) need_val "$1" "${2:-}"; args+=(-H 'Content-Type: application/json' -d "$2"); shift 2 ;;
      *) echo "raw 不支持的参数: $1" >&2; exit 2 ;;
    esac
  done
  read_call "$m" "$p" ${args[@]+"${args[@]}"}
  exit 0
fi

while [[ $# -gt 0 ]]; do
  case "$1" in
    --page)          need_val "$1" "${2:-}"; page="$2"; shift 2 ;;
    --limit)         need_val "$1" "${2:-}"; limit="$2"; shift 2 ;;
    --node-id)       need_val "$1" "${2:-}"; node_id="$2"; shift 2 ;;
    --id)            need_val "$1" "${2:-}"; id="$2"; shift 2 ;;
    --pid)           need_val "$1" "${2:-}"; pid="$2"; shift 2 ;;
    --opt)           need_val "$1" "${2:-}"; opt="$2"; shift 2 ;;
    --branch-name)   need_val "$1" "${2:-}"; branch_name="$2"; shift 2 ;;
    --remark)        need_val "$1" "${2:-}"; remark="$2"; shift 2 ;;
    --build-id)      need_val "$1" "${2:-}"; build_id_raw="$2"; shift 2 ;;
    --line)          need_val "$1" "${2:-}"; line="$2"; shift 2 ;;
    --log-id)        need_val "$1" "${2:-}"; log_id="$2"; shift 2 ;;
    --repository-id) need_val "$1" "${2:-}"; repository_id="$2"; shift 2 ;;
    --machine-id)    need_val "$1" "${2:-}"; machine_id="$2"; shift 2 ;;
    --confirm)       CONFIRM=true; shift ;;
    *) echo "未知参数: $1" >&2; usage ;;
  esac
done

FORM=(-H 'Content-Type: application/x-www-form-urlencoded')

# ---------------- 只读命令 ----------------
case "$cmd" in
  capabilities)
    cat <<'EOF'
jpom-ops 能力覆盖（Jpom 2.11.x REST）：
  已覆盖：工作空间、节点(含监控/进程/分组)、项目(列表/详情)、构建(配置/详情/历史/日志/分组/仓库/分支)、
          服务端与节点脚本(列表/日志)、资产(机器/SSH/FTP/Docker)、项目监控、分发、系统备份、集群、
          用户/权限组/登录日志/操作日志、工作空间环境变量、托管日志、系统信息与版本检查。
  写操作（默认禁用，需 JPOM_OPS_ALLOW_WRITE=true + --confirm）：
          触发构建/取消/重建、项目启停重启、杀进程、在线升级。
  不在范围（WebSocket/交互式通道，无法用 REST 安全封装）：
          终端(SSH/Docker CLI/节点控制台)、脚本在线执行、文件浏览与上传、Docker 命令下发。
EOF
    ;;
  workspaces)          read_call GET /user/my-workspace ;;
  workspace-list)      read_call GET /system/workspace/list_all ;;
  nodes)               read_call POST /node/list_data.json "${FORM[@]}" --data-urlencode "page=$page" -d "limit=$limit" ;;
  node-groups)         read_call GET /node/list_group_all.json ;;
  node-monitor)
    require_flag --node-id "$node_id"
    read_call POST /node/node_monitor_data.json "${FORM[@]}" --data-urlencode "nodeId=$node_id" ;;
  node-process)
    require_flag --node-id "$node_id"
    read_call POST /node/processList "${FORM[@]}" --data-urlencode "nodeId=$node_id" ;;
  node-machine-info)
    require_flag --node-id "$node_id"
    read_call GET "/node/machine-info?nodeId=$node_id" ;;
  projects)
    require_flag --node-id "$node_id"
    read_call POST /node/project_list "${FORM[@]}" --data-urlencode "nodeId=$node_id" -d "page=$page" -d "limit=$limit" ;;
  project-info)
    require_flag --node-id "$node_id"
    pi_args=("${FORM[@]}" --data-urlencode "nodeId=$node_id" -d "page=$page" -d "limit=$limit")
    [[ -n "$id" ]] && pi_args+=(-d "id=$id")
    read_call POST /node/manage/get_project_info "${pi_args[@]}" ;;
  builds)              read_call POST /build/list "${FORM[@]}" --data-urlencode "page=$page" -d "limit=$limit" ;;
  build-get)
    require_flag --id "$id"
    read_call GET "/build/get?id=$id" ;;
  build-history)       read_call POST /build/history/history_list.json "${FORM[@]}" --data-urlencode "page=$page" -d "limit=$limit" ;;
  build-log)
    require_flag --id "$id"
    require_flag --build-id "$build_id_raw"
    [[ "$build_id_raw" =~ ^[0-9]+$ ]] || { echo "--build-id 必须为正整数" >&2; exit 2; }
    [[ "$line" =~ ^[0-9]+$ ]] || { echo "--line 必须为正整数" >&2; exit 2; }
    read_call POST /build/manage/get-now-log "${FORM[@]}" --data-urlencode "id=$id" -d "buildId=$build_id_raw" -d "line=$line" ;;
  build-groups)        read_call GET /build/list_group_all ;;
  repositories)        read_call POST /build/repository/list "${FORM[@]}" --data-urlencode "page=$page" -d "limit=$limit" ;;
  repository-groups)   read_call GET /build/repository/list-group ;;
  branches)
    require_flag --repository-id "$repository_id"
    read_call POST /build/branch-list "${FORM[@]}" --data-urlencode "repositoryId=$repository_id" ;;
  scripts)             read_call POST /script/list "${FORM[@]}" --data-urlencode "page=$page" -d "limit=$limit" ;;
  scripts-all)         read_call GET /script/list-all ;;
  script-logs)         read_call POST /script_log/list "${FORM[@]}" --data-urlencode "page=$page" -d "limit=$limit" ;;
  node-scripts)
    require_flag --node-id "$node_id"
    read_call POST /node/script/list_all "${FORM[@]}" --data-urlencode "nodeId=$node_id" -d "page=$page" -d "limit=$limit" ;;
  node-script-logs)
    require_flag --node-id "$node_id"
    read_call POST /node/script_log/list "${FORM[@]}" --data-urlencode "nodeId=$node_id" -d "page=$page" -d "limit=$limit" ;;
  machines)            read_call POST /system/assets/machine/list-data "${FORM[@]}" --data-urlencode "page=$page" -d "limit=$limit" ;;
  ssh-assets)          read_call POST /system/assets/ssh/list-data "${FORM[@]}" --data-urlencode "page=$page" -d "limit=$limit" ;;
  ftp-assets)          read_call POST /system/assets/ftp/list-data "${FORM[@]}" --data-urlencode "page=$page" -d "limit=$limit" ;;
  docker-assets)       read_call POST /system/assets/docker/list-data "${FORM[@]}" --data-urlencode "page=$page" -d "limit=$limit" ;;
  monitors)            read_call POST /monitor/list_data.json "${FORM[@]}" --data-urlencode "page=$page" -d "limit=$limit" ;;
  dispatch)            read_call POST /outgiving/dispatch-list "${FORM[@]}" --data-urlencode "page=$page" -d "limit=$limit" ;;
  dispatch-logs)       read_call POST /outgiving/log_list_data.json "${FORM[@]}" --data-urlencode "page=$page" -d "limit=$limit" ;;
  backups)             read_call POST /system/backup/list "${FORM[@]}" --data-urlencode "page=$page" -d "limit=$limit" ;;
  clusters)            read_call POST /cluster/list "${FORM[@]}" --data-urlencode "page=$page" -d "limit=$limit" ;;
  users)               read_call POST /user/get_user_list "${FORM[@]}" --data-urlencode "page=$page" -d "limit=$limit" ;;
  permission-groups)   read_call POST /user-permission-group/get-list "${FORM[@]}" --data-urlencode "page=$page" -d "limit=$limit" ;;
  workspace-env)       read_call POST /system/workspace_env/list "${FORM[@]}" --data-urlencode "page=$page" -d "limit=$limit" ;;
  login-logs)          read_call POST /user/login-log/list-data "${FORM[@]}" --data-urlencode "page=$page" -d "limit=$limit" ;;
  operate-logs)        read_call POST /user/list-operate-log-data "${FORM[@]}" --data-urlencode "page=$page" -d "limit=$limit" ;;
  recent-logs)         read_call POST /user/recent-log-data "${FORM[@]}" --data-urlencode "limit=$limit" ;;
  log-read)            read_call POST /log-read/list "${FORM[@]}" --data-urlencode "page=$page" -d "limit=$limit" ;;
  system-info)         read_call POST /system/info "${FORM[@]}" ;;
  check-version)       read_call POST /system/check_version.json "${FORM[@]}" ;;
  server-logs)         read_call POST /system/log_data.json "${FORM[@]}" ;;
# ---------------- 写命令（需 ALLOW_WRITE + --confirm） ----------------
  build-start)
    require_flag --id "$id"
    write_guard "build-start id=$id"
    bs_args=("${FORM[@]}" --data-urlencode "id=$id")
    [[ -n "$branch_name" ]] && bs_args+=(-d "branchName=$branch_name")
    [[ -n "$remark" ]] && bs_args+=(-d "buildRemark=$remark")
    do_call POST /build/manage/start "${bs_args[@]}" ;;
  build-cancel)
    require_flag --id "$id"
    write_guard "build-cancel id=$id"
    do_call POST /build/manage/cancel "${FORM[@]}" --data-urlencode "id=$id" ;;
  build-rerelease)
    require_flag --log-id "$log_id"
    write_guard "build-rerelease logId=$log_id"
    do_call POST /build/manage/reRelease "${FORM[@]}" --data-urlencode "logId=$log_id" ;;
  project-operate)
    require_flag --node-id "$node_id"
    require_flag --id "$id"
    require_flag --opt "$opt"
    case "$opt" in start|stop|restart) ;; *) echo "--opt 仅支持 start|stop|restart" >&2; exit 2 ;; esac
    write_guard "project-operate node=$node_id project=$id opt=$opt"
    do_call POST /node/manage/operate "${FORM[@]}" --data-urlencode "nodeId=$node_id" -d "id=$id" -d "opt=$opt" ;;
  node-kill-pid)
    require_flag --node-id "$node_id"
    require_flag --pid "$pid"
    write_guard "node-kill-pid node=$node_id pid=$pid"
    kill_args=("${FORM[@]}" --data-urlencode "nodeId=$node_id" --data-urlencode "pid=$pid")
    [[ -n "$machine_id" ]] && kill_args+=(--data-urlencode "machineId=$machine_id")
    do_call POST /node/kill.json "${kill_args[@]}" ;;
  system-upgrade)
    write_guard "system-upgrade（将真实触发在线升级）"
    if [[ -n "$machine_id" ]]; then
      do_call GET "/system/remote_upgrade.json?machineId=$machine_id"
    else
      do_call GET /system/remote_upgrade.json
    fi ;;
  ""|-h|--help) usage ;;
  *)
    echo "未知命令: $cmd" >&2
    usage
    ;;
esac
