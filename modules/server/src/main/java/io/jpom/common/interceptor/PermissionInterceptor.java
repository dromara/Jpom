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
package io.jpom.common.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.interceptor.InterceptorPattens;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.common.BaseServerController;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.UserModel;
import io.jpom.permission.SystemPermission;
import io.jpom.service.node.NodeService;
import io.jpom.system.AgentException;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 权限拦截器
 *
 * @author jiangzeyin
 * @date 2019/03/16.
 */
@InterceptorPattens(sort = 1)
public class PermissionInterceptor extends BaseJpomInterceptor {

	private NodeService nodeService;


	private void init() {
		if (nodeService == null) {
			nodeService = SpringUtil.getBean(NodeService.class);
		}
	}

	private SystemPermission getSystemPermission(HandlerMethod handlerMethod) {
		SystemPermission systemPermission = handlerMethod.getMethodAnnotation(SystemPermission.class);
		if (systemPermission == null) {
			systemPermission = handlerMethod.getBeanType().getAnnotation(SystemPermission.class);
		}
		return systemPermission;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {
		this.init();
		this.addNode(request);
		UserModel userModel = BaseServerController.getUserModel();
		if (userModel == null || userModel.isSuperSystemUser()) {
			// 没有登录、或者超级管理自己放过
			return true;
		}
		SystemPermission systemPermission = this.getSystemPermission(handlerMethod);
		if (systemPermission == null) {
			return true;
		}
		if (!userModel.isSystemUser()) {
			this.errorMsg(request, response);
			return false;
		}
		if (systemPermission.superUser() && !userModel.isSuperSystemUser()) {
			this.errorMsg(request, response);
			return false;
		}
		return true;
	}

	private void addNode(HttpServletRequest request) {
		String nodeId = request.getParameter("nodeId");
		if (!StrUtil.isBlankOrUndefined(nodeId)) {
			// 节点信息
			NodeModel nodeModel = nodeService.getByKey(nodeId);
			if (nodeModel != null && !nodeModel.isOpenStatus()) {
				throw new AgentException(nodeModel.getName() + "节点未启用");
			}
			request.setAttribute("node", nodeModel);
		}
	}

	private void errorMsg(HttpServletRequest request, HttpServletResponse response) throws IOException {

		JsonMessage<String> jsonMessage = new JsonMessage<>(302, "你没有权限:-2");
		ServletUtil.write(response, jsonMessage.toString(), MediaType.APPLICATION_JSON_VALUE);
	}
}
