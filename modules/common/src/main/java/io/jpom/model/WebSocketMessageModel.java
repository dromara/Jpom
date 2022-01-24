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
package io.jpom.model;

import com.alibaba.fastjson.JSONObject;

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
