#!/bin/bash
#
# Copyright (c) 2019 Of Him Code Technology Studio
# Jpom is licensed under Mulan PSL v2.
# You can use this software according to the terms and conditions of the Mulan PSL v2.
# You may obtain a copy of Mulan PSL v2 at:
# 			http://license.coscl.org.cn/MulanPSL2
# THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
# See the Mulan PSL v2 for more details.
#

# =============================================================================
# 开源项目提交/推送前的敏感信息检查（启发式，不能替代人工审阅）
#
# 检查范围：
#   1. 暂存区新增/修改的行（提交前，git add 之后执行）
#   2. 新增/未跟踪且未被 .gitignore 忽略的文件（提交前）
#   3. 指定提交区间的新增行（推送前，如 origin/dev..HEAD）
#
# 用法：
#   bash script/check-sensitive.sh                     # 检查工作区根仓（暂存区 + 未跟踪文件）
#   bash script/check-sensitive.sh --all               # 检查根仓 + 全部子模块
#   bash script/check-sensitive.sh --repo jpom-parent  # 只检查指定子模块/仓库
#   bash script/check-sensitive.sh --range origin/dev..HEAD   # 检查待推送的提交
#   bash script/check-sensitive.sh --all --range origin/dev..HEAD
#   bash script/check-sensitive.sh --strict            # 低置信度告警也返回失败
#
# 退出码：0 = 未发现需阻断的问题；1 = 发现疑似敏感信息，需人工确认/脱敏。
#
# 建议同时安装专用工具做兜底（二选一即可，能力更强）：
#   gitleaks detect --source . --redact
#   trufflehog git file://. --only-verified
# =============================================================================

set -u

ROOT_DIR=$(cd "$(dirname "$0")/.." && pwd)
SCAN_ALL=0
STRICT=0
TARGET_REPO=""
RANGE=""

while [ $# -gt 0 ]; do
  case "$1" in
    --all) SCAN_ALL=1; shift ;;
    --strict) STRICT=1; shift ;;
    --repo)
      TARGET_REPO="${2:-}"
      shift 2 2>/dev/null || shift
      ;;
    --range)
      RANGE="${2:-}"
      shift 2 2>/dev/null || shift
      ;;
    -h|--help)
      sed -n '20,32p' "$0" | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *)
      echo "未知参数：$1（使用 --help 查看用法）" >&2
      exit 2
      ;;
  esac
done

if [ -t 1 ]; then
  RED=$'\033[31m'; YELLOW=$'\033[33m'; GREEN=$'\033[32m'; RESET=$'\033[0m'
else
  RED=''; YELLOW=''; GREEN=''; RESET=''
fi

FAILED=0
WARNED=0

fail() {
  FAILED=$((FAILED + 1))
  printf '%s[FAIL]%s %s\n' "$RED" "$RESET" "$*"
}

warn() {
  WARNED=$((WARNED + 1))
  printf '%s[WARN]%s %s\n' "$YELLOW" "$RESET" "$*"
}

# 高置信度特征：命中即失败
HIGH_PATTERNS=(
  'BEGIN [A-Z0-9 ]*PRIVATE KEY'
  'AKIA[0-9A-Z]{16}'
  'ASIA[0-9A-Z]{16}'
  'gh[pousr]_[A-Za-z0-9]{30,}'
  'github_pat_[A-Za-z0-9_]{40,}'
  'glpat-[A-Za-z0-9_-]{20,}'
  'xox[abprs]-[A-Za-z0-9-]{10,}'
  'sk-[A-Za-z0-9]{20,}'
  'AIza[0-9A-Za-z_-]{35}'
  '://[^/[:space:]:@]+:[^/[:space:]@]+@'
)

# 低置信度特征：默认告警（--strict 时失败）
LOW_PATTERNS=(
  '(password|passwd|pwd|secret|token|api[_-]?key|apikey|access[_-]?key|secret[_-]?key|private[_-]?key|auth[_-]?token)[[:space:]]*[:=][[:space:]]*["'\'']?[A-Za-z0-9_./+=-]{8,}'
  '(^|[^0-9])1[3-9][0-9]{9}([^0-9]|$)'
  '(^|[^0-9])(10\.|192\.168\.|172\.(1[6-9]|2[0-9]|3[01])\.)[0-9]{1,3}\.[0-9]{1,3}'
)

# 不应提交的敏感文件名（新增/未跟踪文件）
SENSITIVE_NAME_PATTERN='(^|/)(id_rsa|id_dsa|id_ecdsa|id_ed25519)(\..*)?$|\.(pem|key|p12|pfx|jks|keystore|kdbx|ppk|ovpn)$|(^|/)rclone\.conf$|(^|/)\.npmrc$|(^|/)\.pypirc$'

# 仓库中已有的示例/占位值、常见假数据，避免误报（按需补充）
ALLOW_PATTERN='7094f673-2c53-4fc1-82e7-86e528449d97|\$\{|<[A-Za-z_]+>|xxx|xxxxx|your[_-]|change[_-]?me|placeholder|example|sample|demo|fake|mock|dummy'

# 明显不是硬编码秘密的上下文（仅用于跳过低置信度规则）
CONTEXT_SKIP_PATTERN='getenv|getProperty|getHeader|getParameter|System\.getenv|process\.env|@Value|request\.|getToken|setToken|tokenize|secretKeySpec'

is_allowed() {
  printf '%s' "$1" | grep -Eqi -- "$ALLOW_PATTERN"
}

# scan_line <label> <file> <line-no> <content>
scan_line() {
  local label="$1" file="$2" lineno="$3" content="$4" p
  for p in "${HIGH_PATTERNS[@]}"; do
    if printf '%s' "$content" | grep -Eq -- "$p"; then
      fail "$label:$file:$lineno 疑似硬编码密钥/凭据（规则：$p）"
      return 0
    fi
  done
  is_allowed "$content" && return 0
  printf '%s' "$content" | grep -Eqi -- "$CONTEXT_SKIP_PATTERN" && return 0
  for p in "${LOW_PATTERNS[@]}"; do
    if printf '%s' "$content" | grep -Eq -- "$p"; then
      warn "$label:$file:$lineno 疑似敏感信息，请人工确认（规则：$p）"
      return 0
    fi
  done
}

# scan_file_content <label> <repo-dir> <rel-path>
scan_file_content() {
  local label="$1" dir="$2" rel="$3" abs="$2/$3" lineno=0 line
  [ -f "$abs" ] || return 0
  # 跳过检查器自身
  [ "$rel" = "script/check-sensitive.sh" ] && return 0
  # 跳过二进制与超大文件（超大文件单独提示）
  if [ "$(wc -c < "$abs")" -gt 2000000 ]; then
    warn "$label:$rel 文件大于 2MB，请人工确认是否包含隐私/用户数据"
    return 0
  fi
  grep -Iq . "$abs" 2>/dev/null || return 0
  while IFS= read -r line; do
    lineno=$((lineno + 1))
    scan_line "$label" "$rel" "$lineno" "$line"
  done < "$abs"
}

# scan_diff <label> <repo-dir> <git-diff-args...>
scan_diff_added_lines() {
  local label="$1" dir="$2" line in_file="" content
  shift 2
  while IFS= read -r line; do
    case "$line" in
      "+++ b/"*) in_file="${line#+++ b/}"; continue ;;
      "+++ "*|"--- "*) continue ;;
    esac
    [ -z "$in_file" ] && continue
    # 跳过检查器自身（脚本内的规则字符串会命中自身）
    [ "$in_file" = "script/check-sensitive.sh" ] && continue
    [ "${line:0:1}" = "+" ] || continue
    [ "${line:0:2}" = "++" ] && continue
    content="${line:1}"
    scan_line "$label" "$in_file" "新增行" "$content"
  done < <(git -C "$dir" diff --unified=0 --no-color --no-ext-diff "$@" 2>/dev/null)
}

# check_repo <label> <repo-dir>
check_repo() {
  local label="$1" dir="$2" rel content

  if ! git -C "$dir" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    warn "$label 不是 git 仓库，跳过：$dir"
    return 0
  fi

  echo ">>> 检查 $label（$dir）"

  # 1) 暂存区新增/修改内容
  if [ -n "$(git -C "$dir" diff --cached --name-only --diff-filter=ACMR 2>/dev/null)" ]; then
    scan_diff_added_lines "$label(暂存区)" "$dir" --cached
  fi

  # 2) 指定提交区间（推送前检查）
  if [ -n "$RANGE" ]; then
    scan_diff_added_lines "$label($RANGE)" "$dir" "$RANGE"
  fi

  # 3) 新增/未跟踪文件
  while IFS= read -r rel; do
    [ -n "$rel" ] || continue
    if printf '%s' "$rel" | grep -Eq -- "$SENSITIVE_NAME_PATTERN"; then
      fail "$label:$rel 疑似密钥/凭据文件，禁止提交（如确需提交请脱敏并说明）"
      continue
    fi
    scan_file_content "$label(未跟踪)" "$dir" "$rel"
  done < <(git -C "$dir" ls-files --others --exclude-standard 2>/dev/null)

  # 4) 暂存区新增文件同样检查文件名
  while IFS= read -r rel; do
    [ -n "$rel" ] || continue
    if printf '%s' "$rel" | grep -Eq -- "$SENSITIVE_NAME_PATTERN"; then
      fail "$label:$rel 疑似密钥/凭据文件，禁止提交（如确需提交请脱敏并说明）"
    fi
  done < <(git -C "$dir" diff --cached --name-only --diff-filter=A 2>/dev/null)
}

if [ -n "$TARGET_REPO" ]; then
  case "$TARGET_REPO" in
    /*) check_repo "$TARGET_REPO" "$TARGET_REPO" ;;
    *) check_repo "$TARGET_REPO" "$ROOT_DIR/$TARGET_REPO" ;;
  esac
elif [ "$SCAN_ALL" = "1" ]; then
  check_repo "根仓" "$ROOT_DIR"
  while IFS= read -r sub; do
    [ -n "$sub" ] || continue
    check_repo "$sub" "$ROOT_DIR/$sub"
  done < <(git -C "$ROOT_DIR" config -f .gitmodules --get-regexp 'submodule\..*\.path' 2>/dev/null | awk '{print $2}')
else
  check_repo "当前仓库" "$ROOT_DIR"
fi

echo
if [ "$FAILED" -gt 0 ]; then
  printf '%s发现 %s 处疑似敏感信息，已阻断。请脱敏/移除后再提交或推送。%s\n' "$RED" "$FAILED" "$RESET"
  printf '如果确认是误报，请改用假数据，或在脚本的 ALLOW_PATTERN 中精确加入该占位值。\n'
  exit 1
fi

if [ "$WARNED" -gt 0 ]; then
  printf '%s有 %s 处疑似敏感信息告警，请逐条人工确认（--strict 可将其视为失败）。%s\n' "$YELLOW" "$WARNED" "$RESET"
  [ "$STRICT" = "1" ] && exit 1
  exit 0
fi

printf '%s检查通过：未发现疑似密钥、密码、token 或隐私信息。%s\n' "$GREEN" "$RESET"
exit 0
