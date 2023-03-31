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
package org.dromara.jpom.model.data;

import cn.hutool.core.annotation.PropIgnore;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.model.BaseMachineModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.jpom.db.TableName;

/**
 * 节点实体
 *
 * @author jiangzeyin
 * @since 2019/4/16
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "NODE_INFO", name = "节点信息")
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
