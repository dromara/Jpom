# -*- coding: utf-8 -*-
import requests

OK = '200 OK'
FOUND = '302 FOUND'
TEXT_HEADER = [('Content-type', 'text/plain')]


def handler(environ, start_response):
    # 查询版本
    result = requests.get('https://keepbx.gitee.io/jpom/versions.json')
    json = result.json()
    tag_name = json['tag_name']
    if tag_name.strip() == '':
        start_response(OK, TEXT_HEADER)
        return [bytes('没有tagName', encoding="utf8")]
    tag_name = tag_name.replace('v', '')
    # 处理请求参数
    try:
        query_string = environ['QUERY_STRING']
    except (KeyError):
        query_string = ""
    pars = query_string.split('&')
    type = 'Server'
    for par in pars:
        if par.startswith('type='):
            type = par.strip().split("=")[1]
    # 重定向到下载地址
    url = "https://jpom-releases.oss-cn-hangzhou.aliyuncs.com/" + type.lower() + "-" + tag_name + "-release.zip"
    start_response(FOUND, [('Location', url)])
    return [bytes(url, encoding="utf8")]
