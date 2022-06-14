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
package io.jpom.socket;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.system.init.OperateLogController;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 代理socket 会话
 *
 * @author jiangzeyin
 * @since 2019/4/16
 */
@Slf4j
public class ProxySession extends WebSocketClient {
    private final WebSocketSession session;
    private final OperateLogController logController;

    /**
     * 等待连接成功
     */
    private void loopOpen() {
        int count = 0;
        while (!this.isOpen() && count < 20) {
            ThreadUtil.sleep(500);
            count++;
        }
    }

    public ProxySession(String uri, Integer timeOut, WebSocketSession session) throws URISyntaxException, InterruptedException {
        super(new URI(uri));
        //this(new URI(uri), session);
        Objects.requireNonNull(session);
        this.session = session;
        if (timeOut == null) {
            this.connect();
        } else {
            this.connectBlocking(Math.max(timeOut, 1), TimeUnit.SECONDS);
        }
        this.loopOpen();
        logController = SpringUtil.getBean(OperateLogController.class);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {

    }

    @Override
    public void onMessage(String message) {
        try {
            session.sendMessage(new TextMessage(message));
        } catch (IOException e) {
            log.error("发送消息失败", e);
        }
        try {
            JSONObject jsonObject = JSONObject.parseObject(message);
            String reqId = jsonObject.getString("reqId");
            if (StrUtil.isNotEmpty(reqId)) {
                logController.updateLog(reqId, message);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        try {
            session.close();
        } catch (IOException e) {
            log.error("关闭错误", e);
        }
    }

    @Override
    public void onError(Exception ex) {
        try {
            session.sendMessage(new TextMessage("agent服务端发生异常" + ExceptionUtil.stacktraceToString(ex)));
//            SocketSessionUtil.send(session, );
        } catch (IOException ignored) {
        }
        log.error("发生错误", ex);
    }

    @Override
    public void send(String text) {
        try {
            super.send(text);
        } catch (Exception e) {
            log.error("转发消息失败", e);
        }
    }
}
