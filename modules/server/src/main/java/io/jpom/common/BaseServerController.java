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
package io.jpom.common;

import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import io.jpom.common.interceptor.LoginInterceptor;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.UserModel;
import io.jpom.service.node.NodeService;
import io.jpom.system.JpomRuntimeException;
import io.jpom.system.ServerConfigBean;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;

/**
 * Jpom server 端
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
public abstract class BaseServerController extends BaseJpomController {
	private static final ThreadLocal<UserModel> USER_MODEL_THREAD_LOCAL = new ThreadLocal<>();

	public static final String NODE_ID = "nodeId";

	@Resource
	protected NodeService nodeService;

	protected NodeModel getNode() {
		NodeModel nodeModel = tryGetNode();
		if (nodeModel == null) {
			throw new JpomRuntimeException("节点信息不正确");
		}
		return nodeModel;
	}

	protected NodeModel tryGetNode() {
		String nodeId = getParameter(NODE_ID);
		if (StrUtil.isEmpty(nodeId)) {
			return null;
		}
		return nodeService.getByKey(nodeId);
	}

	@Override
	public void resetInfo() {
		USER_MODEL_THREAD_LOCAL.set(getUserModel());
	}

	public static void resetInfo(UserModel userModel) {
		USER_MODEL_THREAD_LOCAL.set(userModel);
	}

	protected UserModel getUser() {
		return getUserByThreadLocal();
	}

	/**
	 * 从线程 缓存中获取 用户信息
	 *
	 * @return 用户
	 */
	public static UserModel getUserByThreadLocal() {
		UserModel userModel = USER_MODEL_THREAD_LOCAL.get();
		Assert.notNull(userModel, ServerConfigBean.AUTHORIZE_TIME_OUT_CODE + StrUtil.EMPTY);
		return userModel;
	}

	public static void remove() {
		USER_MODEL_THREAD_LOCAL.remove();
	}

	public static UserModel getUserModel() {
		ServletRequestAttributes servletRequestAttributes = tryGetRequestAttributes();
		if (servletRequestAttributes == null) {
			return null;
		}
		return (UserModel) servletRequestAttributes.getAttribute(LoginInterceptor.SESSION_NAME, RequestAttributes.SCOPE_SESSION);
	}

	/**
	 * 处理分页的时间字段
	 *
	 * @param page    分页
	 * @param entity  条件
	 * @param colName 字段名称
	 */
	protected void doPage(Page page, Entity entity, String colName) {
		String time = getParameter("time");
		colName = colName.toUpperCase();
		page.addOrder(new Order(colName, Direction.DESC));
		// 时间
		if (StrUtil.isNotEmpty(time)) {

		}
	}
}
