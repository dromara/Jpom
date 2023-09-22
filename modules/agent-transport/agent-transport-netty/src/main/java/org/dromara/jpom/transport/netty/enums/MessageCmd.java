package org.dromara.jpom.transport.netty.enums;

/**
 * Message支持的消息类型
 *
 * @author Hong
 * @since 2023/08/22
 */
public enum MessageCmd {
    /**
     * 未知消息
     */
    NONE(0, "none"),
    /**
     * 心跳消息
     */
    HEARTBEAT(1, "heartbeat"),
    /**
     * 注册消息
     */
    REGISTER(2, "register"),
    /**
     * 文本消息
     */
    TEXT(3, "text"),
    /**
     * 文件二进制消息
     */
    FILE(4, "file"),
    /**
     * 机器基础信息、统计趋势
     */
    AGENT_STAT_INFO(5, "agent_stat_info"),
    /**
     * 进程消息
     */
    PROCESS_LIST(6, "process_list"),
    /**
     * 文件消息
     */
    DISK_INFO(7, "disk_info"),
    /**
     * 硬件硬盘消息
     */
    HW_DISK_INFO(8, "hw_disk_info"),
    /**
     * 网络消息
     */
    NETWORK_INTERFACES(9, "network_interfaces");

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
