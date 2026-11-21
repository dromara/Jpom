/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.sshproject;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschUtil;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.func.assets.model.MachineSshModel;
import org.dromara.jpom.func.assets.server.MachineSshServer;
import cn.hutool.extra.spring.SpringUtil;

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * SSH 项目日志跟随器
 * <p>
 * 独立开一条 `tail -n N -F` 通道（不复用执行通道，避免一次操作把日志跟随打断）；
 * 关闭时断开通道与会话，避免线程/连接泄漏。
 *
 * @author bwcx_jzy
 * @since 2026/11/20
 */
@Slf4j
public class SshProjectLogTailer implements AutoCloseable {

    private final Session session;
    private final ChannelExec channel;
    private volatile boolean closed;

    private SshProjectLogTailer(Session session, ChannelExec channel) {
        this.session = session;
        this.channel = channel;
    }

    /**
     * 开始跟随
     *
     * @param machine   资产
     * @param filePath  远端文件绝对路径
     * @param initLines 初始回放行数（0 表示只跟随）
     * @param handler   行回调
     * @return 跟随器
     * @throws Exception 建立失败
     */
    public static SshProjectLogTailer start(MachineSshModel machine, String filePath, int initLines, LineHandler handler) throws Exception {
        MachineSshServer machineSshServer = SpringUtil.getBean(MachineSshServer.class);
        Session session = machineSshServer.getSessionByModel(machine);
        Charset charset = machine.charset();
        String command = StrUtil.format("tail -n {} -F {}", Math.max(initLines, 0), SshProjectShell.quote(filePath));
        ChannelExec channel = (ChannelExec) JschUtil.createChannel(session, ChannelType.EXEC);
        channel.setCommand(StrUtil.bytes(command, charset));
        channel.setInputStream(null);
        InputStream in = channel.getInputStream();
        channel.connect(Math.max(machine.timeout(), 5000));
        SshProjectLogTailer tailer = new SshProjectLogTailer(session, channel);
        Thread thread = new Thread(() -> {
            try {
                IoUtil.readLines(in, charset, handler);
            } catch (Exception e) {
                if (!tailer.closed) {
                    log.warn("ssh project log tail stopped: {}", e.getMessage());
                }
            }
        }, "ssh-project-log-" + machine.getId());
        thread.setDaemon(true);
        thread.start();
        return tailer;
    }

    @Override
    public void close() {
        this.closed = true;
        try {
            JschUtil.close(channel);
        } catch (Exception ignored) {
            // ignore
        }
        try {
            JschUtil.close(session);
        } catch (Exception ignored) {
            // ignore
        }
    }
}
