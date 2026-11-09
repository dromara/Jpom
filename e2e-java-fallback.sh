#!/usr/bin/env bash
# project-ops：java 可执行文件解析回退自测（PATH -> JAVA_HOME -> 常见路径）
set -u
cd "$(dirname "$0")/go-agent"
FAIL=0
TMP=$(mktemp -d)
mkdir -p "$TMP/conf" "$TMP/data" "$TMP/projects/app"
printf 'jpom:\n  authorize:\n    agent-name: jpomAgent\nserver:\n  port: 2126\n' > "$TMP/conf/application.yml"
printf '{"project":["%s"]}' "$TMP/projects" > "$TMP/data/whitelistDirectory.json"
printf 'not-a-jar' > "$TMP/projects/app/app.jar"

refresh_token() {
  for i in $(seq 1 50); do
    if [ -f "$TMP/data/agent_authorize.json" ]; then break; fi
    sleep 0.1
  done
  TOKEN=$(python3 - "$TMP" <<'PYTOKEN'
import sys, json, hashlib
base = sys.argv[1]
data = json.load(open(base + "/data/agent_authorize.json"))
print(hashlib.sha1((data["agentName"] + "@" + data["agentPwd"]).encode()).hexdigest())
PYTOKEN
)
}
start_agent() {
  env PATH="$1" JAVA_HOME="$2" HOME=/tmp ./build/agent --base "$TMP" --conf "$TMP/conf/application.yml" > "$TMP/agent.log" 2>&1 &
  AGENT_PID=$!
  for i in $(seq 1 50); do
    if curl -s -o /dev/null "http://127.0.0.1:2126/index"; then refresh_token; return 0; fi
    sleep 0.2
  done
  return 1
}
stop_agent() { kill "$AGENT_PID" 2>/dev/null; sleep 0.4; }
post() {
  local path=$1
  shift
  curl -s -X POST "http://127.0.0.1:2126$path" -H "Jpom-Agent-Authorize: $TOKEN" -H "workspaceId: DEFAULT" "$@"
}

echo "== 场景1：PATH 无 java 且未配置 JAVA_HOME =="
if ! start_agent "/usr/bin:/bin" ""; then echo "FAIL: agent 启动失败"; cat "$TMP/agent.log"; exit 1; fi
post /manage/saveProject -d "id=JAVA01&name=java解析&runMode=Jar&whitelistDirectory=$TMP/projects&lib=/app&autoStart=false" > "$TMP/save.json"
post /manage/operate -d "id=JAVA01&opt=start" > "$TMP/start1.json"
python3 - "$TMP" <<'PYCHECK1'
import json, sys
data = json.load(open(sys.argv[1] + "/start1.json"))
assert data["code"] == 500, data
assert "java executable not found" in data["msg"], data
print("PASS: 未找到 java 时 operate start 返回明确错误 ->", data["msg"][:160])
PYCHECK1
stop_agent

echo "== 场景2：PATH 无 java，但 JAVA_HOME 指向真实 JDK =="
JAVA_BIN=$(command -v java || true)
if [ -z "$JAVA_BIN" ]; then
  echo "SKIP: 当前环境没有 java"
else
  REAL_JAVA_HOME=$(dirname "$(dirname "$(readlink -f "$JAVA_BIN")")")
  echo "JAVA_HOME=$REAL_JAVA_HOME"
  if ! start_agent "/usr/bin:/bin" "$REAL_JAVA_HOME"; then echo "FAIL: agent 启动失败"; cat "$TMP/agent.log"; exit 1; fi
  post /manage/operate -d "id=JAVA01&opt=start" > "$TMP/start2.json"
  python3 - "$TMP" <<'PYCHECK2'
import json, sys
data = json.load(open(sys.argv[1] + "/start2.json"))
msg = data["msg"]
assert "java executable not found" not in msg, msg
print("PASS: JAVA_HOME 回退生效 ->", msg[:160])
PYCHECK2
  stop_agent
fi
if grep -qi "panic" "$TMP/agent.log"; then echo "FAIL: agent panic"; FAIL=1; else echo "PASS: 无 panic"; fi
rm -rf "$TMP"
if [ $FAIL -ne 0 ]; then exit 1; fi
echo "JAVA FALLBACK ALL PASSED"

