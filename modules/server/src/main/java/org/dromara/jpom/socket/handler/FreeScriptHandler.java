/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.socket.handler;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.socket.BaseProxyHandler;
import org.dromara.jpom.socket.ConsoleCommandOp;
import org.dromara.jpom.system.ExtConfigBean;
import org.dromara.jpom.transport.IProxyWebSocket;
import org.dromara.jpom.util.CommandUtil;
import org.dromara.jpom.util.SocketSessionUtil;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * @author bwcx_jzy1
 * @since 2024/4/26
 */
@Feature(cls = ClassFeature.FREE_SCRIPT, method = MethodFeature.EXECUTE)
@Slf4j
public class FreeScriptHandler extends BaseProxyHandler {

    public FreeScriptHandler() {
        super(NodeUrl.FreeScriptRun);
    }

    @Override
    protected Object[] getParameters(Map<String, Object> attributes) {
        return new Object[]{};
    }

    @Override
    protected String handleTextMessage(Map<String, Object> attributes, WebSocketSession session, IProxyWebSocket proxySession, JSONObject json, ConsoleCommandOp consoleCommandOp) throws IOException {

        String content = json.getString("content");
        if (StrUtil.isEmpty(content)) {
            SocketSessionUtil.send(session, I18nMessageUtil.get("i18n.no_content_to_execute.66aa"));
            session.close();
            return null;
        }

        MachineNodeModel machine = (MachineNodeModel) attributes.get("machine");
        String osName = machine.getOsName();
        String template = StrUtil.EMPTY;
        boolean appendTemplate = json.getBooleanValue("appendTemplate");
        if (appendTemplate && StrUtil.isNotEmpty(osName)) {
            InputStream templateInputStream;
            if (osName.startsWith("Windows")) {
                templateInputStream = ExtConfigBean.getConfigResourceInputStream("/exec/template." + CommandUtil.SUFFIX_WINDOWS);
            } else {
                templateInputStream = ExtConfigBean.getConfigResourceInputStream("/exec/template." + CommandUtil.SUFFIX_UNIX);
            }
            template = IoUtil.readUtf8(templateInputStream);
        }

        String uuid = IdUtil.fastSimpleUUID();
        json.put("tag", uuid);
        json.put("content", template + content);
        String path = json.getString("path");
        json.put("path", StrUtil.emptyToDefault(path, "./"));
        json.put("environment", new JSONObject());
        attributes.put("uuidTag", uuid);
        proxySession.send(json.toString());
        return null;
    }

    @Override
    protected void onProxyMessage(WebSocketSession session, String msg) {
        if (StrUtil.equals(msg, "JPOM_SYSTEM_TAG:" + session.getAttributes().get("uuidTag"))) {
            // 执行结束
            try {
                session.close();
            } catch (IOException e) {
                log.error(I18nMessageUtil.get("i18n.close_client_session_exception.530a"), e);
            }
            return;
        }
        super.onProxyMessage(session, msg);
    }
}
