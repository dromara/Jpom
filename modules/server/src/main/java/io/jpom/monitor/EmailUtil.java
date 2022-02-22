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

import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.model.data.MailAccountModel;
import io.jpom.model.data.MonitorModel;
import io.jpom.plugin.IPlugin;
import io.jpom.plugin.PluginFactory;
import io.jpom.service.system.SystemParametersServer;

import java.util.HashMap;
import java.util.Map;

/**
 * 邮件工具
 *
 * @author Arno
 */
public class EmailUtil implements INotify {

    private static SystemParametersServer systemParametersServer;
    private static MailAccountModel config;

    @Override
    public void send(MonitorModel.Notify notify, String title, String context) throws Exception {
        String value = notify.getValue();
        EmailUtil.send(value, title, context);
    }

    private static void init() {
        if (systemParametersServer == null) {
            systemParametersServer = SpringUtil.getBean(SystemParametersServer.class);
        }
    }

    /**
     * 加载配置信息
     */
    public static void refreshConfig() {
        if (config == null) {
            init();
        }
        config = systemParametersServer.getConfig(MailAccountModel.ID, MailAccountModel.class);
    }


    /**
     * 发送邮箱
     *
     * @param email   收件人
     * @param title   标题
     * @param context 内容
     */
    public static void send(String email, String title, String context) throws Exception {
        if (config == null) {
            // 没有数据才加载
            refreshConfig();
        }
        //
        Map<String, Object> mailMap = new HashMap<>(10);
        mailMap.put("toEmail", email);
        mailMap.put("title", title);
        mailMap.put("context", context);
        //
        IPlugin plugin = PluginFactory.getPlugin("email");
        plugin.execute(JSONObject.toJSON(config), mailMap);
    }
}
