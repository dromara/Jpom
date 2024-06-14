/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.manage;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.common.AgentConst;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.EnvironmentMapBuilder;
import org.dromara.jpom.model.RunMode;
import org.dromara.jpom.model.data.NodeProjectInfoModel;
import org.dromara.jpom.service.BaseWorkspaceOptService;
import org.dromara.jpom.service.system.AgentWorkspaceEnvVarService;
import org.dromara.jpom.system.ExtConfigBean;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;
import java.util.Map;

/**
 * 项目管理
 *
 * @author bwcx_jzy
 */
@Service
public class ProjectInfoService extends BaseWorkspaceOptService<NodeProjectInfoModel> {

    private final AgentWorkspaceEnvVarService agentWorkspaceEnvVarService;

    public ProjectInfoService(AgentWorkspaceEnvVarService agentWorkspaceEnvVarService) {
        super(AgentConst.PROJECT);
        this.agentWorkspaceEnvVarService = agentWorkspaceEnvVarService;
    }

    @Override
    public void updateItem(NodeProjectInfoModel data) {
        super.updateItem(data);
    }

    /**
     * 获取原始项目信息
     *
     * @param nodeProjectInfoModel 项目信息
     * @return model
     */
    public NodeProjectInfoModel resolveModel(NodeProjectInfoModel nodeProjectInfoModel) {
        RunMode runMode = nodeProjectInfoModel.getRunMode();
        if (runMode != RunMode.Link) {
            return nodeProjectInfoModel;
        }
        NodeProjectInfoModel item = this.getItem(nodeProjectInfoModel.getLinkId());
        Assert.notNull(item, I18nMessageUtil.get("i18n.soft_link_project_does_not_exist.8ad2") + nodeProjectInfoModel.getLinkId());
        return item;
    }

    /**
     * 解析lib路径
     *
     * @param nodeProjectInfoModel 项目
     * @return 项目的 lib 路径（文件路径）
     */
    public String resolveLibPath(NodeProjectInfoModel nodeProjectInfoModel) {
        RunMode runMode = nodeProjectInfoModel.getRunMode();
        if (runMode == RunMode.Link) {
            NodeProjectInfoModel item = this.getItem(nodeProjectInfoModel.getLinkId());
            Assert.notNull(item, I18nMessageUtil.get("i18n.soft_link_project_does_not_exist.4e4f"));
            return item.allLib();
        }
        return nodeProjectInfoModel.allLib();
    }

    /**
     * 解析lib路径
     *
     * @param nodeProjectInfoModel 项目
     * @return 项目的 lib 路径（文件路径）
     */
    public File resolveLibFile(NodeProjectInfoModel nodeProjectInfoModel) {
        String path = this.resolveLibPath(nodeProjectInfoModel);
        return FileUtil.file(path);
    }

    /**
     * 解析项目的日志路径
     *
     * @param nodeProjectInfoModel 项目
     * @return path
     */
    private File resolveLogFile(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel originalModel) {
        String id = nodeProjectInfoModel.getId();
        File logPath = this.resolveLogPath(nodeProjectInfoModel, originalModel);
        return FileUtil.file(logPath, id + ".log");
    }

    /**
     * 解析项目的日志路径
     *
     * @param nodeProjectInfoModel 项目
     * @return path
     */
    private File resolveLogPath(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel originalModel) {
        String id = nodeProjectInfoModel.getId();
        Assert.hasText(id, I18nMessageUtil.get("i18n.project_id_not_found.b87e"));
        String loggedPath = originalModel.logPath();
        if (StrUtil.isNotEmpty(loggedPath)) {
            return FileUtil.file(loggedPath, id);
        }
        String path = ExtConfigBean.getPath();
        return FileUtil.file(path, "project-log", id);
    }

    /**
     * 解析项目的日志路径
     *
     * @param nodeProjectInfoModel 项目
     * @return path
     */
    public String resolveAbsoluteLog(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel originalModel) {
        File file = this.resolveAbsoluteLogFile(nodeProjectInfoModel, originalModel);
        return FileUtil.getAbsolutePath(file);
    }

    /**
     * 解析项目的日志路径
     *
     * @param nodeProjectInfoModel 项目
     * @return path
     */
    public File resolveAbsoluteLogFile(NodeProjectInfoModel nodeProjectInfoModel) {
        NodeProjectInfoModel infoModel = this.resolveModel(nodeProjectInfoModel);
        return this.resolveLogFile(nodeProjectInfoModel, infoModel);
    }

    /**
     * 解析项目的日志路径
     *
     * @param nodeProjectInfoModel 项目
     * @return path
     */
    public File resolveAbsoluteLogFile(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel originalModel) {
        File file = this.resolveLogFile(nodeProjectInfoModel, originalModel);
        // auto create dir
        FileUtil.touch(file);
        return file;
    }

    /**
     * 解析日志备份路径
     *
     * @param nodeProjectInfoModel 项目
     * @return file
     */
    public File resolveLogBack(NodeProjectInfoModel nodeProjectInfoModel) {
        NodeProjectInfoModel infoModel = this.resolveModel(nodeProjectInfoModel);
        return this.resolveLogBack(nodeProjectInfoModel, infoModel);
    }

    /**
     * 解析日志备份路径
     *
     * @param nodeProjectInfoModel 项目
     * @return file
     */
    public File resolveLogBack(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel originalModel) {
        File logPath = this.resolveLogPath(nodeProjectInfoModel, originalModel);
        return FileUtil.file(logPath, "back");
    }

    /**
     * 获取环境变量
     *
     * @param workspaceId 工作空间ID
     * @return map
     */
    public Map<String, String> getEnv(String workspaceId) {
        EnvironmentMapBuilder env = agentWorkspaceEnvVarService.getEnv(workspaceId);
        return env.environment();
    }
}
