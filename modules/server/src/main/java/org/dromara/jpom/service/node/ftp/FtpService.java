/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package org.dromara.jpom.service.node.ftp;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.SftpException;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.configuration.BuildExtConfig;
import org.dromara.jpom.func.assets.model.MachineFtpModel;
import org.dromara.jpom.func.assets.server.MachineFtpServer;
import org.dromara.jpom.model.data.FtpModel;
import org.dromara.jpom.plugins.JschLogger;
import org.dromara.jpom.service.h2db.BaseWorkspaceService;
import org.dromara.jpom.util.LogRecorder;
import org.dromara.jpom.util.MySftp;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;


/**
 * @author wxyShine
 * @since 2025/05/29
 */
@Service
@Slf4j
public class FtpService extends BaseWorkspaceService<FtpModel> {

    @Resource
    @Lazy
    private MachineFtpServer machineFtpServer;
    private final BuildExtConfig buildExtConfig;

    public FtpService(BuildExtConfig buildExtConfig) {
        this.buildExtConfig = buildExtConfig;
        JSch.setLogger(JschLogger.LOGGER);
    }

    @Override
    protected void fillSelectResult(FtpModel data) {
        if (data == null) {
            return;
        }
        if (!StrUtil.startWithIgnoreCase(data.getPassword(), ServerConst.REF_WORKSPACE_ENV)) {
            // 隐藏密码字段
            data.setPassword(null);
        }
    }

    @Override
    protected void fillInsert(FtpModel ftpModel) {
        super.fillInsert(ftpModel);
        ftpModel.setHost(StrUtil.EMPTY);
        ftpModel.setUser(StrUtil.EMPTY);
        ftpModel.setPort(0);
    }


    /**
     * 获取 ftp 配置对象
     *
     * @param ftpModel ftpModel
     * @return session
     */
    public MachineFtpModel getMachineSshModel(FtpModel ftpModel) {
        MachineFtpModel ftpModel1 = machineFtpServer.getByKey(ftpModel.getMachineFtpId(), false);
        Assert.notNull(ftpModel1, I18nMessageUtil.get("i18n.asset_ssh_not_exist.cd43"));
        return ftpModel1;
    }


    /**
     * 上传文件
     *
     * @param machineSshModel ssh
     * @param remotePath      远程路径
     * @param desc            文件夹或者文件
     */
/*    public void uploadDir(MachineSshModel machineSshModel, String remotePath, File desc) {
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
    }*/

    /**
     * 下载文件
     *
     * @param ftpModel   实体
     * @param remoteFile 远程文件
     * @param save       文件对象
     * @throws IOException   io
     * @throws SftpException sftp
     */
  /*  public void download(FtpModel ftpModel, String remoteFile, File save) throws IOException, SftpException {
        Session session = null;
        ChannelSftp channel = null;
        OutputStream output = null;
        try (Ftp ftp = new Ftp(machineFtpServer.toFtpConfig(machineFtpModel), ftpMode)) {
            session = this.getSessionByModel(ftpModel);
            channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
            output = Files.newOutputStream(save.toPath());
            channel.get(remoteFile, output);
        } finally {
            IoUtil.close(output);
            JschUtil.close(channel);
            JschUtil.close(session);
        }
    }*/

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
                FtpModel data = super.getByKey(id, false, entity -> entity.set("workspaceId", nowWorkspaceId));
                Assert.notNull(data, I18nMessageUtil.get("i18n.no_corresponding_ssh_info.d864"));
                //
                FtpModel where = new FtpModel();
                where.setWorkspaceId(workspaceId);
                where.setMachineFtp(data.getMachineFtp());
                FtpModel ftpModel = super.queryByBean(where);
                Assert.isNull(ftpModel, I18nMessageUtil.get("i18n.workspace_ssh_already_exists.ccc0"));
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
                data.setCharset(null);
                data.setTimeout(null);
                super.insert(data);
            });
    }

    public long countByMachine(String machineSshId) {
        FtpModel ftpModel = new FtpModel();
        ftpModel.setMachineFtpId(machineSshId);
        return this.count(ftpModel);
    }

    public void existsSsh(String workspaceId, String machineFtpId) {
        //
        FtpModel where = new FtpModel();
        where.setWorkspaceId(workspaceId);
        where.setMachineFtpId(machineFtpId);
        FtpModel data = this.queryByBean(where);
        Assert.isNull(data, () -> I18nMessageUtil.get("i18n.ssh_already_exists_in_workspace.569e") + data.getName());
    }

    public boolean existsFtp2(String workspaceId, String machineFtpId) {
        //
        FtpModel where = new FtpModel();
        where.setWorkspaceId(workspaceId);
        where.setMachineFtpId(machineFtpId);
        return this.exists(where);
    }

    public void insert(MachineFtpModel machineFtpModel, String workspaceId) {
        FtpModel data = new FtpModel();
        data.setWorkspaceId(workspaceId);
        data.setName(machineFtpModel.getName());
        data.setGroup(machineFtpModel.getGroupName());
        data.setMachineFtpId(machineFtpModel.getId());
        data.setMachineFtpId(machineFtpModel.getId());
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
                    logRecorder.system(I18nMessageUtil.get("i18n.upload_progress_with_units.44ad"), desc,
                        FileUtil.readableFileSize(now), FileUtil.readableFileSize(max),
                        NumberUtil.formatPercent(((float) now / max), 0)
                    );
                }
            }
        };
    }

    public FtpModel getByMachineSshId(String id) {
        FtpModel model = new FtpModel();
        model.setMachineFtpId(id);
        return queryByBean(model);
    }
}
