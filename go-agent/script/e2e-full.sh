#!/usr/bin/env bash
#
# Copyright (c) 2019 Of Him Code Technology Studio
# Jpom is licensed under Mulan PSL v2.
#
# e2e-full.sh — Java 插件端 vs Go 插件端「全端点（含破坏性）」隔离实例对照测试
#
# 用途：
#   在同一台 Linux 测试机上准备两个完全隔离的插件端实例（Java / Go），使用同一份基线
#   conf/data（INSTALL.json 重新生成、installId 区分），用同一套请求参数依次执行 NodeUrl
#   全部 HTTP 端点（只读 + 破坏性写），逐项比对 HTTP 状态 / Content-Type / body.code /
#   body.msg（zh-CN）/ data 结构（动态值归一化），以及操作后 data/*.json、项目目录、文件
#   备份目录等落盘状态。输出 PASS/DIFF/FAIL 结果（result.json + summary.md）。
#
#   驱动逻辑在同目录 e2e-full-driver.py（由本脚本调用，可单独阅读用例清单）。
#
# 运行位置：Linux 测试机本地（脚本直接启动/停止两个实例，不走 SSH）。
# 依赖：bash、python3(>=3.6)、curl、JDK8（启动 Java 插件端并编译测试 jar）。
# 安全：拒绝使用生产端口（2122/2123）、拒绝把测试目录设为基线目录，开始/结束校验生产
#       Java 插件端（默认 2123）仍正常运行；只停止命令行含实例目录的进程。
#
# 用法：
#   bash e2e-full.sh [选项]
#     --keep                测试结束后保留两个实例（便于排查）
#     --clean-only          只清理实例目录与进程，不跑测试
#     --no-start            复用已启动的实例（跳过准备/启动，直接跑请求对照）
#     --only REGEX          只跑名称匹配的用例（python re.search）
#     --go-only             仅启动 Go 隔离实例，按契约/i18n 预期值执行破坏性端点
#                           （不启动 Java 实例；适合 Java 期望值来自契约、无实时 Java 对照的场景）
#     --build-go            在本地用 script/go.sh 构建 Go 二进制
#     --go-binary PATH      Go 插件端二进制（默认 <go>/build/agent-e2e-linux-amd64）
#     --java-jar PATH       Java 插件端 jar（默认取基线 lib/Agent-*.jar 最新）
#     --baseline DIR        基线插件端目录（含 conf/ data/ lib/），默认 /home/user/jpom/agent
#     --root DIR            测试根目录，默认 /home/user/jpom-e2e
#     --java-port N         Java 实例端口，默认 2127
#     --go-port N           Go 实例端口，默认 2128
#     --jdk-home DIR        JDK8 目录，默认 /usr/java/jdk8u402-b06
#     --lang LANG           请求语言头，默认 zh-CN
#     --result-dir DIR      结果输出目录，默认 <root>/results
#     --verbose             打印每个用例的执行过程
#
# 环境变量（等价于参数）：BASELINE_DIR JDK_HOME GO_BINARY JAVA_AGENT_JAR E2E_ROOT
#   JAVA_PORT GO_PORT RESULT_DIR LANG_HEADER FIXTURE_HTTP_PORT BACKUP_COUNT PROD_JAVA_PORT
#
# 复跑示例（脱敏，host 用占位符）：
#   # 1) 本机构建 Go 二进制
#   bash script/go.sh build -o build/agent-e2e-linux-amd64 .
#   # 2) 拷贝脚本到测试机
#   scp script/e2e-full.sh script/e2e-full-driver.py build/agent-e2e-linux-amd64 user@<test-host>:/tmp/
#   # 3) 在测试机执行（基线目录/JDK 按需覆盖）
#   ssh user@<test-host> 'bash /tmp/e2e-full.sh --go-binary /tmp/agent-e2e-linux-amd64'
#   # 4) 结果：<root>/results/summary.md、<root>/results/result.json
#
set -uo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
GO_PROJECT_DIR=$(cd "$SCRIPT_DIR/.." && pwd)

BASELINE_DIR=${BASELINE_DIR:-/home/user/jpom/agent}
JDK_HOME=${JDK_HOME:-${JDK8_HOME:-/usr/java/jdk8u402-b06}}
GO_BINARY=${GO_BINARY:-$GO_PROJECT_DIR/build/agent-e2e-linux-amd64}
JAVA_AGENT_JAR=${JAVA_AGENT_JAR:-}
JAVA_PORT=${JAVA_PORT:-2127}
GO_PORT=${GO_PORT:-2128}
E2E_ROOT=${E2E_ROOT:-/home/user/jpom-e2e}
RESULT_DIR=${RESULT_DIR:-$E2E_ROOT/results}
LANG_HEADER=${LANG_HEADER:-zh-CN}
FIXTURE_HTTP_PORT=${FIXTURE_HTTP_PORT:-8999}
BACKUP_COUNT=${BACKUP_COUNT:-3}
PROD_JAVA_PORT=${PROD_JAVA_PORT:-2123}

KEEP=0
CLEAN_ONLY=0
NO_START=0
ONLY=""
VERBOSE=0
BUILD_GO=0
GO_ONLY=0

log()  { printf '%s [e2e] %s\n' "$(date '+%H:%M:%S')" "$*" >&2; }
warn() { printf '%s [e2e][WARN] %s\n' "$(date '+%H:%M:%S')" "$*" >&2; }
die()  { printf '%s [e2e][FATAL] %s\n' "$(date '+%H:%M:%S')" "$*" >&2; exit 1; }

usage() { sed -n '2,56p' "$0" | sed 's/^# \{0,1\}//'; exit 0; }

while [ $# -gt 0 ]; do
  case "$1" in
    --keep) KEEP=1 ;;
    --keep=false) KEEP=0 ;;
    --clean-only) CLEAN_ONLY=1 ;;
    --no-start) NO_START=1 ;;
    --build-go) BUILD_GO=1 ;;
    --verbose) VERBOSE=1 ;;
    --go-only) GO_ONLY=1 ;;
    --only) shift; ONLY=${1:-} ;;
    --go-binary) shift; GO_BINARY=${1:-} ;;
    --java-jar) shift; JAVA_AGENT_JAR=${1:-} ;;
    --baseline) shift; BASELINE_DIR=${1:-} ;;
    --root) shift; E2E_ROOT=${1:-} ;;
    --java-port) shift; JAVA_PORT=${1:-} ;;
    --go-port) shift; GO_PORT=${1:-} ;;
    --jdk-home) shift; JDK_HOME=${1:-} ;;
    --lang) shift; LANG_HEADER=${1:-} ;;
    --result-dir) shift; RESULT_DIR=${1:-} ;;
    -h|--help) usage ;;
    *) die "未知参数: $1（--help 查看用法）" ;;
  esac
  shift
done

JAVA_BASE=$E2E_ROOT/java
GO_BASE=$E2E_ROOT/go
APP_ROOT_JAVA=$E2E_ROOT/java-apps
APP_ROOT_GO=$E2E_ROOT/go-apps
APP_JAVA=$APP_ROOT_JAVA/app
APP_GO=$APP_ROOT_GO/app
FIXTURES=$E2E_ROOT/fixtures

FIXTURE_PID=""

# ---------- 基础检查 ----------
preflight() {
  [ -d "$BASELINE_DIR" ] || die "基线目录不存在: $BASELINE_DIR"
  [ -f "$BASELINE_DIR/conf/application.yml" ] || die "基线缺少 conf/application.yml: $BASELINE_DIR"
  [ -f "$BASELINE_DIR/data/INSTALL.json" ] || die "基线缺少 data/INSTALL.json: $BASELINE_DIR"
  [ -x "$JDK_HOME/bin/java" ] || die "JDK8 不存在: $JDK_HOME/bin/java"
  command -v python3 >/dev/null 2>&1 || die "缺少 python3"
  command -v curl >/dev/null 2>&1 || die "缺少 curl"
  [ -f "$SCRIPT_DIR/e2e-full-driver.py" ] || die "缺少驱动脚本: $SCRIPT_DIR/e2e-full-driver.py"

  case "$E2E_ROOT" in
    /|/home|/home/user|"$BASELINE_DIR"|"$BASELINE_DIR"/*) die "E2E_ROOT 不允许为 $E2E_ROOT（必须与基线/生产目录隔离）" ;;
  esac
  case "$JAVA_PORT$GO_PORT" in *2123*|*2122*) die "测试端口不允许使用生产端口 2122/2123" ;; esac
  [ "$JAVA_PORT" != "$GO_PORT" ] || die "Java/Go 实例端口不能相同"

  if [ "$GO_ONLY" != "1" ]; then
    if [ -z "$JAVA_AGENT_JAR" ]; then
      JAVA_AGENT_JAR=$(ls -1 "$BASELINE_DIR"/lib/Agent-*.jar 2>/dev/null | sort -V | tail -1)
    fi
    if [ -z "$JAVA_AGENT_JAR" ] || [ ! -f "$JAVA_AGENT_JAR" ]; then
      die "找不到 Java Agent jar（--java-jar 指定）"
    fi
  else
    JAVA_AGENT_JAR="<go-only: 不启动 Java 实例>"
  fi

  if [ ! -x "$GO_BINARY" ] && [ "$BUILD_GO" = "1" ]; then
    log "构建 Go 二进制: $GO_BINARY"
    ( cd "$GO_PROJECT_DIR" && bash script/go.sh build -o "$GO_BINARY" . ) || die "Go 构建失败"
  fi
  [ -x "$GO_BINARY" ] || die "Go 二进制不存在或不可执行: $GO_BINARY（先在本机构建后 scp，或加 --build-go）"

  log "基线目录: $BASELINE_DIR"
  log "Java jar : $JAVA_AGENT_JAR"
  log "Go 二进制: $GO_BINARY"
  log "测试根目录: $E2E_ROOT（Java=$JAVA_BASE:$JAVA_PORT, Go=$GO_BASE:$GO_PORT）"
}

port_busy() {
  python3 - "$1" <<'PY'
import socket, sys
port = int(sys.argv[1])
s = socket.socket()
s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
try:
    s.bind(("127.0.0.1", port))
    print("free")
except OSError:
    print("busy")
finally:
    s.close()
PY
}

agent_auth() {
  python3 - "$1" <<'PY'
import hashlib, json, sys
d = json.load(open(sys.argv[1]))
print(hashlib.sha1((d["agentName"] + "@" + d["agentPwd"]).encode()).hexdigest())
PY
}

prod_check() {
  local expect=$1 auth code
  auth=$(agent_auth "$BASELINE_DIR/data/agent_authorize.json")
  code=$(curl -s -o /dev/null -m 8 -w '%{http_code}' -X POST \
      -H "Jpom-Agent-Authorize: $auth" -H 'Accept-Language: zh-CN' \
      "http://127.0.0.1:${PROD_JAVA_PORT}/info" || true)
  if [ "$expect" = "alive" ]; then
    [ "$code" = "200" ] || die "生产 Java 插件端 127.0.0.1:${PROD_JAVA_PORT} 不可用（HTTP $code）；为安全起见中止"
    log "生产 Java 插件端 127.0.0.1:${PROD_JAVA_PORT} 正常（HTTP 200）"
  else
    if [ "$code" = "200" ]; then
      log "测试结束后生产 Java 插件端仍正常（HTTP 200）"
    else
      warn "测试结束后生产 Java 插件端 127.0.0.1:${PROD_JAVA_PORT} 异常（HTTP $code），请立即检查！"
    fi
  fi
}

# ---------- 实例准备 ----------
# 按实例目录在 /proc 中定位插件端进程（pidfile 丢失时的兜底；路径唯一，不会误杀）
find_instance_pids() {
  local base=$1 p pid
  for p in /proc/[0-9]*; do
    pid=${p#/proc/}
    [ -r "$p/cmdline" ] || continue
    case "$(tr '\0' ' ' < "$p/cmdline" 2>/dev/null)" in
      *"$base/bin/agent"*|*"$base/lib/Agent-"*) echo "$pid" ;;
    esac
  done
}

stop_instance() {
  local base=$1 pidfile="$1/bin/agent.pid" pid pids
  pids=$(cat "$pidfile" 2>/dev/null || true)
  pids="$pids $(find_instance_pids "$base" | tr '\n' ' ')"
  for pid in $pids; do
    [ -n "$pid" ] && [ -d "/proc/$pid" ] || continue
    kill "$pid" 2>/dev/null || true
    for _ in $(seq 1 150); do kill -0 "$pid" 2>/dev/null || break; sleep 0.1; done
    if kill -0 "$pid" 2>/dev/null; then kill -9 "$pid" 2>/dev/null || true; fi
    log "已停止实例: $base (pid=$pid)"
  done
  rm -f "$pidfile"
}

setup_instance() {
  local kind=$1 base=$2 app=$3 port=$4 jar_name=$5
  local app_root
  app_root=$(dirname "$app")
  stop_instance "$base"
  rm -rf "$base" "$app_root"
  mkdir -p "$base/bin" "$base/conf" "$base/data" "$base/logs" "$base/project-log" "$base/tmp" "$base/oldJars"
  mkdir -p "$app/sub" "$app/upload"

  cp -a "$BASELINE_DIR/data/." "$base/data/" || die "复制基线 data 失败"
  cp "$BASELINE_DIR/conf/application.yml" "$base/conf/application.yml" || die "复制 application.yml 失败"
  if [ -f "$BASELINE_DIR/conf/logback.xml" ]; then cp "$BASELINE_DIR/conf/logback.xml" "$base/conf/"; fi

  if [ "$kind" = "java" ]; then
    mkdir -p "$base/lib"
    cp "$JAVA_AGENT_JAR" "$base/lib/" || die "复制 Java jar 失败"
  else
    cp "$GO_BINARY" "$base/bin/agent" || die "复制 Go 二进制失败"
    chmod +x "$base/bin/agent"
    if [ -f "$GO_PROJECT_DIR/script/AgentGo.sh" ]; then
      cp "$GO_PROJECT_DIR/script/AgentGo.sh" "$base/bin/AgentGo.sh"
      chmod +x "$base/bin/AgentGo.sh"
    fi
  fi

  # 修改端口、开启项目文件备份（两种实例同一份配置，保证对照公平）
  python3 - "$base/conf/application.yml" "$port" "$BACKUP_COUNT" <<'PY'
import re, sys
path, port, backup = sys.argv[1], int(sys.argv[2]), int(sys.argv[3])
text = open(path, encoding="utf-8").read()
new, n = re.subn(r"(?m)^(\s*port:\s*)\d+", r"\g<1>%d" % port, text, count=1)
assert n == 1, "application.yml 未找到 server.port"
new, n2 = re.subn(r"(?m)^(\s*file-backup-count:\s*)\d+", r"\g<1>%d" % backup, new, count=1)
assert n2 == 1, "application.yml 未找到 project.file-backup-count"
# E2E 专用：基线 conf 的 file-backup-suffix 中 ^.+\\.(?i)(txt)$ 被 YAML 单引号保留为双反斜杠，
# 正则实际不匹配 .txt（Java 同行为），会让备份用例全部落在非后缀文件上而误报“备份为空”。
# 这里把该正则归一化为单反斜杠（仅测试实例，不影响发布包/生产 conf）。
new, n3 = re.subn(r"(?m)^(\s*file-backup-suffix:.*)$",
                     lambda m: m.group(0).replace("\\\\", "\\"), new, count=1)

open(path, "w", encoding="utf-8").write(new)
PY
  [ $? -eq 0 ] || die "改写 application.yml 失败: $base"

  # 重新生成 INSTALL.json（区分 installId）
  python3 - "$base/data/INSTALL.json" <<'PY'
import json, sys, time, uuid
path = sys.argv[1]
data = {
    "installId": uuid.uuid4().hex,
    "installTime": time.strftime("%Y-%m-%d %H:%M:%S"),
    "desc": "请勿删除此文件,删除后关联 id 将失效",
}
json.dump(data, open(path, "w", encoding="utf-8"), ensure_ascii=False, indent=0)
PY
  [ $? -eq 0 ] || die "生成 INSTALL.json 失败: $base"

  # 测试项目文件树（每实例独立目录，避免共享目录互相破坏）
  printf 'hello e2e\n' > "$app/config.txt"
  printf '#!/bin/sh\necho e2e-run\n' > "$app/run.sh"
  printf 'inner file e2e\n' > "$app/sub/inner.txt"
  cp "$FIXTURES/demo.jar" "$app/$jar_name" || die "复制 demo.jar 失败"

  # 系统日志夹具：一个可删除的旧日志（>1 天）与一个不可删除的新日志
  printf 'e2e old system log fixture\n' > "$base/logs/e2e-old.log"
  touch -d '2 days ago' "$base/logs/e2e-old.log"
  printf 'e2e new system log fixture\n' > "$base/logs/e2e-new.log"

  log "实例目录已准备: $base (port=$port, app=$app, kind=$kind)"
}

setup_fixtures() {
  rm -rf "$FIXTURES"
  mkdir -p "$FIXTURES/demosrc" "$FIXTURES/democlasses" "$FIXTURES/remote"
  printf 'remote fixture file for e2e\n' > "$FIXTURES/remote/remote.txt"
  printf 'upload fixture content for e2e\n' > "$FIXTURES/upload.txt"
  cat > "$FIXTURES/demosrc/Demo.java" <<'JAVA'
public class Demo {
    public static void main(String[] args) throws Exception {
        System.out.println("e2e-demo-start");
        for (int i = 0; i < 120; i++) {
            System.out.println("e2e-demo tick " + i);
            System.out.flush();
            Thread.sleep(500);
        }
    }
}
JAVA
  printf 'Main-Class: Demo\n' > "$FIXTURES/manifest.txt"
  "$JDK_HOME/bin/javac" -d "$FIXTURES/democlasses" "$FIXTURES/demosrc/Demo.java" >/dev/null 2>&1 \
    || die "编译 demo.jar 失败（需要 JDK8 javac）"
  ( cd "$FIXTURES/democlasses" && "$JDK_HOME/bin/jar" cfm "$FIXTURES/demo.jar" "$FIXTURES/manifest.txt" . ) >/dev/null \
    || die "打包 demo.jar 失败"
  log "测试夹具已生成: $FIXTURES/demo.jar"
}

start_http_fixture() {
  ( cd "$FIXTURES/remote" || exit 1
    exec nohup python3 -m http.server "$FIXTURE_HTTP_PORT" --bind 127.0.0.1 >/dev/null 2>&1 ) &
  FIXTURE_PID=$!
  echo "$FIXTURE_PID" > "$FIXTURES/http.pid"
  for _ in $(seq 1 50); do
    if curl -s -m 2 -o /dev/null "http://127.0.0.1:${FIXTURE_HTTP_PORT}/remote.txt"; then
      log "远程下载夹具 HTTP 服务已启动: http://127.0.0.1:${FIXTURE_HTTP_PORT}/"
      return 0
    fi
    sleep 0.2
  done
  die "无法启动远程下载夹具 HTTP 服务（端口 $FIXTURE_HTTP_PORT）"
}

stop_http_fixture() {
  if [ -n "$FIXTURE_PID" ]; then
    kill "$FIXTURE_PID" 2>/dev/null || true
    rm -f "$FIXTURES/http.pid"
    FIXTURE_PID=""
  fi
}

start_instance() {
  local kind=$1 base=$2 jar pid
  if [ "$kind" = "java" ]; then
    jar=$(ls -1 "$base"/lib/Agent-*.jar | sort -V | tail -1)
    ( cd "$base" || exit 1
      exec env PATH="$JDK_HOME/bin:$PATH" JPOM_LOG="$base/logs" nohup "$JDK_HOME/bin/java" \
        -Djpom.application.tag=JPOM_AGENT_APPLICATION -server -Xms128m -Xmx512m \
        -Djava.awt.headless=true -Dfile.encoding=UTF-8 \
        -Dlogging.config="$base/conf/logback.xml" \
        -Dspring.config.location="$base/conf/application.yml" \
        -Djava.io.tmpdir="$base/tmp" \
        -jar "$jar" start >> "$base/logs/stdout.log" 2>&1 ) &
    pid=$!
  else
    ( cd "$base" || exit 1
      exec env PATH="$JDK_HOME/bin:$PATH" JPOM_LOG="$base/logs" nohup "$base/bin/agent" -base "$base" \
        >> "$base/logs/stdout.log" 2>&1 ) &
    pid=$!
  fi
  echo "$pid" > "$base/bin/agent.pid"
  log "实例已启动: $kind ($base, pid=$pid)"
}

wait_ready() {
  local base=$1 port=$2 name=$3 auth code
  auth=$(agent_auth "$base/data/agent_authorize.json")
  for i in $(seq 1 120); do
    code=$(curl -s -o /dev/null -m 3 -w '%{http_code}' -X POST \
        -H "Jpom-Agent-Authorize: $auth" -H 'Accept-Language: zh-CN' \
        "http://127.0.0.1:${port}/info" 2>/dev/null || true)
    if [ "$code" = "200" ]; then
      log "$name 实例就绪（127.0.0.1:${port}，等待 ${i}s）"
      return 0
    fi
    sleep 1
  done
  warn "$name 实例未就绪，日志尾部："
  tail -40 "$base/logs/stdout.log" >&2 || true
  return 1
}

cleanup() {
  local rc=$?
  log "清理开始（rc=$rc）"
  stop_http_fixture
  if [ "$KEEP" != "1" ]; then
    stop_instance "$GO_BASE"
    if [ "$GO_ONLY" != "1" ]; then
      stop_instance "$JAVA_BASE"
    fi
  else
    log "--keep 指定，保留实例：Java=$JAVA_BASE Go=$GO_BASE（停止：kill \$(cat <base>/bin/agent.pid)）"
  fi
  if [ "$NO_START" != "1" ] && [ "$CLEAN_ONLY" != "1" ]; then
    prod_check after || true
  fi
  exit $rc
}
trap cleanup EXIT INT TERM

# ---------- 主流程 ----------
preflight
prod_check before

if [ "$CLEAN_ONLY" = "1" ]; then
  stop_instance "$GO_BASE"
  stop_instance "$JAVA_BASE"
  rm -rf "$JAVA_BASE" "$GO_BASE" "$APP_JAVA" "$APP_GO" "$FIXTURES"
  log "已清理测试实例目录"
  exit 0
fi

if [ "$NO_START" != "1" ]; then
  for p in "$GO_PORT" "$FIXTURE_HTTP_PORT"; do
    [ "$(port_busy "$p")" = "free" ] || die "端口 $p 已被占用（请先停止残留实例或改用 --java-port/--go-port）"
  done
  mkdir -p "$RESULT_DIR" "$E2E_ROOT"
  setup_fixtures
  if [ "$GO_ONLY" != "1" ]; then
    setup_instance java "$JAVA_BASE" "$APP_JAVA" "$JAVA_PORT" "e2eapp.jar"
  fi
  setup_instance go   "$GO_BASE"   "$APP_GO"   "$GO_PORT"   "e2eapp.jar"
  start_http_fixture
  if [ "$GO_ONLY" != "1" ]; then
    start_instance java "$JAVA_BASE"
  fi
  start_instance go "$GO_BASE"
  if [ "$GO_ONLY" != "1" ]; then
    wait_ready "$JAVA_BASE" "$JAVA_PORT" "Java" || die "Java 实例启动失败"
  fi
  wait_ready "$GO_BASE" "$GO_PORT" "Go" || die "Go 实例启动失败"
fi

export E2E_JAVA_BASE="$JAVA_BASE" E2E_GO_BASE="$GO_BASE"
export E2E_APP_JAVA="$APP_JAVA" E2E_APP_GO="$APP_GO"
export E2E_JAVA_URL="http://127.0.0.1:${JAVA_PORT}" E2E_GO_URL="http://127.0.0.1:${GO_PORT}"
export E2E_RESULT_DIR="$RESULT_DIR" E2E_LANG="$LANG_HEADER"
export E2E_ONLY="$ONLY" E2E_VERBOSE="$VERBOSE"
export E2E_FIXTURE_URL="http://127.0.0.1:${FIXTURE_HTTP_PORT}"
export E2E_JDK_HOME="$JDK_HOME" E2E_BASELINE_DIR="$BASELINE_DIR"
export E2E_PROD_PORT="$PROD_JAVA_PORT" E2E_BACKUP_COUNT="$BACKUP_COUNT"
export E2E_GO_ONLY="$GO_ONLY" E2E_APP_ROOT_JAVA="$APP_ROOT_JAVA" E2E_APP_ROOT_GO="$APP_ROOT_GO"

log "开始执行端点对照（结果目录: $RESULT_DIR）"
python3 "$SCRIPT_DIR/e2e-full-driver.py"
DRIVER_RC=$?
if [ "$DRIVER_RC" != "0" ]; then
  die "对照驱动执行失败（rc=$DRIVER_RC）"
fi

log "完成。摘要: $RESULT_DIR/summary.md ；机器可读结果: $RESULT_DIR/result.json"