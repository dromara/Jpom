/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common.forward;

import cn.hutool.core.lang.Opt;
import cn.hutool.extra.spring.SpringUtil;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.configuration.NodeConfig;
import org.dromara.jpom.system.ServerConfig;
import org.dromara.jpom.transport.DataContentType;
import org.dromara.jpom.transport.IUrlItem;

import java.util.Map;
import java.util.Optional;

/**
 * @author bwcx_jzy
 * @since 2023/2/18
 */
public class DefaultUrlItem implements IUrlItem {
    private final NodeUrl nodeUrl;
    private final Integer timeout;
    private final String workspaceId;
    private final DataContentType dataContentType;
    private final Map<String, String> header;

    public DefaultUrlItem(NodeUrl nodeUrl, Integer timeout, String workspaceId, DataContentType dataContentType, Map<String, String> header) {
        this.nodeUrl = nodeUrl;
        this.timeout = timeout;
        this.workspaceId = workspaceId;
        this.dataContentType = dataContentType;
        this.header = header;
    }

    @Override
    public String path() {
        return nodeUrl.getUrl();
    }

    @Override
    public Integer timeout() {
        if (nodeUrl.isFileTimeout()) {
            ServerConfig serverConfig = SpringUtil.getBean(ServerConfig.class);
            NodeConfig configNode = serverConfig.getNode();
            return configNode.getUploadFileTimeout();
        } else {
            return Optional.of(nodeUrl.getTimeout())
                .flatMap(timeOut -> {
                    if (timeOut == 0) {
                        // 读取节点配置的超时时间
                        return Optional.ofNullable(timeout);
                    }
                    // 值 < 0  url 指定不超时
                    return timeOut > 0 ? Optional.of(timeOut) : Optional.empty();
                })
                .map(timeOut -> {
                    if (timeOut <= 0) {
                        return null;
                    }
                    // 超时时间不能小于 2 秒
                    return Math.max(timeOut, 2);
                })
                .orElse(null);
        }
    }

    @Override
    public String workspaceId() {
        return Opt.ofBlankAble(workspaceId).orElse(Const.WORKSPACE_DEFAULT_ID);
    }

    @Override
    public DataContentType contentType() {
        return dataContentType;
    }

    @Override
    public Map<String, String> header() {
        return header;
    }
}
