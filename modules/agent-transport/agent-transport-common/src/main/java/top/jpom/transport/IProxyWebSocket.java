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
