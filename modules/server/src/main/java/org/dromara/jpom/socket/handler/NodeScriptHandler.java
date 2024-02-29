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

import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.node.NodeScriptCacheModel;
import org.dromara.jpom.model.node.NodeScriptExecuteLogCacheModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.service.node.script.NodeScriptExecuteLogServer;
import org.dromara.jpom.service.node.script.NodeScriptServer;
import org.dromara.jpom.socket.BaseProxyHandler;
import org.dromara.jpom.socket.ConsoleCommandOp;
import org.dromara.jpom.transport.IProxyWebSocket;

import java.io.IOException;
import java.util.Map;

/**
 * 脚本模板消息控制器
 *
 * @author bwcx_jzy
 * @since 2019/4/24
 */
@Feature(cls = ClassFeature.NODE_SCRIPT, method = MethodFeature.EXECUTE)
public class NodeScriptHandler extends BaseProxyHandler {

    public NodeScriptHandler() {
        super(NodeUrl.Script_Run);
    }

    @Override
    protected Object[] getParameters(Map<String, Object> attributes) {
        NodeScriptCacheModel scriptModel = (NodeScriptCacheModel) attributes.get("dataItem");
        return new Object[]{"id", attributes.get("scriptId"), "workspaceId", scriptModel.getWorkspaceId()};
    }

    @Override
    protected String handleTextMessage(Map<String, Object> attributes, IProxyWebSocket proxySession, JSONObject json, ConsoleCommandOp consoleCommandOp) throws IOException {
        if (consoleCommandOp != ConsoleCommandOp.heart) {
            super.logOpt(this.getClass(), attributes, json);
        }
        if (consoleCommandOp == ConsoleCommandOp.start) {
            // 开始执行
            String executeId = this.createLog(attributes);
            json.put(Const.SOCKET_MSG_TAG, Const.SOCKET_MSG_TAG);
            json.put("executeId", executeId);
        }
        proxySession.send(json.toString());
        return null;
    }

    /**
     * 创建执行日志
     *
     * @param attributes 参数属性
     * @return 执行ID
     */
    private String createLog(Map<String, Object> attributes) {
        NodeModel nodeInfo = (NodeModel) attributes.get("nodeInfo");
        UserModel userModel = (UserModel) attributes.get("userInfo");
        NodeScriptCacheModel dataItem = (NodeScriptCacheModel) attributes.get("dataItem");
        NodeScriptExecuteLogServer logServer = SpringUtil.getBean(NodeScriptExecuteLogServer.class);
        NodeScriptServer nodeScriptServer = SpringUtil.getBean(NodeScriptServer.class);
        //
        try {
            BaseServerController.resetInfo(userModel);
            //
            NodeScriptCacheModel nodeScriptCacheModel = new NodeScriptCacheModel();
            nodeScriptCacheModel.setId(dataItem.getId());
            nodeScriptCacheModel.setLastRunUser(userModel.getId());
            nodeScriptServer.updateById(nodeScriptCacheModel);
            //
            NodeScriptExecuteLogCacheModel nodeScriptExecuteLogCacheModel = new NodeScriptExecuteLogCacheModel();
            nodeScriptExecuteLogCacheModel.setScriptId(dataItem.getScriptId());
            nodeScriptExecuteLogCacheModel.setNodeId(nodeInfo.getId());
            nodeScriptExecuteLogCacheModel.setScriptName(dataItem.getName());
            nodeScriptExecuteLogCacheModel.setTriggerExecType(0);
            nodeScriptExecuteLogCacheModel.setWorkspaceId(nodeInfo.getWorkspaceId());
            logServer.insert(nodeScriptExecuteLogCacheModel);
            return nodeScriptExecuteLogCacheModel.getId();
        } finally {
            BaseServerController.removeAll();
        }
    }
}
