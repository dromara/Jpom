/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.socket;

import cn.hutool.core.lang.Tuple;
import cn.keepbx.jpom.Type;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.websocket.Constants;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.JpomManifest;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.configuration.AgentConfig;
import org.dromara.jpom.model.AgentFileModel;
import org.dromara.jpom.model.UploadFileModel;
import org.dromara.jpom.model.WebSocketMessageModel;
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
        try {
            setLanguage(session);
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
        } finally {
            clearLanguage();
        }
    }


    @OnMessage
    public void onMessage(String message, Session session) throws Exception {
        try {
            setLanguage(session);
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
                    log.warn(I18nMessageUtil.get("i18n.ignored_operation.edee"), message);
                    break;
            }
            SocketSessionUtil.send(session, model.toString());
            //session.sendMessage(new TextMessage(model.toString()));
        } finally {
            clearLanguage();
        }
    }

    /**
     * @param message byte 消息
     * @param session 会话
     * @throws Exception 异常
     * @see Constants#DEFAULT_BUFFER_SIZE
     */
    @OnMessage(maxMessageSize = 5 * 1024 * 1024)
    public void onMessage(byte[] message, Session session) throws Exception {
        try {
            setLanguage(session);
            UploadFileModel uploadFileModel = UPLOAD_FILE_INFO.get(session.getId());
            uploadFileModel.save(message);
            // 更新进度
            WebSocketMessageModel model = new WebSocketMessageModel("updateNode", uploadFileModel.getId());
            model.setData(uploadFileModel);
            SocketSessionUtil.send(session, model.toString());
            //		session.sendMessage(new TextMessage(model.toString()));
        } finally {
            clearLanguage();
        }
    }

    /**
     * 重启
     *
     * @param session 回话
     * @return 结果
     */
    public String restart(Session session) {
        String result = Const.UPGRADE_MSG.get();
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
            result = I18nMessageUtil.get("i18n.restart_failed.f92a") + e.getMessage();
            log.error(I18nMessageUtil.get("i18n.restart_failed.f92a"), e);
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
