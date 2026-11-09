#!/usr/bin/env bash
# Copyright (c) 2019 Of Him Code Technology Studio
# Jpom is licensed under Mulan PSL v2.
#
# 开发验证部署脚本：把 Go agent 交叉编译后部署到 dev 虚拟机的独立目录，
# 以 2124 端口 + 独立数据目录运行（绝不改动生产 Java agent 的 2123/数据）。
#
# 用法：
#   bash script/dev-deploy.sh            # 构建 + 部署 + 启动 + 健康检查
#   bash script/dev-deploy.sh stop       # 停止测试实例
#   bash script/dev-deploy.sh logs       # 查看日志
#   bash script/dev-deploy.sh status     # 进程状态
#
# 可覆盖环境变量：DEV_HOST(必填，如 user@host)、DEV_DIR(默认 /home/user/jpom-go-agent-test)、
#                 DEV_PORT(默认 2124)、JAVA_AGENT_DIR(默认 /home/user/jpom/agent)
set -euo pipefail
cd "$(dirname "$0")/.."

# 真实 dev 主机地址通过 DEV_HOST 环境变量提供（不把内网地址写入仓库）
DEV_HOST="${DEV_HOST:-user@127.0.0.1}"
DEV_DIR="${DEV_DIR:-/home/user/jpom-go-agent-test}"
DEV_PORT="${DEV_PORT:-2124}"
GO_AGENT_VERSION="${GO_AGENT_VERSION:-dev-go}"
JAVA_AGENT_DIR="${JAVA_AGENT_DIR:-/home/user/jpom/agent}"
SSH_OPTS=(-F /dev/null -o StrictHostKeyChecking=accept-new -o UserKnownHostsFile=/tmp/dev_kh -o ConnectTimeout=8)
SSH=(ssh "${SSH_OPTS[@]}" "$DEV_HOST")
SCP=(scp "${SSH_OPTS[@]}")

cmd="${1:-deploy}"
case "$cmd" in
  stop)
    "${SSH[@]}" "if [ -f $DEV_DIR/bin/agent.pid ]; then kill \$(cat $DEV_DIR/bin/agent.pid) 2>/dev/null || true; rm -f $DEV_DIR/bin/agent.pid; fi"
    echo "stopped $DEV_DIR"
    exit 0
    ;;
  logs)
    "${SSH[@]}" "tail -n 120 $DEV_DIR/logs/agent.log 2>/dev/null; echo '--- stdout ---'; tail -n 40 $DEV_DIR/logs/stdout.log 2>/dev/null"
    exit 0
    ;;
  status)
    "${SSH[@]}" "if [ -f $DEV_DIR/bin/agent.pid ] && kill -0 \$(cat $DEV_DIR/bin/agent.pid) 2>/dev/null; then echo running pid=\$(cat $DEV_DIR/bin/agent.pid); else echo stopped; fi; curl -s -m 3 -o /dev/null -w 'http=%{http_code}\n' http://127.0.0.1:$DEV_PORT/ || true"
    exit 0
    ;;
  deploy) ;;
  *) echo "unknown command: $cmd" >&2; exit 2 ;;
esac

# 1) 构建 linux/amd64 静态二进制
mkdir -p build dist
bash script/go.sh build -trimpath -ldflags "-s -w -X main.version=$GO_AGENT_VERSION -X main.buildTime=$(date -u +%Y-%m-%dT%H:%M:%SZ)" -o build/agent-linux-amd64 .
echo "build ok: build/agent-linux-amd64"

# 2) 准备远端目录（首次从 Java agent 复制 conf/data 作为兼容性基线）
"${SSH[@]}" bash -s <<REMOTE
set -e
mkdir -p $DEV_DIR/bin $DEV_DIR/conf $DEV_DIR/data $DEV_DIR/logs $DEV_DIR/project-log $DEV_DIR/tmp
if [ ! -f $DEV_DIR/conf/application.yml ]; then
  cp $JAVA_AGENT_DIR/conf/application.yml $DEV_DIR/conf/application.yml
  # 测试实例使用独立端口，避免与生产 Java agent 冲突
  sed -i 's/^  port: 2123/  port: $DEV_PORT/' $DEV_DIR/conf/application.yml
fi
if [ ! -f $DEV_DIR/data/project.json ]; then
  cp -a $JAVA_AGENT_DIR/data/. $DEV_DIR/data/
  # 测试实例使用独立 installId，避免与生产节点身份冲突
  rm -f $DEV_DIR/data/INSTALL.json
  rm -rf $DEV_DIR/data/temp
fi
if [ -f $DEV_DIR/bin/agent.pid ]; then kill \$(cat $DEV_DIR/bin/agent.pid) 2>/dev/null || true; fi; fuser -k $DEV_PORT/tcp 2>/dev/null || true; sleep 0.8
REMOTE

# 3) 上传二进制并启动
"${SCP[@]}" -q build/agent-linux-amd64 "$DEV_HOST:$DEV_DIR/bin/agent"
"${SSH[@]}" "chmod +x $DEV_DIR/bin/agent; cd $DEV_DIR; nohup env JPOM_DEBUG=1 JAVA_HOME=/usr/java/jdk8u402-b06 PATH=/usr/java/jdk8u402-b06/bin:$PATH ./bin/agent -base $DEV_DIR > $DEV_DIR/logs/stdout.log 2>&1 < /dev/null & echo \$! > $DEV_DIR/bin/agent.pid; sleep 1.2; cat $DEV_DIR/bin/agent.pid"

# 4) 健康检查（用与 Java agent 相同的鉴权方式）
"${SSH[@]}" bash -s <<REMOTE
set -e
H=\$(python3 -c "import json,sys;d=json.load(open('$DEV_DIR/data/agent_authorize.json'));sys.stdout.write(d['agentName']+'@'+d['agentPwd'])" | sha1sum | cut -d' ' -f1)
echo "--- health ---"
curl -s -m 5 -o /dev/null -w "index http=%{http_code}\n" http://127.0.0.1:$DEV_PORT/
curl -s -m 5 -H "Jpom-Agent-Authorize: \$H" -H "workspaceId: DEFAULT" -X POST http://127.0.0.1:$DEV_PORT/info | head -c 500
echo
REMOTE
echo "deployed: $DEV_HOST:$DEV_DIR (port $DEV_PORT)"