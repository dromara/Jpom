/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseNodeGroupModel;

/**
 * @author bwcx_jzy
 * @since 2021/12/5
 */
@TableName(value = "PROJECT_INFO",
    nameKey = "i18n.project_info.6674")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProjectInfoCacheModel extends BaseNodeGroupModel {

    private String projectId;
    /**
     * 项目自动启动
     */
    private Boolean autoStart;

    private String name;

    private String mainClass;
    private String lib;
    /**
     * 授权目录
     */
    private String whitelistDirectory;
    /**
     * 日志目录
     */
    private String logPath;
    /**
     * jvm 参数
     */
    private String jvm;
    /**
     * java main 方法参数
     */
    private String args;
    /**
     * 副本
     */
    private String javaCopyItemList;
    /**
     * WebHooks
     */
    private String token;

    private String runMode;
    /**
     * 节点分发项目，不允许在项目管理中编辑
     */
    private Boolean outGivingProject;
    /**
     * -Djava.ext.dirs=lib -cp conf:run.jar
     * 填写【lib:conf】
     */
    private String javaExtDirsCp;
    /**
     * DSL 内容
     */
    private String dslContent;
    /**
     * 排序
     */
    private Float sortValue;

    /**
     * 触发器 token
     */
    private String triggerToken;

    @Override
    public String dataId() {
        return getProjectId();
    }

    @Override
    public void dataId(String id) {
        setProjectId(id);
    }
}
