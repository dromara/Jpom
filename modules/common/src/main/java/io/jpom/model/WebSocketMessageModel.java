package io.jpom.model;

import com.alibaba.fastjson.JSONObject;
import org.springframework.web.socket.TextMessage;

/**
 * websocket发送和接收消息Model
 *
 * @author lf
 */
public class WebSocketMessageModel {

    private String command;
    private String nodeId;
    private Object params;
    private Object data;

    public WebSocketMessageModel(String command, String nodeId) {
        this.command = command;
        this.nodeId = nodeId;
        this.data = "";
    }

    public static WebSocketMessageModel getInstance(TextMessage message) {
        JSONObject commandObj = JSONObject.parseObject(message.getPayload());
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

    public void setData(Object data) {
        this.data = data;
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
        JSONObject result = new JSONObject();
        result.put("command", this.command);
        result.put("nodeId", this.nodeId);
        result.put("params", this.params);
        result.put("data", this.data);
        return result.toJSONString();
    }
}
