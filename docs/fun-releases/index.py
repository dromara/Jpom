# -*- coding: utf-8 -*-

import requests

OK = '200 OK'
response_headers = [('Content-type', 'text/plain')]


def handler(environ, start_response):
    # 查询版本
    result = requests.get('https://api.github.com/repos/jiangzeyin/Jpom/releases/latest')
    json = result.json()
    tag_name = json['tag_name']
    if tag_name.strip() == '':
        start_response(OK, response_headers)
        return "没有发布版1"
    tag_name = tag_name.replace('v', '')
    # 处理请求参数
    type = 'Server'
    try:
        query_string = environ['QUERY_STRING']
    except (KeyError):
        query_string = ""
    pars = query_string.split('&')
    for par in pars:
        if par.strip() == 'type':
            type = par.strip().split("=")
    # 重定向
    url = "https://jpom-releases.oss-cn-hangzhou.aliyuncs.com/" + type + "-" + tag_name + "-release.zip"
    start_response('302 FOUND', [('Location', url)])
    return tag_name
