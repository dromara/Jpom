#!/usr/bin/env python3
# Copyright (c) 2019 Of Him Code Technology Studio
# Jpom is licensed under Mulan PSL v2.
# 从 Java 插件端的 i18n properties 生成 Go 端嵌入用的 messages.gen.json。
# 用法：python3 script/gen-i18n.py
#
# 解析规则与 java.util.Properties 保持一致：
#   - 逻辑行续行：行尾单个反斜杠表示续行（偶数个反斜杠不续行），续行行首空白忽略；
#   - 键值分隔：首个未转义的 =、: 或空白；
#   - 转义还原：\! \= \: \# \\ 还原为字符本身，\t \n \r \f 还原为控制字符，
#     \uXXXX 还原为 Unicode 字符（\ 必须最后按单遍扫描处理，避免二次转义）。
import json
import os
import string

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES_DIR = os.path.join(ROOT, 'internal', 'i18n', 'resources')
OUT = os.path.join(ROOT, 'internal', 'i18n', 'messages.gen.json')
LANGS = {
    'messages_zh_CN.properties': 'zh-CN',
    'messages_zh_HK.properties': 'zh-HK',
    'messages_zh_TW.properties': 'zh-TW',
    'messages_en_US.properties': 'en-US',
}
RESULT = {}


def unescape(value):
    """按 Java Properties 规则做单遍反转义。"""
    result = []
    index = 0
    length = len(value)
    while index < length:
        char = value[index]
        if char != '\\':
            result.append(char)
            index += 1
            continue
        index += 1
        if index >= length:
            # 行尾孤立反斜杠：保留
            result.append('\\')
            break
        escape = value[index]
        index += 1
        if escape == 'u':
            hex_part = value[index:index + 4]
            if len(hex_part) == 4 and all(item in string.hexdigits for item in hex_part):
                result.append(chr(int(hex_part, 16)))
                index += 4
            else:
                result.append('u')
        elif escape == 't':
            result.append('\t')
        elif escape == 'n':
            result.append('\n')
        elif escape == 'r':
            result.append('\r')
        elif escape == 'f':
            result.append('\f')
        else:
            # \\ -> \, \! -> !, \= -> =, \: -> :, \# -> # 等
            result.append(escape)
    return ''.join(result)


def logical_lines(path):
    """把物理行合并为 Properties 逻辑行（处理行尾续行）。"""
    with open(path, 'r', encoding='utf-8') as handle:
        physical = handle.read().splitlines()
    result = []
    pending = ''
    for raw in physical:
        line = pending + raw.lstrip() if pending else raw
        stripped = line.rstrip()
        trailing = len(stripped) - len(stripped.rstrip('\\'))
        if trailing % 2 == 1:
            pending = stripped[:-1]
            continue
        result.append(line)
        pending = ''
    if pending:
        result.append(pending)
    return result


def split_key_value(line):
    """按首个未转义的 = / : / 空白 拆分 key 与 value。"""
    index = 0
    length = len(line)
    while index < length:
        char = line[index]
        if char == '\\':
            index += 2
            continue
        if char in '=:':
            return line[:index], line[index + 1:].lstrip()
        if char in ' \t\f':
            rest = line[index:].lstrip()
            if rest[:1] in ('=', ':'):
                return line[:index], rest[1:].lstrip()
            return line[:index], rest
        index += 1
    return line, ''


def read_properties(path):
    data = {}
    for line in logical_lines(path):
        stripped = line.strip()
        if not stripped or stripped.startswith(('#', '!')):
            continue
        key, value = split_key_value(line)
        key = unescape(key.strip())
        if not key:
            continue
        data[key] = unescape(value)
    return data


for filename, lang in LANGS.items():
    path = os.path.join(RES_DIR, filename)
    for key, value in read_properties(path).items():
        RESULT.setdefault(key, {})[lang] = value

# Go 版新增文案（Java properties 中不存在，如升级/回滚提示）叠加进同一份数据，
# 供 Go 端 embed 与 script/i18n-check.py 统一校验；已有 key 以 Java 为准。
GO_OVERLAY = os.path.join(ROOT, 'internal', 'i18n', 'messages_go.json')
if os.path.isfile(GO_OVERLAY):
    with open(GO_OVERLAY, 'r', encoding='utf-8') as overlay_handle:
        overlay = json.load(overlay_handle)
    for key, entry in overlay.items():
        if key in RESULT:
            continue
        RESULT[key] = entry

with open(OUT, 'w', encoding='utf-8') as handle:
    json.dump(RESULT, handle, ensure_ascii=False, indent=0, sort_keys=True)
    handle.write('\n')
print('generated', OUT, 'keys:', len(RESULT))
