package io.jpom.controller.node.command;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import io.jpom.common.BaseServerController;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.CommandModel;
import io.jpom.model.data.UserModel;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.node.command.CommandService;
import io.jpom.util.StringUtil;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author : Arno
 * @description : 命令管理
 * @since : 2021/12/6 21:42
 */
@RestController
@RequestMapping(value = "/node/command")
@Feature(cls = ClassFeature.COMMAND)
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
	 * @param model 命令信息
	 * @return result
	 */
	@RequestMapping(value = "edit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EDIT)
	public String edit(@RequestBody CommandModel model) {
		if (StrUtil.isEmpty(model.getName())) {
			return JsonMessage.getString(400, "请输入命令名称");
		}
		if (StrUtil.isEmpty(model.getCommand())) {
			return JsonMessage.getString(400, "请输入命令内容");
		}
		if (StrUtil.isEmpty(model.getExecutionPath())) {
			model.setExecutionPath("~/");
		}
		if (StrUtil.isEmpty(model.getExecutionRole())) {
			model.setExecutionRole("root");
		}
		if (ObjectUtil.isNull(model.getTimeout())) {
			model.setTimeout(60);
		}
		UserModel user = getUser();
		model.setModifyUser(user.getId());
		if (ObjectUtil.isNull(model.getId())) {
			commandService.insert(model);
		} else {
			commandService.updateById(model);
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
	public String batch(String id, String executionRole, String executionPath, String params,
						@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "运行命令内容不能为空")) String command,
						@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "运行节点不能为空")) String nodes,
						@ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "超时时间不正确") int timeout,
						@ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "命令类型不正确") int type) {
		if (StrUtil.isEmpty(nodes)) {
			return JsonMessage.getString(400, "请选择执行节点");
		}
		CommandModel commandModel = commandService.getByKey(id);
		if (CollUtil.isNotEmpty(commandModel.params())) {
			if (StrUtil.isEmpty(params)) {
				return JsonMessage.getString(400, "请设置运行参数");
			}
			List<CommandModel.CommandParam> commandParams = StringUtil.jsonConvertArray(params, CommandModel.CommandParam.class);
			if (CollUtil.isEmpty(commandParams) || commandParams.size() != commandModel.params().size()) {
				return JsonMessage.getString(400, "运行参数错误");
			}
			for (CommandModel.CommandParam param : commandModel.params()) {
				CommandModel.CommandParam commandParam = commandParams.stream().filter(x -> x.getName().equals(param.getName())).findFirst().orElse(null);
				if (null == commandParam) {
					return JsonMessage.getString(400, param.getName() + " 运行参数错误");
				}
				command = command.replace(String.format("{{%s}}", param.getName()), commandParam.getVal());
			}
		}
		CommandModel model = new CommandModel();
		model.setParams(params);
		model.setCommand(command);
		model.setId(id);
		model.setTimeout(timeout);
		model.setExecutionPath(executionPath);
		model.setExecutionRole(executionRole);
		model.setType(type);
		commandService.executeBatch(model, StringUtil.jsonConvertArray(nodes, String.class));
		return JsonMessage.getString(200, "操作成功");
	}
}
