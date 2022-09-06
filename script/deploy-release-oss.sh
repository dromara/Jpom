#!/bin/bash

DIR_PATH=$(cd `dirname $0`; pwd)"/../"

echo ${DIR_PATH}

now_version=`cat ${DIR_PATH}/jpom-parent/docs/version.txt`

if [ ! -n "$now_version" ]; then
    echo "ERROR: 版本不存在，请确认 /jpom-parent/docs/version.txt 中信息正确"
    exit
fi

OS="$(uname)"
case "${OS}" in
Linux)
	ossUtil='ossutil64'
	;;
Darwin)
	ossUtil='ossutilmac64'
	;;
*)
	echo "Unsupported os: ${OS}"
	exit 1
	;;
esac

# 同步到 agent oss 中
${ossUtil} cp ${DIR_PATH}/jpom-parent/modules/agent/target/agent-${now_version}-release.tar.gz oss://jpom-releases/release/${now_version}/ -f
${ossUtil} cp ${DIR_PATH}/jpom-parent/modules/agent/target/agent-${now_version}-release.tar.gz.sha1 oss://jpom-releases/release/${now_version}/ -f
${ossUtil} cp ${DIR_PATH}/jpom-parent/modules/agent/target/agent-${now_version}-release.zip oss://jpom-releases/release/${now_version}/ -f
${ossUtil} cp ${DIR_PATH}/jpom-parent/modules/agent/target/agent-${now_version}-release.zip.sha1 oss://jpom-releases/release/${now_version}/ -f

# 同步到 server oss 中
${ossUtil} cp ${DIR_PATH}/jpom-parent/modules/server/target/server-${now_version}-release.tar.gz oss://jpom-releases/release/${now_version}/ -f
${ossUtil} cp ${DIR_PATH}/jpom-parent/modules/server/target/server-${now_version}-release.tar.gz.sha1 oss://jpom-releases/release/${now_version}/ -f
${ossUtil} cp ${DIR_PATH}/jpom-parent/modules/server/target/server-${now_version}-release.zip oss://jpom-releases/release/${now_version}/ -f
${ossUtil} cp ${DIR_PATH}/jpom-parent/modules/server/target/server-${now_version}-release.zip.sha1 oss://jpom-releases/release/${now_version}/ -f

rm -f jpom-${now_version}.zip

zip -rj jpom-${now_version}.zip ${DIR_PATH}/jpom-parent/modules/server/target/server-${now_version}-release.zip ${DIR_PATH}/jpom-parent/modules/agent/target/agent-${now_version}-release.zip

# 完整的压缩包
${ossUtil} cp ${DIR_PATH}/jpom-${now_version}.zip oss://jpom-releases/release/${now_version}/ -f

rm -f jpom-${now_version}.zip
