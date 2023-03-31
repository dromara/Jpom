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
package org.dromara.jpom.service.node.ssh;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.extra.ssh.Sftp;
import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.func.assets.model.MachineSshModel;
import org.dromara.jpom.func.assets.server.MachineSshServer;
import org.dromara.jpom.model.data.SshModel;
import org.dromara.jpom.service.h2db.BaseGroupService;
import org.dromara.jpom.system.extconf.BuildExtConfig;
import org.dromara.jpom.util.LogRecorder;
import org.dromara.jpom.util.MySftp;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author bwcx_jzy
 * @since 2021/12/4
 */
@Service
@Slf4j
public class SshService extends BaseGroupService<SshModel> implements Logger {

    @Resource
    @Lazy
    private MachineSshServer machineSshServer;
    private final BuildExtConfig buildExtConfig;

    public SshService(BuildExtConfig buildExtConfig) {
        this.buildExtConfig = buildExtConfig;
        JSch.setLogger(this);
    }

    @Override
    protected void fillSelectResult(SshModel data) {
        if (data == null) {
            return;
        }
        if (!StrUtil.startWithIgnoreCase(data.getPassword(), ServerConst.REF_WORKSPACE_ENV)) {
            // 隐藏密码字段
            data.setPassword(null);
        }
        //data.setPassword(null);
        data.setPrivateKey(null);
    }

    @Override
    protected void fillInsert(SshModel sshModel) {
        super.fillInsert(sshModel);
        sshModel.setHost(StrUtil.EMPTY);
        sshModel.setUser(StrUtil.EMPTY);
        sshModel.setPort(0);
    }

    /**
     * 获取 ssh 回话
     *
     * @param sshModel sshModel
     * @return session
     */
    public Session getSessionByModel(SshModel sshModel) {
        MachineSshModel machineSshModel = this.getMachineSshModel(sshModel);
        return machineSshServer.getSessionByModelNoFill(machineSshModel);
    }

    /**
     * 获取 ssh 回话
     *
     * @param sshModel sshModel
     * @return session
     */
    public Session getSessionByModel(MachineSshModel sshModel) {
        return machineSshServer.getSessionByModelNoFill(sshModel);
    }


    /**
     * 获取 ssh 配置对象
     *
     * @param sshModel sshModel
     * @return session
     */
    public MachineSshModel getMachineSshModel(SshModel sshModel) {
        MachineSshModel sshModel1 = machineSshServer.getByKey(sshModel.getMachineSshId(), false);
        Assert.notNull(sshModel1, "不存在对应的资产SSH");
        return sshModel1;
    }


    /**
     * 上传文件
     *
     * @param machineSshModel ssh
     * @param remotePath      远程路径
     * @param desc            文件夹或者文件
     */
    public void uploadDir(MachineSshModel machineSshModel, String remotePath, File desc) {
        Session session = null;
        ChannelSftp channel = null;
        // MachineSshModel machineSshModel = this.getMachineSshModel(sshModel);
        try {
            session = this.getSessionByModel(machineSshModel);
            channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
            try (Sftp sftp = new Sftp(channel, machineSshModel.charset(), machineSshModel.timeout())) {
                sftp.syncUpload(desc, remotePath);
            }
            //uploadDir(channel, remotePath, desc, sshModel.getCharsetT());
        } finally {
            JschUtil.close(channel);
            JschUtil.close(session);
        }
    }

    /**
     * 下载文件
     *
     * @param sshModel   实体
     * @param remoteFile 远程文件
     * @param save       文件对象
     * @throws IOException   io
     * @throws SftpException sftp
     */
    public void download(SshModel sshModel, String remoteFile, File save) throws IOException, SftpException {
        Session session = null;
        ChannelSftp channel = null;
        OutputStream output = null;
        try {
            session = this.getSessionByModel(sshModel);
            channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
            output = Files.newOutputStream(save.toPath());
            channel.get(remoteFile, output);
        } finally {
            IoUtil.close(output);
            JschUtil.close(channel);
            JschUtil.close(session);
        }
    }

    /**
     * 将ssh信息同步到其他工作空间
     *
     * @param ids            多给节点ID
     * @param nowWorkspaceId 当前的工作空间ID
     * @param workspaceId    同步到哪个工作空间
     */
    public void syncToWorkspace(String ids, String nowWorkspaceId, String workspaceId) {
        StrUtil.splitTrim(ids, StrUtil.COMMA)
                .forEach(id -> {
                    SshModel data = super.getByKey(id, false, entity -> entity.set("workspaceId", nowWorkspaceId));
                    Assert.notNull(data, "没有对应的ssh信息");
                    //
                    SshModel where = new SshModel();
                    where.setWorkspaceId(workspaceId);
                    where.setMachineSsh(data.getMachineSsh());
                    SshModel sshModel = super.queryByBean(where);
                    Assert.isNull(sshModel, "对应的工作空间已经存在当前 SSH 啦");
                    // 不存在则添加 信息
                    data.setId(null);
                    data.setWorkspaceId(workspaceId);
                    data.setCreateTimeMillis(null);
                    data.setModifyTimeMillis(null);
                    data.setModifyUser(null);
                    data.setHost(null);
                    data.setUser(null);
                    data.setPassword(null);
                    data.setPort(null);
                    data.setConnectType(null);
                    data.setCharset(null);
                    data.setPrivateKey(null);
                    data.setTimeout(null);
                    super.insert(data);
                });
    }

    @Override
    public boolean isEnabled(int level) {
        switch (level) {
            case DEBUG:
                return log.isDebugEnabled();
            case INFO:
                return log.isInfoEnabled();
            case WARN:
                return log.isWarnEnabled();
            case ERROR:
            case FATAL:
                return log.isErrorEnabled();
            default:
                log.warn("未知的 jsch 日志级别：{}", level);
                return false;
        }
    }

    @Override
    public void log(int level, String message) {
        switch (level) {
            case DEBUG:
                // info 日志太多 记录维 debug
            case INFO:
                log.debug(message);
                break;
            case WARN:
                if (StrUtil.isWrap(message, "Permanently added", "to the list of known hosts.")) {
                    // 避免过多日志
                    log.debug(message);
                } else {
                    log.warn(message);
                }
                break;
            case ERROR:
            case FATAL:
                log.error(message);
                break;
            default:
                log.warn("未知的 jsch 日志级别：{} {}", level, message);
        }
    }

    public long countByMachine(String machineSshId) {
        SshModel nodeModel = new SshModel();
        nodeModel.setMachineSshId(machineSshId);
        return this.count(nodeModel);
    }

    public void existsSsh(String workspaceId, String machineSshId) {
        //
        SshModel where = new SshModel();
        where.setWorkspaceId(workspaceId);
        where.setMachineSshId(machineSshId);
        SshModel data = this.queryByBean(where);
        Assert.isNull(data, () -> "对应工作空间已经存在该 ssh 啦:" + data.getName());
    }

    public boolean existsSsh2(String workspaceId, String machineSshId) {
        //
        SshModel where = new SshModel();
        where.setWorkspaceId(workspaceId);
        where.setMachineSshId(machineSshId);
        return this.exists(where);
    }

    public void insert(MachineSshModel machineSshModel, String workspaceId) {
        SshModel data = new SshModel();
        data.setWorkspaceId(workspaceId);
        data.setName(machineSshModel.getName());
        data.setGroup(machineSshModel.getGroupName());
        data.setMachineSshId(machineSshModel.getId());
        this.insert(data);
    }

    /**
     * 创建文件上传 进度
     *
     * @param logRecorder 日志记录器
     * @return 进度监听
     */
    public MySftp.ProgressMonitor createProgressMonitor(LogRecorder logRecorder) {
        Set<Integer> progressRangeList = ConcurrentHashMap.newKeySet((int) Math.floor((float) 100 / buildExtConfig.getLogReduceProgressRatio()));
        return new MySftp.ProgressMonitor() {
            @Override
            public void rest() {
                progressRangeList.clear();
            }

            @Override
            public void progress(String desc, long max, long now) {
                double progressPercentage = Math.floor(((float) now / max) * 100);
                int progressRange = (int) Math.floor(progressPercentage / buildExtConfig.getLogReduceProgressRatio());
                if (progressRangeList.add(progressRange)) {
                    //  total, progressSize
                    logRecorder.system("上传文件进度:{} {}/{} {} ", desc,
                            FileUtil.readableFileSize(now), FileUtil.readableFileSize(max),
                            NumberUtil.formatPercent(((float) now / max), 0)
                    );
                }
            }
        };
    }
}
