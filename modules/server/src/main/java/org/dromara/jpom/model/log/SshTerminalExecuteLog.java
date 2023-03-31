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
package org.dromara.jpom.model.log;

import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.model.BaseWorkspaceModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.jpom.db.TableName;

/**
 * ssh 终端执行日志
 *
 * @author jiangzeyin
 * @since 2021/08/04
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "SSHTERMINALEXECUTELOG", name = "ssh 终端执行日志")
@Data
@NoArgsConstructor
public class SshTerminalExecuteLog extends BaseWorkspaceModel {
    /**
     * 操作ip
     */
    private String ip;
    /**
     * 用户ip
     */
    private String userId;
    /**
     * sshid
     */
    private String sshId;
    /**
     * 名称
     */
    private String sshName;
    /**
     * 执行的命令
     */
    private String commands;
    /**
     * 浏览器标识
     */
    private String userAgent;

    /**
     * 是否拒绝执行,true 运行执行，false 拒绝执行
     */
    private Boolean refuse;

    private String machineSshId;

    private String machineSshName;

    public void setUserAgent(String userAgent) {
        this.userAgent = StrUtil.maxLength(userAgent, 280);
    }
}
