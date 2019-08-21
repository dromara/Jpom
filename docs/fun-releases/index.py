# -*- coding: utf-8 -*-
import requests

END = b'OK\n'
ERROR_END = b'EROOR\n'
OK = '200 OK'
Headers = [('Content-type', 'text/plain')]


def handler(environ, start_response):
    # 查询版本
    result = requests.get('https://api.github.com/repos/jiangzeyin/Jpom/releases/latest')
    json = result.json()
    tag_name = json['tag_name']
    if tag_name.strip() == '':
        start_response(OK, Headers)
        return [ERROR_END]
    tag_name = tag_name.replace('v', '')
    # 处理请求参数
    try:
        query_string = environ['QUERY_STRING']
    except (KeyError):
        query_string = ""
    pars = query_string.split('&')
    type = 'Server'
    for par in pars:
        if par.strip() == 'type':
            type = par.strip().split("=")
    url = "https://jpom-releases.oss-cn-hangzhou.aliyuncs.com/" + type.lower() + "-" + tag_name + "-release.zip"
    start_response('302 FOUND', [('Location', url)])
    return [END]
