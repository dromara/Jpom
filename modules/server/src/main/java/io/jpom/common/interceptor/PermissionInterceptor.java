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

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.interceptor.InterceptorPattens;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.common.BaseServerController;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.UserModel;
import io.jpom.permission.SystemPermission;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.node.NodeService;
import io.jpom.system.AgentException;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 权限拦截器
 *
 * @author jiangzeyin
 * @date 2019/03/16.
 */
@InterceptorPattens(sort = 1)
public class PermissionInterceptor extends BaseJpomInterceptor {

	private NodeService nodeService;
	private static final MethodFeature[] DEMO = new MethodFeature[]{
			MethodFeature.DEL,
			MethodFeature.UPLOAD,
			MethodFeature.REMOTE_DOWNLOAD,
			MethodFeature.EXECUTE};


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
		boolean systemPermission = this.checkSystemPermission(userModel, response, handlerMethod);
		if (!systemPermission) {
			return false;
		}
		Feature feature = handlerMethod.getMethodAnnotation(Feature.class);
		if (feature == null) {
			return true;
		}
		MethodFeature method = feature.method();
		if (ArrayUtil.contains(DEMO, method) && userModel.isDemoUser()) {
			this.errorMsg(response, "演示系统不能使用该功能,如果完整体验请部署后使用");
			return false;
		}
		return true;
	}

	private boolean checkSystemPermission(UserModel userModel, HttpServletResponse response, HandlerMethod handlerMethod) {

		SystemPermission systemPermission = this.getSystemPermission(handlerMethod);
		if (systemPermission == null) {
			return true;
		}
		if (!userModel.isSystemUser()) {
			this.errorMsg(response, "你没有权限:-2");
			return false;
		}
		if (systemPermission.superUser() && !userModel.isSuperSystemUser()) {
			this.errorMsg(response, "你没有权限:-2");
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

	private void errorMsg(HttpServletResponse response, String msg) {
		JsonMessage<String> jsonMessage = new JsonMessage<>(302, msg);
		ServletUtil.write(response, jsonMessage.toString(), MediaType.APPLICATION_JSON_VALUE);
	}
}
