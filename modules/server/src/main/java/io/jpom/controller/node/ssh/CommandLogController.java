package io.jpom.controller.node.ssh;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.CommandExecLogModel;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.node.command.CommandExecLogService;
import io.jpom.util.CommandUtil;
import io.jpom.util.LimitQueue;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;

/**
 * 命令执行日志
 *
 * @author bwcx_jzy
 * @since 2021/12/23
 */
@RestController
@RequestMapping(value = "/node/ssh_command_log")
@Feature(cls = ClassFeature.SSH_COMMAND_LOG)
public class CommandLogController extends BaseServerController {

	private final CommandExecLogService commandExecLogService;

	public CommandLogController(CommandExecLogService commandExecLogService) {
		this.commandExecLogService = commandExecLogService;
	}

	/**
	 * 分页获取命令信息
	 *
	 * @return result
	 */
	@RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String page() {
		PageResultDto<CommandExecLogModel> page = commandExecLogService.listPage(getRequest());
		return JsonMessage.getString(200, "", page);
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
		CommandExecLogModel execLogModel = commandExecLogService.getByKey(id);
		Assert.notNull(execLogModel, "没有对应的记录");
		File logFile = execLogModel.logFile();
		boolean fastDel = CommandUtil.systemFastDel(logFile);
		Assert.state(!fastDel, "清理日志文件失败");
		//
		commandExecLogService.delByKey(id);
		return JsonMessage.getString(200, "操作成功");
	}

	/**
	 * 删除命令
	 *
	 * @param commandId 命令ID
	 * @param batchId   批次ID
	 * @return result
	 */
	@GetMapping(value = "batch_list", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String batchList(@ValidatorItem String commandId, @ValidatorItem String batchId) {
		CommandExecLogModel commandExecLogModel = new CommandExecLogModel();
		commandExecLogModel.setCommandId(commandId);
		commandExecLogModel.setBatchId(batchId);
		List<CommandExecLogModel> commandExecLogModels = commandExecLogService.listByBean(commandExecLogModel);

		return JsonMessage.getString(200, "", commandExecLogModels);
	}

	/**
	 * 获取日志
	 *
	 * @param id   id
	 * @param line 需要获取的行号
	 * @return json
	 */
	@RequestMapping(value = "log", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String log(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String id,
					  @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "line") int line) {
		CommandExecLogModel item = commandExecLogService.getByKey(id, getRequest());
		Assert.notNull(item, "没有对应数据");

		File file = item.logFile();
		if (!FileUtil.exist(file)) {
			return JsonMessage.getString(200, "还没有日志信息");
		}
		Assert.state(FileUtil.isFile(file), "日志文件错误");

		JSONObject data = new JSONObject();
		// 运行中
		Integer status = item.getStatus();
		data.put("run", status != null && status == CommandExecLogModel.Status.ING.getCode());
		// 读取文件
		int linesInt = Math.max(line, 1);
		LimitQueue<String> lines = new LimitQueue<>(1000);
		final int[] readCount = {0};
		FileUtil.readLines(file, CharsetUtil.CHARSET_UTF_8, (LineHandler) line1 -> {
			readCount[0]++;
			if (readCount[0] < linesInt) {
				return;
			}
			lines.add(line1);
		});
		// 下次应该获取的行数
		data.put("line", readCount[0] + 1);
		data.put("getLine", linesInt);
		data.put("dataLines", lines);
		return JsonMessage.getString(200, "", data);
	}

	@RequestMapping(value = "download_log", method = RequestMethod.GET)
	@ResponseBody
	@Feature(method = MethodFeature.DOWNLOAD)
	public void downloadLog(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String logId) {
		CommandExecLogModel item = commandExecLogService.getByKey(logId);
		Assert.notNull(item, "没有对应数据");
		File logFile = item.logFile();
		if (!FileUtil.exist(logFile)) {
			return;
		}
		if (logFile.isFile()) {
			ServletUtil.write(getResponse(), logFile);
		}
	}
}
