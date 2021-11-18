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
package io.jpom.system.init;

import cn.hutool.core.util.ObjectUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import io.jpom.JpomApplication;
import io.jpom.common.JpomManifest;
import io.jpom.util.JvmUtil;

/**
 * @author bwcx_jzy
 * @date 2019/9/5
 */
class CheckDuplicateRun {

	static void check() {
		try {
			Class<?> appClass = JpomApplication.getAppClass();
			String pid = String.valueOf(JpomManifest.getInstance().getPid());
			Integer mainClassPid = JvmUtil.findMainClassPid(appClass.getName());
			if (pid.equals(ObjectUtil.toString(mainClassPid))) {
				return;
			}
			DefaultSystemLog.getLog().warn("Jpom 程序建议一个机器上只运行一个对应的程序：" + JpomApplication.getAppType() + "  pid:" + mainClassPid);
		} catch (Exception e) {
			DefaultSystemLog.getLog().error("检查异常", e);
		}
	}
}

