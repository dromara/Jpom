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
package io.jpom.common.forward;

import cn.hutool.core.lang.Opt;
import cn.hutool.extra.spring.SpringUtil;
import io.jpom.common.Const;
import io.jpom.system.ServerConfig;
import top.jpom.transport.DataContentType;
import top.jpom.transport.IUrlItem;

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

    public DefaultUrlItem(NodeUrl nodeUrl, Integer timeout, String workspaceId, DataContentType dataContentType) {
        this.nodeUrl = nodeUrl;
        this.timeout = timeout;
        this.workspaceId = workspaceId;
        this.dataContentType = dataContentType;
    }

    @Override
    public String path() {
        return nodeUrl.getUrl();
    }

    @Override
    public Integer timeout() {
        if (nodeUrl.isFileTimeout()) {
            ServerConfig serverConfig = SpringUtil.getBean(ServerConfig.class);
            ServerConfig.NodeConfig configNode = serverConfig.getNode();
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
}
