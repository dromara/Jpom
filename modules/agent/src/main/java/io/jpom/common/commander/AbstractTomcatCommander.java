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
package io.jpom.common.commander;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import io.jpom.common.commander.impl.LinuxTomcatCommander;
import io.jpom.common.commander.impl.WindowsTomcatCommander;
import io.jpom.model.data.TomcatInfoModel;
import io.jpom.system.JpomRuntimeException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * tomcat命令执行工具类
 *
 * @author LF
 */
public abstract class AbstractTomcatCommander {

	private static AbstractTomcatCommander abstractTomcatCommander;

	public static AbstractTomcatCommander getInstance() {
		if (abstractTomcatCommander != null) {
			return abstractTomcatCommander;
		}
		if (SystemUtil.getOsInfo().isLinux()) {
			// Linux系统
			abstractTomcatCommander = new LinuxTomcatCommander();
		} else if (SystemUtil.getOsInfo().isWindows()) {
			// Windows系统
			abstractTomcatCommander = new WindowsTomcatCommander();
		} else if (SystemUtil.getOsInfo().isMac()) {
			abstractTomcatCommander = new LinuxTomcatCommander();
		} else {
			throw new JpomRuntimeException("不支持的：" + SystemUtil.getOsInfo().getName());
		}
		return abstractTomcatCommander;
	}

	/**
	 * 执行tomcat命令
	 *
	 * @param tomcatInfoModel tomcat信息
	 * @param cmd             执行的命令，包括start stop
	 * @return 返回tomcat启动结果
	 */
	public abstract String execCmd(TomcatInfoModel tomcatInfoModel, String cmd);

	/**
	 * 检查tomcat状态
	 *
	 * @param tomcatInfoModel tomcat信息
	 * @param cmd             操作命令
	 * @return 状态结果
	 */
	protected String getStatus(TomcatInfoModel tomcatInfoModel, String cmd) {
		String strReturn = "start".equals(cmd) ? "stopped" : "started";
		int i = 0;
		while (i < 10) {
			int result = 0;
			String url = String.format("http://127.0.0.1:%d/", tomcatInfoModel.getPort());
			HttpRequest httpRequest = new HttpRequest(url);
			// 设置超时时间为3秒
			httpRequest.setConnectionTimeout(3000);
			try {
				httpRequest.execute();
				result = 1;
			} catch (Exception ignored) {
			}

			i++;
			if ("start".equals(cmd) && result == 1) {
				strReturn = "started";
				break;
			}
			if ("stop".equals(cmd) && result == 0) {
				strReturn = "stopped";
				break;
			}
			ThreadUtil.sleep(1000);
		}
		return strReturn;
	}

	protected void exec(String command, boolean close) {
		DefaultSystemLog.getLog().info(command);
		try {
			// 执行命令
			Process process = Runtime.getRuntime().exec(command);
			process.getInputStream().close();
			process.getErrorStream().close();
			process.getOutputStream().close();
			process.waitFor(5, TimeUnit.SECONDS);
			if (close) {
				process.destroy();
			}
		} catch (IOException | InterruptedException e) {
			DefaultSystemLog.getLog().error("tomcat执行名称失败", e);
		}
	}
}
