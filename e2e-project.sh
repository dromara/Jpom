#!/usr/bin/env bash
# project-ops 第二轮 E2E：项目域端点 + G5 GBK + G8 错误码/断链 + i18n
set -u
cd "$(dirname "$0")/go-agent"
TMP=$(mktemp -d)
cp -r ../../go-agent-ref/conf "$TMP/conf"
cp -r ../../go-agent-ref/data "$TMP/data"
mkdir -p "$TMP/projects/gbk"
python3 - "$TMP" <<'PYPREP'
import sys, json, os
base = sys.argv[1]
wl_path = base + "/data/whitelistDirectory.json"
wl = json.load(open(wl_path))
if base + "/projects" not in wl.get("project", []):
    wl.setdefault("project", []).append(base + "/projects")
open(wl_path, "w").write(json.dumps(wl))
data_path = base + "/data/project.json"
data = json.load(open(data_path))
data["BROKEN1"] = {
    "id": "BROKEN1", "name": "断链项目", "runMode": "Link", "linkId": "MISSING1",
    "workspaceId": "DEFAULT", "createTime": "2024-01-01 00:00:00", "logPath": "",
    "mainClass": "", "token": "", "outGivingProject": False, "javaExtDirsCp": "",
    "whitelistDirectory": base + "/projects", "lib": "/broken",
}
open(data_path, "w").write(json.dumps(data))
script_path = base + "/projects/gbk/status.sh"
with open(script_path, "wb") as f:
    f.write(b"#!/bin/bash\nprintf '\\xe4\\xb8\\xad\\xe6\\x96\\x87'\n")
os.chmod(script_path, 0o755)
PYPREP
python3 - "$TMP" <<'PYPORT'
import sys
path = sys.argv[1] + "/conf/application.yml"
content = open(path).read().replace("port: 2123", "port: 2124")
open(path, "w").write(content)
PYPORT
TOKEN=$(python3 - "$TMP" <<'PYTOKEN'
import sys, json, hashlib
base = sys.argv[1]
data = json.load(open(base + "/data/agent_authorize.json"))
print(hashlib.sha1((data["agentName"] + "@" + data["agentPwd"]).encode()).hexdigest())
PYTOKEN
)
./build/agent --base "$TMP" --conf "$TMP/conf/application.yml" > "$TMP/agent.log" 2>&1 &
AGENT_PID=$!
trap 'kill $AGENT_PID 2>/dev/null' EXIT
for i in $(seq 1 60); do
  if curl -s -o /dev/null "http://127.0.0.1:2124/index"; then break; fi
  sleep 0.2
done

api_post() {
  local path=$1
  shift
  curl -s -X POST "http://127.0.0.1:2124$path" \
    -H "Jpom-Agent-Authorize: $TOKEN" \
    -H "workspaceId: DEFAULT" \
    -H "Jpom-Server-UserName: e2e-user" "$@"
}
api_post_lang() {
  local lang=$1
  local path=$2
  shift 2
  curl -s -X POST "http://127.0.0.1:2124$path" \
    -H "Jpom-Agent-Authorize: $TOKEN" \
    -H "workspaceId: DEFAULT" \
    -H "Accept-Language: $lang" "$@"
}
api_get() {
  local path=$1
  shift
  curl -s "http://127.0.0.1:2124$path" \
    -H "Jpom-Agent-Authorize: $TOKEN" \
    -H "workspaceId: DEFAULT" "$@"
}

FAIL=0
check() {
  if python3 -c "import sys,json; d=json.load(open(sys.argv[1])); assert $2, d" "$1"; then
    echo "PASS: $3"
  else
    echo "FAIL: $3"
    head -c 500 "$1"
    echo
    FAIL=1
  fi
}

echo "== 未授权访问 =="
curl -s -X POST "http://127.0.0.1:2124/manage/getProjectInfo" > "$TMP/unauth.json"
check "$TMP/unauth.json" "d['code']==900 and '授权' in d['msg']" "未授权访问 code=900"

echo "== getProjectInfo 黄金响应对比 =="
api_post /manage/getProjectInfo > "$TMP/info.json"
python3 - "$TMP" <<'PYGOLD'
import sys, json
base = sys.argv[1]
actual = json.load(open(base + "/info.json"))
golden = json.load(open("../../go-agent-ref/golden/project-info.json"))
assert actual["code"] == 200, actual
items = [item for item in actual["data"] if item["id"] != "BROKEN1"]
assert len(items) == len(golden["data"]), (len(items), len(golden["data"]))
for index, want in enumerate(golden["data"]):
    assert items[index] == want, (index, items[index], want)
print("PASS: getProjectInfo 与 Java 黄金响应一致（%d 个项目）" % len(items))
PYGOLD

echo "== getProjectItem / 状态 / 端口 =="
api_post /manage/getProjectItem -d "id=6KA03M" > "$TMP/item-jar.json"
check "$TMP/item-jar.json" "d['code']==200 and d['data']['runMode']=='Jar'" "getProjectItem Jar"
api_post /manage/getProjectItem -d "id=798M00" > "$TMP/item-dsl.json"
check "$TMP/item-dsl.json" "d['code']==200 and isinstance(d['data']['canReload'],bool) and len(d['data']['dslProcessInfo'])==4" "getProjectItem Dsl"
api_post /manage/getProjectItem -d "id=NOTEXIST" > "$TMP/item-none.json"
check "$TMP/item-none.json" "d['code']==200 and 'data' not in d" "getProjectItem 不存在省略 data"
api_post /manage/getProjectStatus -d "id=6KA03M" > "$TMP/status.json"
check "$TMP/status.json" "d['code']==200 and d['data']['statusMsg'].startswith('stopped')" "getProjectStatus"
api_post /manage/getProjectPort -d 'ids=["6KA03M"]' > "$TMP/port.json"
check "$TMP/port.json" "d['code']==200 and d['data']['6KA03M']['port']=='-'" "getProjectPort"

echo "== G8：断链项目边界（Java 语义） =="
api_post /manage/getProjectItem -d "id=BROKEN1" > "$TMP/broken-item.json"
check "$TMP/broken-item.json" "d['code']==405 and '被软链的项目已经不存在啦' in d['msg'] and 'MISSING1' in d['msg']" "断链 getProjectItem=405"
api_post /manage/getProjectStatus -d "id=BROKEN1" > "$TMP/broken-status.json"
check "$TMP/broken-status.json" "d['code']==200 and d['data']=={}" "断链 getProjectStatus=200 空对象"
api_post /manage/operate -d "id=BROKEN1&opt=start" > "$TMP/broken-operate.json"
check "$TMP/broken-operate.json" "d['code']==405 and '被软链的项目已经不存在啦' in d['msg']" "断链 operate=405"
api_post /manage/deleteProject -d "id=BROKEN1" > "$TMP/broken-del.json"
check "$TMP/broken-del.json" "d['code']==405" "断链 deleteProject=405"
api_post /manage/log/logSize -d "id=BROKEN1" > "$TMP/broken-logsize.json"
check "$TMP/broken-logsize.json" "d['code']==405" "断链 logSize=405"
api_post /manage/log/logBack -d "id=BROKEN1" > "$TMP/broken-logback.json"
check "$TMP/broken-logback.json" "d['code']==405" "断链 logBack=405"
api_post /manage/log/resetLog -d "id=BROKEN1" > "$TMP/broken-reset.json"
check "$TMP/broken-reset.json" "d['code']==500 and d['msg']=='重置日志失败'" "断链 resetLog=500 重置日志失败"
api_get "/manage/log/export?id=BROKEN1" > "$TMP/broken-export.json"
check "$TMP/broken-export.json" "d['code']==405" "断链 export=405"
api_get "/manage/log/logBack_download?id=BROKEN1&key=x.log" > "$TMP/broken-dl.json"
check "$TMP/broken-dl.json" "d['code']==400 and d['msg'].startswith('下载失败')" "断链 logBack_download=400 下载失败"

echo "== G8：saveProject 错误码（405/500） =="
api_post /manage/saveProject -d "id=BADDSL1&name=bad&runMode=Dsl&whitelistDirectory=$TMP/projects&lib=/gbk" > "$TMP/bad-dsl-missing.json"
check "$TMP/bad-dsl-missing.json" "d['code']==405 and 'dsl' in d['msg'].lower()" "DSL 缺少内容=405"
api_post /manage/saveProject --data-urlencode "dslContent=run: [invalid" -d "id=BADDSL2&name=bad&runMode=Dsl&whitelistDirectory=$TMP/projects&lib=/gbk" > "$TMP/bad-dsl-yaml.json"
check "$TMP/bad-dsl-yaml.json" "d['code']==500 and d['msg'].startswith('服务异常：')" "DSL YAML 解析失败=500 服务异常"
api_post /manage/saveProject -d "id=BADLIB&name=bad&runMode=Jar&whitelistDirectory=$TMP/projects&lib=../escape" > "$TMP/bad-lib.json"
check "$TMP/bad-lib.json" "d['code']==405 and '提升目录' in d['msg']" "越级路径=405"

echo "== N1：@ValidatorItem 参数校验 code=400（Java 实测语义） =="
api_post /manage/getProjectStatus -d "" > "$TMP/n1-status-id.json"
check "$TMP/n1-status-id.json" "d['code']==400 and d['msg']=='项目id 不正确'" "getProjectStatus 缺 id=400"
api_post /manage/operate -d "opt=start" > "$TMP/n1-operate-id.json"
check "$TMP/n1-operate-id.json" "d['code']==400 and d['msg']=='项目id 不正确'" "operate 缺 id=400"
api_post /manage/operate -d "id=6KA03M" > "$TMP/n1-operate-opt.json"
check "$TMP/n1-operate-opt.json" "d['code']==400 and d['msg']=='操作参数缺失'" "operate 缺 opt=400"
api_post /manage/operate -d "id=6KA03M&opt=status" > "$TMP/n1-operate-status.json"
check "$TMP/n1-operate-status.json" "d['code']==405 and '不支持当前操作' in d['msg']" "operate canOpt=false（业务 Assert）=405"
api_post /manage/operate -d "id=6KA03M&opt=no-such-opt" > "$TMP/n1-operate-invalid.json"
check "$TMP/n1-operate-invalid.json" "d['code']==405 and d['msg']=='请选择操作类型'" "operate 非法枚举 opt=405 请选择操作类型（与 Java 一致）"
api_post /manage/deleteProject -d "" > "$TMP/n1-delete-id.json"
check "$TMP/n1-delete-id.json" "d['code']==200 and d['msg']=='项目不存在'" "deleteProject 缺 id=200 项目不存在（无 @ValidatorItem）"
api_post /manage/log/logSize -d "" > "$TMP/n1-logsize-id.json"
check "$TMP/n1-logsize-id.json" "d['code']==405 and d['msg'].startswith('获取项目信息失败')" "logSize 缺 id=405（业务 Assert）"
api_post /manage/getProjectPort -d "" > "$TMP/n1-port-ids.json"
check "$TMP/n1-port-ids.json" "d['code']==405 and d['msg']=='没有要获取的信息'" "getProjectPort 缺 ids=405（Assert）"

echo "== i18n（en-US） =="
api_post_lang en-US /manage/operate -d "id=6KA03M" > "$TMP/en-operate.json"
check "$TMP/en-operate.json" "d['code']==400 and d['msg']=='Missing operating parameters'" "en-US operate 缺参（@ValidatorItem=400）"
api_post_lang en-US /manage/getProjectItem -d "id=BROKEN1" > "$TMP/en-broken.json"
check "$TMP/en-broken.json" "d['code']==405 and 'soft-chained' in d['msg'].lower()" "en-US 断链文案"

echo "== G5：项目日志 GBK 编码 =="
DSL_CONTENT=$(printf 'run:\n  status:\n    scriptId: status.sh\n')
api_post /manage/saveProject --data-urlencode "dslContent=$DSL_CONTENT" \
  -d "id=GBK001&name=GBK项目&runMode=Dsl&whitelistDirectory=$TMP/projects&lib=/gbk&logCharset=GBK" > "$TMP/gbk-save.json"
check "$TMP/gbk-save.json" "d['code']==200 and d['msg']=='新增成功！'" "保存 GBK DSL 项目"
api_post /manage/getProjectStatus -d "id=GBK001" > "$TMP/gbk-status.json"
check "$TMP/gbk-status.json" "d['code']==200 and d['data']['statusMsg']=='中文'" "GBK 项目 status 执行（UTF-8 脚本输出解码）"
api_get "/manage/log/export?id=GBK001" > "$TMP/gbk-export.log"
if python3 - "$TMP" <<'PYGBK'
import sys
raw = open(sys.argv[1] + "/gbk-export.log", "rb").read()
assert raw, "导出日志为空"
try:
    raw.decode("utf-8")
    raise AssertionError("GBK 日志被写成 UTF-8")
except UnicodeDecodeError:
    pass
text = raw.decode("gbk")
assert "中文" in text, text
print("PASS: 项目控制台日志按 logCharset=GBK 写入/导出（GBK 中文）")
PYGBK
then
  :
else
  echo "FAIL: GBK 日志校验失败"
  xxd "$TMP/gbk-export.log" | head -5
  FAIL=1
fi

echo "== 常规日志端点回归 =="
api_post /manage/log/logSize -d "id=6KA03M" > "$TMP/logsize.json"
check "$TMP/logsize.json" "d['code']==200 and d['data']['logBack']==False" "logSize"
api_post /manage/log/logBack -d "id=6KA03M" > "$TMP/logback.json"
check "$TMP/logback.json" "d['code']==200 and d['data']['id']=='6KA03M'" "logBack 列表"
api_post /manage/log/resetLog -d "id=6KA03M" > "$TMP/reset.json"
check "$TMP/reset.json" "d['code']==200 and d['msg']=='重置成功'" "resetLog"
api_post /manage/log/logBack_delete -d "id=6KA03M" > "$TMP/logdel-noname.json"
check "$TMP/logdel-noname.json" "d['code']==405 and d['msg']=='没有对应到文件'" "logBack_delete 无 name=405"
api_post /manage/log/logBack_delete -d "id=6KA03M&name=x.log" > "$TMP/logdel-nodir.json"
check "$TMP/logdel-nodir.json" "d['code']==500 and d['msg']=='没有对应文件夹'" "logBack_delete 目录不存在=500 原文"

echo "== 数据回写/稳定性 =="
python3 - "$TMP" <<'PYDATA'
import sys, json
base = sys.argv[1]
data = json.load(open(base + "/data/project.json"))
assert data["846Z6S"]["outGivingProject"] in (True, False)
assert isinstance(data["6KA03M"]["autoStart"], bool)
print("PASS: project.json 布尔字段为 bool 类型")
PYDATA
if grep -qi "panic" "$TMP/agent.log"; then
  echo "FAIL: agent 日志存在 panic"
  tail -20 "$TMP/agent.log"
  FAIL=1
else
  echo "PASS: agent 日志无 panic"
fi

if [ $FAIL -ne 0 ]; then
  echo "E2E FAILED"
  exit 1
fi
echo "E2E ALL PASSED"

