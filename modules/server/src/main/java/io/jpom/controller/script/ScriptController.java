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
package io.jpom.controller.script;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.pattern.CronPattern;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.common.interceptor.PermissionInterceptor;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.script.ScriptModel;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.service.node.script.NodeScriptServer;
import io.jpom.service.script.ScriptServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2022/1/19
 */
@RestController
@RequestMapping(value = "/script")
@Feature(cls = ClassFeature.SCRIPT)
public class ScriptController extends BaseServerController {

	private final ScriptServer scriptServer;
	private final NodeScriptServer nodeScriptServer;

	public ScriptController(ScriptServer scriptServer,
							NodeScriptServer nodeScriptServer) {
		this.scriptServer = scriptServer;
		this.nodeScriptServer = nodeScriptServer;
	}

	/**
	 * get script list
	 *
	 * @return json
	 */
	@RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String scriptList() {
		PageResultDto<ScriptModel> pageResultDto = scriptServer.listPage(getRequest());
		return JsonMessage.getString(200, "success", pageResultDto);
	}

	@RequestMapping(value = "save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String save(String id,
					   @ValidatorItem String context,
					   @ValidatorItem String name,
					   String autoExecCron,
					   String defArgs,
					   String description, String nodeIds) {
		ScriptModel scriptModel = new ScriptModel();
		scriptModel.setId(id);
		scriptModel.setContext(context);
		scriptModel.setName(name);
		scriptModel.setNodeIds(nodeIds);
		scriptModel.setDescription(description);
		scriptModel.setDefArgs(defArgs);

		Assert.hasText(scriptModel.getContext(), "内容为空");
		//
		if (StrUtil.isNotEmpty(autoExecCron)) {
			UserModel user = getUser();
			Assert.state(!user.isDemoUser(), PermissionInterceptor.DEMO_TIP);
			try {
				new CronPattern(autoExecCron);
			} catch (Exception e) {
				throw new IllegalArgumentException("定时执行表达式格式不正确");
			}
			scriptModel.setAutoExecCron(autoExecCron);
		} else {
			scriptModel.setAutoExecCron(StrUtil.EMPTY);
		}
		//
		String oldNodeIds = null;
		if (StrUtil.isEmpty(id)) {
			scriptServer.insert(scriptModel);
		} else {
			HttpServletRequest request = getRequest();
			ScriptModel byKey = scriptServer.getByKey(id, request);
			Assert.notNull(byKey, "没有对应的数据");
			oldNodeIds = byKey.getNodeIds();
			scriptServer.updateById(scriptModel, request);
		}
		this.syncNodeScript(scriptModel, oldNodeIds);
		return JsonMessage.getString(200, "修改成功");
	}

	private void syncNodeScript(ScriptModel scriptModel, String oldNode) {
		List<String> oldNodeIds = StrUtil.splitTrim(oldNode, StrUtil.COMMA);
		List<String> newNodeIds = StrUtil.splitTrim(scriptModel.getNodeIds(), StrUtil.COMMA);
		Collection<String> delNode = CollUtil.subtract(oldNodeIds, newNodeIds);
		UserModel user = getUser();
		// 删除
		for (String s : delNode) {
			NodeModel byKey = nodeService.getByKey(s, getRequest());
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("id", scriptModel.getId());
			JsonMessage<String> request = NodeForward.request(byKey, NodeUrl.Script_Del, user, jsonObject);
			Assert.state(request.getCode() == 200, "处理 " + byKey.getName() + " 节点删除脚本失败" + request.getMsg());
			nodeScriptServer.syncNode(byKey);
		}
		// 更新
		for (String newNodeId : newNodeIds) {
			NodeModel byKey = nodeService.getByKey(newNodeId, getRequest());
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("id", scriptModel.getId());
			jsonObject.put("type", "sync");
			jsonObject.put("context", scriptModel.getContext());
			jsonObject.put("autoExecCron", scriptModel.getAutoExecCron());
			jsonObject.put("defArgs", scriptModel.getDefArgs());
			jsonObject.put("description", scriptModel.getDescription());
			jsonObject.put("name", scriptModel.getName());
			jsonObject.put("workspaceId", scriptModel.getWorkspaceId());
			JsonMessage<String> request = NodeForward.request(byKey, NodeUrl.Script_Save, user, jsonObject);
			Assert.state(request.getCode() == 200, "处理 " + byKey.getName() + " 节点同步脚本失败" + request.getMsg());
			nodeScriptServer.syncNode(byKey);
		}
	}

	@RequestMapping(value = "del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String del(String id) {
		ScriptModel server = scriptServer.getByKey(id);
		if (server != null) {
			File file = server.scriptPath();
			boolean del = FileUtil.del(file);
			Assert.state(del, "清理脚本文件失败");
			scriptServer.delByKey(id);
		}
		return JsonMessage.getString(200, "删除成功");
	}
}
