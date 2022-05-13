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
package io.jpom;

import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.EnableCommonBoot;
import io.jpom.common.ServerOpenApi;
import io.jpom.common.Type;
import io.jpom.common.interceptor.AuthorizeInterceptor;
import io.jpom.system.init.AutoRegSeverNode;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * jpom 启动类
 *
 * @author jiangzeyin
 * @since 2017/9/14.
 */
@SpringBootApplication
@ServletComponentScan
@EnableCommonBoot
public class JpomAgentApplication {

	/**
	 * 启动执行
	 *
	 * @param args 参数
	 * @throws Exception 异常
	 */
	public static void main(String[] args) throws Exception {
		long time = SystemClock.now();
		JpomApplication jpomApplication = new JpomApplication(Type.Agent, JpomAgentApplication.class, args);
		jpomApplication
				// 拦截器
				.addInterceptor(AuthorizeInterceptor.class)
				// 添加 参数 url 解码
				//				.addHandlerMethodArgumentResolver(UrlDecodeHandlerMethodArgumentResolver.class)
				.run(args);
		// 自动向服务端推送
		autoPushToServer(args);
		Console.log("Time-consuming to start this time：{}", DateUtil.formatBetween(SystemClock.now() - time, BetweenFormatter.Level.MILLISECOND));
	}

	/**
	 * 自动推送 插件端信息到服务端
	 *
	 * @param args 参数
	 */
	private static void autoPushToServer(String[] args) {
		int i = ArrayUtil.indexOf(args, ServerOpenApi.PUSH_NODE_KEY);
		if (i == ArrayUtil.INDEX_NOT_FOUND) {
			return;
		}
		String arg = ArrayUtil.get(args, i + 1);
		if (StrUtil.isEmpty(arg)) {
			Console.error("not found auto-push-to-server url");
			return;
		}
		AutoRegSeverNode.autoPushToServer(arg);
	}

}
