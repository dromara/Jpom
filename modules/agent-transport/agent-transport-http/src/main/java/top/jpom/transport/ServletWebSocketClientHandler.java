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

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.unit.DataSize;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author bwcx_jzy
 * @since 2022/12/26
 */
@Slf4j
public class ServletWebSocketClientHandler extends AbstractWebSocketHandler implements IProxyWebSocket {

    private static final StandardWebSocketClient CLIENT = new StandardWebSocketClient();

    private WebSocketSession session;
    private final Integer timeout;
    private final String uriTemplate;
    private Consumer<String> consumerText;
    private WebSocketConnectionManager manager;

    public ServletWebSocketClientHandler(String uriTemplate, Integer timeout) {
        this.uriTemplate = uriTemplate;
        this.timeout = timeout;
    }

    @Override
    public void onMessage(Consumer<String> consumer) {
        this.consumerText = consumer;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Optional.ofNullable(this.consumerText).ifPresent(consumer -> consumer.accept(message.getPayload()));
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 发送消息时间限制 60 秒
        // 缓存 5m 消息
        this.session = new ConcurrentWebSocketSessionDecorator(session, 60 * 1000, (int) DataSize.ofMegabytes(5).toBytes());
    }

    @Override
    public void close() throws IOException {
        if (this.manager == null) {
            return;
        }
        this.manager.stop();
        this.manager = null;
        this.session = null;
    }

    @Override
    public boolean connect() {
        Assert.isNull(this.manager, "The connection has been established, do not repeat the connection");
        this.manager = new WebSocketConnectionManager(CLIENT, this, this.uriTemplate);
        this.manager.start();
        return this.blocking(1);
    }

    @Override
    public boolean connectBlocking() {
        int maxTimeout = Optional.ofNullable(this.timeout).orElse(5 * 60);
        return this.connectBlocking(maxTimeout);
    }

    @Override
    public boolean connectBlocking(int seconds) {
        if (this.connect()) {
            return true;
        }
        return this.blocking(seconds);
    }

    private boolean blocking(int seconds) {
        int waitTime = 0;
        do {
            if (this.isConnected()) {
                return true;
            }
            waitTime++;
            ThreadUtil.sleep(500, TimeUnit.MILLISECONDS);
        } while (waitTime * 2 <= seconds);
        return false;
    }

    @Override
    public void send(String msg) throws IOException {
        Assert.notNull(this.session, "还没有连接上");
        session.sendMessage(new TextMessage(msg));
    }

    @Override
    public void send(ByteBuffer bytes) throws IOException {
        Assert.notNull(this.session, "还没有连接上");
        session.sendMessage(new BinaryMessage(bytes));
    }

    @Override
    public boolean isConnected() {
        if (this.manager == null) {
            return false;
        }
        return this.manager.isConnected();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("websocket 出现错误：{}", session.getId(), exception);
    }
}
