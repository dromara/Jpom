package org.dromara.jpom.transport.netty.enums;

/**
 * Message支持的消息类型
 *
 * @author Hong
 * @since 2023/08/22
 */
public enum MessageCmd {
    NONE(0, "none"),
    HEARTBEAT(1, "heartbeat"),
    REGISTER(2, "register"),
    TEXT(3, "text"),
    FILE(4, "file");

    private final int cmd;

    private final String text;

    MessageCmd(int cmd, String text) {
        this.cmd = cmd;
        this.text = text;
    }

    public int getCmd() {
        return cmd;
    }

    public String getText() {
        return text;
    }

    public static MessageCmd getCmd(int cmd) {
        for (MessageCmd value : MessageCmd.values()) {
            if (value.cmd == cmd) {
                return value;
            }
        }
        return NONE;
    }
}
