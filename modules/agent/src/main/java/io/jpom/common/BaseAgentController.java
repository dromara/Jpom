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

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.controller.base.AbstractController;
import io.jpom.model.data.NodeProjectInfoModel;
import io.jpom.service.manage.ProjectInfoService;
import io.jpom.system.ConfigBean;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

/**
 * agent 端
 *
 * @author jiangzeyin
 * @since 2019/4/17
 */
public abstract class BaseAgentController extends BaseJpomController {
	@Resource
	protected ProjectInfoService projectInfoService;

	protected String getUserName() {
		return getUserName(getRequest());
	}

	/**
	 * 获取server 端操作人
	 *
	 * @param request req
	 * @return name
	 */
	private static String getUserName(HttpServletRequest request) {
		String name = ServletUtil.getHeaderIgnoreCase(request, ConfigBean.JPOM_SERVER_USER_NAME);
		name = CharsetUtil.convert(name, CharsetUtil.CHARSET_ISO_8859_1, CharsetUtil.CHARSET_UTF_8);
		name = StrUtil.emptyToDefault(name, StrUtil.DASHED);
		return URLUtil.decode(name, CharsetUtil.CHARSET_UTF_8);
	}

	/**
	 * 获取server 端操作人
	 *
	 * @return name
	 */
	public static String getNowUserName() {
		ServletRequestAttributes servletRequestAttributes = AbstractController.tryGetRequestAttributes();
		if (servletRequestAttributes == null) {
			return StrUtil.DASHED;
		}
		HttpServletRequest request = servletRequestAttributes.getRequest();
		return getUserName(request);
	}

	protected String getWorkspaceId() {
		return ServletUtil.getHeader(getRequest(), Const.WORKSPACEID_REQ_HEADER, CharsetUtil.CHARSET_UTF_8);
	}

	/**
	 * 获取拦截器中缓存的项目信息
	 *
	 * @return NodeProjectInfoModel
	 */
	protected NodeProjectInfoModel getProjectInfoModel() {
		NodeProjectInfoModel nodeProjectInfoModel = tryGetProjectInfoModel();
		Objects.requireNonNull(nodeProjectInfoModel, "获取项目信息失败");
		return nodeProjectInfoModel;
	}

	/**
	 * 根据 项目ID 获取项目信息
	 *
	 * @return NodeProjectInfoModel
	 */
	protected NodeProjectInfoModel getProjectInfoModel(String id) {
		NodeProjectInfoModel nodeProjectInfoModel = tryGetProjectInfoModel(id);
		Objects.requireNonNull(nodeProjectInfoModel, "获取项目信息失败");
		return nodeProjectInfoModel;
	}

	protected NodeProjectInfoModel tryGetProjectInfoModel() {
		String id = getParameter("id");
		return tryGetProjectInfoModel(id);
	}

	protected NodeProjectInfoModel tryGetProjectInfoModel(String id) {
		NodeProjectInfoModel nodeProjectInfoModel = null;
		if (StrUtil.isNotEmpty(id)) {
			nodeProjectInfoModel = projectInfoService.getItem(id);
		}
		return nodeProjectInfoModel;
	}
}
