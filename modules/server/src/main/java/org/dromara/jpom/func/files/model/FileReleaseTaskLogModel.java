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
package org.dromara.jpom.func.files.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseWorkspaceModel;

/**
 * @author bwcx_jzy
 * @since 2023/3/18
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "FILE_RELEASE_TASK_LOG", name = "文件发布任务记录", parents = FileStorageModel.class)
@Data
@NoArgsConstructor
public class FileReleaseTaskLogModel extends BaseWorkspaceModel {
    /**
     * 父级任务id
     */
    public static final String TASK_ROOT_ID = "task-root";

    /**
     * 任务名
     */
    private String name;
    /**
     * 任务id
     *
     * @see FileReleaseTaskLogModel#TASK_ROOT_ID
     */
    private String taskId;
    /**
     * 文件 id
     *
     * @see FileStorageModel#getId()
     */
    private String fileId;
    /**
     * 任务类型 0 ssh 1 节点
     */
    private Integer taskType;
    /**
     * 发布路径
     */
    private String releasePath;
    /**
     * 任务关联的数据id
     */
    private String taskDataId;
    /**
     * 任务状态， 0 等待开始 1 进行中 2 任务结束 3 失败 4 取消任务
     */
    private Integer status;
    /**
     * 状态描述
     */
    private String statusMsg;
    /**
     * 发布之前的脚本
     */
    private String beforeScript;
    /**
     * 发布后的脚本
     */
    private String afterScript;
}
