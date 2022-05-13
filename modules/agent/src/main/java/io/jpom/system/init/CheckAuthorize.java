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
package io.jpom.system.init;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import io.jpom.common.JpomManifest;
import io.jpom.system.AgentAuthorize;
import io.jpom.system.ConfigBean;
import io.jpom.system.ExtConfigBean;
import io.jpom.util.JvmUtil;

import java.io.File;

/**
 * 检查授权信息
 *
 * @author jiangzeyin
 * @since 2019/4/17
 */
@PreLoadClass
public class CheckAuthorize {

	@PreLoadMethod
	private static void checkAgentAuthorize() {
		AgentAuthorize.getInstance();
	}

	@PreLoadMethod
	private static void checkJps() {
		boolean exist = JvmUtil.exist(JpomManifest.getInstance().getPid());
		JvmUtil.setJpsNormal(exist);
	}

	/**
	 * 恢复脚本模板路径
	 */
	@PreLoadMethod
	private static void repairScriptPath() {
		if (!JpomManifest.getInstance().isDebug()) {
			if (StrUtil.compareVersion(JpomManifest.getInstance().getVersion(), "2.4.2") < 0) {
				return;
			}
		}
		File oldDir = FileUtil.file(ExtConfigBean.getInstance().getPath(), ConfigBean.SCRIPT_DIRECTORY);
		if (!oldDir.exists()) {
			return;
		}
		File newDir = FileUtil.file(ConfigBean.getInstance().getDataPath(), ConfigBean.SCRIPT_DIRECTORY);
		FileUtil.move(oldDir, newDir, true);
	}
}
