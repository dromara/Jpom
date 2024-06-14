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

import cn.hutool.core.util.ObjectUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseWorkspaceModel;
import org.dromara.jpom.model.data.MonitorModel;

/**
 * 监控日志
 *
 * @author bwcx_jzy
 * @since 2019/7/13
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "MONITORNOTIFYLOG",
    nameKey = "i18n.monitoring_notifications.de94", parents = MonitorModel.class)
@Data
public class MonitorNotifyLog extends BaseWorkspaceModel {


    private String nodeId;
    private String projectId;
    /**
     * 异常发生时间
     */
    private Long createTime;
    private String title;
    private String content;
    /**
     * 项目状态状态
     */
    private Boolean status;
    /**
     * 通知方式
     *
     * @see MonitorModel.NotifyType
     */
    private Integer notifyStyle;
    /**
     * 通知发送状态
     */
    private Boolean notifyStatus;
    /**
     * 监控id
     */
    private String monitorId;
    /**
     * 通知对象
     */
    private String notifyObject;
    /**
     * 通知异常消息
     */
    private String notifyError;

    public boolean status() {
        return ObjectUtil.defaultIfNull(status, false);
    }
}
