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
package io.jpom.service.h2db;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.common.BaseServerController;
import io.jpom.model.BaseWorkspaceModel;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.UserBindWorkspaceModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.data.WorkspaceModel;
import io.jpom.service.user.UserBindWorkspaceService;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 工作空间 通用 service
 *
 * @author bwcx_jzy
 * @since 2021/8/13
 */
public abstract class BaseWorkspaceService<T extends BaseWorkspaceModel> extends BaseDbService<T> {

	/**
	 * 根据工作空间查询
	 *
	 * @param request 请求
	 * @return list
	 */
	public List<T> listByWorkspace(HttpServletRequest request) {
		String workspaceId = this.getCheckUserWorkspace(request);
		Entity entity = Entity.create();
		entity.set("workspaceId", workspaceId);
		List<Entity> entities = super.queryList(entity);
		return super.entityToBeanList(entities);
	}

	/**
	 * 根据主键ID + 请信息查询
	 *
	 * @param keyValue ID
	 * @param request  请求
	 * @return data
	 */
	public T getByKey(String keyValue, HttpServletRequest request) {
		String workspace = this.getCheckUserWorkspace(request);
		return super.getByKey(keyValue, true, entity -> entity.set("workspaceId", workspace));
	}

	/**
	 * 根据主键ID + 用户ID
	 *
	 * @param keyValue ID
	 * @param userId   用户ID
	 * @return data
	 */
	public T getByKey(String keyValue, String userId) {
		T byKey = super.getByKey(keyValue);
		this.checkUserWorkspace(byKey.getWorkspaceId(), userId);
		return byKey;
	}

	@Override
	protected void fillInsert(T t) {
		super.fillInsert(t);
		if (StrUtil.isEmpty(t.getWorkspaceId())) {
			// 自动绑定 工作空间ID
			HttpServletRequest request = BaseServerController.getRequestAttributes().getRequest();
			String workspaceId = getCheckUserWorkspace(request);
			t.setWorkspaceId(workspaceId);
		} else {
			// 检查权限
			this.checkUserWorkspace(t.getWorkspaceId());
		}
	}

	@Override
	public PageResultDto<T> listPage(HttpServletRequest request) {
		// 验证工作空间权限
		Map<String, String> paramMap = ServletUtil.getParamMap(request);
		String workspaceId = this.getCheckUserWorkspace(request);
		paramMap.put("workspaceId", workspaceId);
		return super.listPage(paramMap);
	}

	/**
	 * 删除
	 *
	 * @param keyValue 主键
	 * @param request  请求信息
	 * @return 影响行数
	 */
	public int delByKey(String keyValue, HttpServletRequest request) {
		String workspace = this.getCheckUserWorkspace(request);
		return super.delByKey(keyValue, entity -> entity.set("workspaceId", workspace));
	}

	/**
	 * 获取 工作空间ID 并判断是否有权限
	 *
	 * @param request 请求对象
	 * @return 工作空间ID
	 */
	public String getCheckUserWorkspace(HttpServletRequest request) {
		String workspaceId = ServletUtil.getHeader(request, WorkspaceModel.REQ_HEADER, CharsetUtil.CHARSET_UTF_8);
		Assert.hasText(workspaceId, "请选择工作空间");
		//
		this.checkUserWorkspace(workspaceId);
		return workspaceId;
	}

	/**
	 * 判断用户是否有对呀工作空间权限
	 *
	 * @param workspaceId 工作空间ID
	 */
	private void checkUserWorkspace(String workspaceId) {
		// 查询绑定的权限
		UserModel userModel = BaseServerController.getUserByThreadLocal();
		UserBindWorkspaceModel workspaceModel = new UserBindWorkspaceModel();
		workspaceModel.setId(UserBindWorkspaceModel.getId(userModel.getId(), workspaceId));
		UserBindWorkspaceService userBindWorkspaceService = SpringUtil.getBean(UserBindWorkspaceService.class);
		boolean exists = userBindWorkspaceService.exists(workspaceModel);
		Assert.state(exists, "没有对应的工作空间权限");
	}

	/**
	 * 判断用户是否有对呀工作空间权限
	 *
	 * @param workspaceId 工作空间ID
	 * @param userId      用户ID
	 */
	private void checkUserWorkspace(String workspaceId, String userId) {
		// 查询绑定的权限
		UserBindWorkspaceModel workspaceModel = new UserBindWorkspaceModel();
		workspaceModel.setId(UserBindWorkspaceModel.getId(userId, workspaceId));
		UserBindWorkspaceService userBindWorkspaceService = SpringUtil.getBean(UserBindWorkspaceService.class);
		boolean exists = userBindWorkspaceService.exists(workspaceModel);
		Assert.state(exists, "没有对应的工作空间权限");
	}
}
