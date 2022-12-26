package top.jpom.transport;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

/**
 * @author bwcx_jzy
 * @since 2022/12/26
 */
public interface IProxyWebSocket {

    /**
     * 关闭连接
     *
     * @throws IOException 关闭异常
     */
    void close() throws IOException;

    /**
     * 打开连接
     *
     * @return 打开状态
     */
    boolean open();

    /**
     * 重新打开连接
     *
     * @return 打开状态
     * @throws IOException 关闭异常
     */
    default boolean reopen() throws IOException {
        this.close();
        return this.open();
    }

    /**
     * 发送消息
     *
     * @param msg 消息
     * @throws IOException 发送异常
     */
    void send(String msg) throws IOException;

    /**
     * 发送消息
     *
     * @param bytes 消息
     * @throws IOException 发送异常
     */
    void send(ByteBuffer bytes) throws IOException;

    /**
     * 收到消息
     *
     * @param consumer 回调
     */
    void onMessage(Consumer<String> consumer);

    /**
     * 是否连接上
     *
     * @return true
     */
    boolean isConnected();
}
