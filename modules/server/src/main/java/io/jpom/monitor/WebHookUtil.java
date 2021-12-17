/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.monitor;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.model.data.MonitorModel;
import org.springframework.http.MediaType;

/**
 * 钉钉工具
 *
 * @author Arno
 */
public class WebHookUtil implements INotify {

    /**
     * 发送钉钉群自定义机器人消息
     *
     * @param notify  通知对象
     * @param title   描述标签
     * @param context 消息内容
     */
    @Override
    public void send(MonitorModel.Notify notify, String title, String context) {
        JSONObject text = new JSONObject();
        JSONObject param = new JSONObject();
        //消息内容
        text.put("content", title + "\n" + context);
        param.put("msgtype", "text");
        param.put("text", text);
        HttpRequest request = HttpUtil.
                createPost(notify.getValue()).
                contentType(MediaType.APPLICATION_JSON_VALUE).
                body(param.toJSONString());
        request.execute();
    }
}
