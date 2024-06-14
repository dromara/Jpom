/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.system.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseUserModifyDbModel;

/**
 * @author bwcx_jzy
 * @since 2023/8/19
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "CLUSTER_INFO",
    nameKey = "i18n.cluster_info.32e0")
@Data
public class ClusterInfoModel extends BaseUserModifyDbModel {
    /**
     * 集群Id
     */
    private String clusterId;
    /**
     * 集群名称
     */
    private String name;
    /**
     * 集群地址
     */
    private String url;
    /**
     * 集群关联的分组
     */
    private String linkGroup;
    /**
     * 最后心跳时间
     */
    private Long lastHeartbeat;
    /**
     * 主机名
     */
    private String localHostName;
    /**
     * jpom 版本
     */
    private String jpomVersion;
    /**
     * 集群地址状态消息
     */
    private String statusMsg;
}
