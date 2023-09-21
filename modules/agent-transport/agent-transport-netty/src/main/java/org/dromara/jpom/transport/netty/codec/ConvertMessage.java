package org.dromara.jpom.transport.netty.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.jpom.transport.netty.enums.MessageCmd;
import org.dromara.jpom.transport.protocol.*;
import org.dromara.jpom.transport.protocol.extend.RegisterDeviceImpl;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 解析到的数据转成Message对象
 *
 * @author Hong
 * @since 2023/08/22
 */
public class ConvertMessage {

    private static final ObjectMapper json = new ObjectMapper();

    public static Message convert(int cmd, Map<String, String> headerMap, byte[] content) throws JsonProcessingException {
        MessageCmd messageCmd = MessageCmd.getCmd(cmd);
        if (messageCmd == MessageCmd.HEARTBEAT) {
            return new HeartbeatMessage();
        } else if (messageCmd == MessageCmd.REGISTER) {
            return new RegisterMessage(headerMap, json.readValue(new String(content, StandardCharsets.UTF_8), RegisterDeviceImpl.class));
        } else if (messageCmd == MessageCmd.TEXT) {
            return new TextMessage(headerMap, new String(content, StandardCharsets.UTF_8));
        } else if (messageCmd == MessageCmd.FILE) {
            return new FileMessage(headerMap, content);
        } else if (messageCmd == MessageCmd.AGENT_STAT_INFO) {
            return new PushStatInfoMessage(headerMap, new String(content, StandardCharsets.UTF_8));
        } else if (messageCmd == MessageCmd.PROCESS_LIST) {
            return new ProcessListMessage(headerMap, new String(content, StandardCharsets.UTF_8));
        } else if (messageCmd == MessageCmd.DISK_INFO) {
            return new DiskInfoMessage(headerMap, new String(content, StandardCharsets.UTF_8));
        } else if (messageCmd == MessageCmd.HW_DISK_INFO) {
            return new HwDiskInfoMessage(headerMap, new String(content, StandardCharsets.UTF_8));
        } else if (messageCmd == MessageCmd.NETWORK_INTERFACES) {
            return new NetworkInterfacesMessage(headerMap, new String(content, StandardCharsets.UTF_8));
        }
        return new NoneMessage(headerMap, content);
    }
}
