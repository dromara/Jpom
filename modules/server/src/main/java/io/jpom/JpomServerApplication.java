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
package io.jpom;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ArrayUtil;
import cn.jiangzeyin.common.EnableCommonBoot;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.jiangzeyin.common.spring.event.ApplicationEventLoad;
import io.jpom.common.Type;
import io.jpom.common.interceptor.IpInterceptor;
import io.jpom.common.interceptor.LoginInterceptor;
import io.jpom.common.interceptor.OpenApiInterceptor;
import io.jpom.model.data.SystemIpConfigModel;
import io.jpom.permission.CacheControllerFeature;
import io.jpom.service.system.SystemParametersServer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * jpom 启动类
 *
 * @author jiangzeyin
 * @date 2017/9/14
 */
@SpringBootApplication
@ServletComponentScan
@EnableCommonBoot
public class JpomServerApplication implements ApplicationEventLoad {


	/**
	 * 启动执行
	 * --rest:ip_config
	 *
	 * @param args 参数
	 * @throws Exception 异常
	 */
	public static void main(String[] args) throws Exception {
		JpomApplication jpomApplication = new JpomApplication(Type.Server, JpomServerApplication.class, args);
		jpomApplication
				// 拦截器
				.addInterceptor(IpInterceptor.class)
				.addInterceptor(LoginInterceptor.class)
				.addInterceptor(OpenApiInterceptor.class)
//				.addInterceptor(PermissionInterceptor.class)
				.run(args);
		//
		if (ArrayUtil.containsIgnoreCase(args, "--rest:ip_config")) {
			// 重置 ip 白名单配置
			SystemParametersServer parametersServer = SpringUtil.getBean(SystemParametersServer.class);
			parametersServer.delByKey(SystemIpConfigModel.ID);
			Console.log("清除 IP 白名单配置成功");
		}
	}


	@Override
	public void applicationLoad() {
		CacheControllerFeature.init();
	}
}
