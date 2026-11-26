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
 * SSH 终端会话（录制 / 回看）
 * <p>
 * 一次终端连接一条记录，录制内容存 asciicast v2 文件（data/ssh_terminal_record/...）。
 *
 * @author bwcx_jzy
 * @since 2026/11/26
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "SSHTERMINALSESSION",
    nameKey = "i18n.ssh_terminal_session.1864", parents = SshModel.class)
@Data
@NoArgsConstructor
public class SshTerminalSessionModel extends BaseWorkspaceModel {
    /**
     * 录制中
     */
    public static final int STATUS_RECORDING = 0;
    /**
     * 正常结束
     */
    public static final int STATUS_CLOSED = 1;
    /**
     * 异常中断（连接被强杀、录制写入失败等）
     */
    public static final int STATUS_ABORTED = 2;

    /**
     * 操作人 id
     */
    private String userId;
    /**
     * 工作空间内的 SSH id（可为空）
     */
    private String sshId;
    private String sshName;
    /**
     * SSH 资产 id
     */
    private String machineSshId;
    private String machineSshName;
    /**
     * 初始终端列
     */
    private Integer cols;
    /**
     * 初始终端行
     */
    private Integer rows;
    /**
     * 开始时间
     */
    private Long startTime;
    /**
     * 结束时间
     */
    private Long endTime;
    /**
     * 时长（毫秒）
     */
    private Long duration;
    /**
     * 录制文件大小（字节）
     */
    private Long bytes;
    /**
     * 命令条数
     */
    private Integer commandCount;
    /**
     * 状态 {@link #STATUS_RECORDING} / {@link #STATUS_CLOSED} / {@link #STATUS_ABORTED}
     */
    private Integer status;
    /**
     * 来源 ip
     */
    private String ip;
    /**
     * 浏览器标识
     */
    private String userAgent;
    /**
     * 录制文件名（相对 data/ssh_terminal_record 的路径，形如 20261126/xxx.cast）
     */
    private String castFile;
    /**
     * 录制器版本
     */
    private Integer recordVersion;

    public void setUserAgent(String userAgent) {
        this.userAgent = StrUtil.maxLength(userAgent, 280);
    }
}
