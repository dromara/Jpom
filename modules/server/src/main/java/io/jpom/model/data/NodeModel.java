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
package io.jpom.model.data;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.BaseGroupModel;
import io.jpom.service.h2db.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;

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
public class NodeModel extends BaseGroupModel {

    private String url;
    private String loginName;
    private String loginPwd;
    private String name;

    /**
     * 节点协议
     */
    private String protocol;
    /**
     * 开启状态，如果关闭状态就暂停使用节点 1 启用
     */
    private Integer openStatus;
    /**
     * 节点超时时间
     */
    private Integer timeOut;
    /**
     * 绑定的sshId
     */
    private String sshId;
    /**
     * 锁定类型
     */
    private String unLockType;
    /**
     * 监控周期
     *
     * @see io.jpom.model.Cycle
     */
    @Deprecated
    private Integer cycle;
    /**
     * http 代理
     */
    private String httpProxy;
    /**
     * https 代理 类型
     */
    private String httpProxyType;

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

    /**
     * 获取 授权的信息
     *
     * @return sha1
     */
    public String toAuthorize() {
        return SecureUtil.sha1(loginName + "@" + loginPwd);
    }

    public String getRealUrl(NodeUrl nodeUrl) {
        return StrUtil.format("{}://{}{}", getProtocol().toLowerCase(), getUrl(), nodeUrl.getUrl());
    }

    /**
     * 获取节点的代理
     *
     * @return proxy
     */
    public Proxy proxy() {
        String httpProxy = this.getHttpProxy();
        if (StrUtil.isNotEmpty(httpProxy)) {
            List<String> split = StrUtil.splitTrim(httpProxy, StrUtil.COLON);
            String host = CollUtil.getFirst(split);
            int port = Convert.toInt(CollUtil.getLast(split), 0);
            String type = this.getHttpProxyType();
            Proxy.Type type1 = EnumUtil.fromString(Proxy.Type.class, type, Proxy.Type.HTTP);
            return new Proxy(type1, new InetSocketAddress(host, port));
        }
        return null;
    }

    /**
     * 创建代理
     *
     * @param type      代理类型
     * @param httpProxy 代理地址
     * @return proxy
     */
    public static Proxy crateProxy(String type, String httpProxy) {
        if (StrUtil.isNotEmpty(httpProxy)) {
            List<String> split = StrUtil.splitTrim(httpProxy, StrUtil.COLON);
            String host = CollUtil.getFirst(split);
            int port = Convert.toInt(CollUtil.getLast(split), 0);
            Proxy.Type type1 = EnumUtil.fromString(Proxy.Type.class, type, Proxy.Type.HTTP);
            return new Proxy(type1, new InetSocketAddress(host, port));
        }
        return null;
    }
}
