#!/usr/bin/env bash
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
# Jpom 回归单元测试入口
#
# 背景：仓库根 pom 默认 `skipTests=true` 且 `maven.test.skip=true`，`mvn package` / CI 默认
# **不会执行任何单测**。共享基础类/安全修复如果只靠“能编译+手工点几下”，很容易像 2.11.13 的
# `workspaceId:in` 回归一样漏到正式版。因此：改动共享基础类（BaseDbService / Base*Service /
# 拦截器等）或安全相关代码，提交前必须运行本脚本，且修改共享逻辑时必须补行为级单测。
#
# 用法：
#   bash script/run-regression-tests.sh              # 运行核心回归测试集
#   bash script/run-regression-tests.sh --list       # 查看测试列表
#   bash script/run-regression-tests.sh --tests A,B  # 追加/运行指定测试（simple class name）
#   bash script/run-regression-tests.sh --offline    # Maven 离线模式
#   bash script/run-regression-tests.sh --all        # 运行 modules/server 全部单测（较慢，可能依赖环境）
#
# 环境变量：
#   MVN                     mvn 可执行文件（默认 PATH 中的 mvn）
#   JAVA_HOME               JDK 8（与发布构建一致）
#   JPOM_MVN_REPO_LOCAL     Maven 本地仓库目录（本机 ~/.m2 只读时使用）
#   JPOM_MVN_ARGS           追加的 Maven 参数（空格分隔）
#
# 说明：新增共享基础类/高危接口的回归测试时，请把测试类名加入 DEFAULT_TESTS，
#      让本地、CI（.github/workflows/build.yml、.workflow/MasterPipeline.yml）与
#      script/release.sh 的 tests 阶段自动覆盖。
#-----------------------------------------------------------

set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)

# 核心回归测试集：类名不包含包名（surefire -Dtest 语法）
DEFAULT_TESTS=(
	# 通用分页过滤：字段名白名单/值绑定/`:in` 后缀（2.11.13 工作空间隔离回归的守卫）
	BaseDbServiceListPageFilterTest
	BaseDbServiceBuildWhereTest
	# 权限拦截 fail-closed：权限管理模块的 MVC 处理器必须声明方法级 @Feature
	HandlerFeatureAnnotationTest
)

MVN_BIN=${MVN:-mvn}
OFFLINE=${JPOM_MVN_OFFLINE:-0}
ALL_TESTS=0
EXTRA_TESTS=""

usage() {
	sed -n '14,36p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
}

while [ $# -gt 0 ]; do
	case "$1" in
	-h | --help)
		usage
		exit 0
		;;
	--list)
		printf '%s\n' "${DEFAULT_TESTS[@]}"
		exit 0
		;;
	--offline)
		OFFLINE=1
		shift
		;;
	--tests)
		EXTRA_TESTS=${2:-}
		[ -n "$EXTRA_TESTS" ] || {
			echo "错误：--tests 需要参数，如 --tests FooTest,BarTest" >&2
			exit 2
		}
		shift 2
		;;
	--all)
		ALL_TESTS=1
		shift
		;;
	*)
		echo "未知参数：$1（--help 查看用法）" >&2
		exit 2
		;;
	esac
done

if ! command -v "$MVN_BIN" >/dev/null 2>&1; then
	echo "错误：未找到 mvn（可用 MVN=/path/to/mvn 指定）" >&2
	exit 2
fi

mvn_args=(-B -pl modules/server -am -Dlicense.skip=true)
# 必须同时覆盖两个属性：根 pom 同时设置了 skipTests=true 与 maven.test.skip=true
if [ "$ALL_TESTS" = "1" ]; then
	mvn_args+=(-Dmaven.test.skip=false -DskipTests=false)
	echo "==> 运行 modules/server 全部单测（--all）"
else
	test_filter=$(IFS=,; echo "${DEFAULT_TESTS[*]}")
	if [ -n "$EXTRA_TESTS" ]; then
		test_filter="${test_filter},${EXTRA_TESTS}"
	fi
	mvn_args+=(-Dmaven.test.skip=false -DskipTests=false -Dtest="$test_filter" -DfailIfNoTests=false)
	echo "==> 运行回归测试：${test_filter}"
fi
if [ "$OFFLINE" = "1" ]; then
	mvn_args+=(-o)
fi
if [ -n "${JPOM_MVN_REPO_LOCAL:-}" ]; then
	mvn_args+=("-Dmaven.repo.local=${JPOM_MVN_REPO_LOCAL}")
fi
if [ -n "${JPOM_MVN_ARGS:-}" ]; then
	# shellcheck disable=SC2206
	read -r -a extra_args <<<"${JPOM_MVN_ARGS}"
	mvn_args+=("${extra_args[@]}")
fi

cd "$ROOT_DIR"
echo "+ mvn ${mvn_args[*]} test"
if ! "$MVN_BIN" "${mvn_args[@]}" test; then
	echo "" >&2
	echo "回归测试失败：请修复后再提交/发版。共享基础类改动必须补行为级单测（不能只测工具函数）。" >&2
	echo "如需查看已注册的核心测试：bash script/run-regression-tests.sh --list" >&2
	exit 1
fi
echo "==> 回归测试通过"
