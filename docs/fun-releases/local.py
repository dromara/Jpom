# -*- coding:utf-8 -*-
# -*- created by: mo -*-


import requests
import tornado.ioloop
import tornado.web


class MainHandler(tornado.web.RequestHandler):
    def get(self):
        """get请求"""
        type = self.get_argument('type')
        result = requests.get('https://api.github.com/repos/dromara/Jpom/releases/latest')
        json = result.json()
        tag_name = json['tag_name']
        if tag_name.strip() == '':
            self.write("没有发布版")
            return
        tag_name = tag_name.replace('v', '')
        print(tag_name)
        url = "https://jpom-releases.oss-cn-hangzhou.aliyuncs.com/" + type + "-" + tag_name + "-release.zip"
        print(url)
        self.redirect(url)


application = tornado.web.Application([(r"/", MainHandler), ])

if __name__ == "__main__":
    application.listen(8868)
    tornado.ioloop.IOLoop.instance().start()
