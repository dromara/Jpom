/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.transport;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

/**
 * @author bwcx_jzy
 * @since 2022/12/26
 */
public interface IProxyWebSocket extends AutoCloseable {

    /**
     * 关闭连接
     *
     * @throws IOException 关闭异常
     */
    void close() throws IOException;

    /**
     * 打开连接,默认停留一秒
     *
     * @return 打开状态
     */
    boolean connect();

    /**
     * 重新打开连接
     *
     * @return 打开状态
     * @throws IOException 关闭异常
     */
    default boolean reconnect() throws IOException {
        this.close();
        return this.connect();
    }

    /**
     * 重新打开连接
     *
     * @return 打开状态
     * @throws IOException 关闭异常
     */
    default boolean reconnectBlocking() throws IOException {
        this.close();
        return this.connectBlocking();
    }

    /**
     * 打开连接，使用节点配置的超时时间
     *
     * @return 打开状态
     */
    boolean connectBlocking();

    /**
     * 打开连接，阻塞指定时间
     *
     * @param seconds 阻塞时间  建议大于 1秒
     * @return 打开状态
     */
    boolean connectBlocking(int seconds);

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

    /**
     * 获取关闭状态描述
     *
     * @return 状态描述
     */
    String getCloseStatusMsg();
}
