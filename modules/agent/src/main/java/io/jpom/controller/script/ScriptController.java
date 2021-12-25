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

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.pattern.CronPattern;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HtmlUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseAgentController;
import io.jpom.model.data.NodeScriptModel;
import io.jpom.service.script.ScriptServer;
import io.jpom.socket.ScriptProcessBuilder;
import io.jpom.system.AgentConfigBean;
import io.jpom.system.ExtConfigBean;
import io.jpom.util.CommandUtil;
import io.jpom.util.LimitQueue;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;

/**
 * 脚本管理
 *
 * @author jiangzeyin
 * @date 2019/4/24
 */
@RestController
@RequestMapping(value = "/script")
public class ScriptController extends BaseAgentController {

	private final ScriptServer scriptServer;

	public ScriptController(ScriptServer scriptServer) {
		this.scriptServer = scriptServer;
	}

	@RequestMapping(value = "list.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String list() {
		return JsonMessage.getString(200, "", scriptServer.list());
	}

	@RequestMapping(value = "item.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String item(String id) {
		return JsonMessage.getString(200, "", scriptServer.getItem(id));
	}

	@RequestMapping(value = "save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String save(NodeScriptModel nodeScriptModel, String type) {
		Assert.notNull(nodeScriptModel, "没有数据");
		Assert.hasText(nodeScriptModel.getContext(), "内容为空");
		//
		String autoExecCron = nodeScriptModel.getAutoExecCron();
		if (StrUtil.isNotEmpty(autoExecCron)) {
			try {
				new CronPattern(autoExecCron);
			} catch (Exception e) {
				throw new IllegalArgumentException("定时执行表达式格式不正确");
			}
		}
		//
		nodeScriptModel.setContext(HtmlUtil.unescape(nodeScriptModel.getContext()));
		NodeScriptModel eModel = scriptServer.getItem(nodeScriptModel.getId());
		if ("add".equalsIgnoreCase(type)) {
			Assert.isNull(eModel, "id已经存在啦");

			nodeScriptModel.setId(IdUtil.fastSimpleUUID());
			File file = nodeScriptModel.getFile(true);
			if (file.exists() || file.isDirectory()) {
				return JsonMessage.getString(405, "当地id路径文件已经存在来，请修改");
			}
			scriptServer.addItem(nodeScriptModel);
			return JsonMessage.getString(200, "添加成功");
		}
		Assert.notNull(eModel, "对应数据不存在");
		eModel.setName(nodeScriptModel.getName());
		eModel.setAutoExecCron(autoExecCron);
		eModel.setContext(nodeScriptModel.getContext());
		scriptServer.updateItem(eModel);
		return JsonMessage.getString(200, "修改成功");
	}

	@RequestMapping(value = "del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String del(String id) {
		scriptServer.deleteItem(id);
		return JsonMessage.getString(200, "删除成功");
	}

	@RequestMapping(value = "upload.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String upload() throws IOException {
		MultipartFileBuilder multipartFileBuilder = createMultipart()
				.addFieldName("file").setFileExt("bat", "sh");
		multipartFileBuilder.setSavePath(AgentConfigBean.getInstance().getTempPathName());
		multipartFileBuilder.setUseOriginalFilename(true);
		String path = multipartFileBuilder.save();
		File file = FileUtil.file(path);
		String context = FileUtil.readString(path, ExtConfigBean.getInstance().getConsoleLogCharset());
		Assert.hasText(context, "脚本内容为空");

		String name = file.getName();
		String id = SecureUtil.sha1(name);
		NodeScriptModel eModel = scriptServer.getItem(id);
		if (eModel != null) {
			return JsonMessage.getString(405, "对应脚本模板已经存在啦");
		}
		eModel = new NodeScriptModel();
		eModel.setId(id);
		eModel.setName(name);
		eModel.setContext(context);
		file = eModel.getFile(true);
		if (file.exists() || file.isDirectory()) {
			return JsonMessage.getString(405, "当地id路径文件已经存在来，请修改");
		}
		scriptServer.addItem(eModel);
		return JsonMessage.getString(200, "导入成功");
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
		NodeScriptModel item = scriptServer.getItem(id);
		Assert.notNull(item, "没有对应数据");
		File logFile = item.logFile(executeId);
		Assert.state(FileUtil.isFile(logFile), "日志文件错误");

		JSONObject data = new JSONObject();
		// 运行中
		data.put("run", ScriptProcessBuilder.isRun(executeId));
		// 读取文件
		int linesInt = Convert.toInt(line, 1);
		LimitQueue<String> lines = new LimitQueue<>(1000);
		final int[] readCount = {0};
		FileUtil.readLines(logFile, CharsetUtil.CHARSET_UTF_8, (LineHandler) line1 -> {
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
		return JsonMessage.getString(200, "ok", data);
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
		NodeScriptModel item = scriptServer.getItem(id);
		if (item == null) {
			return JsonMessage.getString(200, "对应的脚本模版已经不存在拉");
		}
		Assert.notNull(item, "没有对应数据");
		File logFile = item.logFile(executeId);
		boolean fastDel = CommandUtil.systemFastDel(logFile);
		Assert.state(!fastDel, "删除日志文件失败");
		return JsonMessage.getString(200, "删除成功");
	}
}
