/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.data;

import cn.hutool.core.annotation.PropIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.model.BaseMachineModel;

/**
 * 节点实体
 *
 * @author bwcx_jzy
 * @see MachineNodeModel
 * @since 2019/4/16
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "NODE_INFO",
    nameKey = "i18n.node_info.2dcf")
@Data
@NoArgsConstructor
public class NodeModel extends BaseMachineModel {

    @Deprecated
    private String url;
    @Deprecated
    private String loginName;
    @Deprecated
    private String loginPwd;
    private String name;

    /**
     * 节点协议
     */
    @Deprecated
    private String protocol;
    /**
     * 开启状态，如果关闭状态就暂停使用节点 1 启用
     */
    private Integer openStatus;
    /**
     * 节点超时时间
     */
    @Deprecated
    private Integer timeOut;
    /**
     * 绑定的sshId
     */
    private String sshId;

    /**
     * http 代理
     */
    @Deprecated
    private String httpProxy;
    /**
     * https 代理 类型
     */
    @Deprecated
    private String httpProxyType;
    /**
     * 排序
     */
    private Float sortValue;

    @PropIgnore
    private MachineNodeModel machineNodeData;

    @PropIgnore
    private WorkspaceModel workspace;
    /**
     * jpom 项目数
     */
    private Integer jpomProjectCount;
    /**
     * jpom 脚本数据
     */
    private Integer jpomScriptCount;

    public boolean isOpenStatus() {
        return openStatus != null && openStatus == 1;
    }

    public NodeModel(String id) {
        this.setId(id);
    }

    public NodeModel(String id, String workspaceId) {
        this.setId(id);
        this.setWorkspaceId(workspaceId);
    }
}
