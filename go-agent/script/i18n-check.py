#!/usr/bin/env python3
# Copyright (c) 2019 Of Him Code Technology Studio
# Jpom is licensed under Mulan PSL v2.
#
# i18n 检查工具：
#   1) 校验 Go 源码里引用的 i18n key 都存在于 messages.gen.json
#   2) 扫描 internal/ 下（排除 i18n 自身与 _test.go、注释）仍硬编码的中文字符串，输出待改清单
#   3) 可选：--strict 时发现任何中文硬编码即退出码 1（CI 用）
#
# 用法：python3 script/i18n-check.py [--strict]
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MESSAGES = os.path.join(ROOT, 'internal', 'i18n', 'messages.gen.json')
STRICT = '--strict' in sys.argv

CJK = re.compile(r'[\u4e00-\u9fff]')
KEY_REF = re.compile(r'"(i18n\.[A-Za-z0-9_.]+)"')

with open(MESSAGES, encoding='utf-8') as handle:
    messages = json.load(handle)

missing_keys = []
hardcoded = []
for base, dirs, files in os.walk(os.path.join(ROOT, 'internal')):
    if 'i18n' in base.split(os.sep):
        continue
    for name in files:
        if not name.endswith('.go') or name.endswith('_test.go'):
            continue
        path = os.path.join(base, name)
        with open(path, encoding='utf-8') as handle:
            for lineno, line in enumerate(handle, 1):
                stripped = line.strip()
                if stripped.startswith('//'):
                    continue
                for key in KEY_REF.findall(line):
                    if key not in messages:
                        missing_keys.append((path, lineno, key))
                # 字符串字面量中的中文（粗筛；资源/测试/注释除外）
                code = line.split('//')[0]
                for match in re.finditer(r'"([^"\\]*(?:\\.[^"\\]*)*)"', code):
                    literal = match.group(1)
                    if CJK.search(literal) and not literal.startswith('i18n.'):
                        hardcoded.append((os.path.relpath(path, ROOT), lineno, literal[:80]))

print('=== i18n key 检查 ===')
print('messages keys:', len(messages))
if missing_keys:
    print('缺失 key:', len(missing_keys))
    for item in missing_keys[:50]:
        print('  %s:%d %s' % item)
else:
    print('所有引用的 i18n key 均存在')

print('=== 硬编码中文（待接入 i18n）===')
print('数量:', len(hardcoded))
for item in hardcoded[:200]:
    print('  %s:%d %s' % item)

if missing_keys or (STRICT and hardcoded):
    sys.exit(1)
