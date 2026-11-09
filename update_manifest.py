#!/usr/bin/env python3
#
# Copyright (c) 2019 Of Him Code Technology Studio
# Jpom is licensed under Mulan PSL v2.
# You can use this software according to the terms and conditions of the Mulan PSL v2.
# You may obtain a copy of Mulan PSL v2 at:
# 			http://license.coscl.org.cn/MulanPSL2
# THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
# See the Mulan PSL v2 for more details.
#
# 刷新 download_link 分支的 JDK 下载清单（jdk/<版本>/<架构>）。
#
# 数据来源：Adoptium API 的“最新 GA 包文件名” + 清华 TUNA 直链；写入前对每个 URL 做 HEAD 校验。
# 运行环境：GitHub Actions（.github/workflows/update-jdk.yml，master 分支）或本地 Python 3。
#
# 行为约定：
#   - 只更新清单中已存在的“版本/架构”组合，不新增/删除文件；
#   - 解析失败或 URL 非 200 时保留旧值并告警（不会产生 404 清单）；
#   - 只有在内容变化时才 commit/push；
#   - 支持 --dry-run：只打印差异，不提交。

import json
import os
import re
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

TUNA_BASE = 'https://mirrors.tuna.tsinghua.edu.cn/Adoptium'
ADOPTIUM_API = ('https://api.adoptium.net/v3/assets/latest/{v}/hotspot'
                '?os=linux&architecture={a}&image_type=jdk&vendor=eclipse')
# 与 server 侧 DockerYmlDsl.javaPluginCheck 白名单对齐（8/11/17/18/21/25），并保留清单中已有的 19/20
VERSIONS = ['8', '11', '17', '18', '19', '20', '21', '25']
ARCHS = ['aarch64', 'arm', 'ppc64le', 's390x', 'x64']
UA = 'jpom-jdk-refresh/3.0 (+https://gitee.com/dromara/Jpom)'
JDK_DIR = Path(__file__).resolve().parent / 'jdk'
DRY_RUN = '--dry-run' in sys.argv[1:]


def http(url, method='GET', timeout=30):
    req = urllib.request.Request(url, method=method, headers={'User-Agent': UA})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return resp.status, resp.read()


def latest_name(version, arch):
    """Adoptium API 返回的最新 GA 文件名（x64/aarch64/... 中的 filename）。"""
    try:
        status, body = http(ADOPTIUM_API.format(v=version, a=arch))
        if status != 200:
            return None
        data = json.loads(body)
        if not data:
            return None
        return data[0]['binary']['package']['name']
    except Exception as exc:  # noqa: BLE001 - 网络异常统一降级
        print('WARN adoptium api %s/%s: %s' % (version, arch, exc), file=sys.stderr)
        return None


def listing_name(version, arch):
    """兜底：TUNA 目录页第一个 tar.gz（部分网络环境目录页会被 403）。"""
    try:
        status, body = http('%s/%s/jdk/%s/linux/' % (TUNA_BASE, version, arch))
        if status != 200:
            return None
        text = body.decode('utf-8', 'ignore')
        pattern = r'(OpenJDK[0-9A-Za-z._-]+-jdk_%s_linux_hotspot_[0-9A-Za-z._-]+\.tar\.gz)' % re.escape(arch)
        match = re.search(pattern, text)
        return match.group(1) if match else None
    except Exception as exc:  # noqa: BLE001
        print('WARN tuna listing %s/%s: %s' % (version, arch, exc), file=sys.stderr)
        return None


def head_ok(url):
    try:
        status, _ = http(url, method='HEAD', timeout=25)
        return status == 200
    except urllib.error.HTTPError as exc:
        return exc.code == 200
    except Exception as exc:  # noqa: BLE001
        print('WARN head %s: %s' % (url, exc), file=sys.stderr)
        return False


def resolve_url(version, arch):
    for name in (latest_name(version, arch), listing_name(version, arch)):
        if not name:
            continue
        url = '%s/%s/jdk/%s/linux/%s' % (TUNA_BASE, version, arch, name)
        if head_ok(url):
            return url
        print('WARN not reachable: %s' % url, file=sys.stderr)
    return None


def main():
    if not JDK_DIR.is_dir():
        print('ERROR jdk dir not found: %s' % JDK_DIR, file=sys.stderr)
        return 1

    changed = 0
    unresolved = []
    api_ok = 0
    for version in VERSIONS:
        for arch in ARCHS:
            target = JDK_DIR / version / arch
            if not target.exists():
                continue  # 只维护已有组合，避免清单结构漂移
            old_url = target.read_text().strip()
            if latest_name(version, arch):
                api_ok += 1
            url = resolve_url(version, arch)
            if url is None:
                unresolved.append('%s/%s' % (version, arch))
                print('KEEP %s/%s (old: %s)' % (version, arch, old_url))
                continue
            if url == old_url:
                print('OK   %s/%s' % (version, arch))
                continue
            target.write_text(url + '\n')
            changed += 1
            print('UPD  %s/%s %s' % (version, arch, url))

    print('changed files: %d' % changed)
    if unresolved:
        print('WARN unresolved: %s（已保留旧值）' % ', '.join(unresolved), file=sys.stderr)

    if api_ok == 0:
        print('ERROR Adoptium API 全部请求失败，疑似网络问题，放弃本次更新', file=sys.stderr)
        return 1

    if changed == 0:
        print('no jdk changes, skip commit/push')
        return 0
    if DRY_RUN:
        print('dry-run: skip commit/push')
        return 0

    def run(*args, check=True):
        ret = subprocess.run(args, check=False)
        if check and ret.returncode != 0:
            raise SystemExit('command failed: %s' % ' '.join(args))
        return ret.returncode

    run('git', 'config', 'user.name', 'jpom jdk')
    run('git', 'config', 'user.email', os.environ.get('GITEE_COMMIT_EMAIL', 'bwcx_jzy@163.com'))
    run('git', 'add', 'jdk')
    if not subprocess.run(['git', 'status', '--porcelain', '--', 'jdk'],
                          capture_output=True, text=True, check=False).stdout.strip():
        print('no staged change after git add, skip')
        return 0
    run('git', 'commit', '-m', 'update jdk')

    # 并发/网络抖动时重试；失败以非 0 退出，让 workflow 红掉而不是静默停更
    for attempt in range(1, 4):
        if run('git', 'push', 'origin', 'HEAD:download_link', check=False) == 0:
            print('pushed jdk manifest update')
            return 0
        print('WARN push failed (attempt %d/3)' % attempt, file=sys.stderr)
        if attempt < 3:
            run('git', 'pull', '--rebase', 'origin', 'download_link', check=False)
            time.sleep(5 * attempt)
    print('ERROR git push failed after retries', file=sys.stderr)
    return 1


if __name__ == '__main__':
    sys.exit(main())
