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
package io.jpom.service.system;

import cn.hutool.core.util.ClassUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.common.Const;
import io.jpom.model.BaseWorkspaceModel;
import io.jpom.model.data.WorkspaceModel;
import io.jpom.service.h2db.BaseDbService;
import io.jpom.service.h2db.TableName;
import io.jpom.service.node.NodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * @author bwcx_jzy
 * @since 2021/12/3
 */
@Service
@Slf4j
public class WorkspaceService extends BaseDbService<WorkspaceModel> {

	/**
	 * 检查初始化 默认的工作空间
	 */
	public void checkInitDefault() {
		WorkspaceModel workspaceModel = super.getByKey(Const.WORKSPACE_DEFAULT_ID);
		if (workspaceModel != null) {
			return;
		}

		WorkspaceModel defaultWorkspace = new WorkspaceModel();
		defaultWorkspace.setId(Const.WORKSPACE_DEFAULT_ID);
		defaultWorkspace.setName("默认");
		defaultWorkspace.setDescription("系统默认的工作空间,不能删除");
		super.insert(defaultWorkspace);

		log.info("init created default workspace");
	}


	/**
	 * 将没有工作空间ID 的数据添加默认值
	 */
	public void convertNullWorkspaceId() {
		Set<Class<?>> classes = ClassUtil.scanPackage("io.jpom.model", BaseWorkspaceModel.class::isAssignableFrom);
		for (Class<?> aClass : classes) {
			TableName tableName = aClass.getAnnotation(TableName.class);
			if (tableName == null) {
				continue;
			}
			String sql = "update " + tableName.value() + " set workspaceId=? where workspaceId is null";
			NodeService nodeService = SpringUtil.getBean(NodeService.class);
			int execute = nodeService.execute(sql, Const.WORKSPACE_DEFAULT_ID);
			if (execute > 0) {
				log.info("convertNullWorkspaceId {} {}", tableName.value(), execute);
			}
		}

	}
}
