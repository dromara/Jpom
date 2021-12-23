package io.jpom.controller.node.ssh;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.CommandExecLogModel;
import io.jpom.model.data.CommandModel;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.node.command.CommandService;
import io.jpom.util.CommandUtil;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 命令管理
 *
 * @author : Arno
 * @since : 2021/12/6 21:42
 */
@RestController
@RequestMapping(value = "/node/ssh_command")
@Feature(cls = ClassFeature.SSH_COMMAND)
public class CommandInfoController extends BaseServerController {

	private final CommandService commandService;

	public CommandInfoController(CommandService commandService) {
		this.commandService = commandService;
	}

	/**
	 * 分页获取命令信息
	 *
	 * @return result
	 */
	@RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String page() {
		PageResultDto<CommandModel> page = commandService.listPage(getRequest());
		return JsonMessage.getString(200, "", page);
	}

	/**
	 * 创建命令
	 *
	 * @param data 命令信息
	 * @return result
	 */
	@RequestMapping(value = "edit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EDIT)
	public String edit(@RequestBody JSONObject data) {
		String name = data.getString("name");
		String command = data.getString("command");
		String desc = data.getString("desc");
		String defParams = data.getString("defParams");
		Assert.hasText(name, "请输入命令名称");
		Assert.hasText(command, "请输入命令内容");
		String id = data.getString("id");
		//
		CommandModel commandModel = new CommandModel();
		commandModel.setName(name);
		commandModel.setCommand(command);
		commandModel.setDesc(desc);
		commandModel.setSshIds(data.getString("sshIds"));
		//
		if (StrUtil.isNotEmpty(defParams)) {
			List<CommandModel.CommandParam> params = CommandModel.params(defParams);
			if (params == null) {
				commandModel.setDefParams(StrUtil.EMPTY);
			} else {
				commandModel.setDefParams(JSONObject.toJSONString(params));
			}
		} else {
			commandModel.setDefParams(StrUtil.EMPTY);
		}

		if (StrUtil.isEmpty(id)) {
			commandService.insert(commandModel);
		} else {
			commandModel.setId(id);
			commandService.updateById(commandModel);
		}
		return JsonMessage.getString(200, "操作成功");
	}

	/**
	 * 删除命令
	 *
	 * @param id id
	 * @return result
	 */
	@RequestMapping(value = "del", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.DEL)
	public String del(String id) {
		File logFileDir = CommandExecLogModel.logFileDir(id);
		boolean fastDel = CommandUtil.systemFastDel(logFileDir);
		Assert.state(!fastDel, "清理日志文件失败");
		//
		commandService.delByKey(id);
		return JsonMessage.getString(200, "操作成功");
	}

	/**
	 * 批量执行命令
	 *
	 * @return result
	 */
	@RequestMapping(value = "batch", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EXECUTE)
	public String batch(String id,
						String params,
						@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "运行节点不能为空") String nodes) throws IOException {
		Assert.hasText(id, "请选择执行对命令");
		Assert.hasText(nodes, "请选择执行节点");
		String batchId = commandService.executeBatch(id, params, nodes);
		return JsonMessage.getString(200, "操作成功", batchId);
	}
}
