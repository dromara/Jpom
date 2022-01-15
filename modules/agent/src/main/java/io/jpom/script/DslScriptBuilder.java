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
package io.jpom.script;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.model.data.DslYmlDto;
import io.jpom.model.data.NodeProjectInfoModel;
import io.jpom.model.data.NodeScriptModel;
import io.jpom.service.script.NodeScriptServer;
import io.jpom.system.ExtConfigBean;
import io.jpom.util.CommandUtil;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * dsl 执行脚本
 *
 * @author bwcx_jzy
 * @since 2022/1/15
 */
public class DslScriptBuilder extends BaseRunScript implements Runnable {

	private final NodeScriptModel scriptModel;
	private final String args;
	private final NodeProjectInfoModel nodeProjectInfoModel;

	private Process process;
	private InputStream inputStream;

	private DslScriptBuilder(NodeScriptModel scriptModel, NodeProjectInfoModel nodeProjectInfoModel, String args, String log) {
		super(FileUtil.file(log));
		this.scriptModel = scriptModel;
		this.nodeProjectInfoModel = nodeProjectInfoModel;
		this.args = args;
	}

	private DslScriptBuilder(NodeScriptModel scriptModel, NodeProjectInfoModel nodeProjectInfoModel, String args) {
		super(null);
		this.scriptModel = scriptModel;
		this.nodeProjectInfoModel = nodeProjectInfoModel;
		this.args = args;
	}

	/**
	 * 初始化
	 */
	private ProcessBuilder init() {
		String id = nodeProjectInfoModel.getId();
		File scriptFile = scriptModel.scriptFile("_" + id);
		// 替换内容
		String context = scriptModel.getContext();
		context = StrUtil.replace(context, "#{PROJECT_ID}", id);
		context = StrUtil.replace(context, "#{PROJECT_NAME}", nodeProjectInfoModel.getName());
		context = StrUtil.replace(context, "#{PROJECT_PATH}", nodeProjectInfoModel.allLib());
		FileUtil.writeString(context, scriptFile, ExtConfigBean.getInstance().getConsoleLogCharset());
		//
		String script = FileUtil.getAbsolutePath(scriptFile);
		ProcessBuilder processBuilder = new ProcessBuilder();
		List<String> command = StrUtil.splitTrim(args, StrUtil.SPACE);
		command.add(0, script);
		if (SystemUtil.getOsInfo().isLinux() || SystemUtil.getOsInfo().isMac()) {
			command.add(0, CommandUtil.SUFFIX);
		}
		DefaultSystemLog.getLog().info(CollUtil.join(command, StrUtil.SPACE));
		processBuilder.redirectErrorStream(true);
		processBuilder.command(command);
		return processBuilder;
	}

	@Override
	public void run() {
		try {
			ProcessBuilder processBuilder = this.init();
			//
			process = processBuilder.start();
			inputStream = process.getInputStream();
			IoUtil.readLines(inputStream, ExtConfigBean.getInstance().getConsoleLogCharset(), (LineHandler) DslScriptBuilder.this::handle);
			//
			int waitFor = process.waitFor();
			//
			this.handle("execute done:" + waitFor + " time:" + DateUtil.now());
		} catch (Exception e) {
			DefaultSystemLog.getLog().error("执行异常", e);
			String msg = "执行异常：" + e.getMessage();
			this.end(msg);
		}
	}

	/**
	 * 执行
	 */
	public void execute() {
		ThreadUtil.execute(this);
	}

	/**
	 * 执行
	 */
	public String syncExecute() {
		ProcessBuilder processBuilder = this.init();
		try {
			//
			process = processBuilder.start();
			inputStream = process.getInputStream();
			List<String> result = new ArrayList<>();
			IoUtil.readLines(inputStream, ExtConfigBean.getInstance().getConsoleLogCharset(), (LineHandler) result::add);
			//
			int waitFor = process.waitFor();
			//
			result.add(0, "" + waitFor);
			return CollUtil.join(result, StrUtil.CRLF);
		} catch (Exception e) {
			DefaultSystemLog.getLog().error("执行异常", e);
			String msg = "执行异常：" + e.getMessage();
			this.end(msg);
			return msg;
		}
	}

	@Override
	protected void end(String msg) {
		if (this.process != null) {
			// windows 中不能正常关闭
			IoUtil.close(inputStream);
			this.process.destroy();
		}
	}

	/**
	 * 执行
	 *
	 * @param scriptProcess 脚本流程
	 * @param log           日志
	 */
	public static String run(DslYmlDto.BaseProcess scriptProcess, NodeProjectInfoModel nodeProjectInfoModel, String log) {
		NodeScriptServer nodeScriptServer = SpringUtil.getBean(NodeScriptServer.class);
		String scriptId = scriptProcess.getScriptId();
		NodeScriptModel item = nodeScriptServer.getItem(scriptId);
		if (item == null) {
			return "脚本模版不存在";
		}
		DslScriptBuilder builder = new DslScriptBuilder(item, nodeProjectInfoModel, scriptProcess.getScriptArg(), log);
		builder.execute();
		return null;
	}

	/**
	 * 执行
	 *
	 * @param scriptProcess 脚本流程
	 */
	public static String syncRun(DslYmlDto.BaseProcess scriptProcess, NodeProjectInfoModel nodeProjectInfoModel) {
		NodeScriptServer nodeScriptServer = SpringUtil.getBean(NodeScriptServer.class);
		String scriptId = scriptProcess.getScriptId();
		NodeScriptModel item = nodeScriptServer.getItem(scriptId);
		if (item == null) {
			return "脚本模版不存在";
		}
		DslScriptBuilder builder = new DslScriptBuilder(item, nodeProjectInfoModel, scriptProcess.getScriptArg());
		return builder.syncExecute();
	}
}
