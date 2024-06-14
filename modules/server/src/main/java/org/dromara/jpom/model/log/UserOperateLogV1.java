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

/**
 * 用户操作日志
 *
 * @author bwcx_jzy
 * @since 2019/4/19
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "USEROPERATELOGV1",
    nameKey = "i18n.user_operation_log.2233", workspaceBind = 2)
@Data
public class UserOperateLogV1 extends BaseWorkspaceModel {
    /**
     * 操作ip
     */
    private String ip;
    /**
     * 用户ip
     */
    private String userId;
    /**
     * 节点id
     */
    private String nodeId;
    /**
     * 操作时间
     */
    private Long optTime;
    /**
     * 操作状态,业务状态码
     */
    private Integer optStatus;
    /**
     * 完整消息
     */
    private String resultMsg;
    /**
     * 请求参数
     */
    private String reqData;
    /**
     * 数据id
     */
    private String dataId;
    /**
     * 数据名称
     */
    private String dataName;
    /**
     * 浏览器标识
     */
    private String userAgent;

    private String classFeature;
    private String methodFeature;
    /**
     * 工作空间名称
     */
    private String workspaceName;

    private String username;
}
