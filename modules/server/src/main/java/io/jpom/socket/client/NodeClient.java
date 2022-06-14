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
package io.jpom.socket.client;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.model.WebSocketMessageModel;
import io.jpom.model.data.NodeModel;
import io.jpom.system.init.OperateLogController;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * 节点Client
 *
 * @author lf
 */
@Slf4j
public class NodeClient extends WebSocketClient {
    private final WebSocketSession session;
    private final OperateLogController logController;
    private final NodeModel nodeModel;


    public NodeClient(String uri, NodeModel nodeModel, WebSocketSession session) throws URISyntaxException, InterruptedException {
        super(new URI(uri));
        // 添加 http proxy
        Proxy proxy = nodeModel.proxy();
        if (proxy != null) {
            setProxy(proxy);
        }
        this.session = session;
        this.nodeModel = nodeModel;
        //
        Integer timeOut = nodeModel.getTimeOut();
        if (timeOut == null) {
            this.connect();
        } else {
            this.connectBlocking(timeOut, TimeUnit.SECONDS);
        }
        this.loopOpen();
        logController = SpringUtil.getBean(OperateLogController.class);
    }

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

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        // 连接成功后获取版本信息
        getVersion();
    }

    public void getVersion() {
        WebSocketMessageModel command = new WebSocketMessageModel("getVersion", this.nodeModel.getId());
        send(command.toString());
    }

    @Override
    public void onMessage(String message) {
        try {
            // 不能并发向同一个客户端发送消息 @author jzy 2021-08-03
            synchronized (session.getId()) {
                session.sendMessage(new TextMessage(message));
            }
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

    }

    @Override
    public void send(String text) {
        super.send(text);
    }


    @Override
    public void close() {
        try {
            super.close();
        } catch (Exception e) {
            log.error("关闭异常", e);
        }
    }

    @Override
    public void send(ByteBuffer bytes) {
        super.send(bytes);
    }

    @Override
    public void onError(Exception e) {
        log.error("发生异常", e);
    }
}
