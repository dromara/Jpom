/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.log;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseWorkspaceModel;
import org.dromara.jpom.model.outgiving.OutGivingModel;
import org.dromara.jpom.model.outgiving.OutGivingNodeProject;

/**
 * 项目分发日志
 *
 * @author bwcx_jzy
 * @since 2019/7/19
 **/
@EqualsAndHashCode(callSuper = true)
@TableName(value = "OUTGIVINGLOG",
    nameKey = "i18n.distribute_log.c612", parents = OutGivingModel.class, workspaceBind = 3)
@Data
public class OutGivingLog extends BaseWorkspaceModel {
    /**
     * 分发id
     */
    private String outGivingId;
    /**
     * 状态
     *
     * @see OutGivingNodeProject.Status
     */
    private Integer status;
    /**
     * 开始时间
     */
    private Long startTime;
    /**
     * 结束时间
     */
    private Long endTime;
    /**
     * 处理消息
     */
    private String result;
    /**
     * 节点id
     */
    private String nodeId;
    /**
     * 项目id
     */
    private String projectId;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 进度信息
     */
    private Long progressSize;
    /**
     * 分发方式
     * upload: "手动上传",
     * download: "远程下载",
     * "build-trigger": "构建触发",
     * "use-build": "构建产物",
     */
    private String mode;
    /**
     * 数据
     */
    private String modeData;
}
