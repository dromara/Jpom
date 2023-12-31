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
package org.dromara.jpom.service.manage;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.common.AgentConst;
import org.dromara.jpom.model.RunMode;
import org.dromara.jpom.model.data.NodeProjectInfoModel;
import org.dromara.jpom.service.BaseWorkspaceOptService;
import org.dromara.jpom.system.ExtConfigBean;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;

/**
 * 项目管理
 *
 * @author bwcx_jzy
 */
@Service
public class ProjectInfoService extends BaseWorkspaceOptService<NodeProjectInfoModel> {

    public ProjectInfoService() {
        super(AgentConst.PROJECT);
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
        Assert.notNull(item, "被软链的项目已经不存在啦，" + nodeProjectInfoModel.getLinkId());
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
            Assert.notNull(item, "软链项目已经不存在啦");
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
        Assert.hasText(id, "没有项目id");
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

}
