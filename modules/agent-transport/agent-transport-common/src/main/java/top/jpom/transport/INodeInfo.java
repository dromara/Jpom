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
package top.jpom.transport;

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
