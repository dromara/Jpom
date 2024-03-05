/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model;

import cn.keepbx.jpom.model.BaseJsonModel;
import com.alibaba.fastjson2.JSONObject;

/**
 * websocket发送和接收消息Model
 *
 * @author lf
 */
public class WebSocketMessageModel extends BaseJsonModel {

    private String command;
    private String nodeId;
    private Object params;
    private Object data;

    public WebSocketMessageModel(String command, String nodeId) {
        this.command = command;
        this.nodeId = nodeId;
        this.data = "";
    }

    public static WebSocketMessageModel getInstance(String message) {
        JSONObject commandObj = JSONObject.parseObject(message);
        String command = commandObj.getString("command");
        String nodeId = commandObj.getString("nodeId");
        WebSocketMessageModel model = new WebSocketMessageModel(command, nodeId);
        model.setParams(commandObj.get("params"));
        model.setData(commandObj.get("data"));

        return model;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public Object getData() {
        return data;
    }

    public WebSocketMessageModel setData(Object data) {
        this.data = data;
        return this;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public Object getParams() {
        return params;
    }

    public void setParams(Object params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
