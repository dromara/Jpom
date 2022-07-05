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
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.*;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschRuntimeException;
import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.extra.ssh.Sftp;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import io.jpom.model.data.SshModel;
import io.jpom.service.h2db.BaseWorkspaceService;
import io.jpom.system.ConfigBean;
import io.jpom.util.JschUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * @author bwcx_jzy
 * @since 2021/12/4
 */
@Service
public class SshService extends BaseWorkspaceService<SshModel> {

    @Override
    protected void fillSelectResult(SshModel data) {
        if (data == null) {
            return;
        }
        data.setPassword(null);
        data.setPrivateKey(null);
    }

//    /**
//     * 获取 ssh 回话
//     *
//     * @param sshId id
//     * @return session
//     */
//    public static Session getSession(String sshId) {
//        SshModel sshModel = SpringUtil.getBean(SshService.class).getByKey(sshId, false);
//        return getSessionByModel(sshModel);
//    }

    /**
     * 获取 ssh 回话
     *
     * @param sshModel sshModel
     * @return session
     */
    public static Session getSessionByModel(SshModel sshModel) {
        Session session = null;
        int timeout = sshModel.timeout();
        SshModel.ConnectType connectType = sshModel.connectType();
        if (connectType == SshModel.ConnectType.PASS) {
            session = JschUtil.openSession(sshModel.getHost(), sshModel.getPort(), sshModel.getUser(), sshModel.getPassword(), timeout);

        } else if (connectType == SshModel.ConnectType.PUBKEY) {
            File rsaFile = null;
            String privateKey = sshModel.getPrivateKey();
            if (StrUtil.startWith(privateKey, URLUtil.FILE_URL_PREFIX)) {
                String rsaPath = StrUtil.removePrefix(privateKey, URLUtil.FILE_URL_PREFIX);
                rsaFile = FileUtil.file(rsaPath);
            } else if (StrUtil.startWith(privateKey, JschUtils.HEADER)) {
                // 直接采用 private key content 登录，无需写入文件
                session = JschUtils.createSession(sshModel.getHost(),
                    sshModel.getPort(),
                    sshModel.getUser(),
                    StrUtil.trim(privateKey),
                    sshModel.password());
            } else if (StrUtil.isEmpty(privateKey)) {
                File home = FileUtil.getUserHomeDir();
                Assert.notNull(home, "用户目录没有找到");
                File identity = FileUtil.file(home, ".ssh", "identity");
                rsaFile = FileUtil.isFile(identity) ? identity : null;
                File idRsa = FileUtil.file(home, ".ssh", "id_rsa");
                rsaFile = FileUtil.isFile(idRsa) ? idRsa : rsaFile;
                File idDsa = FileUtil.file(home, ".ssh", "id_dsa");
                rsaFile = FileUtil.isFile(idDsa) ? idDsa : rsaFile;
                Assert.notNull(rsaFile, "用户目录没有找到私钥信息");
            } else {
                //这里的实现，用于把 private key 写入到一个临时文件中，此方式不太采取
                File tempPath = ConfigBean.getInstance().getTempPath();
                String sshFile = StrUtil.emptyToDefault(sshModel.getId(), IdUtil.fastSimpleUUID());
                rsaFile = FileUtil.file(tempPath, "ssh", sshFile);
                FileUtil.writeString(privateKey, rsaFile, CharsetUtil.UTF_8);
            }
            // 如果是私钥正文，则 session 已经初始化了
            if (session == null) {
                // 简要私钥文件是否存在
                Assert.state(FileUtil.isFile(rsaFile), "私钥文件不存在：" + FileUtil.getAbsolutePath(rsaFile));
                session = JschUtil.createSession(sshModel.getHost(),
                    sshModel.getPort(), sshModel.getUser(), FileUtil.getAbsolutePath(rsaFile), sshModel.password());
            }
            try {
                session.connect(timeout);
            } catch (JSchException e) {
                throw new JschRuntimeException(e);
            }
        } else {
            throw new IllegalArgumentException("不支持的模式");
        }
        try {
            session.setServerAliveInterval((int) TimeUnit.SECONDS.toMillis(5));
            session.setServerAliveCountMax(5);
        } catch (JSchException ignored) {
        }
        return session;
    }

    /**
     * 检查是否存在正在运行的进程
     *
     * @param sshModel ssh
     * @param tag      标识
     * @return true 存在运行中的
     * @throws IOException IO
     */
    public boolean checkSshRun(SshModel sshModel, String tag) throws IOException {
        return this.checkSshRunPid(sshModel, tag) != null;
    }

    /**
     * 获取 ssh 中的 环境 版本
     *
     * @param sshModel ssh
     * @return 返回
     * @throws IOException IO
     */
    public String checkCommand(SshModel sshModel, String command) throws IOException {
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
    public Integer checkSshRunPid(SshModel sshModel, String tag) throws IOException {
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
        Charset charset = sshModel.charset();
        return this.exec(sshModel, (s, session) -> {
            // 执行命令
            String exec, error;
            try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                exec = JschUtil.exec(session, s, charset, stream);
                error = new String(stream.toByteArray(), charset);
                if (StrUtil.isNotEmpty(error)) {
                    error = " 错误：" + error;
                }
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            return exec + error;
        }, command);
    }

    /**
     * ssh 执行模版命令
     *
     * @param sshModel ssh
     * @param command  命令
     * @return 执行结果
     * @throws IOException io
     */
    public String exec(SshModel sshModel, BiFunction<String, Session, String> function, String... command) throws IOException {
        if (ArrayUtil.isEmpty(command)) {
            return "没有任何命令";
        }
        Session session = null;
        InputStream sshExecTemplateInputStream = null;
        Sftp sftp = null;
        try {
            String tempId = SecureUtil.sha1(sshModel.getId() + ArrayUtil.join(command, StrUtil.COMMA));
            File buildSsh = FileUtil.file(ConfigBean.getInstance().getTempPath(), "ssh_temp", tempId + ".sh");
            String sshExecTemplate;
            if (ArrayUtil.contains(command, "#disabled-template-auto-evn")) {
                sshExecTemplate = StrUtil.EMPTY;
            } else {
                sshExecTemplateInputStream = ResourceUtil.getStream("classpath:/bin/execTemplate.sh");
                sshExecTemplate = IoUtil.readUtf8(sshExecTemplateInputStream);
            }
            StringBuilder stringBuilder = new StringBuilder(sshExecTemplate);
            for (String s : command) {
                stringBuilder.append(s).append(StrUtil.LF);
            }
            Charset charset = sshModel.charset();
            FileUtil.writeString(stringBuilder.toString(), buildSsh, charset);
            //
            session = getSessionByModel(sshModel);
            // 上传文件
            sftp = new Sftp(session, charset, sshModel.timeout());
            String home = sftp.home();
            String path = home + "/.jpom/";
            String destFile = path + IdUtil.fastSimpleUUID() + ".sh";
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
            IoUtil.close(sshExecTemplateInputStream);
            JschUtil.close(session);
        }
    }

//    /**
//     * 执行命令
//     *
//     * @param sshModel ssh
//     * @param command  命令
//     * @return 结果
//     * @throws IOException   io
//     * @throws JSchException jsch
//     */
//    public String exec(SshModel sshModel, String command) throws IOException, JSchException {
//        Session session = null;
//        try {
//            session = getSessionByModel(sshModel);
//            return exec(session, sshModel.getCharsetT(), command);
//        } finally {
//            JschUtil.close(session);
//        }
//    }
//
//    private String exec(Session session, Charset charset, String command) throws IOException, JSchException {
//        ChannelExec channel = null;
//        try {
//            channel = (ChannelExec) JschUtil.createChannel(session, ChannelType.EXEC);
//            // 添加环境变量
//            channel.setCommand(ServerExtConfigBean.getInstance().getSshInitEnv() + " && " + command);
//            InputStream inputStream = channel.getInputStream();
//            InputStream errStream = channel.getErrStream();
//            channel.connect();
//            // 读取结果
//            String result = IoUtil.read(inputStream, charset);
//            //
//            String error = IoUtil.read(errStream, charset);
//            return result + error;
//        } finally {
//            JschUtil.close(channel);
//        }
//    }

//	private List<String> execCommand(SshModel sshModel, String command) throws IOException, JSchException {
//		Session session = null;
//		ChannelExec channel = null;
//		try {
//			session = getSessionByModel(sshModel);
//			channel = (ChannelExec) JschUtil.createChannel(session, ChannelType.EXEC);
//			// 添加环境变量
//			channel.setCommand(ServerExtConfigBean.getInstance().getSshInitEnv() + " && " + command);
//			InputStream inputStream = channel.getInputStream();
//			InputStream errStream = channel.getErrStream();
//			channel.connect();
//			Charset charset = sshModel.getCharsetT();
//			// 运行中
//			List<String> result = new ArrayList<>();
//			IoUtil.readLines(inputStream, charset, (LineHandler) result::add);
//			IoUtil.readLines(errStream, charset, (LineHandler) result::add);
//			return result;
//		} finally {
//			JschUtil.close(channel);
//			JschUtil.close(session);
//		}
//	}

    /**
     * 上传文件
     *
     * @param sshModel   ssh
     * @param remotePath 远程路径
     * @param desc       文件夹或者文件
     */
    public void uploadDir(SshModel sshModel, String remotePath, File desc) {
        Session session = null;
        ChannelSftp channel = null;
        try {
            session = getSessionByModel(sshModel);
            channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
            try (Sftp sftp = new Sftp(channel, sshModel.charset(), sshModel.timeout())) {
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
            session = getSessionByModel(sshModel);
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
                where.setHost(data.getHost());
                where.setPort(data.getPort());
                where.setUser(data.getUser());
                where.setConnectType(data.getConnectType());
                SshModel sshModel = super.queryByBean(where);
                if (sshModel == null) {
                    // 不存在则添加 信息
                    data.setId(null);
                    data.setWorkspaceId(workspaceId);
                    data.setCreateTimeMillis(null);
                    data.setModifyTimeMillis(null);
                    data.setModifyUser(null);
                    super.insert(data);
                } else {
                    // 修改信息
                    SshModel update = new SshModel(sshModel.getId());
                    update.setUser(data.getUser());
                    update.setName(data.getName());
                    update.setCharset(data.getCharset());
                    update.setAllowEditSuffix(data.getAllowEditSuffix());
                    update.setFileDirs(data.getFileDirs());
                    update.setNotAllowedCommand(data.getNotAllowedCommand());
                    update.setPassword(data.getPassword());
                    update.setPrivateKey(data.getPrivateKey());
                    super.updateById(update);
                }
            });
    }
}
