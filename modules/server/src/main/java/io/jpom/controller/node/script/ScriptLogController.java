package io.jpom.controller.node.script;

import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.ScriptExecuteLogCacheModel;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.node.script.ScriptExecuteLogServer;
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
public class ScriptLogController extends BaseServerController {

	private final ScriptExecuteLogServer scriptExecuteLogServer;

	public ScriptLogController(ScriptExecuteLogServer scriptExecuteLogServer) {
		this.scriptExecuteLogServer = scriptExecuteLogServer;
	}

	/**
	 * get script log list
	 *
	 * @return json
	 */
	@RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String scriptList() {
		PageResultDto<ScriptExecuteLogCacheModel> pageResultDto = scriptExecuteLogServer.listPageNode(getRequest());
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
		ScriptExecuteLogCacheModel executeLogModel = scriptExecuteLogServer.queryByBean(scriptExecuteLogCacheModel);
		Assert.notNull(executeLogModel, "没有对应的执行日志");
		JsonMessage<Object> request = NodeForward.request(node, getRequest(), NodeUrl.SCRIPT_DEL_LOG);
		if (request.getCode() == HttpStatus.OK.value()) {
			scriptExecuteLogServer.delByKey(executeId);
		}
		return request.toString();
	}
}
