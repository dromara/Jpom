#!/usr/bin/env python3
# Copyright (c) 2019 Of Him Code Technology Studio
# Jpom is licensed under Mulan PSL v2.
#
# 为 Go 源码中硬编码的中文文案推荐 Java i18n key（按 zh-CN 值精确/模板匹配）。
# 输出 TSV：file<TAB>line<TAB>literal<TAB>key<TAB>confidence
# 用法：python3 script/i18n-suggest.py [--out path]
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MESSAGES = os.path.join(ROOT, 'internal', 'i18n', 'messages.gen.json')
CJK = re.compile(r'[\u4e00-\u9fff]')

with open(MESSAGES, encoding='utf-8') as handle:
    messages = json.load(handle)
zh = {v.get('zh-CN'): k for k, v in messages.items() if v.get('zh-CN')}

def canonical(text):
    # 把 Go 的格式化占位与 Java 的 {} 统一
    text = re.sub(r'%[-+0-9.#]*[a-zA-Z]', '{}', text)
    text = text.replace('：', ':').replace('，', ',').replace('（', '(').replace('）', ')')
    return re.sub(r'\s+', '', text)

zh_canon = {}
for value, key in zh.items():
    zh_canon.setdefault(canonical(value), key)

out = []
for base, dirs, files in os.walk(os.path.join(ROOT, 'internal')):
    if 'i18n' in base.split(os.sep):
        continue
    for name in files:
        if not name.endswith('.go') or name.endswith('_test.go'):
            continue
        path = os.path.join(base, name)
        with open(path, encoding='utf-8') as handle:
            for lineno, line in enumerate(handle, 1):
                code = line.split('//')[0]
                for match in re.finditer(r'"([^"\\]*(?:\\.[^"\\]*)*)"', code):
                    literal = match.group(1)
                    if not CJK.search(literal):
                        continue
                    key = zh.get(literal)
                    conf = 'exact' if key else None
                    if not key:
                        key = zh_canon.get(canonical(literal))
                        conf = 'template' if key else None
                    out.append((os.path.relpath(path, ROOT), str(lineno), literal.replace('\t', ' '), key or '', conf or 'none'))

target = sys.argv[sys.argv.index('--out') + 1] if '--out' in sys.argv else None
lines = ['\t'.join(item) for item in out]
if target:
    with open(target, 'w', encoding='utf-8') as handle:
        handle.write('\n'.join(lines) + '\n')
    exact = sum(1 for item in out if item[4] == 'exact')
    template = sum(1 for item in out if item[4] == 'template')
    print('total:', len(out), 'exact:', exact, 'template:', template, 'unmatched:', len(out) - exact - template)
else:
    for line in lines:
        print(line)
