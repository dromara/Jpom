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

import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.NodeModel;
import io.jpom.model.node.ScriptCacheModel;
import io.jpom.permission.*;
import io.jpom.service.node.script.NodeScriptExecuteLogServer;
import io.jpom.service.node.script.NodeScriptServer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 脚本管理
 *
 * @author jiangzeyin
 * @since 2019/4/24
 */
@RestController
@RequestMapping(value = "/node/script")
@Feature(cls = ClassFeature.NODE_SCRIPT)
@NodeDataPermission(cls = NodeScriptServer.class)
public class NodeScriptController extends BaseServerController {

	private final NodeScriptServer nodeScriptServer;
	private final NodeScriptExecuteLogServer nodeScriptExecuteLogServer;

	public NodeScriptController(NodeScriptServer nodeScriptServer,
								NodeScriptExecuteLogServer nodeScriptExecuteLogServer) {
		this.nodeScriptServer = nodeScriptServer;
		this.nodeScriptExecuteLogServer = nodeScriptExecuteLogServer;
	}

	/**
	 * get script list
	 *
	 * @return json
	 * @author Hotstrip
	 */
	@RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String scriptList() {
		PageResultDto<ScriptCacheModel> pageResultDto = nodeScriptServer.listPageNode(getRequest());
		return JsonMessage.getString(200, "success", pageResultDto);
	}

	/**
	 * load node script list
	 * 加载节点脚本列表
	 *
	 * @return json
	 * @author Hotstrip
	 */
	@PostMapping(value = "list_all", produces = MediaType.APPLICATION_JSON_VALUE)
	public String listAll() {
		PageResultDto<ScriptCacheModel> modelPageResultDto = nodeScriptServer.listPage(getRequest());
		return JsonMessage.getString(200, "", modelPageResultDto);
	}


	@GetMapping(value = "item.json", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String item() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.Script_Item).toString();
	}

	/**
	 * 保存脚本
	 *
	 * @return json
	 */
	@RequestMapping(value = "save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EDIT)
	public String save(String autoExecCron) {
		NodeModel node = getNode();
		this.checkCron(autoExecCron);
		JsonMessage<Object> request = NodeForward.request(node, getRequest(), NodeUrl.Script_Save);
		if (request.getCode() == HttpStatus.OK.value()) {
			nodeScriptServer.syncNode(node);
		}
		return request.toString();
	}

	@RequestMapping(value = "del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.DEL)
	public String del(@ValidatorItem String id) {
		NodeModel node = getNode();
		HttpServletRequest request = getRequest();
		JsonMessage<Object> requestData = NodeForward.request(node, request, NodeUrl.Script_Del);
		if (requestData.getCode() == HttpStatus.OK.value()) {
			nodeScriptServer.syncNode(node);
			// 删除日志
			nodeScriptExecuteLogServer.delCache(id, node.getId(), request);
		}
		return requestData.toString();
	}

	/**
	 * 导入脚本
	 *
	 * @return json
	 */
	@RequestMapping(value = "upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.UPLOAD)
	public String upload() {
		NodeModel node = getNode();
		JsonMessage<String> stringJsonMessage = NodeForward.requestMultipart(node, getMultiRequest(), NodeUrl.Script_Upload);
		if (stringJsonMessage.getCode() == HttpStatus.OK.value()) {
			nodeScriptServer.syncNode(node);
		}
		return stringJsonMessage.toString();
	}

	/**
	 * 同步脚本模版
	 *
	 * @return json
	 */
	@GetMapping(value = "sync", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.DEL)
	public String syncProject() {
		//
		NodeModel node = getNode();
		int cache = nodeScriptServer.delCache(node.getId(), getRequest());
		String msg = nodeScriptServer.syncExecuteNode(node);
		return JsonMessage.getString(200, "主动清除 " + cache + StrUtil.SPACE + msg);
	}

	/**
	 * 删除节点缓存的所有脚本模版
	 *
	 * @return json
	 */
	@GetMapping(value = "clear_all", produces = MediaType.APPLICATION_JSON_VALUE)
	@SystemPermission(superUser = true)
	@Feature(method = MethodFeature.DEL)
	public String clearAll() {
		Entity where = Entity.create();
		where.set("id", " <> id");
		int del = nodeScriptServer.del(where);
		return JsonMessage.getString(200, "成功删除" + del + "条脚本模版缓存");
	}

}
