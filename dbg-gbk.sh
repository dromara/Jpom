#!/usr/bin/env bash
set -u
cd "$(dirname "$0")/go-agent"
TMP=$(mktemp -d)
mkdir -p "$TMP/conf" "$TMP/data" "$TMP/projects/gbk"
printf 'jpom:\n  authorize:\n    agent-name: jpomAgent\n  project:\n    log:\n      file-charset: GBK\n  system:\n    console-charset: GBK\nserver:\n  port: 2141\n' > "$TMP/conf/application.yml"
printf '{"project":["%s"]}' "$TMP/projects" > "$TMP/data/whitelistDirectory.json"
python3 - "$TMP" <<'PY'
import sys, os
base=sys.argv[1]
with open(base+"/projects/gbk/status.sh","wb") as f:
    f.write(b"#!/bin/bash\nprintf '\\xd6\\xd0\\xce\\xc4'\n")
os.chmod(base+"/projects/gbk/status.sh",0o755)
PY
./build/agent --base "$TMP" --conf "$TMP/conf/application.yml" > "$TMP/agent.log" 2>&1 &
AP=$!
trap 'kill $AP 2>/dev/null' EXIT
for i in $(seq 1 50); do curl -s -o /dev/null "http://127.0.0.1:2141/index" && break; sleep 0.2; done
TOKEN=$(python3 - "$TMP" <<'PYTOKEN'
import sys, json, hashlib
data = json.load(open(sys.argv[1] + "/data/agent_authorize.json"))
print(hashlib.sha1((data["agentName"] + "@" + data["agentPwd"]).encode()).hexdigest())
PYTOKEN
)
DSL_CONTENT=$(printf 'run:\n  status:\n    scriptId: status.sh\n')
curl -s -X POST "http://127.0.0.1:2141/manage/saveProject" -H "Jpom-Agent-Authorize: $TOKEN" -H "workspaceId: DEFAULT" \
  --data-urlencode "dslContent=$DSL_CONTENT" \
  -d "id=GBK001&name=GBK项目&runMode=Dsl&whitelistDirectory=$TMP/projects&lib=/gbk&logCharset=GBK"; echo
echo '--- status ---'
curl -s -X POST "http://127.0.0.1:2141/manage/getProjectStatus" -H "Jpom-Agent-Authorize: $TOKEN" -H "workspaceId: DEFAULT" -d "id=GBK001"; echo
echo '--- files ---'
find "$TMP/project-log" -type f -exec ls -la {} \; 2>/dev/null
echo '--- log content (xxd) ---'
xxd "$TMP/project-log/GBK001/GBK001.log" 2>/dev/null | head -6
echo '--- export ---'
curl -s -H "Jpom-Agent-Authorize: $TOKEN" "http://127.0.0.1:2141/manage/log/export?id=GBK001" | xxd | head -6
echo '--- agent log tail ---'
tail -8 "$TMP/agent.log"
echo TMP_DIR=$TMP

