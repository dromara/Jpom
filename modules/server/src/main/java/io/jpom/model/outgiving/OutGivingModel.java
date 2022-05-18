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
package io.jpom.model.outgiving;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import io.jpom.model.BaseEnum;
import io.jpom.model.BaseWorkspaceModel;
import io.jpom.service.h2db.TableName;
import io.jpom.util.StringUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 分发实体
 *
 * @author jiangzeyin
 * @since 2019/4/21
 */
@TableName(value = "OUT_GIVING", name = "节点分发")
@Data
@EqualsAndHashCode(callSuper = true)
public class OutGivingModel extends BaseWorkspaceModel {
    /**
     * 名称
     */
    private String name;
    /**
     * 分发间隔时间
     */
    private Integer intervalTime;
    /**
     * 节点下的项目列表
     */
    private String outGivingNodeProjectList;
    /**
     * 分发后的操作
     */
    private Integer afterOpt;
    /**
     * 是否清空旧包发布
     */
    private Boolean clearOld;
    /**
     * 是否为单独创建的分发项目
     */
    private Boolean outGivingProject;

    /**
     * 状态
     */
    private Integer status;


    public boolean clearOld() {
        return clearOld != null && clearOld;
    }

    public boolean outGivingProject() {
        return outGivingProject != null && outGivingProject;
    }


    public List<OutGivingNodeProject> outGivingNodeProjectList() {
        return StringUtil.jsonConvertArray(outGivingNodeProjectList, OutGivingNodeProject.class);
    }

    public void outGivingNodeProjectList(List<OutGivingNodeProject> outGivingNodeProjectList) {
        if (outGivingNodeProjectList == null) {
            this.outGivingNodeProjectList = null;
        } else {
            this.outGivingNodeProjectList = JSON.toJSONString(outGivingNodeProjectList);
        }
    }

    /**
     * 判断是否包含某个项目id
     *
     * @param projectId 项目id
     * @return true 包含
     */
    public boolean checkContains(String nodeId, String projectId) {
        return getNodeProject(nodeId, projectId) != null;
    }

    /**
     * 获取节点的项目信息
     *
     * @param nodeId    节点
     * @param projectId 项目
     * @return outGivingNodeProject
     */
    public OutGivingNodeProject getNodeProject(String nodeId, String projectId) {
        List<OutGivingNodeProject> thisPs = outGivingNodeProjectList();
        return getNodeProject(thisPs, nodeId, projectId);
    }

    /**
     * 从指定数组中获取对应信息
     *
     * @param outGivingNodeProjects 节点项目列表
     * @param nodeId                节点id
     * @param projectId             项目id
     * @return 实体
     */
    private OutGivingNodeProject getNodeProject(List<OutGivingNodeProject> outGivingNodeProjects, String nodeId, String projectId) {
        if (outGivingNodeProjects == null) {
            return null;
        }
        for (OutGivingNodeProject outGivingNodeProject1 : outGivingNodeProjects) {
            if (StrUtil.equalsIgnoreCase(outGivingNodeProject1.getProjectId(), projectId) && StrUtil.equalsIgnoreCase(outGivingNodeProject1.getNodeId(), nodeId)) {
                return outGivingNodeProject1;
            }
        }
        return null;
    }

    /**
     * 获取已经删除的节点项目
     *
     * @param newsProject 要比较的分发项目
     * @return 已经删除过的
     */
    public List<OutGivingNodeProject> getDelete(List<OutGivingNodeProject> newsProject) {
        List<OutGivingNodeProject> old = outGivingNodeProjectList();
        if (old == null || old.isEmpty()) {
            return null;
        }
        List<OutGivingNodeProject> delete = new ArrayList<>();
        old.forEach(outGivingNodeProject -> {
            if (getNodeProject(newsProject, outGivingNodeProject.getNodeId(), outGivingNodeProject.getProjectId()) != null) {
                return;
            }
            delete.add(outGivingNodeProject);
        });
        return delete;
    }

    /**
     * 状态
     */
    @Getter
    public enum Status implements BaseEnum {
        /**
         *
         */
        NO(0, "未分发"),
        ING(1, "分发中"),
        DONE(2, "分发结束"),
        ;
        private final int code;
        private final String desc;

        Status(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }
}
