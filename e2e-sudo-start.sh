#!/usr/bin/env bash
# 验证 command-use-sudo=true 时项目启动命令不加 sudo（Java 行为）
set -u
cd "$(dirname "$0")/go-agent"
TMP=$(mktemp -d)
mkdir -p "$TMP/conf" "$TMP/data" "$TMP/projects/app" "$TMP/fakebin"
printf 'jpom:\n  authorize:\n    agent-name: jpomAgent\n  project:\n    status-wait-time: 1\n  system:\n    command-use-sudo: true\nserver:\n  port: 2127\n' > "$TMP/conf/application.yml"
printf '{"project":["%s"]}' "$TMP/projects" > "$TMP/data/whitelistDirectory.json"
printf 'jar' > "$TMP/projects/app/app.jar"
MARKER="$TMP/java-invoked.txt"
export MARKER
cat > "$TMP/fakebin/java" <<'FAKE'
#!/usr/bin/env bash
echo "$@" > "$MARKER"
sleep 0.2
FAKE
chmod +x "$TMP/fakebin/java"
env PATH="$TMP/fakebin:/usr/bin:/bin" ./build/agent --base "$TMP" --conf "$TMP/conf/application.yml" > "$TMP/agent.log" 2>&1 &
AP=$!
trap 'kill $AP 2>/dev/null; rm -rf "$TMP"' EXIT
for i in $(seq 1 50); do curl -s -o /dev/null "http://127.0.0.1:2127/index" && break; sleep 0.2; done
TOKEN=$(python3 - "$TMP" <<'PYTOKEN'
import sys, json, hashlib
data = json.load(open(sys.argv[1] + "/data/agent_authorize.json"))
print(hashlib.sha1((data["agentName"] + "@" + data["agentPwd"]).encode()).hexdigest())
PYTOKEN
)
curl -s -X POST "http://127.0.0.1:2127/manage/saveProject" -H "Jpom-Agent-Authorize: $TOKEN" -H "workspaceId: DEFAULT" \
  -d "id=SUDOE2E&name=sudo-e2e&runMode=ClassPath&mainClass=com.example.Main&whitelistDirectory=$TMP/projects&lib=/app" > "$TMP/save.json"
curl -s -X POST "http://127.0.0.1:2127/manage/operate" -H "Jpom-Agent-Authorize: $TOKEN" -H "workspaceId: DEFAULT" -d "id=SUDOE2E&opt=start" > "$TMP/start.json"
for i in $(seq 1 30); do [ -f "$MARKER" ] && break; sleep 0.2; done
if [ -f "$MARKER" ]; then
  echo "PASS: command-use-sudo=true 时启动命令未加 sudo，java 参数: $(head -c 200 "$MARKER")"
else
  echo "FAIL: command-use-sudo=true 时启动命令未执行"
  tail -10 "$TMP/agent.log"
  exit 1
fi

