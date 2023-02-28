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
package io.jpom.service.node.ssh;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.extra.ssh.Sftp;
import com.jcraft.jsch.*;
import io.jpom.JpomApplication;
import io.jpom.common.ServerConst;
import io.jpom.func.assets.model.MachineSshModel;
import io.jpom.func.assets.server.MachineSshServer;
import io.jpom.model.data.SshModel;
import io.jpom.service.h2db.BaseGroupService;
import io.jpom.system.ExtConfigBean;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * @author bwcx_jzy
 * @since 2021/12/4
 */
@Service
@Slf4j
public class SshService extends BaseGroupService<SshModel> implements Logger {

    private final JpomApplication jpomApplication;
    @Resource
    @Lazy
    private MachineSshServer machineSshServer;

    public SshService(JpomApplication jpomApplication) {
        this.jpomApplication = jpomApplication;
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
        return machineSshServer.getByKey(sshModel.getMachineSshId(), false);
    }

    /**
     * 获取 ssh 中的 环境 版本
     *
     * @param sshModel ssh
     * @return 返回
     * @throws IOException IO
     */
    public String checkCommand(MachineSshModel sshModel, String command) throws IOException {
        // 检查  环境
        return this.exec(sshModel, "command -v " + command);
//		return CollUtil.join(commandResult, StrUtil.COMMA);
    }

    /**
     * 检查是否存在正在运行的进程
     *
     * @param sshModel ssh
     * @param tag      标识
     * @return true 存在运行中的
     * @throws IOException IO
     */
    public Integer checkSshRunPid(MachineSshModel sshModel, String tag) throws IOException {
        String ps = StrUtil.format("ps -ef | grep -v 'grep' | egrep {}", tag);
        // 运行中
        String exec = this.exec(sshModel, ps);
        List<String> result = StrUtil.splitTrim(exec, StrUtil.LF);
        return result.stream().map(s -> {
            List<String> split = StrUtil.splitTrim(s, StrUtil.SPACE);
            return Convert.toInt(CollUtil.get(split, 1));
        }).filter(Objects::nonNull).findAny().orElse(null);
    }

    /**
     * ssh 执行模版命令
     *
     * @param sshModel ssh
     * @param command  命令
     * @return 执行结果
     * @throws IOException io
     */
    public String exec(SshModel sshModel, String... command) throws IOException {
        MachineSshModel machineSshModel = this.getMachineSshModel(sshModel);
        return this.exec(machineSshModel, command);
    }

    /**
     * ssh 执行模版命令
     *
     * @param machineSshModel ssh
     * @param command         命令
     * @return 执行结果
     * @throws IOException io
     */
    public String exec(MachineSshModel machineSshModel, String... command) throws IOException {
        Charset charset = machineSshModel.charset();
        return this.exec(machineSshModel, (s, session) -> {
            // 执行命令
            String exec, error;
            try (ByteArrayOutputStream errStream = new ByteArrayOutputStream()) {
                exec = JschUtil.exec(session, s, charset, errStream);
                error = new String(errStream.toByteArray(), charset);
                if (StrUtil.isNotEmpty(error)) {
                    error = " 错误：" + error;
                }
            } catch (IOException e) {
                throw Lombok.sneakyThrow(e);
            }
            return exec + error;
        }, command);
    }

    /**
     * ssh 执行模版命令
     *
     * @param machineSshModel ssh
     * @param command         命令
     * @return 执行结果
     * @throws IOException io
     */
    public String exec(MachineSshModel machineSshModel, BiFunction<String, Session, String> function, String... command) throws IOException {
        if (ArrayUtil.isEmpty(command)) {
            return "没有任何命令";
        }
        Session session = null;
        Sftp sftp = null;
        try {
            String tempId = SecureUtil.sha1(machineSshModel.getId() + ArrayUtil.join(command, StrUtil.COMMA));
            File buildSsh = FileUtil.file(jpomApplication.getTempPath(), "ssh_temp", tempId + ".sh");
            InputStream sshExecTemplateInputStream = ExtConfigBean.getConfigResourceInputStream("/exec/template.sh");
            String sshExecTemplate = IoUtil.readUtf8(sshExecTemplateInputStream);

            StringBuilder stringBuilder = new StringBuilder(sshExecTemplate);
            for (String s : command) {
                stringBuilder.append(s).append(StrUtil.LF);
            }
            Charset charset = machineSshModel.charset();
            FileUtil.writeString(stringBuilder.toString(), buildSsh, charset);
            //
            session = this.getSessionByModel(machineSshModel);
            // 上传文件
            sftp = new Sftp(session, charset, machineSshModel.timeout());
            String path = StrUtil.format("{}/.jpom/", sftp.home());
            String destFile = StrUtil.format("{}{}.sh", path, IdUtil.fastSimpleUUID());
            sftp.mkDirs(path);
            sftp.upload(destFile, buildSsh);
            // 执行命令
            try {
                String commandSh = "bash " + destFile;
                return function.apply(commandSh, session);
            } finally {
                try {
                    // 删除 ssh 中临时文件
                    sftp.delFile(destFile);
                } catch (Exception ignored) {
                }
                // 删除临时文件
                FileUtil.del(buildSsh);
            }
        } finally {
            IoUtil.close(sftp);
            JschUtil.close(session);
        }
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

    public void insert(MachineSshModel machineSshModel, String workspaceId) {
        SshModel data = new SshModel();
        data.setWorkspaceId(workspaceId);
        data.setName(machineSshModel.getName());
        data.setGroup(machineSshModel.getGroupName());
        data.setMachineSshId(machineSshModel.getId());
        this.insert(data);
    }
}
