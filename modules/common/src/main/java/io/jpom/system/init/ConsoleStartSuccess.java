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

import cn.hutool.core.lang.Console;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.common.JpomManifest;
import io.jpom.common.Type;
import io.jpom.cron.IAsyncLoad;
import io.jpom.cron.ICron;
import io.jpom.system.ConfigBean;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * @author bwcx_jzy
 * @since Created Time 2021/8/2
 */
@PreLoadClass(value = Integer.MAX_VALUE)
@Slf4j
public class ConsoleStartSuccess {


	@PreLoadMethod(value = Integer.MAX_VALUE - 1)
	@SuppressWarnings("rawtypes")
	private static void statLoad() {
		ThreadUtil.execute(() -> {
			// 加载定时器
			Map<String, ICron> cronMap = SpringUtil.getApplicationContext().getBeansOfType(ICron.class);
			cronMap.forEach((name, iCron) -> {
				int startCron = iCron.startCron();
				if (startCron > 0) {
					log.debug("{} scheduling has been started:{}", name, startCron);
				}
			});
			Map<String, IAsyncLoad> asyncLoadMap = SpringUtil.getApplicationContext().getBeansOfType(IAsyncLoad.class);
			asyncLoadMap.forEach((name, asyncLoad) -> asyncLoad.startLoad());
			//
		});
	}

	/**
	 * 输出启动成功的 日志
	 */
	@PreLoadMethod(value = Integer.MAX_VALUE)
	private static void success() {
		Type type = JpomManifest.getInstance().getType();
		int port = ConfigBean.getInstance().getPort();
		String localhostStr = NetUtil.getLocalhostStr();
		String url = StrUtil.format("http://{}:{}", localhostStr, port);
		if (type == Type.Server) {
			Console.log("{} Successfully started,Can use happily => {} 【The current address is for reference only】", type, url);
		} else if (type == Type.Agent) {
			Console.log("{} Successfully started,Please go to the server to configure and use,Current node address => {} 【The current address is for reference only】", type, url);
		}
	}
}
