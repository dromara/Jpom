/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.log;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseWorkspaceModel;
import org.dromara.jpom.model.data.SshModel;

/**
 * ssh 终端执行日志
 *
 * @author bwcx_jzy
 * @since 2021/08/04
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "SSHTERMINALEXECUTELOG",
    nameKey = "i18n.ssh_terminal_execution_log.58f1", parents = SshModel.class)
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

    /**
     * 所属终端会话 id（录制开关开启时才有值）
     */
    private String sessionId;

    /**
     * 会话内命令序号
     */
    private Integer seq;

    /**
     * 命令来源：OSC133 由 shell integration 上报（命令原文与退出码可信），LEGACY 为按键推断
     */
    private String source;

    public void setUserAgent(String userAgent) {
        this.userAgent = StrUtil.maxLength(userAgent, 280);
    }
}
