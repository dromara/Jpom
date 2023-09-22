package org.dromara.jpom.transport.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.jpom.transport.netty.enums.MessageCmd;
import org.dromara.jpom.transport.protocol.extend.RegisterDevice;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * 注册消息
 * @author Hong
 * @since 2023/09/22
**/
public class RegisterMessage implements Message, RegisterDevice {

    private static final ObjectMapper json = new ObjectMapper();

    private final String id;
    private final MessageCmd cmd;
    private final Map<String, String> header;
    private final RegisterDevice content;

    public RegisterMessage(Map<String, String> header, RegisterDevice content) {
        this.id = header.getOrDefault("id", UUID.randomUUID().toString());
        this.cmd = MessageCmd.REGISTER;
        this.header = header;
        this.content = content;
    }

    @Override
    public String messageId() {
        return id;
    }

    @Override
    public MessageCmd cmd() {
        return cmd;
    }

    @Override
    public Map<String, String> header() {
        return header;
    }

    @Override
    public byte[] content() {
        try {
            return json.writeValueAsString(content).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getName() {
        return content.getName();
    }

    @Override
    public String getVersion() {
        return content.getVersion();
    }

    @Override
    public String getHost() {
        return content.getHost();
    }

    @Override
    public int getPort() {
        return content.getPort();
    }
}
