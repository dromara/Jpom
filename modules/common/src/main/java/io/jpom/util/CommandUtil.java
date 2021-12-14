/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 码之科技工作室
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
package io.jpom.util;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import io.jpom.system.ExtConfigBean;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 命令行工具
 *
 * @author jiangzeyin
 * @date 2019/4/15
 */
public class CommandUtil {
	/**
	 * 系统命令
	 */
	private static final List<String> COMMAND = new ArrayList<>();
	/**
	 * 文件后缀
	 */
	public static final String SUFFIX;

	static {
		if (SystemUtil.getOsInfo().isLinux()) {
			//执行linux系统命令
			COMMAND.add("/bin/sh");
			COMMAND.add("-c");
		} else if (SystemUtil.getOsInfo().isMac()) {
			COMMAND.add("/bin/sh");
			COMMAND.add("-c");
		} else {
			COMMAND.add("cmd");
			COMMAND.add("/c");
		}

		if (SystemUtil.getOsInfo().isWindows()) {
			SUFFIX = "bat";
		} else {
			SUFFIX = "sh";
		}
	}

	public static List<String> getCommand() {
		return ObjectUtil.clone(COMMAND);
	}

    /*public static String execCommand(String command) {
        String newCommand = StrUtil.replace(command, StrUtil.CRLF, StrUtil.SPACE);
        newCommand = StrUtil.replace(newCommand, StrUtil.LF, StrUtil.SPACE);
        String result = "error";
        try {
            result = exec(new String[]{newCommand}, null);
        } catch (Exception e) {
            DefaultSystemLog.getLog().error("执行命令异常", e);
            result += e.getMessage();
        }
        return result;
    }*/

	public static String execSystemCommand(String command) {
		return execSystemCommand(command, null);
	}

	/**
	 * 在指定文件夹下执行命令
	 *
	 * @param command 命令
	 * @param file    文件夹
	 * @return msg
	 */
	public static String execSystemCommand(String command, File file) {
		String newCommand = StrUtil.replace(command, StrUtil.CRLF, StrUtil.SPACE);
		newCommand = StrUtil.replace(newCommand, StrUtil.LF, StrUtil.SPACE);
		String result = "error";
		try {
			List<String> commands = getCommand();
			commands.add(newCommand);
			String[] cmd = commands.toArray(new String[]{});
			result = exec(cmd, file);
		} catch (Exception e) {
			DefaultSystemLog.getLog().error("执行命令异常", e);
			result += e.getMessage();
		}
		return result;
	}

	/**
	 * 执行命令
	 *
	 * @param cmd 命令行
	 * @return 结果
	 * @throws IOException IO
	 */
	private static String exec(String[] cmd, File file) throws IOException {
		Process process = new ProcessBuilder(cmd).directory(file).redirectErrorStream(true).start();
		Charset charset = ExtConfigBean.getInstance().getConsoleLogCharset();
		String result = RuntimeUtil.getResult(process, charset);
		DefaultSystemLog.getLog().debug("exec {} {} {}", charset.name(), Arrays.toString(cmd), result);
		return result;
	}

	/**
	 * 异步执行命令
	 *
	 * @param file    文件夹
	 * @param command 命令
	 * @throws IOException 异常
	 */
	public static void asyncExeLocalCommand(File file, String command) throws Exception {
		String newCommand = StrUtil.replace(command, StrUtil.CRLF, StrUtil.SPACE);
		newCommand = StrUtil.replace(newCommand, StrUtil.LF, StrUtil.SPACE);
		//
		DefaultSystemLog.getLog().debug(newCommand);
		List<String> commands = getCommand();
		commands.add(newCommand);
		ProcessBuilder pb = new ProcessBuilder(commands);
		if (file != null) {
			pb.directory(file);
		}
		pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
		pb.redirectError(ProcessBuilder.Redirect.INHERIT);
		pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
		pb.start();
	}

	/**
	 * 判断是否包含删除命令
	 *
	 * @param script 命令行
	 * @return true 包含
	 */
	public static boolean checkContainsDel(String script) {
		// 判断删除
		String[] commands = StrUtil.splitToArray(script, StrUtil.LF);
		for (String commandItem : commands) {
			if (checkContainsDelItem(commandItem)) {
				return true;
			}
		}
		return false;
	}

	private static boolean checkContainsDelItem(String script) {
		String[] split = StrUtil.splitToArray(script, StrUtil.SPACE);
		if (SystemUtil.getOsInfo().isWindows()) {
			for (String s : split) {
				if (StrUtil.startWithAny(s, "rd", "del")) {
					return true;
				}
				if (StrUtil.containsAnyIgnoreCase(s, " rd", " del")) {
					return true;
				}
			}
		} else {
			for (String s : split) {
				if (StrUtil.startWithAny(s, "rm", "\\rm")) {
					return true;
				}
				if (StrUtil.containsAnyIgnoreCase(s, " rm", " \\rm", "&rm", "&\\rm")) {
					return true;
				}
			}
		}
		return false;
	}
}
