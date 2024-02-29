/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.socket.handler;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.socket.BaseProxyHandler;
import org.dromara.jpom.socket.ConsoleCommandOp;
import org.dromara.jpom.transport.IProxyWebSocket;

import java.io.IOException;
import java.util.Map;

/**
 * 插件端系统日志消息控制器
 *
 * @author bwcx_jzy
 * @since 2023/12/26
 */
@Feature(cls = ClassFeature.AGENT_LOG, method = MethodFeature.EXECUTE)
@Slf4j
public class AgentLogHandler extends BaseProxyHandler {

    public AgentLogHandler() {
        super(NodeUrl.Socket_SystemLog);
    }

    @Override
    protected Object[] getParameters(Map<String, Object> attributes) {
        return new Object[]{};
    }

    @Override
    protected String handleTextMessage(Map<String, Object> attributes, IProxyWebSocket proxySession, JSONObject json, ConsoleCommandOp consoleCommandOp) throws IOException {
        proxySession.send(json.toString());
        return null;
    }
}
