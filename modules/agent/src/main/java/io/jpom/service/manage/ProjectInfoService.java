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
package io.jpom.service.manage;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import io.jpom.common.BaseAgentController;
import io.jpom.model.data.NodeProjectInfoModel;
import io.jpom.model.data.ProjectRecoverModel;
import io.jpom.service.BaseWorkspaceOptService;
import io.jpom.system.AgentConfigBean;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;

/**
 * 项目管理
 *
 * @author jiangzeyin
 */
@Service
public class ProjectInfoService extends BaseWorkspaceOptService<NodeProjectInfoModel> {
	@Resource
	private ProjectRecoverService projectRecoverService;

	public ProjectInfoService() {
		super(AgentConfigBean.PROJECT);
	}

//    public HashSet<String> getAllGroup() {
//        //获取所有分组
//        List<NodeProjectInfoModel> nodeProjectInfoModels = list();
//        HashSet<String> hashSet = new HashSet<>();
//        if (nodeProjectInfoModels == null) {
//            return hashSet;
//        }
//        for (NodeProjectInfoModel nodeProjectInfoModel : nodeProjectInfoModels) {
//            hashSet.add(nodeProjectInfoModel.getGroup());
//        }
//        return hashSet;
//    }


	/**
	 * 删除项目
	 *
	 * @param id 项目
	 */
	@Override
	public void deleteItem(String id) {
		NodeProjectInfoModel projectInfo = getItem(id);
		String userId = BaseAgentController.getNowUserName();
		super.deleteItem(id);
		// 添加回收记录
		ProjectRecoverModel projectRecoverModel = new ProjectRecoverModel(projectInfo);
		projectRecoverModel.setDelUser(userId);
		projectRecoverService.addItem(projectRecoverModel);
	}


	@Override
	public void addItem(NodeProjectInfoModel nodeProjectInfoModel) {
		nodeProjectInfoModel.setCreateTime(DateUtil.now());
		super.addItem(nodeProjectInfoModel);
	}

	/**
	 * 查看项目控制台日志文件大小
	 *
	 * @param nodeProjectInfoModel 项目
	 * @param copyItem             副本
	 * @return 文件大小
	 */
	public String getLogSize(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel.JavaCopyItem copyItem) {
		if (nodeProjectInfoModel == null) {
			return null;
		}
		File file = copyItem == null ? new File(nodeProjectInfoModel.getLog()) : nodeProjectInfoModel.getLog(copyItem);
		if (file.exists()) {
			long fileSize = file.length();
			if (fileSize <= 0) {
				return null;
			}
			return FileUtil.readableFileSize(fileSize);
		}
		return null;
	}
}
