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
import io.jpom.model.BaseWorkspaceModel;
import io.jpom.service.h2db.TableName;
import io.jpom.util.StringUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 日志阅读
 *
 * @author bwcx_jzy
 * @since 2022/5/15
 */
@TableName(value = "LOG_READ", name = "日志阅读")
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
