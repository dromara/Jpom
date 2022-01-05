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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.StrUtil;
import io.jpom.common.commander.BaseUnixProjectCommander;
import io.jpom.model.system.NetstatModel;
import io.jpom.util.CommandUtil;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * MacOSProjectCommander
 * <p>
 * some commands cannot execute success on Mac OS
 *
 * @author Hotstrip
 */
public class MacOsProjectCommander extends BaseUnixProjectCommander {

	@Override
	public List<NetstatModel> listNetstat(int pId, boolean listening) {
		String cmd;
		if (listening) {
			cmd = "lsof -n -P -iTCP -sTCP:LISTEN |grep " + pId + " | head -20";
		} else {
			cmd = "lsof -n -P -iTCP -sTCP:CLOSE_WAIT |grep " + pId + " | head -20";
		}
		return this.listNetstat(cmd);
	}

	protected List<NetstatModel> listNetstat(String cmd) {
		String result = CommandUtil.execSystemCommand(cmd);
		List<String> netList = StrSplitter.splitTrim(result, StrUtil.LF, true);
		if (CollUtil.isEmpty(netList)) {
			return null;
		}
		return netList.stream().map(str -> {
			List<String> list = StrSplitter.splitTrim(str, " ", true);
			if (list.size() < 10) {
				return null;
			}
			NetstatModel netstatModel = new NetstatModel();
			netstatModel.setProtocol(list.get(7));
			//netstatModel.setReceive(list.get(1));
			//netstatModel.setSend(list.get(2));
			netstatModel.setLocal(list.get(8));
			netstatModel.setForeign(list.get(4));
			if ("tcp".equalsIgnoreCase(netstatModel.getProtocol())) {
				netstatModel.setStatus(CollUtil.get(list, 9));
				netstatModel.setName(CollUtil.get(list, 0));
			} else {
				netstatModel.setStatus(StrUtil.DASHED);
				netstatModel.setName(CollUtil.get(list, 5));
			}

			return netstatModel;
		}).filter(Objects::nonNull).collect(Collectors.toList());
	}
}
