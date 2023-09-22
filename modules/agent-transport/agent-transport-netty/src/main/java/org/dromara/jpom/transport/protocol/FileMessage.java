package org.dromara.jpom.transport.protocol;

import org.dromara.jpom.transport.netty.enums.MessageCmd;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

/**
 * 文文件字节流消息
 * @author Hong
 * @since 2023/09/22
**/
public class FileMessage implements Message {

    private final String id;

    private final MessageCmd cmd;
    private final Map<String, String> header;
    private final byte[] content;

    public FileMessage(Map<String, String> header, byte[] content) {
        this.id = header.getOrDefault("id", UUID.randomUUID().toString());
        this.cmd = MessageCmd.FILE;
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

    public InputStream getInputStream() throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(this.content)) {
            return inputStream;
        }
    }
}
