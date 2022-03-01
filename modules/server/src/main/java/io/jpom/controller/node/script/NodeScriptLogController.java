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
package io.jpom.controller.node.script;

import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.NodeModel;
import io.jpom.model.node.ScriptExecuteLogCacheModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.service.node.script.NodeScriptExecuteLogServer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author bwcx_jzy
 * @since 2021/12/24
 */
@RestController
@RequestMapping(value = "/node/script_log")
@Feature(cls = ClassFeature.NODE_SCRIPT_LOG)
public class NodeScriptLogController extends BaseServerController {

	private final NodeScriptExecuteLogServer nodeScriptExecuteLogServer;

	public NodeScriptLogController(NodeScriptExecuteLogServer nodeScriptExecuteLogServer) {
		this.nodeScriptExecuteLogServer = nodeScriptExecuteLogServer;
	}

	/**
	 * get script log list
	 *
	 * @return json
	 */
	@RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String scriptList() {
		PageResultDto<ScriptExecuteLogCacheModel> pageResultDto = nodeScriptExecuteLogServer.listPageNode(getRequest());
		return JsonMessage.getString(200, "", pageResultDto);
	}

	/**
	 * 查日志
	 *
	 * @return json
	 */
	@RequestMapping(value = "log", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String log() {
		NodeModel node = getNode();
		return NodeForward.request(node, getRequest(), NodeUrl.SCRIPT_LOG).toString();
	}

	/**
	 * 删除日志
	 *
	 * @param id        模版ID
	 * @param executeId 日志ID
	 * @return json
	 */
	@RequestMapping(value = "del", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.DEL)
	public String del(@ValidatorItem String id, String executeId) {
		NodeModel node = getNode();
		ScriptExecuteLogCacheModel scriptExecuteLogCacheModel = new ScriptExecuteLogCacheModel();
		scriptExecuteLogCacheModel.setId(executeId);
		scriptExecuteLogCacheModel.setScriptId(id);
		scriptExecuteLogCacheModel.setNodeId(node.getId());
		ScriptExecuteLogCacheModel executeLogModel = nodeScriptExecuteLogServer.queryByBean(scriptExecuteLogCacheModel);
		Assert.notNull(executeLogModel, "没有对应的执行日志");
		JsonMessage<Object> request = NodeForward.request(node, getRequest(), NodeUrl.SCRIPT_DEL_LOG);
		if (request.getCode() == HttpStatus.OK.value()) {
			nodeScriptExecuteLogServer.delByKey(executeId);
		}
		return request.toString();
	}
}
