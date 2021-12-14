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

import cn.hutool.core.lang.Console;
import cn.hutool.core.net.NetUtil;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import io.jpom.common.JpomManifest;
import io.jpom.common.Type;
import io.jpom.system.ConfigBean;

/**
 * @author bwcx_jzy
 * @since Created Time 2021/8/2
 */
@PreLoadClass(value = Integer.MAX_VALUE)
public class ConsoleStartSuccess {

	/**
	 * 输出启动成功的 日志
	 */
	@PreLoadMethod(value = Integer.MAX_VALUE)
	private static void success() {
		Type type = JpomManifest.getInstance().getType();
		if (type == Type.Server) {
			Console.log(type + "Successfully started,Can use happily => http://{}:{} 【The current address is for reference only】", NetUtil.getLocalhostStr(), ConfigBean.getInstance().getPort());
		} else if (type == Type.Agent) {
			Console.log(type + "Successfully started,Please go to the server to configure and use,Current node address => http://{}:{} 【The current address is for reference only】", NetUtil.getLocalhostStr(), ConfigBean.getInstance().getPort());
		}
	}
}
