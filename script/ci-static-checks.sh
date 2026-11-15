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

#-----------------------------------------------------------
# Jpom 基础静态检查（GitHub Actions build.yml 的 static job 调用，也可本地运行）
#
# 不依赖任何 secrets，检查项：
#   1. bash -n：全部受版本控制（含未跟踪、未被忽略）的 *.sh 语法
#   2. shellcheck：若已安装，按 --shellcheck-severity（默认 error）扫描带 shebang 的脚本；
#      不带 shebang 的模板片段（resources/config_default 下的片段）跳过，避免 SC2148 噪音
#   3. python3 + PyYAML：解析 .github/workflows/*.yml（含重复 key / on / jobs / steps 结构校验）
#   4. git diff --check：指定基线以来的空白错误（--diff-base 或 DIFF_BASE）
#
# 用法：
#   bash script/ci-static-checks.sh
#   bash script/ci-static-checks.sh --diff-base origin/dev
#   bash script/ci-static-checks.sh --skip-shellcheck --diff-base HEAD~1
#
# 参数：
#   --diff-base REF          检查 git diff --check REF...HEAD（未指定或 REF 不可用时跳过并提示）
#   --shellcheck-severity S  shellcheck 级别：error（默认）/warning/info/style/none
#   --skip-shellcheck        跳过 shellcheck
#   --skip-yaml              跳过 workflow YAML 解析
#   --skip-bash              跳过 bash -n
#   -h | --help              显示用法
#
# 环境变量：
#   DIFF_BASE                等同 --diff-base
#   SHELLCHECK_SEVERITY      等同 --shellcheck-severity
#-----------------------------------------------------------

set -euo pipefail

repo_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$repo_dir"

diff_base="${DIFF_BASE:-}"
shellcheck_severity="${SHELLCHECK_SEVERITY:-error}"
run_bash=1
run_yaml=1
run_shellcheck=1
failures=()

log() { echo "[ci-static] $*"; }
warn() { echo "[ci-static][WARN] $*" >&2; }
die() {
	echo "[ci-static][ERROR] $*" >&2
	exit 2
}

usage() {
	awk '/^# 用法：/{f=1} f{print} f&&/^#----/{exit}' "${BASH_SOURCE[0]}" | awk '{sub(/^# ?/, ""); print}'
}

while [ $# -gt 0 ]; do
	case "$1" in
	--diff-base)
		diff_base="${2:?--diff-base 需要参数}"
		shift 2
		;;
	--shellcheck-severity)
		shellcheck_severity="${2:?--shellcheck-severity 需要参数}"
		shift 2
		;;
	--skip-shellcheck)
		run_shellcheck=0
		shift
		;;
	--skip-yaml)
		run_yaml=0
		shift
		;;
	--skip-bash)
		run_bash=0
		shift
		;;
	-h | --help)
		usage
		exit 0
		;;
	*)
		die "未知参数：$1（--help 查看用法）"
		;;
	esac
done

record_failure() {
	failures+=("$1")
}

# 列出需要检查的 shell 脚本：受版本控制的 + 本地未跟踪但未被忽略的（方便提交前自检）
list_shell_scripts() {
	git ls-files -z --cached --others --exclude-standard -- '*.sh' | while IFS= read -r -d '' f; do
		[ -f "$f" ] && echo "$f"
	done
}

check_bash_syntax() {
	local total=0 bad=0 f out
	while IFS= read -r f; do
		total=$((total + 1))
		if ! out=$(bash -n "$f" 2>&1); then
			bad=$((bad + 1))
			log "FAIL bash -n：$f"
			echo "$out" >&2
		fi
	done < <(list_shell_scripts)
	if [ "$total" -eq 0 ]; then
		warn "未找到需要检查的 shell 脚本"
		return 0
	fi
	if [ "$bad" -gt 0 ]; then
		record_failure "bash -n（${bad}/${total} 个脚本语法错误）"
		return 1
	fi
	log "OK bash -n：${total} 个脚本语法正确"
	return 0
}

check_shellcheck() {
	if [ "$run_shellcheck" -eq 0 ] || [ "$shellcheck_severity" = "none" ]; then
		log "跳过 shellcheck（已禁用）"
		return 0
	fi
	local sc_bin
	if ! sc_bin=$(command -v shellcheck); then
		warn "未安装 shellcheck，跳过该项（CI 的 ubuntu-latest 已预装；本地可 apt/brew 安装）"
		return 0
	fi
	local total=0 checked=0 skipped=0 bad=0 f out first
	while IFS= read -r f; do
		total=$((total + 1))
		first=$(head -n 1 -- "$f" 2>/dev/null || true)
		if [ "${first:0:2}" != "#!" ]; then
			# 不带 shebang 的多为被 Jpom 复制/引用的模板片段，不是独立可执行脚本
			skipped=$((skipped + 1))
			continue
		fi
		checked=$((checked + 1))
		if ! out=$("$sc_bin" --severity="$shellcheck_severity" --format=gcc -- "$f" 2>&1); then
			bad=$((bad + 1))
			log "FAIL shellcheck（${shellcheck_severity}）：$f"
			echo "$out" >&2
		fi
	done < <(list_shell_scripts)
	if [ "$bad" -gt 0 ]; then
		record_failure "shellcheck（${bad}/${checked} 个脚本，级别 ${shellcheck_severity}）"
		return 1
	fi
	log "OK shellcheck（${shellcheck_severity}）：检查 ${checked} 个脚本，跳过 ${skipped} 个无 shebang 片段"
	return 0
}

check_workflow_yaml() {
	if ! command -v python3 >/dev/null 2>&1; then
		record_failure "python3 不可用，无法解析 workflow YAML"
		return 1
	fi
	if python3 - <<'PY'
import glob
import sys

try:
    import yaml
except ImportError:
    sys.exit("未安装 PyYAML：pip install pyyaml（CI static job 会先安装）")


class UniqueKeyLoader(yaml.SafeLoader):
    """在解析阶段拦截重复 key：workflow 里的重复 key 会被静默覆盖，属于常见 CI 事故。"""


def construct_mapping(loader, node, deep=False):
    loader.flatten_mapping(node)
    seen = set()
    for key_node, value_node in node.value:
        key = loader.construct_object(key_node, deep=deep)
        try:
            hash(key)
        except TypeError:
            continue
        if key in seen:
            raise yaml.constructor.ConstructorError(
                None, None, "重复的 key：%r" % (key,), key_node.start_mark
            )
        seen.add(key)
    return yaml.SafeLoader.construct_mapping(loader, node, deep=deep)


UniqueKeyLoader.add_constructor(
    yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG, construct_mapping
)

files = sorted(glob.glob(".github/workflows/*.yml") + glob.glob(".github/workflows/*.yaml"))
if not files:
    sys.exit("未找到 .github/workflows/*.yml")

failed = 0
workflows = 0
print("[ci-static] 检查 %d 个 workflow YAML ..." % len(files))
for path in files:
    try:
        with open(path, encoding="utf-8") as fh:
            doc = yaml.load(fh, Loader=UniqueKeyLoader)
    except yaml.YAMLError as exc:
        print("[ci-static] FAIL %s：YAML 解析失败：%s" % (path, exc), file=sys.stderr)
        failed += 1
        continue
    if not isinstance(doc, dict):
        print("[ci-static] FAIL %s：顶层不是映射" % path, file=sys.stderr)
        failed += 1
        continue
    jobs = doc.get("jobs")
    if jobs is None:
        # .github/workflows 下也可能放非 workflow 的配置（如 codecov.yml），不算失败
        print("[ci-static] SKIP %s：非 GitHub Actions workflow（无 jobs 段）" % path)
        continue
    workflows += 1
    # PyYAML 按 YAML 1.1 会把未加引号的 on 解析成布尔 True
    on_key = "on" if "on" in doc else (True if True in doc else None)
    errors = []
    if on_key is None:
        errors.append("缺少 on 触发器段")
    if not isinstance(jobs, dict) or not jobs:
        errors.append("jobs 必须是非空映射")
    else:
        for job_id, job in jobs.items():
            if not isinstance(job, dict):
                errors.append("job %s 不是映射" % job_id)
                continue
            # 复用工作流（uses:）不要求 runs-on/steps
            if "uses" in job:
                continue
            if not job.get("runs-on"):
                errors.append("job %s 缺少 runs-on" % job_id)
            steps = job.get("steps")
            if not isinstance(steps, list) or not steps:
                errors.append("job %s 缺少 steps" % job_id)
                continue
            for index, step in enumerate(steps):
                if not isinstance(step, dict):
                    errors.append("job %s 的第 %d 个 step 不是映射" % (job_id, index + 1))
                    continue
                if not step.get("run") and not step.get("uses"):
                    errors.append(
                        "job %s 的第 %d 个 step 既没有 run 也没有 uses" % (job_id, index + 1)
                    )
    if errors:
        for error in errors:
            print("[ci-static] FAIL %s：%s" % (path, error), file=sys.stderr)
        failed += 1
    else:
        print("[ci-static] OK %s（%d 个 job）" % (path, len(jobs)))
if failed:
    sys.exit("%d 个 workflow 文件校验失败" % failed)
print("[ci-static] OK workflow YAML：%d 个 workflow" % workflows)
PY
	then
		return 0
	fi
	record_failure "workflow YAML 解析/结构校验"
	return 1
}

check_git_diff() {
	if [ -z "$diff_base" ]; then
		log "未指定 --diff-base/DIFF_BASE，跳过 git diff --check"
		return 0
	fi
	if ! git rev-parse --verify --quiet "${diff_base}^{commit}" >/dev/null 2>&1; then
		warn "基线 ${diff_base} 在本仓库不可用（浅克隆/新分支/强制推送常见），跳过 git diff --check"
		return 0
	fi
	local out
	if ! out=$(git diff --check "${diff_base}...HEAD" 2>&1); then
		log "FAIL git diff --check ${diff_base}...HEAD"
		echo "$out" >&2
		record_failure "git diff --check ${diff_base}...HEAD"
		return 1
	fi
	log "OK git diff --check ${diff_base}...HEAD（无空白错误）"
	return 0
}

log "仓库：$repo_dir"
if [ "$run_bash" -eq 1 ]; then
	check_bash_syntax || true
fi
if [ "$run_shellcheck" -eq 1 ]; then
	check_shellcheck || true
fi
if [ "$run_yaml" -eq 1 ]; then
	check_workflow_yaml || true
fi
check_git_diff || true

echo
if [ "${#failures[@]}" -gt 0 ]; then
	echo "[ci-static][FAIL] 共 ${#failures[@]} 项检查失败：" >&2
	for item in "${failures[@]}"; do
		echo "  - $item" >&2
	done
	exit 1
fi
log "全部静态检查通过"
