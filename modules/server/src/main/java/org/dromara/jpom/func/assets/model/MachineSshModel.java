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
package org.dromara.jpom.func.assets.model;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseGroupNameModel;
import org.dromara.jpom.model.data.SshModel;
import org.dromara.jpom.plugins.ISshInfo;

import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author bwcx_jzy
 * @see SshModel
 * @since 2023/2/25
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "MACHINE_SSH_INFO", name = "机器SSH信息")
@Data
@NoArgsConstructor
public class MachineSshModel extends BaseGroupNameModel implements ISshInfo {

    /**
     * 主机地址
     */
    private String host;
    /**
     * 端口
     */
    private Integer port;
    /**
     * 登录账号
     */
    private String user;
    /**
     * 账号密码，证书密码
     */
    private String password;
    /**
     * 编码格式
     */
    private String charset;
    /**
     * ssh 私钥
     */
    private String privateKey;

    private String connectType;
    /**
     * 节点超时时间
     */
    private Integer timeout;

    /**
     * ssh连接状态
     * <p>
     * 状态{0，无法连接，1 正常}
     */
    private Integer status;
    /**
     * 状态消息
     */
    private String statusMsg;

    private String allowEditSuffix;
    /**
     * 系统名称
     */
    private String osName;

    public void setOsName(String osName) {
        this.osName = StrUtil.maxLength(osName, 45);
    }

    /**
     * 机器主机名
     */
    private String hostName;

    public void setHostName(String hostName) {
        this.hostName = StrUtil.maxLength(hostName, 240);
    }

    /**
     * 系统版本
     */
    private String osVersion;
    /**
     * 总内存
     */
    private Long osMoneyTotal;
    /**
     * 硬盘总大小
     */
    private Long osFileStoreTotal;
    /**
     * CPU 型号
     */
    private String osCpuIdentifierName;
    /**
     * CPU数
     */
    private Integer osCpuCores;
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
    private Double osMaxOccupyDisk;
    /**
     * 占用磁盘 分区名
     */
    private String osMaxOccupyDiskName;
    /**
     * 负载
     */
    private String osLoadAverage;
    /**
     * 系统运行时间（自启动以来的时间）。
     * 自启动以来的毫秒数。
     */
    private Long osSystemUptime;
    /**
     * jpom 查询进程号
     */
    private Integer jpomAgentPid;
    /**
     * java 版本
     */
    private String javaVersion;
    /**
     * 服务器中的 docker 信息
     */
    private String dockerInfo;

    public MachineSshModel(String id) {
        setId(id);
    }

    public void allowEditSuffix(List<String> allowEditSuffix) {
        if (allowEditSuffix == null) {
            this.allowEditSuffix = null;
        } else {
            this.allowEditSuffix = JSONArray.toJSONString(allowEditSuffix);
        }
    }

    @Override
    public String id() {
        return getId();
    }

    @Override
    public String host() {
        return getHost();
    }

    @Override
    public String user() {
        return getUser();
    }

    @Override
    public String password() {
        return getPassword();
    }

    @Override
    public String privateKey() {
        return getPrivateKey();
    }

    @Override
    public int port() {
        return ObjectUtil.defaultIfNull(getPort(), 0);
    }

    @Override
    public ISshInfo.ConnectType connectType() {
        return EnumUtil.fromString(MachineSshModel.ConnectType.class, this.connectType, MachineSshModel.ConnectType.PASS);
    }

    /**
     * 超时时间
     *
     * @return 最小值 1 秒钟
     */
    @Override
    public int timeout() {
        if (this.timeout == null) {
            return (int) TimeUnit.SECONDS.toMillis(5);
        }
        return (int) TimeUnit.SECONDS.toMillis(Math.max(1, this.timeout));
    }

    @Override
    public Charset charset() {
        return CharsetUtil.parse(this.getCharset(), CharsetUtil.CHARSET_UTF_8);
    }
}
