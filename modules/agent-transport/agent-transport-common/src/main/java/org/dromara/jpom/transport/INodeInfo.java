/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.transport;

import java.net.Proxy;

/**
 * 节点通讯的 接口
 *
 * @author bwcx_jzy
 * @since 2022/12/23
 */
public interface INodeInfo {

    /**
     * 节点名称
     *
     * @return 名称
     */
    String name();

    /**
     * 节点 url
     * <p>
     * HOST:PORT
     *
     * @return 节点 url
     */
    String url();

    /**
     * 协议
     *
     * @return http
     */
    String scheme();

    /**
     * 节点 授权信息
     * sha1(user@pwd)
     *
     * @return 用户
     */
    String authorize();

    /**
     * 节点通讯代理
     *
     * @return proxy
     */
    Proxy proxy();

    /**
     * 超时时间
     *
     * @return 超时时间 单位秒
     */
    Integer timeout();

    /**
     * 传输加密方式
     *
     * @return 传输加密方式 0 不加密 1 BASE64 2 AES
     */
    Integer transportEncryption();
}
