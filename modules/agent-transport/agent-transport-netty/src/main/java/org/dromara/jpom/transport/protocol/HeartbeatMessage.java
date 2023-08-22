package org.dromara.jpom.transport.protocol;

import org.dromara.jpom.transport.netty.enums.MessageCmd;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HeartbeatMessage implements Message {

    private final String id;
    private final MessageCmd cmd;
    private final Map<String, String> header;
    private final byte[] content;

    public HeartbeatMessage() {
        this.id = UUID.randomUUID().toString();
        this.cmd = MessageCmd.HEARTBEAT;
        this.header = new HashMap<>(0);
        this.content = new byte[0];
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
        return content;
    }

}
