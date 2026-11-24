/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.data;

import cn.hutool.core.annotation.PropIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseWorkspaceModel;

/**
 * SSH 项目（不依赖插件端的脚本型项目）
 * <p>
 * 项目真源完全在服务端：一棵 SSH 机器 + 工作目录 + 生命周期 YAML（start/status/stop/restart/reload）。
 *
 * @author bwcx_jzy
 * @since 2026/11/20
 */
@TableName(value = "SSH_PROJECT_INFO",
    nameKey = "i18n.ssh_project_info.2f61")
@Data
@EqualsAndHashCode(callSuper = true)
public class SshProjectModel extends BaseWorkspaceModel {

    /**
     * 项目名称
     */
    private String name;
    /**
     * 分组
     */
    private String group;
    /**
     * 资产 ssh id
     */
    private String machineSshId;
    /**
     * 工作空间下的 ssh 记录 id
     */
    private String sshId;
    /**
     * 工作目录
     */
    private String path;
    /**
     * 生命周期 yml 配置（start/status/stop/restart/reload + config.log）
     */
    private String dslContent;
    /**
     * 创建来源模板（仅展示，不参与运行）
     */
    private String templateKey;
    /**
     * 日志编码
     */
    private String logCharset;
    /**
     * 自动启动
     */
    private Boolean autoStart;
    /**
     * 触发器 token
     */
    private String triggerToken;
    /**
     * 排序
     */
    private Float sortValue;
    /**
     * 机器 SSH 展示名（关联资产/工作空间 SSH 记录的名称，不落库）
     */
    @PropIgnore
    private String sshName;
    /**
     * 最近一次状态：running / stopped / error
     */
    private String lastStatus;
    /**
     * 最近一次状态的主进程
     */
    private Integer lastPid;
    /**
     * 最近一次状态探测到的端口
     */
    private String lastPort;
    /**
     * 最近一次状态消息
     */
    private String lastStatusMsg;
    /**
     * 最近一次状态刷新时间
     */
    private Long lastRefreshTime;

    public boolean running() {
        return "running".equalsIgnoreCase(this.lastStatus);
    }
}
