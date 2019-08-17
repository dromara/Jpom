package cn.keepbx.jpom.service.node.ssh;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschUtil;
import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.model.data.SshModel;
import cn.keepbx.jpom.system.JpomRuntimeException;
import cn.keepbx.jpom.system.ServerConfigBean;
import cn.keepbx.permission.BaseDynamicService;
import cn.keepbx.plugin.ClassFeature;
import com.alibaba.fastjson.JSONArray;
import com.jcraft.jsch.*;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author bwcx_jzy
 * @date 2019/8/9
 */
@Service
public class SshService extends BaseOperService<SshModel> implements BaseDynamicService {

    public SshService() {
        super(ServerConfigBean.SSH_LIST);
    }

    @Override
    public void addItem(SshModel sshModel) {
        sshModel.setId(IdUtil.fastSimpleUUID());
        super.addItem(sshModel);
    }

    @Override
    public JSONArray listToArray(String dataId) {
        return (JSONArray) JSONArray.toJSON(this.list());
    }

    @Override
    public List<SshModel> list() {
        return (List<SshModel>) filter(super.list(), ClassFeature.SSH);
    }

    public Session getSession(SshModel sshModel) {
        return JschUtil.openSession(sshModel.getHost(), sshModel.getPort(), sshModel.getUser(), sshModel.getPassword());
    }

    /**
     * 检查是否存在正在运行的进程
     *
     * @param sshModel ssh
     * @param tag      标识
     * @return true 存在运行中的
     * @throws IOException   IO
     * @throws JSchException jsch
     */
    public boolean checkSshRun(SshModel sshModel, String tag) throws IOException, JSchException {
        String ps = StrUtil.format("ps -ef | grep -v 'grep' | egrep {}", tag);
        Session session = null;
        ChannelExec channel = null;
        try {
            session = getSession(sshModel);
            channel = (ChannelExec) JschUtil.createChannel(session, ChannelType.EXEC);
            channel.setCommand(ps);
            InputStream inputStream = channel.getInputStream();
            InputStream errStream = channel.getErrStream();
            channel.connect();
            Charset charset = sshModel.getCharsetT();
            // 运行中
            AtomicBoolean run = new AtomicBoolean(false);
            IoUtil.readLines(inputStream, charset, (LineHandler) s -> {
                run.set(true);
            });
            if (run.get()) {
                return true;
            }
            run.set(false);
            AtomicReference<String> error = new AtomicReference<>();
            IoUtil.readLines(errStream, charset, (LineHandler) s -> {
                run.set(true);
                error.set(s);
            });
            if (run.get()) {
                throw new JpomRuntimeException("检查异常:" + error.get());
            }
        } finally {
            JschUtil.close(channel);
            JschUtil.close(session);
        }
        return false;
    }

    /**
     * 执行命令
     *
     * @param sshModel ssh
     * @param command  命令
     * @return 结果
     * @throws IOException   io
     * @throws JSchException jsch
     */
    public String exec(SshModel sshModel, String command) throws IOException, JSchException {
        Session session = null;
        try {
            session = getSession(sshModel);
            return exec(session, sshModel.getCharsetT(), command);
        } finally {
            JschUtil.close(session);
        }
    }

    private String exec(Session session, Charset charset, String command) throws IOException, JSchException {
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) JschUtil.createChannel(session, ChannelType.EXEC);
            channel.setCommand(command);
            InputStream inputStream = channel.getInputStream();
            InputStream errStream = channel.getErrStream();
            channel.connect();
            // 读取结果
            String result = IoUtil.read(inputStream, charset);
            //
            String error = IoUtil.read(errStream, charset);
            return result + error;
        } finally {
            JschUtil.close(channel);
        }
    }

    /**
     * 上传文件
     *
     * @param sshModel   ssh
     * @param remotePath 远程路径
     * @param desc       文件夹或者文件
     * @throws FileNotFoundException 文件异常
     * @throws SftpException         ftp
     */
    public void uploadDir(SshModel sshModel, String remotePath, File desc) throws FileNotFoundException, SftpException {
        Session session = null;
        ChannelSftp channel = null;
        try {
            session = getSession(sshModel);
            channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
            uploadDir(channel, remotePath, desc);
        } finally {
            JschUtil.close(channel);
            JschUtil.close(session);
        }
    }

    private void uploadDir(ChannelSftp channel, String remotePath, File file) throws FileNotFoundException, SftpException {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null || files.length <= 0) {
                return;
            }
            for (File f : files) {
                if (f.isDirectory()) {
                    String mkdir = FileUtil.normalize(remotePath + "/" + f.getName());
                    this.uploadDir(channel, mkdir, f);
                } else {
                    this.uploadDir(channel, remotePath, f);
                }
            }
        } else {
            mkdir(channel, remotePath);
            String name = file.getName();
            channel.put(new FileInputStream(file), name);
        }
    }

    private void mkdir(ChannelSftp channel, String remotePath) {
        try {
            channel.mkdir(remotePath);
        } catch (SftpException ignored) {
        }
        try {
            channel.cd(remotePath);
        } catch (SftpException e) {
            throw new RuntimeException("切换目录失败：" + remotePath, e);
        }
    }

    /**
     * 下载文件
     *
     * @param sshModel
     * @param remoteFile
     * @param save
     * @throws FileNotFoundException
     * @throws SftpException
     */
    public void download(SshModel sshModel, String remoteFile, File save) throws FileNotFoundException, SftpException {
        Session session = null;
        ChannelSftp channel = null;
        OutputStream output = null;
        try {
            session = getSession(sshModel);
            channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
            output = new FileOutputStream(save);
            channel.get(remoteFile, output);
        } finally {
            IoUtil.close(output);
            JschUtil.close(channel);
            JschUtil.close(session);
        }
    }
}
