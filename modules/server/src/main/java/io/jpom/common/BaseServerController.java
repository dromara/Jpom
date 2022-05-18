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
package io.jpom.common;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.pattern.CronPattern;
import io.jpom.common.interceptor.LoginInterceptor;
import io.jpom.common.interceptor.PermissionInterceptor;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.UserModel;
import io.jpom.service.node.NodeService;
import io.jpom.system.ServerConfigBean;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;

/**
 * Jpom server 端
 *
 * @author jiangzeyin
 * @since 2019/4/16
 */
public abstract class BaseServerController extends BaseJpomController {
	private static final ThreadLocal<UserModel> USER_MODEL_THREAD_LOCAL = new ThreadLocal<>();

	public static final String NODE_ID = "nodeId";

	@Resource
	protected NodeService nodeService;

	protected NodeModel getNode() {
		NodeModel nodeModel = tryGetNode();
		Assert.notNull(nodeModel, "节点信息不正确,对应对节点不存在");
		return nodeModel;
	}

	protected NodeModel tryGetNode() {
		String nodeId = getParameter(NODE_ID);
		if (StrUtil.isEmpty(nodeId)) {
			return null;
		}
		return nodeService.getByKey(nodeId);
	}

	/**
	 * 验证 cron 表达式, demo 账号不能开启 cron
	 *
	 * @param cron cron
	 * @return 原样返回
	 */
	protected String checkCron(String cron) {
		if (StrUtil.isNotEmpty(cron)) {
			UserModel user = getUser();
			Assert.state(!user.isDemoUser(), PermissionInterceptor.DEMO_TIP);
			try {
				new CronPattern(cron);
			} catch (Exception e) {
				throw new IllegalArgumentException("cron 表达式格式不正确");
			}
		}
		return ObjectUtil.defaultIfNull(cron, StrUtil.EMPTY);
	}

	@Override
	public void resetInfo() {
		USER_MODEL_THREAD_LOCAL.set(getUserModel());
	}

	/**
	 * 为线程设置 用户
	 *
	 * @param userModel 用户
	 */
	public static void resetInfo(UserModel userModel) {
		UserModel userModel1 = USER_MODEL_THREAD_LOCAL.get();
		if (userModel1 != null && userModel == UserModel.EMPTY) {
			// 已经存在，更新为 empty 、跳过
			return;
		}
		USER_MODEL_THREAD_LOCAL.set(userModel);
	}

	protected UserModel getUser() {
		UserModel userByThreadLocal = getUserByThreadLocal();
		Assert.notNull(userByThreadLocal, ServerConfigBean.AUTHORIZE_TIME_OUT_CODE + StrUtil.EMPTY);
		return userByThreadLocal;
	}

	/**
	 * 从线程 缓存中获取 用户信息
	 *
	 * @return 用户
	 */
	public static UserModel getUserByThreadLocal() {
		return USER_MODEL_THREAD_LOCAL.get();
	}

	public static void removeAll() {
		USER_MODEL_THREAD_LOCAL.remove();
	}

	/**
	 * 只清理 是 empty 对象
	 */
	public static void removeEmpty() {
		UserModel userModel = USER_MODEL_THREAD_LOCAL.get();
		if (userModel == UserModel.EMPTY) {
			USER_MODEL_THREAD_LOCAL.remove();
		}
	}

	public static UserModel getUserModel() {
		ServletRequestAttributes servletRequestAttributes = tryGetRequestAttributes();
		if (servletRequestAttributes == null) {
			return null;
		}
		return (UserModel) servletRequestAttributes.getAttribute(LoginInterceptor.SESSION_NAME, RequestAttributes.SCOPE_SESSION);
	}
}
