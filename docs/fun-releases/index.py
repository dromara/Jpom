# -*- coding: utf-8 -*-
import requests

OK = '200 OK'
FOUND = '302 FOUND'
TEXT_HEADER = [('Content-type', 'text/plain')]
TAB_NAME = 'tag_name'


# 使用码云资源
def ossDownload(environ, start_response):
    # 查询版本
    result = requests.get('https://dromara.gitee.io/jpom/docs/versions.json')
    json = result.json()
    return doJson(environ, start_response, json)


# 使用GitHub
def githubOssDownload(environ, start_response):
    # 查询版本
    result = requests.get('https://api.github.com/repos/dromara/Jpom/releases/latest')
    json = result.json()
    return doJson(environ, start_response, json)


# 查询版本号
def showVersion(environ, start_response):
    # 查询版本
    result = requests.get('https://api.github.com/repos/dromara/Jpom/releases/latest')
    json = result.json()
    tag_name = getVersion(json)
    if tag_name == '':
        return responseOK('没有tagName', start_response)
    return responseOK(tag_name, start_response)


def getVersion(json):
    if json and TAB_NAME in json:
        tag_name = json[TAB_NAME]
        if tag_name and tag_name.strip() != '':
            tag_name = tag_name.replace('v', '')
            return tag_name.strip()
        else:
            return ''
    else:
        return ''


# 处理查询到的版本json
def doJson(environ, start_response, json):
    tag_name = getVersion(json)
    if tag_name == '':
        return responseOK('没有tagName', start_response)
    # 处理请求参数
    try:
        query_string = environ['QUERY_STRING']
    except KeyError:
        query_string = ""
    pars = query_string.split('&')
    typeName = 'Server'
    for par in pars:
        if par.startswith('type='):
            typeName = par.strip().split("=")[1]
    # 重定向到下载地址
    url = "https://jpom-releases.oss-cn-hangzhou.aliyuncs.com/" + typeName.lower() + "-" + tag_name + "-release.zip"
    start_response(FOUND, [('Location', url)])
    return [bytes(url, encoding="utf8")]


# 响应ok
def responseOK(value, start_response):
    start_response(OK, TEXT_HEADER)
    return [bytes(value, encoding="utf8")]
