package io.jpom.controller.script;

import cn.hutool.core.io.FileUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.model.PageResultDto;
import io.jpom.model.script.ScriptExecuteLogModel;
import io.jpom.model.script.ScriptModel;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.service.script.ScriptExecuteLogServer;
import io.jpom.service.script.ScriptServer;
import io.jpom.socket.ScriptProcessBuilder;
import io.jpom.util.CommandUtil;
import io.jpom.util.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

/**
 * @author bwcx_jzy
 * @since 2022/1/20
 */

@RestController
@RequestMapping(value = "/script_log")
@Feature(cls = ClassFeature.SCRIPT_LOG)
public class ScriptLogController extends BaseServerController {

	private final ScriptExecuteLogServer scriptExecuteLogServer;
	private final ScriptServer scriptServer;

	public ScriptLogController(ScriptExecuteLogServer scriptExecuteLogServer,
							   ScriptServer scriptServer) {
		this.scriptExecuteLogServer = scriptExecuteLogServer;
		this.scriptServer = scriptServer;
	}

	/**
	 * get script log list
	 *
	 * @return json
	 */
	@RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String scriptList() {
		PageResultDto<ScriptExecuteLogModel> pageResultDto = scriptExecuteLogServer.listPage(getRequest());
		return JsonMessage.getString(200, "success", pageResultDto);
	}

	/**
	 * 删除日志
	 *
	 * @param id        id
	 * @param executeId 执行ID
	 * @return json
	 */
	@RequestMapping(value = "del_log", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String delLog(@ValidatorItem() String id,
						 @ValidatorItem() String executeId) {
		ScriptModel item = scriptServer.getByKey(id, getRequest());
		Assert.notNull(item, "没有对应数据");
		File logFile = item.logFile(executeId);
		boolean fastDel = CommandUtil.systemFastDel(logFile);
		Assert.state(!fastDel, "删除日志文件失败");
		scriptExecuteLogServer.delByKey(executeId);
		return JsonMessage.getString(200, "删除成功");
	}

	/**
	 * 获取的日志
	 *
	 * @param id        id
	 * @param executeId 执行ID
	 * @param line      需要获取的行号
	 * @return json
	 */
	@RequestMapping(value = "log", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getNowLog(@ValidatorItem() String id,
							@ValidatorItem() String executeId,
							@ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "line") int line) {
		ScriptModel item = scriptServer.getByKey(id, getRequest());
		Assert.notNull(item, "没有对应数据");
		File logFile = item.logFile(executeId);
		Assert.state(FileUtil.isFile(logFile), "日志文件错误");
		JSONObject data = FileUtils.readLogFile(logFile, line);
		// 运行中
		data.put("run", ScriptProcessBuilder.isRun(executeId));
		return JsonMessage.getString(200, "ok", data);
	}
}
