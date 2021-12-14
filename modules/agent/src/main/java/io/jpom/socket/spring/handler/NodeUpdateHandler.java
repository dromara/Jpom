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
//package io.jpom.socket.spring.handler;
//
//import cn.jiangzeyin.common.DefaultSystemLog;
//import com.alibaba.fastjson.JSONObject;
//import io.jpom.JpomApplication;
//import io.jpom.common.JpomManifest;
//import io.jpom.model.AgentFileModel;
//import io.jpom.model.WebSocketMessageModel;
//import io.jpom.model.data.UploadFileModel;
//import io.jpom.system.AgentConfigBean;
//import org.springframework.web.socket.BinaryMessage;
//import org.springframework.web.socket.TextMessage;
//import org.springframework.web.socket.WebSocketSession;
//import org.springframework.web.socket.handler.AbstractWebSocketHandler;
//
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * 节点升级websocket处理器
// *
// * @author lf
// */
//public class NodeUpdateHandler extends AbstractWebSocketHandler {
//    private static final Map<String, UploadFileModel> UPLOAD_FILE_INFO = new HashMap<>();
//
//    @Override
//    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
//        // 设置二进制消息的最大长度为1M
//        session.setBinaryMessageSizeLimit(1024 * 1024);
//    }
//
//    @Override
//    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
//
//    }
//
//    @Override
//    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
//        UploadFileModel uploadFileModel = UPLOAD_FILE_INFO.get(session.getId());
//        uploadFileModel.save(message.getPayload().array());
//        // 更新进度
//        WebSocketMessageModel model = new WebSocketMessageModel("updateNode", uploadFileModel.getId());
//        model.setData(uploadFileModel);
//        session.sendMessage(new TextMessage(model.toString()));
//    }
//
//    /**
//     * 重启
//     *
//     * @param session
//     * @return
//     */
//    public String restart(WebSocketSession session) {
//        String result = "重启中";
//        try {
//            UploadFileModel uploadFile = UPLOAD_FILE_INFO.get(session.getId());
//            JpomManifest.releaseJar(uploadFile.getFilePath(), uploadFile.getVersion(), true);
//            JpomApplication.restart();
//        } catch (RuntimeException e) {
//            result = e.getMessage();
//            DefaultSystemLog.getLog().error("重启失败", e);
//        }
//        return result;
//    }
//}
