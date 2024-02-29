/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.monitor;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.model.data.MonitorModel;
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
