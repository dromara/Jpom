/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.assets.model;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseGroupNameModel;
import org.dromara.jpom.transport.INodeInfo;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2023/2/18
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "MACHINE_NODE_INFO",
    nameKey = "i18n.machine_node_info.6a75")
@Data
public class MachineNodeModel extends BaseGroupNameModel implements INodeInfo {

    /**
     * 节点标识：反向建连隧道按该主键路由请求
     *
     * @return 机器节点主键
     */
    @Override
    public String id() {
        return this.getId();
    }

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
     * 状态{0，无法连接，1 正常, 2 授权信息错误, 3 状态码错误，4 资源监控异常}
     */
    private Integer status;
    /**
     * 状态消息
     */
    private String statusMsg;
    /**
     * 是否允许该机器承担构建任务（0 否，1 是）。
     * <p>
     * 只有 Go 插件端（manifest.capabilities 含 build）才能开启；开启后绑定到该机器的节点
     * 才可以被构建配置选为「构建节点」。默认 0，老数据不受影响。
     */
    private Integer buildEnabled;

    /**
     * 允许同时执行的构建任务数（&lt;=0 或空按 1 处理）。
     * <p>
     * 打包机 CPU/内存有限，默认 1 个并发，避免多个 mvn/xcodebuild 互相抢资源。
     */
    private Integer buildMaxConcurrency;

    /**
     * 插件端引擎（java / go），由保存资产与心跳采集时根据 /info manifest 自动识别后落库。
     * <p>
     * 引擎不会频繁变化，存下来供机器列表、「可作为构建节点」开关与构建节点选择直接使用，
     * 不再每次打开页面都去探测节点。空表示尚未识别（老数据升级后第一次心跳前）。
     */
    private String jpomEngine;

    /**
     * 传输方式。0 服务器拉取，1 节点机器推送
     * <p>
     * 注意：这是「状态采集方式」，不是通讯通道；通讯方式见 {@link #connectMode}。
     */
    private Integer transportMode;

    /**
     * 通讯方式（{@link MachineConnectMode}）：0 自动（默认，隧道优先、离线回退正向）、
     * 1 仅反向隧道、2 仅正向拉取。
     * <p>
     * 只有「仅反向隧道」时，节点地址/账号/密码/协议/代理/超时这些正向配置不参与通信
     * （界面禁用、保存不再强制校验），其它方式仍按正向配置连接。
     */
    private Integer connectMode;
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
     * 通讯授权摘要（sha1(账号@密码)），由插件端在反向隧道 HELLO 中上报后落库。
     * <p>
     * 只增字段：老数据为空时仍按 {@code jpomUsername/jpomPassword} 计算，行为不变；
     * 自动注册的机器资产靠它直接可用，不必再让运维手工补密码。
     */
    private String authorizeToken;
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
     * 扩展信息
     */
    private String extendInfo;
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
        if (StrUtil.isNotEmpty(this.authorizeToken)) {
            // 插件端在隧道 HELLO 中上报的摘要优先（自动注册/隧道部署场景）
            return this.authorizeToken;
        }
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

    /**
     * 清空不能下发到浏览器的凭据字段：authorizeToken 等价插件端通讯口令、jpomPassword 是机器登录口令。
     * 单一收口：列表/详情/下拉/节点列表副本等处一律调用本方法。
     */
    public void clearSensitive() {
        this.setAuthorizeToken(null);
        this.setJpomPassword(null);
    }

}
