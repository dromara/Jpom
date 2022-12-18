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
package io.jpom.model.node;

import io.jpom.model.BaseNodeModel;
import io.jpom.service.h2db.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author bwcx_jzy
 * @since 2021/12/5
 */
@TableName(value = "PROJECT_INFO", name = "项目信息")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProjectInfoCacheModel extends BaseNodeModel {

    private String projectId;

    private String name;

    private String mainClass;
    private String lib;
    /**
     * 白名单目录
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

    private String javaCopyItemList;
    /**
     * WebHooks
     */
    private String token;

    private String jdkId;

    private String runMode;
    /**
     * 节点分发项目，不允许在项目管理中编辑
     */
    private Integer outGivingProject;
    /**
     * -Djava.ext.dirs=lib -cp conf:run.jar
     * 填写【lib:conf】
     */
    private String javaExtDirsCp;
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
