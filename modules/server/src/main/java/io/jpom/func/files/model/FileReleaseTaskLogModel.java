package io.jpom.func.files.model;

import io.jpom.model.BaseWorkspaceModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import top.jpom.h2db.TableName;

/**
 * @author bwcx_jzy
 * @since 2023/3/18
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "FILE_RELEASE_TASK_LOG", name = "文件发布任务记录")
@Data
@NoArgsConstructor
public class FileReleaseTaskLogModel extends BaseWorkspaceModel {

    /**
     * 任务名
     */
    private String name;
    /**
     * 任务id
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
     * 任务状态， 0 等待开始 1 进行中 2 成功 3 失败
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
