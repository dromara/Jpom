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
package io.jpom.socket;

import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.JpomApplication;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.SshModel;
import io.jpom.model.data.UserModel;
import io.jpom.plugin.ClassFeature;
import io.jpom.service.node.NodeService;
import io.jpom.service.node.ssh.SshService;
import io.jpom.service.user.RoleService;
import io.jpom.service.user.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * socket 拦截器、鉴权
 *
 * @author jiangzeyin
 * @date 2019/4/19
 */
public class ServerWebSocketInterceptor implements HandshakeInterceptor {

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
		if (request instanceof ServletServerHttpRequest) {
			ServletServerHttpRequest serverHttpRequest = (ServletServerHttpRequest) request;
			HttpServletRequest httpServletRequest = serverHttpRequest.getServletRequest();
			// 判断用户
			String userId = httpServletRequest.getParameter("userId");
			UserService userService = SpringUtil.getBean(UserService.class);
			RoleService roleService = SpringUtil.getBean(RoleService.class);
			UserModel userModel = userService.checkUser(userId);
			if (userModel == null) {
				return false;
			}
			// 验证 node 权限
			String nodeId = httpServletRequest.getParameter("nodeId");
			if (!JpomApplication.SYSTEM_ID.equals(nodeId)) {
				NodeService nodeService = SpringUtil.getBean(NodeService.class);
				NodeModel nodeModel = nodeService.getByKey(nodeId, userModel.getId());
				if (nodeModel == null || roleService.errorDynamicPermission(userModel, ClassFeature.NODE, nodeId)) {
					return false;
				}
				//
				attributes.put("nodeInfo", nodeModel);
			}
			// 判断拦截类型
			String type = httpServletRequest.getParameter("type");
			HandlerType handlerType;
			try {
				handlerType = HandlerType.valueOf(type);
			} catch (Exception e) {
				//throw new JpomRuntimeException("type 错误：" + type);
				DefaultSystemLog.getLog().warn("传入的类型错误：{}", type);
				return false;
			}
			switch (handlerType) {
				case console:
					//控制台
					String projectId = httpServletRequest.getParameter("projectId");
					// 判断权限
					if (roleService.errorDynamicPermission(userModel, ClassFeature.PROJECT, projectId)) {
						return false;
					}
					attributes.put("copyId", httpServletRequest.getParameter("copyId"));
					attributes.put("projectId", projectId);
					break;
				case script:
					// 脚本模板
					String scriptId = httpServletRequest.getParameter("scriptId");
					if (roleService.errorDynamicPermission(userModel, ClassFeature.SCRIPT, scriptId)) {
						return false;
					}
					attributes.put("scriptId", scriptId);
					break;
				case tomcat:
					String tomcatId = httpServletRequest.getParameter("tomcatId");
					if (roleService.errorDynamicPermission(userModel, ClassFeature.TOMCAT, tomcatId)) {
						return false;
					}
					attributes.put("tomcatId", tomcatId);
					break;
				case ssh:
					String sshId = httpServletRequest.getParameter("sshId");

					SshService bean = SpringUtil.getBean(SshService.class);
					SshModel sshModel = bean.getByKey(sshId, userModel.getId());
					if (sshModel == null) {
						return false;
					}
					Map<String, String[]> parameterMap = httpServletRequest.getParameterMap();
					attributes.put("parameterMap", parameterMap);
					attributes.put("sshItem", sshModel);
					break;
				case nodeUpdate:
					if (!userModel.isSystemUser()) {
						return false;
					}
					break;
				default:
					return false;
			}
			//
			String ip = ServletUtil.getClientIP(httpServletRequest);
			attributes.put("ip", ip);
			//
			String userAgent = ServletUtil.getHeaderIgnoreCase(httpServletRequest, HttpHeaders.USER_AGENT);
			attributes.put(HttpHeaders.USER_AGENT, userAgent);
			attributes.put("userInfo", userModel);
			return true;
		}
		return false;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
		if (exception != null) {
			DefaultSystemLog.getLog().error("afterHandshake", exception);
		}
	}
}
