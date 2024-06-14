/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.outgiving;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseWorkspaceModel;
import org.dromara.jpom.util.StringUtil;

import java.util.List;

/**
 * 日志阅读
 *
 * @author bwcx_jzy
 * @since 2022/5/15
 */
@TableName(value = "LOG_READ",
    nameKey = "i18n.log_reading.a4c8")
@Data
@EqualsAndHashCode(callSuper = true)
public class LogReadModel extends BaseWorkspaceModel {

    /**
     * 名称
     */
    private String name;
    /**
     * 节点下的项目列表
     *
     * @see Item
     */
    private String nodeProject;
    /**
     * 缓存操作数据
     */
    private String cacheData;

    /**
     * {"op":"showlog","projectId":"python",
     * "search":true,"useProjectId":"python",
     * "useNodeId":"localhost",
     * "beforeCount":0,"afterCount":10,
     * "head":0,"tail":100,"first":"false",
     * "logFile":"/run.log"}
     */
    @Data
    public static class CacheDta {
        /**
         * 日志文件名称
         */
        private String logFile;
        /**
         * 显示关键词，后多少行
         */
        private Integer afterCount;
        /**
         * 显示关键词，前多少行
         */
        private Integer beforeCount;
        private Integer head;
        private Integer tail;
        private Boolean first;
        /**
         * 搜索关键词
         */
        private String keyword;
        /**
         * 使用等节点ID
         */
        private String useProjectId;
        private String useNodeId;
    }


    public List<Item> nodeProjectList() {
        return StringUtil.jsonConvertArray(nodeProject, Item.class);
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
    public Item getNodeProject(String nodeId, String projectId) {
        List<Item> thisPs = nodeProjectList();
        if (thisPs == null) {
            return null;
        }
        for (Item outGivingNodeProject1 : thisPs) {
            if (StrUtil.equalsIgnoreCase(outGivingNodeProject1.getProjectId(), projectId) && StrUtil.equalsIgnoreCase(outGivingNodeProject1.getNodeId(), nodeId)) {
                return outGivingNodeProject1;
            }
        }
        return null;
    }


    public static class Item extends BaseNodeProject {

    }
}
