/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
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
@TableName(value = "FILE_RELEASE_TASK_LOG",
    nameKey = "i18n.file_publish_task_record.edc4", parents = FileStorageModel.class)
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
     * 文件来源类型
     * 1 文件中心
     * 2 静态文件
     */
    private Integer fileType;
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
