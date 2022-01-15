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
package io.jpom.common.commander.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.StrUtil;
import io.jpom.common.commander.AbstractProjectCommander;
import io.jpom.common.commander.AbstractSystemCommander;
import io.jpom.model.data.NodeProjectInfoModel;
import io.jpom.model.system.NetstatModel;
import io.jpom.util.CommandUtil;
import io.jpom.util.JvmUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * windows 版
 *
 * @author Administrator
 */
public class WindowsProjectCommander extends AbstractProjectCommander {

	@Override
	public String buildJavaCommand(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel.JavaCopyItem javaCopyItem) {
		String classPath = NodeProjectInfoModel.getClassPathLib(nodeProjectInfoModel);
		if (StrUtil.isBlank(classPath)) {
			return null;
		}
		// 拼接命令
		String jvm = javaCopyItem == null ? nodeProjectInfoModel.getJvm() : javaCopyItem.getJvm();
		String tag = javaCopyItem == null ? nodeProjectInfoModel.getId() : javaCopyItem.getTagId();
		String mainClass = nodeProjectInfoModel.getMainClass();
		String args = javaCopyItem == null ? nodeProjectInfoModel.getArgs() : javaCopyItem.getArgs();
		return String.format("%s %s %s " +
						"%s  %s  %s >> %s &",
				getRunJavaPath(nodeProjectInfoModel, true),
				jvm, JvmUtil.getJpomPidTag(tag, nodeProjectInfoModel.allLib()),
				classPath, mainClass, args, nodeProjectInfoModel.getAbsoluteLog(javaCopyItem));
	}

	@Override
	public String stopJava(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel.JavaCopyItem javaCopyItem, int pid) throws Exception {
		String tag = javaCopyItem == null ? nodeProjectInfoModel.getId() : javaCopyItem.getTagId();
		// 如果正在运行，则执行杀进程命令
		String kill = AbstractSystemCommander.getInstance().kill(FileUtil.file(nodeProjectInfoModel.allLib()), pid);
		this.loopCheckRun(nodeProjectInfoModel, javaCopyItem, false);
		return status(tag) + StrUtil.SPACE + kill;
	}

	@Override
	public List<NetstatModel> listNetstat(int pId, boolean listening) {
		String cmd;
		if (listening) {
			cmd = "netstat -nao -p tcp | findstr \"LISTENING\" | findstr " + pId;
		} else {
			cmd = "netstat -nao -p tcp | findstr /V \"CLOSE_WAIT\" | findstr " + pId;
		}
		String result = CommandUtil.execSystemCommand(cmd);
		List<String> netList = StrSplitter.splitTrim(result, StrUtil.LF, true);
		if (netList == null || netList.size() <= 0) {
			return null;
		}
		List<NetstatModel> array = new ArrayList<>();
		for (String str : netList) {
			List<String> list = StrSplitter.splitTrim(str, " ", true);
			if (list.size() < 5) {
				continue;
			}
			NetstatModel netstatModel = new NetstatModel();
			netstatModel.setProtocol(list.get(0));
			netstatModel.setLocal(list.get(1));
			netstatModel.setForeign(list.get(2));
			netstatModel.setStatus(list.get(3));
			netstatModel.setName(list.get(4));
			array.add(netstatModel);
		}
		return array;
	}
}
