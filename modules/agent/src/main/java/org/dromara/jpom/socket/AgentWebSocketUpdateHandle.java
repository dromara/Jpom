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
package org.dromara.jpom.socket;

import cn.hutool.core.lang.Tuple;
import cn.keepbx.jpom.Type;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.JpomManifest;
import org.dromara.jpom.model.AgentFileModel;
import org.dromara.jpom.model.UploadFileModel;
import org.dromara.jpom.model.WebSocketMessageModel;
import org.dromara.jpom.configuration.AgentConfig;
import org.dromara.jpom.util.SocketSessionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 在线升级
 *
 * @author bwcx_jzy
 * @since 2021/8/3
 */
@ServerEndpoint(value = "/node_update")
@Component
@Slf4j
public class AgentWebSocketUpdateHandle extends BaseAgentWebSocketHandle {

    private static final Map<String, UploadFileModel> UPLOAD_FILE_INFO = new HashMap<>();

    private static AgentConfig agentConfig;
    private static MultipartProperties multipartProperties;

    @Autowired
    public void init(AgentConfig agentConfig, MultipartProperties multipartProperties) {
        AgentWebSocketUpdateHandle.agentConfig = agentConfig;
        AgentWebSocketUpdateHandle.multipartProperties = multipartProperties;
        setAgentAuthorize(agentConfig.getAuthorize());
    }

    @OnOpen
    public void onOpen(Session session) {
        if (super.checkAuthorize(session)) {
            return;
        }
        DataSize maxRequestSize = multipartProperties.getMaxRequestSize();
        int max = Optional.ofNullable(maxRequestSize)
            .map(dataSize -> {
                // 最大 10MB
                long value = Math.min(dataSize.toBytes(), DataSize.ofMegabytes(10).toBytes());
                // 最后转换，不然可能出现 0
                int valueInt = (int) value;
                return valueInt > 0 ? valueInt : null;
            })
            .orElseGet(() -> (int) DataSize.ofMegabytes(10).toBytes());

        session.setMaxBinaryMessageBufferSize(max);
        //
    }


    @OnMessage
    public void onMessage(String message, Session session) throws Exception {
        WebSocketMessageModel model = WebSocketMessageModel.getInstance(message);
        switch (model.getCommand()) {
            case "getVersion":
                model.setData(JSONObject.toJSONString(JpomManifest.getInstance()));
                break;
            case "upload":
                AgentFileModel agentFileModel = ((JSONObject) model.getParams()).toJavaObject(AgentFileModel.class);
                UploadFileModel uploadFileModel = new UploadFileModel();
                uploadFileModel.setId(model.getNodeId());
                uploadFileModel.setName(agentFileModel.getName());
                uploadFileModel.setSize(agentFileModel.getSize());
                uploadFileModel.setVersion(agentFileModel.getVersion());
                uploadFileModel.setSavePath(agentConfig.getTempPath().getAbsolutePath());
                uploadFileModel.remove();
                UPLOAD_FILE_INFO.put(session.getId(), uploadFileModel);
                break;
            case "restart":
                model.setData(restart(session));
                break;
            case "heart":
                break;
            default:
                log.warn("忽略的操作：{}", message);
                break;
        }
        SocketSessionUtil.send(session, model.toString());
        //session.sendMessage(new TextMessage(model.toString()));
    }

    @OnMessage
    public void onMessage(byte[] message, Session session) throws Exception {
        UploadFileModel uploadFileModel = UPLOAD_FILE_INFO.get(session.getId());
        uploadFileModel.save(message);
        // 更新进度
        WebSocketMessageModel model = new WebSocketMessageModel("updateNode", uploadFileModel.getId());
        model.setData(uploadFileModel);
        SocketSessionUtil.send(session, model.toString());
//		session.sendMessage(new TextMessage(model.toString()));
    }

    /**
     * 重启
     *
     * @param session 回话
     * @return 结果
     */
    public String restart(Session session) {
        String result = Const.UPGRADE_MSG;
        try {
            UploadFileModel uploadFile = UPLOAD_FILE_INFO.get(session.getId());
            String filePath = uploadFile.getFilePath();
            JsonMessage<Tuple> error = JpomManifest.checkJpomJar(filePath, Type.Agent);
            if (!error.success()) {
                return error.getMsg();
            }
            JpomManifest.releaseJar(filePath, uploadFile.getVersion());
            JpomApplication.restart();
        } catch (Exception e) {
            result = "重启失败" + e.getMessage();
            log.error("重启失败", e);
        }
        return result;
    }

    @Override
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        super.onClose(session, closeReason);
        UPLOAD_FILE_INFO.remove(session.getId());
    }

    @OnError
    @Override
    public void onError(Session session, Throwable thr) {
        super.onError(session, thr);
    }
}
