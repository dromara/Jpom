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
	# 启动时空分组修复：group / groupName 两种字段都要覆盖，NULL 与空串都必须被修复（2026-11-26 流水线分组）
	BaseDbServiceGroupRepairSqlTest
	# 服务端强制过滤条件 vs 客户端同字段参数冲突（P1-1 / P1-2 / P2-1 的守卫）
	BaseDbServiceForcedFilterTest
	ProjectManageControlExportDataTest
	RepositoryControllerWorkspaceTest
	# 构建「获取分支」必须用 fill=false 取仓库凭证（2.11.13 起仓库密码/私钥被脱敏导致 git 认证失败的守卫）
	BuildInfoControllerBranchListTest
	# 构建扩展数据解析：空/非法 extraData 不得变成 500 空指针（#126 复测发现）
	BuildExtraDataUtilTest
	# 仓库编辑凭据回填必须走 scoped 查询：跨工作空间仓库 id 不得把别的仓库密码交给调用者的 gitUrl（2026-11-19 安全报告）
	RepositoryControllerEditCredentialTest
	RepositoryServiceFillSelectResultTest
	# 权限拦截：handler @Feature fail-closed + 节点数据权限 fail-closed
	HandlerFeatureAnnotationTest
	PermissionInterceptorNodeDataTest
	# IP 访问判定：读不到 IP 配置必须回「配置读取失败」而不是 999 IP 拒绝（2026-11-21 重启计划群反馈）
	IpInterceptorAccessTest
	# WebSocket 握手鉴权（Origin / 会话 / 禁用账号）
	ServerWebSocketInterceptorTest
	# 文件日志监听器会话/Tailer 生命周期（agent 控制台 OOM 回归守卫）
	BaseFileTailWatcherTest
	# 文件系统统计过滤：macOS APFS 同容器多卷 / Linux tmpfs+网络盘不得重复累加硬盘总量（2026-11-20 重启计划群反馈回归守卫）
	OshiFileStoreFilterTest
	# 远程下载白名单：主机边界匹配，禁止子域拼接与 userinfo 绕过（SSRF 回归守卫）
	ServerWhitelistTest
	# 全局异常响应脱敏：数据库异常不再回显 SQL/列/表/驱动信息
	BaseExceptionHandlerTest
	# 节点数据同步：旧工作空间不存在时回退到节点当前工作空间（重装服务端后“同步 0 个”回归守卫，Gitee ICH1VK）
	BaseNodeServiceSyncWorkspaceTest
	# 节点状态时效：集群未绑定分组/transportMode 为空时不能永久停在“正常”（2026-11-18 群反馈回归守卫）
	MachineNodeStatusTimeoutTest
	# 插件端主动推送（PUSH）：服务端只接受节点状态主题、校验大小、按最大年龄判定新鲜度（建议三补推送）
	TunnelPushServiceTest
	# 推送快照去重：同一份推送不得重复落库（否则统计日志出现相同 monitorTime 的重复行）
	PushSnapshotTrackerTest
	# 推送节点下线清理：被新连接顶替时保留快照、真正下线才清理（避免监控闪成无法连接）
	TunnelPushSnapshotCleanerTest
	# 推送模式网络延迟口径：落库必须是 0，不能是负数
	MachineNodePushNetworkDelayTest
	# 反向隧道表单请求体必须 URL 编码：值里的 & = + % 不得把参数截断（2.11.14.2 候选验证发现的文件内容截断回归守卫）
	TunnelRequestBodyTest
	# 严格构建：中间命令失败立即中断（仅严格 + sh 注入 set -e/pipefail，Gitee ICW9KZ）
	BuildExecuteManageScriptTest
	# 节点在线升级：引擎判定（engine/旧 Go 端 jarFile 兜底）、Go 升级容器解析、下载地址推导、版本/降级/最低兼容
	AgentPackageResolverTest
	# 在线升级发布包解析：tar.gz/.tgz 提取 lib jar（tar.gz 优先、回退 zip 的下载地址推导），zip/jar 历史行为不回退
	JpomManifestTarGzTest
	RemoteVersionUrlTest
	# 远端版本信息刷新语义：动作侧重试→缓存兜底→可读文案（2.11.14.7 升级/下载链路，共享 common 收口）
	RemoteVersionResolverTest
	RemoteVersionDownloadMessageTest
	# 流水线运行日志按窗口读取（长日志不再每次整文件进内存）
	PipelineLogReaderTest
	# 流水线仓库源：克隆深度必须切系统 git（2026-11-26 群反馈「浅克隆不生效」的守卫）+ 源码缓存行为
	PipelineSourceSyncParameterTest
	PipelineSourceCacheTest
	# 流水线触发上下文：开放接口纯 token 触发必须绑定触发用户/系统身份（2026-11-26「没有对应的用户」缺陷的守卫）
	PipelineRunnerTriggerUserContextTest
	# 触发器统一收口点：查不到 405 / token 生成与重置唯一路径 / 响应 key 与历史一致（B-041 同类缺陷的守卫）
	TriggerUrlAssemblerTest
	UploadShardingExtensionTest
	# 安全修复的既有单测（文件中心 SQL 注入 / 查询值绑定 / 日志路径穿越 / token 等）
	FileStorageApiControllerSortTest
	BaseDbCommonServiceQueryEntityTest
	FileUtilsResolveSubFileTest
	UserServiceCheckUserTest
	ToolsControllerSecurityTest
	DockerWorkspaceScopedLookupTest
	BuildInfoControllerBranchListTest
	LoginControlCaptchaTest
	# SSH 文件管理 new_file_folder 命令注入回归（name 不再拼远程 shell，创建走 SFTP）
	BaseSshFileControllerNewFileFolderTest
	# SSH/FTP 文件管理路径穿越回归（name/newname/fileName 不得穿越 fileDirs 白名单，2026-11-17 安全报告）
	FileUtilsResolveChildPathTest
	# docker 构建 fromTag SQL 注入（VL-001）与 /oauth2-{provide} 反射型 XSS（VL-002）回归（2026-11-18 安全报告）
	DockerInfoServiceQueryByTagTest
	DockerYmlDslFromTagTest
	IndexControlOauth2XssTest
	# docker 资产控制台越权（六个 /system/assets/docker/* 控制器缺 @SystemPermission）与 pull-image 把资产仓库凭据外发到第三方 registry 的回归（2026-11-19 安全报告）
	AssetsDockerConsolePermissionTest
	MachineDockerRegistryCredentialTest
	# 文件发布任务跨工作空间目标（node/SSH id 只做存在性校验）与发布子目录穿越（releasePathSecondary=..）的回归（2026-11-20 安全报告 #6）
	FileReleaseTaskServiceTargetScopeTest
	FileReleaseTaskControllerReleasePathTest
	# i18n 语言包完整性：代码引用的 key 必须在四个语言包齐全、四包 key 集合一致（2026-11-18 误删约 1000 条的守卫）
	I18nBundleConsistencyTest
	# 响应编码兜底：未配置时 UTF-8、显式配置优先、不覆盖请求编码（老安装实例 ??? 乱码的守卫）
	EncodingConfigurationTest
	# 构建版本快照（快速回滚）：忽略目录/临时文件不参与版本内容与指纹、复制不嵌套、清空保留 logs/temp
	ProjectFileVersionUtilTest
	# 版本快照资源开关默认关闭（避免升级后所有项目发布额外占用磁盘）
	AgentDefaultVersionConfigTest
	# 版本快照保留个数 0/空必须回落到默认值（否则插件端判「未启用」，发布成功却没有版本记录 → 快速回滚空列表，2026-11-19 群反馈）
	BuildVersionServiceKeepCountTest
	# 多目标发布失败策略：老配置缺字段按「任一目标失败即终止」，显式关闭才继续剩余目标
	BuildExtraModuleReleaseFailFastTest
	# 多目标发布行为：全部成功 / 首个失败即停并跳过剩余 / 关闭开关继续但仍判失败 / 空目标 / 异常无消息 / 首尾空白
	MultiTargetReleaseTest
	# 发布命令退出码口径：-1（读不到）在严格执行下判失败、非 0 未开严格只记录（2026-11-25 群反馈）
	ReleaseCommandExitCodeTest
	# 「被构建引用」匹配：SSH 项目多选后必须按成员精确匹配（整体等值会漏、like 子串会误伤）
	BuildInfoReleaseTargetMatcherTest
	# SSH 项目多选发布保存：目标去重 / 保序 / 忽略空值（接口直传与前端数组 join 同一口径）
	BuildInfoControllerSshProjectTargetsTest
	# FTP 资产删除守卫必须查 FTP 发布方式（原复制粘贴成 SSH，被构建引用也删得掉）
	FtpControllerDeleteGuardTest
	# SSH 项目（无插件端）接口契约 + 版本快照远端解析/过滤（构建历史快速回滚，2026-11-20）
	SshProjectControllerTest
	SshProjectVersionServiceTest
	# 流水线事件钩子（脚本/webhook）与运行状态机（2.11.16）：事件匹配、单条失败不影响其它、终态不可被改写
	PipelineHookDispatchTest
	PipelineRunStateTest
	PipelineEngineListenerTest
	ScriptHookHandlerTest
	# SSH 终端会话录制（asciicast v2 写入、OSC 133/633 命令标记解析）：开关关闭零产物、标记跨包解析、写入异常不影响会话
	AsciicastTerminalRecorderTest
	OscMarkerScannerTest
	# 文件中心发布方式注册（码值 8 只追加）与事件钩子配置校验（未知事件/非法地址必须在保存时拦下）
	FileStorageReleaseTargetTest
	PipelineDefinitionHooksTest
)

MVN_BIN=${MVN:-mvn}
OFFLINE=${JPOM_MVN_OFFLINE:-0}
ALL_TESTS=0
EXTRA_TESTS=""

usage() {
	# 按注释块标记提取，避免头部/行号调整后 --help 打印错位（license 插件会重排文件头）
	awk '/^# Jpom 回归单元测试入口$/{f=1} f{print} f&&/^#-+$/{exit}' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
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

mvn_args=(-B -pl modules/server,modules/agent -am -Dlicense.skip=true)
# 必须同时覆盖两个属性：根 pom 同时设置了 skipTests=true 与 maven.test.skip=true
if [ "$ALL_TESTS" = "1" ]; then
	mvn_args+=(-Dmaven.test.skip=false -DskipTests=false)
	echo "==> 运行 modules/server 全部单测（--all）"
else
	test_filter=$(IFS=,; echo "${DEFAULT_TESTS[*]}")
	# 额外测试不拼进这条超长 -Dtest（surefire 2.22 在超长列表下会静默跳过尾部类，2026-11-25 实测），
	# 改为后面单独一条 mvn 命令执行，见文末「运行额外测试」。
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
if [ "$ALL_TESTS" != "1" ] && [ -n "${EXTRA_TESTS}" ]; then
	filtered=()
	for a in "${mvn_args[@]}"; do
		case "$a" in
			-Dtest=*) continue ;;
			*) filtered+=("$a") ;;
		esac
	done
	echo "==> 运行额外测试：${EXTRA_TESTS}"
	echo "+ mvn ${filtered[*]} -Dtest=${EXTRA_TESTS} -DfailIfNoTests=false test"
	if ! "$MVN_BIN" "${filtered[@]}" -Dtest="${EXTRA_TESTS}" -DfailIfNoTests=false test; then
		echo "" >&2
		echo "额外测试失败：请修复后再提交/发版。" >&2
		exit 1
	fi
fi
echo "==> 回归测试通过"
