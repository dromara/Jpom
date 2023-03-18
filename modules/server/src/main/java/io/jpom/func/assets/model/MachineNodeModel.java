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
package io.jpom.func.assets.model;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import io.jpom.model.BaseGroupNameModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import top.jpom.h2db.TableName;
import top.jpom.transport.INodeInfo;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2023/2/18
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "MACHINE_NODE_INFO", name = "机器节点信息")
@Data
public class MachineNodeModel extends BaseGroupNameModel implements INodeInfo {
    /**
     * 机器主机名
     */
    private String hostName;

    public void setHostName(String hostName) {
        this.hostName = StrUtil.maxLength(hostName, 240);
    }

    /**
     * 机器的 IP （多个）
     */
    private String hostIpv4s;
    /**
     * 负载
     */
    private String osLoadAverage;
    /**
     * 系统运行时间（自启动以来的时间）。
     * 自启动以来的秒数。
     */
    private Long osSystemUptime;
    /**
     * 系统名称
     */
    private String osName;

    public void setOsName(String osName) {
        this.osName = StrUtil.maxLength(osName, 40);
    }

    /**
     * 系统版本
     */
    private String osVersion;
    /**
     * 硬件版本
     */
    private String osHardwareVersion;
    /**
     * CPU数
     */
    private Integer osCpuCores;
    /**
     * 总内存
     */
    private Long osMoneyTotal;
    /**
     * 交互总内存
     */
    private Long osSwapTotal;
    /**
     * 虚拟总内存
     */
    private Long osVirtualMax;
    /**
     * 硬盘总大小
     */
    private Long osFileStoreTotal;
    /**
     * CPU 型号
     */
    private String osCpuIdentifierName;
    /**
     * 占用cpu
     */
    private Double osOccupyCpu;
    /**
     * 占用内存 （总共）
     */
    private Double osOccupyMemory;
    /**
     * 占用磁盘
     */
    private Double osOccupyDisk;
    /**
     * 节点连接状态
     * <p>
     * 状态{0，无法连接，1 正常, 2 授权信息错误, 3 状态码错误}
     */
    private Integer status;
    /**
     * 状态消息
     */
    private String statusMsg;
    /**
     * 传输方式。0 服务器拉取，1 节点机器推送
     */
    private Integer transportMode;
    /**
     * jpom 通讯地址
     */
    private String jpomUrl;
    /**
     * 节点协议
     */
    private String jpomProtocol;
    /**
     * 通讯登录账号
     */
    private String jpomUsername;
    /**
     * 通讯登录密码
     */
    private String jpomPassword;
    /**
     * 超时时间
     */
    private Integer jpomTimeout;
    /**
     * http 代理
     */
    private String jpomHttpProxy;
    /**
     * http 代理 类型
     */
    private String jpomHttpProxyType;
    /**
     * jpom 版本号
     */
    private String jpomVersion;
    /**
     * jpom 启动时间
     */
    private Long jpomUptime;
    /**
     * Jpom 打包时间
     */
    private String jpomBuildTime;
    /**
     * 网络耗时（延迟）
     */
    private Integer networkDelay;
    /**
     * jpom 项目数
     */
    private Integer jpomProjectCount;
    /**
     * jpom 脚本数据
     */
    private Integer jpomScriptCount;
    /**
     * java 版本
     */
    private String javaVersion;
    /**
     * jvm 总内存
     */
    private Long jvmTotalMemory;
    /**
     * jvm 剩余内存
     */
    private Long jvmFreeMemory;
    /**
     * 模板节点 ，1 模板节点 0 非模板节点
     */
    private Boolean templateNode;
    /**
     * 安装 id
     */
    private String installId;

    /**
     * 传输加密方式 0 不加密 1 BASE64 2 AES
     */
    private Integer transportEncryption;

    @Override
    public String name() {
        return this.getName();
    }

    @Override
    public String url() {
        return this.getJpomUrl();
    }

    @Override
    public String scheme() {
        return getJpomProtocol();
    }

    /**
     * 获取 授权的信息
     *
     * @return sha1
     */
    @Override
    public String authorize() {
        return SecureUtil.sha1(this.jpomUsername + "@" + this.jpomPassword);
    }

    /**
     * 获取节点的代理
     *
     * @return proxy
     */
    @Override
    public Proxy proxy() {
        String httpProxy = this.getJpomHttpProxy();
        if (StrUtil.isNotEmpty(httpProxy)) {
            List<String> split = StrUtil.splitTrim(httpProxy, StrUtil.COLON);
            String host = CollUtil.getFirst(split);
            int port = Convert.toInt(CollUtil.getLast(split), 0);
            String type = this.getJpomHttpProxyType();
            Proxy.Type type1 = EnumUtil.fromString(Proxy.Type.class, type, Proxy.Type.HTTP);
            return new Proxy(type1, new InetSocketAddress(host, port));
        }
        return null;
    }

    @Override
    public Integer timeout() {
        return this.getJpomTimeout();
    }

    @Override
    public Integer transportEncryption() {
        // 需要兼容旧数据
        return ObjectUtil.defaultIfNull(this.getTransportEncryption(), 0);
    }
}
