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

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.script.ScriptExecuteLogModel;
import org.dromara.jpom.model.script.ScriptModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.service.script.ScriptExecuteLogServer;
import org.dromara.jpom.service.script.ScriptServer;
import org.dromara.jpom.socket.BaseProxyHandler;
import org.dromara.jpom.socket.ConsoleCommandOp;
import org.dromara.jpom.socket.ServerScriptProcessBuilder;
import org.dromara.jpom.util.SocketSessionUtil;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;

/**
 * 服务端脚本日志
 *
 * @author bwcx_jzy
 * @since 2022/1/19
 */
@Feature(cls = ClassFeature.SCRIPT, method = MethodFeature.EXECUTE)
public class ServerScriptHandler extends BaseProxyHandler {

    private ScriptExecuteLogServer logServer;
    private ScriptServer nodeScriptServer;

    @Override
    protected void init(WebSocketSession session, Map<String, Object> attributes) throws Exception {
        super.init(session, attributes);
        //
        this.logServer = SpringUtil.getBean(ScriptExecuteLogServer.class);
        this.nodeScriptServer = SpringUtil.getBean(ScriptServer.class);
        ScriptModel scriptModel = (ScriptModel) attributes.get("dataItem");
        this.sendMsg(session, I18nMessageUtil.get("i18n.connection_successful_with_message.5cf2") + scriptModel.getName());
    }

    public ServerScriptHandler() {
        super(null);
    }

    @Override
    protected Object[] getParameters(Map<String, Object> attributes) {
        return new Object[0];
    }

    @Override
    protected String handleTextMessage(Map<String, Object> attributes, WebSocketSession session, JSONObject json, ConsoleCommandOp consoleCommandOp) throws IOException {
        ScriptModel scriptModel = (ScriptModel) attributes.get("dataItem");
        if (consoleCommandOp == ConsoleCommandOp.heart) {
            return null;
        }
        super.logOpt(this.getClass(), attributes, json);
        switch (consoleCommandOp) {
            case start: {

                String args = json.getString("args");
                String executeId = this.createLog(attributes, scriptModel);
                json.put(Const.SOCKET_MSG_TAG, Const.SOCKET_MSG_TAG);
                json.put("executeId", executeId);
                ServerScriptProcessBuilder.addWatcher(scriptModel, executeId, args, session);
                JsonMessage<String> jsonMessage = new JsonMessage<>(200, I18nMessageUtil.get("i18n.start_execution.00d7"));
                JSONObject jsonObject = jsonMessage.toJson();
                jsonObject.putAll(json);
                this.sendMsg(session, jsonObject.toString());
                break;
            }
            case stop: {
                String executeId = json.getString("executeId");
                if (StrUtil.isEmpty(executeId)) {
                    SocketSessionUtil.send(session, I18nMessageUtil.get("i18n.no_execution_id.68dc"));
                    session.close();
                    return null;
                }
                ServerScriptProcessBuilder.stopRun(executeId);
                break;
            }
            default:
                return null;
        }
        return null;
    }

    /**
     * 创建执行日志
     *
     * @param attributes 参数属性
     * @return 执行ID
     */
    private String createLog(Map<String, Object> attributes, ScriptModel scriptModel) {
        UserModel userModel = (UserModel) attributes.get("userInfo");

        //
        try {
            BaseServerController.resetInfo(userModel);
            //
            ScriptModel scriptCacheModel = new ScriptModel();
            scriptCacheModel.setId(scriptModel.getId());
            scriptCacheModel.setLastRunUser(userModel.getId());
            nodeScriptServer.updateById(scriptCacheModel);
            //
            ScriptExecuteLogModel scriptExecuteLogCacheModel = logServer.create(scriptModel, 0);
            return scriptExecuteLogCacheModel.getId();
        } finally {
            BaseServerController.removeAll();
        }
    }


    @Override
    public void destroy(WebSocketSession session) {
        //
        super.destroy(session);
        ServerScriptProcessBuilder.stopWatcher(session);
    }
}
