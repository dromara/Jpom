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
package org.dromara.jpom.common.event;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.func.assets.server.MachineNodeServer;
import org.dromara.jpom.transport.event.MessageEvent;
import org.dromara.jpom.transport.protocol.Message;
import org.dromara.jpom.transport.protocol.PushStatInfoMessage;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MessageHandler implements ApplicationListener<MessageEvent<Message>> {

    @Resource
    private MachineNodeServer machineNodeServer;
    private final Map<String, Long> lastUpdateTime = new HashMap<>();

    @Override
    public void onApplicationEvent(MessageEvent<Message> event) {
        Message message = event.getMessage();
        if (message instanceof PushStatInfoMessage) {
            JSONObject jsonObject = JSON.parseObject(((PushStatInfoMessage) message).text());
            String installId = jsonObject.getString("installId");
            MachineNodeModel machineNodeModel = new MachineNodeModel();
            machineNodeModel.setInstallId(installId);
            machineNodeModel = machineNodeServer.queryByBean(machineNodeModel);
            if (machineNodeModel != null) {
                lastUpdateTime.put(installId, System.currentTimeMillis());
                machineNodeServer.saveStatInfo(machineNodeModel, jsonObject);
                return;
            }
            //TODO 是否新增机器
        }
    }

    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
    public void timeoutHandler() {
        for (Map.Entry<String, Long> entry : lastUpdateTime.entrySet()) {
            if (System.currentTimeMillis() - entry.getValue() >= 30) {
                MachineNodeModel machineNodeModel = new MachineNodeModel();
                machineNodeModel.setInstallId(entry.getKey());
                machineNodeModel = machineNodeServer.queryByBean(machineNodeModel);
                if (machineNodeModel != null) {
                    machineNodeServer.updateStatus(machineNodeModel, 3, "超时未收到推送");
                }
            }
        }
    }

}
