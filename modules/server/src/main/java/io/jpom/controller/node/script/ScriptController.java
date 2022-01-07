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
import io.jpom.model.data.ScriptModel;
import io.jpom.permission.NodeDataPermission;
import io.jpom.permission.SystemPermission;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.node.script.ScriptExecuteLogServer;
import io.jpom.service.node.script.ScriptServer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 脚本管理
 *
 * @author jiangzeyin
 * @date 2019/4/24
 */
@RestController
@RequestMapping(value = "/node/script")
@Feature(cls = ClassFeature.NODE_SCRIPT)
@NodeDataPermission(cls = ScriptServer.class)
public class ScriptController extends BaseServerController {

	private final ScriptServer scriptServer;
	private final ScriptExecuteLogServer scriptExecuteLogServer;

	public ScriptController(ScriptServer scriptServer,
							ScriptExecuteLogServer scriptExecuteLogServer) {
		this.scriptServer = scriptServer;
		this.scriptExecuteLogServer = scriptExecuteLogServer;
	}

	/**
	 * get script list
	 *
	 * @return json
	 * @author Hotstrip
	 */
	@RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String scriptList() {
		PageResultDto<ScriptModel> pageResultDto = scriptServer.listPageNode(getRequest());
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
		PageResultDto<ScriptModel> modelPageResultDto = scriptServer.listPage(getRequest());
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
	public String save() {
		NodeModel node = getNode();
		JsonMessage<Object> request = NodeForward.request(node, getRequest(), NodeUrl.Script_Save);
		if (request.getCode() == HttpStatus.OK.value()) {
			scriptServer.syncNode(node);
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
			scriptServer.syncNode(node);
			// 删除日志
			scriptExecuteLogServer.delCache(id, node.getId(), request);
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
			scriptServer.syncNode(node);
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
		int cache = scriptServer.delCache(node.getId(), getRequest());
		String msg = scriptServer.syncExecuteNode(node);
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
		int del = scriptServer.del(where);
		return JsonMessage.getString(200, "成功删除" + del + "条脚本模版缓存");
	}

}
