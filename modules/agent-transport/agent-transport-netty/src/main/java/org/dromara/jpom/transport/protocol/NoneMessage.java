package org.dromara.jpom.transport.protocol;

import org.dromara.jpom.transport.netty.enums.MessageCmd;

import java.util.Map;
import java.util.UUID;

public class NoneMessage implements Message {

    private final String id;
    private final MessageCmd cmd;
    private final Map<String, String> header;
    private final byte[] content;

    public NoneMessage(Map<String, String> header, byte[] content) {
        this.id = header.getOrDefault("id", UUID.randomUUID().toString());
        this.cmd = MessageCmd.NONE;
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
        return content;
    }

}
