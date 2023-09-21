package org.dromara.jpom.common.event;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.commander.AbstractProjectCommander;
import org.dromara.jpom.transport.event.MessageEvent;
import org.dromara.jpom.transport.protocol.*;
import org.dromara.jpom.util.OshiUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MessageHandler implements ApplicationListener<MessageEvent<Message>> {

    @Override
    public void onApplicationEvent(MessageEvent<Message> event) {
        Message message = event.getMessage();
        if (message instanceof ProcessListMessage) {
            JSONObject jsonData = JSON.parseObject(((ProcessListMessage) message).text());
            String processName = jsonData.getString("processName");
            Integer count = jsonData.getInteger("count");
            processName = StrUtil.emptyToDefault(processName, "java");
            List<JSONObject> processes = OshiUtils.getProcesses(processName, Convert.toInt(count, 20));
            processes = processes.stream()
                    .peek(jsonObject -> {
                        int processId = jsonObject.getIntValue("processId");
                        String port = AbstractProjectCommander.getInstance().getMainPort(processId);
                        jsonObject.put("port", port);
                    })
                    .collect(Collectors.toList());
            Map<String, String> header = new HashMap<>();
            header.put("id", message.messageId());
            event.getChannelService().writeAndFlush(new ProcessListMessage(header, JSON.toJSONString(JsonMessage.success("ok", processes))));
        } else if (message instanceof DiskInfoMessage) {
            List<JSONObject> list = OshiUtils.fileStores();
            Map<String, String> header = new HashMap<>();
            header.put("id", message.messageId());
            event.getChannelService().writeAndFlush(new DiskInfoMessage(header, JSON.toJSONString(JsonMessage.success("", list))));
        } else if (message instanceof HwDiskInfoMessage) {
            Map<String, String> header = new HashMap<>();
            header.put("id", message.messageId());
            List<JSONObject> list = OshiUtils.diskStores();
            event.getChannelService().writeAndFlush(new HwDiskInfoMessage(header, JSON.toJSONString(JsonMessage.success("", list))));
        } else if (message instanceof NetworkInterfacesMessage) {
            Map<String, String> header = new HashMap<>();
            header.put("id", message.messageId());
            List<JSONObject> list = OshiUtils.networkInterfaces();
            event.getChannelService().writeAndFlush(new NetworkInterfacesMessage(header, JSON.toJSONString(JsonMessage.success("", list))));
        }
    }

}
