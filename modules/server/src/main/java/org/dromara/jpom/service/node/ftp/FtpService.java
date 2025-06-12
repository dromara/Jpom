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
import cn.hutool.extra.ftp.Ftp;
import com.jcraft.jsch.JSch;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
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
    public MachineFtpModel getMachineFtpModel(FtpModel ftpModel) {
        MachineFtpModel ftpModel1 = machineFtpServer.getByKey(ftpModel.getMachineFtpId(), false);
        Assert.notNull(ftpModel1, "不存在对应的资产FTP");
        return ftpModel1;
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

    public FtpModel getByMachineFtpId(String id) {
        FtpModel model = new FtpModel();
        model.setMachineFtpId(id);
        return queryByBean(model);
    }

    /**
     * FTP 上传文件（带进度）
     *
     * @param ftp              hutool 的 ftp 对象
     * @param localFile        本地文件
     * @param remotePath       远程文件完整路径
     * @param logRecorder      日志记录器
     * @param progressRatio    进度输出频率，例如 5 表示每 5% 输出一次
     */
    public void uploadWithProgress(Ftp ftp, File localFile, String remotePath,
                                          LogRecorder logRecorder, int progressRatio) throws IOException {
        long total = localFile.length();
        Set<Integer> progressRangeList = ConcurrentHashMap.newKeySet((int) (100f / progressRatio));

        try (InputStream in = Files.newInputStream(localFile.toPath())) {
            ProgressInputStream progressInputStream = new ProgressInputStream(in, total, now -> {
                double percent = Math.floor((double) now / total * 100);
                int range = (int) (percent / progressRatio);
                if (progressRangeList.add(range)) {
                    logRecorder.system(I18nMessageUtil.get("i18n.upload_progress_with_units.44ad"),
                        localFile.getName(),
                        FileUtil.readableFileSize(now),
                        FileUtil.readableFileSize(total),
                        String.format("%.0f%%", percent)
                    );
                }
            });

             remotePath = remotePath + StrUtil.SLASH + localFile.getName();
            ftp.getClient().storeFile(remotePath, progressInputStream);
        }
    }

    /**
     * 包装带进度的 InputStream
     */
    private static class ProgressInputStream extends InputStream {
        private final InputStream in;
        private final long totalSize;
        private long readSize = 0;
        private final Consumer<Long> onProgress;

        public ProgressInputStream(InputStream in, long totalSize, Consumer<Long> onProgress) {
            this.in = in;
            this.totalSize = totalSize;
            this.onProgress = onProgress;
        }

        @Override
        public int read() throws IOException {
            int b = in.read();
            if (b != -1) {
                readSize++;
                onProgress.accept(readSize);
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int count = in.read(b, off, len);
            if (count > 0) {
                readSize += count;
                onProgress.accept(readSize);
            }
            return count;
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }
}
