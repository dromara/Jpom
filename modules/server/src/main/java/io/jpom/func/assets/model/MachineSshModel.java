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

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.EnumUtil;
import com.alibaba.fastjson2.JSONArray;
import io.jpom.model.BaseGroupNameModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import top.jpom.h2db.TableName;

import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author bwcx_jzy
 * @see io.jpom.model.data.SshModel
 * @since 2023/2/25
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "MACHINE_SSH_INFO", name = "机器SSH信息")
@Data
@NoArgsConstructor
public class MachineSshModel extends BaseGroupNameModel {

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

    public MachineSshModel.ConnectType connectType() {
        return EnumUtil.fromString(MachineSshModel.ConnectType.class, this.connectType, MachineSshModel.ConnectType.PASS);
    }

    /**
     * 超时时间
     *
     * @return 最小值 1 分钟
     */
    public int timeout() {
        if (this.timeout == null) {
            return (int) TimeUnit.SECONDS.toMillis(5);
        }
        return (int) TimeUnit.SECONDS.toMillis(Math.max(1, this.timeout));
    }

    public Charset charset() {
        return CharsetUtil.parse(this.getCharset(), CharsetUtil.CHARSET_UTF_8);
    }


    public enum ConnectType {
        /**
         * 账号密码
         */
        PASS,
        /**
         * 密钥
         */
        PUBKEY
    }

}
