/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.socket.handler.record;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.func.assets.model.MachineSshModel;
import org.dromara.jpom.model.data.SshModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.dblog.SshTerminalSessionService;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;

/**
 * 终端录制器工厂（装配点）
 * <p>
 * 开关来自 SSH 资产 {@code terminalRecord}：每次建立终端连接读取一次资产最新值，
 * 因此资产上切换开关后下一个新连接立即生效，无需重启。
 * <p>
 * 新增终端类型（如 Docker exec）时：新增一个实现类 + 在此按类型分发一行，调用方零改动。
 *
 * @author bwcx_jzy
 * @since 2026/11/26
 */
@Slf4j
@Component
public class TerminalRecorderFactory {

    private final SshTerminalSessionService sshTerminalSessionService;

    public TerminalRecorderFactory(SshTerminalSessionService sshTerminalSessionService) {
        this.sshTerminalSessionService = sshTerminalSessionService;
    }

    /**
     * 创建录制器
     *
     * @param machine    SSH 资产（开关来源）
     * @param ssh        工作空间内的 SSH（可为空）
     * @param user       操作人
     * @param ip         来源 ip
     * @param userAgent  浏览器标识
     * @param cols       初始列
     * @param rows       初始行
     * @param charset    终端编码
     * @return 录制器，未开启或创建失败返回 {@link TerminalRecorder#NOOP}
     */
    public TerminalRecorder create(MachineSshModel machine, SshModel ssh, UserModel user, String ip, String userAgent,
                                   int cols, int rows, Charset charset) {
        if (machine == null || !Boolean.TRUE.equals(machine.getTerminalRecord())) {
            return TerminalRecorder.NOOP;
        }
        try {
            TerminalRecordContext context = new TerminalRecordContext();
            context.setSessionId(IdUtil.fastSimpleUUID());
            context.setWorkspaceId(ssh != null ? ssh.getWorkspaceId() : Const.WORKSPACE_DEFAULT_ID);
            context.setUserId(user == null ? null : user.getId());
            if (ssh != null) {
                context.setSshId(ssh.getId());
                context.setSshName(ssh.getName());
            }
            context.setMachineSshId(machine.getId());
            context.setMachineSshName(machine.getName());
            context.setIp(ip);
            context.setUserAgent(userAgent);
            context.setCols(cols <= 0 ? 80 : cols);
            context.setRows(rows <= 0 ? 24 : rows);
            Charset useCharset = charset == null ? Charset.forName("UTF-8") : charset;
            return AsciicastTerminalRecorder.start(context, useCharset, sshTerminalSessionService);
        } catch (Exception e) {
            log.warn(I18nMessageUtil.get("i18n.ssh_terminal_session_write_fail.9a5c"), StrUtil.emptyToDefault(e.getMessage(), e.getClass().getSimpleName()));
            return TerminalRecorder.NOOP;
        }
    }
}
