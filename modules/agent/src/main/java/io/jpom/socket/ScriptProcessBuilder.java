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
package io.jpom.socket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONObject;
import io.jpom.model.data.NodeScriptModel;
import io.jpom.system.ExtConfigBean;
import io.jpom.util.CommandUtil;
import io.jpom.util.SocketSessionUtil;

import javax.websocket.Session;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 脚本执行
 *
 * @author jiangzeyin
 * @date 2019/4/25
 */
public class ScriptProcessBuilder implements Runnable {
	private static final ConcurrentHashMap<String, ScriptProcessBuilder> FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();

	private final ProcessBuilder processBuilder;
	private final Set<Session> sessions = new HashSet<>();
	private final String executeId;
	private final File logFile;
	private Process process;
	private InputStream inputStream;


	private ScriptProcessBuilder(NodeScriptModel nodeScriptModel, String executeId, String args) {
		this.executeId = executeId;
		this.logFile = nodeScriptModel.logFile(executeId);
		File scriptFile = nodeScriptModel.getFile(true);
		//
		String script = FileUtil.getAbsolutePath(scriptFile);
		processBuilder = new ProcessBuilder();
		List<String> command = StrUtil.splitTrim(args, StrUtil.SPACE);
		command.add(0, script);
		if (SystemUtil.getOsInfo().isLinux() || SystemUtil.getOsInfo().isMac()) {
			command.add(0, CommandUtil.SUFFIX);
		}
		DefaultSystemLog.getLog().info(CollUtil.join(command, StrUtil.SPACE));
		processBuilder.redirectErrorStream(true);
		processBuilder.command(command);
	}

	public static void addWatcher(NodeScriptModel nodeScriptModel, String executeId, String args, Session session) {
		ScriptProcessBuilder scriptProcessBuilder = FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP.computeIfAbsent(executeId, file1 -> {
			ScriptProcessBuilder scriptProcessBuilder1 = new ScriptProcessBuilder(nodeScriptModel, executeId, args);
			ThreadUtil.execute(scriptProcessBuilder1);
			return scriptProcessBuilder1;
		});
		if (scriptProcessBuilder.sessions.add(session)) {
			if (FileUtil.exist(scriptProcessBuilder.logFile)) {
				// 读取之前的信息并发送
				FileUtil.readLines(scriptProcessBuilder.logFile, CharsetUtil.CHARSET_UTF_8, (LineHandler) line -> {
					try {
						SocketSessionUtil.send(session, line);
					} catch (IOException e) {
						DefaultSystemLog.getLog().error("发送消息失败", e);
					}
				});
			}
		}
	}

	public static void stopWatcher(Session session) {
		Collection<ScriptProcessBuilder> scriptProcessBuilders = FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP.values();
		for (ScriptProcessBuilder scriptProcessBuilder : scriptProcessBuilders) {
			Set<Session> sessions = scriptProcessBuilder.sessions;
			sessions.removeIf(session1 -> session1.getId().equals(session.getId()));
		}
	}

	/**
	 * 停止脚本命令
	 *
	 * @param executeId 执行ID
	 */
	public static void stopRun(String executeId) {
		ScriptProcessBuilder scriptProcessBuilder = FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP.get(executeId);
		if (scriptProcessBuilder != null) {
			scriptProcessBuilder.end("停止运行");
		}
	}

	@Override
	public void run() {
		//初始化ProcessBuilder对象
		try {
			process = processBuilder.start();
			{
				inputStream = process.getInputStream();
				IoUtil.readLines(inputStream, ExtConfigBean.getInstance().getConsoleLogCharset(), (LineHandler) ScriptProcessBuilder.this::handle);
			}
			int waitFor = process.waitFor();
			JsonMessage<String> jsonMessage = new JsonMessage<>(200, "执行完毕:" + waitFor);
			JSONObject jsonObject = jsonMessage.toJson();
			jsonObject.put("op", ConsoleCommandOp.stop.name());
			this.end(jsonObject.toString());
		} catch (Exception e) {
			DefaultSystemLog.getLog().error("执行异常", e);
			this.end("执行异常：" + e.getMessage());
		}
	}

	/**
	 * 结束执行
	 *
	 * @param msg 响应的消息
	 */
	private void end(String msg) {
		if (this.process != null) {
			// windows 中不能正常关闭
			IoUtil.close(inputStream);
			this.process.destroy();
		}
		Iterator<Session> iterator = sessions.iterator();
		while (iterator.hasNext()) {
			Session session = iterator.next();
			try {
				SocketSessionUtil.send(session, msg);
			} catch (IOException e) {
				DefaultSystemLog.getLog().error("发送消息失败", e);
			}
			iterator.remove();
		}
		FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP.remove(this.executeId);
	}

	/**
	 * 响应
	 *
	 * @param line 信息
	 */
	private void handle(String line) {
		// 写入文件
		List<String> fileLine = new ArrayList<>();
		fileLine.add(line);
		FileUtil.appendLines(fileLine, logFile, CharsetUtil.CHARSET_UTF_8);
		Iterator<Session> iterator = sessions.iterator();
		while (iterator.hasNext()) {
			Session session = iterator.next();
			try {
				SocketSessionUtil.send(session, line);
			} catch (IOException e) {
				DefaultSystemLog.getLog().error("发送消息失败", e);
				iterator.remove();
			}
		}
	}
}
