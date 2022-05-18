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
package io.jpom.common.interceptor;

import cn.hutool.core.net.Ipv4Util;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.interceptor.InterceptorPattens;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.common.ServerOpenApi;
import io.jpom.model.data.SystemIpConfigModel;
import io.jpom.service.system.SystemParametersServer;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * ip 访问限制拦截器
 *
 * @author bwcx_jzy
 * @since 2021/4/18
 */
@InterceptorPattens(sort = -2, exclude = ServerOpenApi.API + "**")
public class IpInterceptor extends BaseJpomInterceptor {

	private static final int IP_ACCESS_CODE = 999;

	@Override
	protected boolean preHandle(HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {
		String clientIp = ServletUtil.getClientIP(request);
		if (StrUtil.equals(NetUtil.LOCAL_IP, clientIp)) {
			return true;
		}
		SystemParametersServer bean = SpringUtil.getBean(SystemParametersServer.class);
		SystemIpConfigModel config = bean.getConfig(SystemIpConfigModel.ID, SystemIpConfigModel.class);
		if (config == null) {
			return true;
		}
		// 判断不允许访问
		String prohibited = config.getProhibited();
		if (StrUtil.isNotEmpty(prohibited) && this.checkIp(prohibited, clientIp, false)) {
			ServletUtil.write(response, JsonMessage.getString(IP_ACCESS_CODE, "Prohibition of access"), MediaType.APPLICATION_JSON_VALUE);
			return false;
		}
		String allowed = config.getAllowed();
		if (StrUtil.isEmpty(allowed) || this.checkIp(allowed, clientIp, true)) {
			return true;
		}
		ServletUtil.write(response, JsonMessage.getString(IP_ACCESS_CODE, "Prohibition of access"), MediaType.APPLICATION_JSON_VALUE);
		return false;
	}


	/**
	 * 检查ip 地址是否可以访问
	 *
	 * @param value    配置的值
	 * @param ip       被检查的 ip 地址
	 * @param checkAll 是否检查开放所有、避免禁止所有 ip 访问
	 * @return true 命中检查项
	 */
	private boolean checkIp(String value, String ip, boolean checkAll) {
		long ipNum = NetUtil.ipv4ToLong(ip);
		String[] split = StrUtil.splitToArray(value, StrUtil.LF);
		boolean check;
		for (String itemIp : split) {
			itemIp = itemIp.trim();
			if (itemIp.startsWith("#")) {
				continue;
			}
			if (checkAll && StrUtil.equals(itemIp, "0.0.0.0")) {
				// 开放所有
				return true;
			}
			if (StrUtil.contains(itemIp, Ipv4Util.IP_MASK_SPLIT_MARK)) {
				// ip段
				String[] itemIps = StrUtil.splitToArray(itemIp, Ipv4Util.IP_MASK_SPLIT_MARK);
				int count1 = StrUtil.count(itemIps[0], StrUtil.DOT);
				int count2 = StrUtil.count(itemIps[1], StrUtil.DOT);
				if (count1 == 3 && count2 == 3) {
					//192.168.1.0/192.168.1.200
					long aBegin = NetUtil.ipv4ToLong(itemIps[0]);
					long aEnd = NetUtil.ipv4ToLong(itemIps[1]);
					check = (ipNum >= aBegin) && (ipNum <= aEnd);
				} else if (count1 == 3 && count2 == 0) {
					//192.168.1.0/24
					String startIp = Ipv4Util.getBeginIpStr(itemIps[0], Integer.parseInt(itemIps[1]));
					String endIp = Ipv4Util.getEndIpStr(itemIps[0], Integer.parseInt(itemIps[1]));
					long aBegin = NetUtil.ipv4ToLong(startIp);
					long aEnd = NetUtil.ipv4ToLong(endIp);
					check = (ipNum >= aBegin) && (ipNum <= aEnd);
				} else {
					check = false;
				}

			} else {
				check = StrUtil.equals(itemIp, ip);
			}
			if (check) {
				return true;
			}
		}
		return false;
	}
}
