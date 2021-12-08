package io.jpom.service.node.ssh;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.extra.ssh.Sftp;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.jcraft.jsch.*;
import io.jpom.model.data.SshModel;
import io.jpom.service.h2db.BaseWorkspaceService;
import io.jpom.system.ConfigBean;
import io.jpom.system.ServerExtConfigBean;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

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

	/**
	 * 获取 ssh 回话
	 *
	 * @param sshId id
	 * @return session
	 */
	public static Session getSession(String sshId) {
		SshModel sshModel = SpringUtil.getBean(SshService.class).getByKey(sshId, false);
		return getSessionByModel(sshModel);
	}

	/**
	 * 获取 ssh 回话
	 *
	 * @param sshModel sshModel
	 * @return session
	 */
	public static Session getSessionByModel(SshModel sshModel) {
		Session session;
		SshModel.ConnectType connectType = sshModel.connectType();
		if (connectType == SshModel.ConnectType.PASS) {
			session = JschUtil.openSession(sshModel.getHost(), sshModel.getPort(), sshModel.getUser(), sshModel.getPassword());

		} else if (connectType == SshModel.ConnectType.PUBKEY) {
			File tempPath = ConfigBean.getInstance().getTempPath();
			String sshFile = StrUtil.emptyToDefault(sshModel.getId(), IdUtil.fastSimpleUUID());
			File ssh = FileUtil.file(tempPath, "ssh", sshFile);
			FileUtil.writeString(sshModel.getPrivateKey(), ssh, CharsetUtil.UTF_8);
			byte[] pas = null;
			if (StrUtil.isNotEmpty(sshModel.getPassword())) {
				pas = sshModel.getPassword().getBytes();
			}
			session = JschUtil.openSession(sshModel.getHost(), sshModel.getPort(), sshModel.getUser(), FileUtil.getAbsolutePath(ssh), pas);
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
	 * @throws IOException   IO
	 * @throws JSchException jsch
	 */
	public boolean checkSshRun(SshModel sshModel, String tag) throws IOException, JSchException {
		return this.checkSshRunPid(sshModel, tag) != null;
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
	public Integer checkSshRunPid(SshModel sshModel, String tag) throws IOException, JSchException {
		String ps = StrUtil.format("ps -ef | grep -v 'grep' | egrep {}", tag);
		Session session = null;
		ChannelExec channel = null;
		try {
			session = getSessionByModel(sshModel);
			channel = (ChannelExec) JschUtil.createChannel(session, ChannelType.EXEC);
			channel.setCommand(ps);
			InputStream inputStream = channel.getInputStream();
			InputStream errStream = channel.getErrStream();
			channel.connect();
			Charset charset = sshModel.getCharsetT();
			// 运行中
			List<String> result = new ArrayList<>();
			IoUtil.readLines(inputStream, charset, (LineHandler) result::add);
			IoUtil.readLines(errStream, charset, (LineHandler) result::add);
			return result.stream().map(s -> {
				List<String> split = StrUtil.splitTrim(s, StrUtil.SPACE);
				return Convert.toInt(CollUtil.get(split, 1));
			}).filter(Objects::nonNull).findAny().orElse(null);
		} finally {
			JschUtil.close(channel);
			JschUtil.close(session);
		}
	}

	public String exec(SshModel sshModel, String... command) throws IOException {
		if (ArrayUtil.isEmpty(command)) {
			return "没有任何命令";
		}
		Session session = null;
		InputStream sshExecTemplateInputStream = null;
		Sftp sftp = null;
		try {
			File buildSsh = FileUtil.file(ConfigBean.getInstance().getTempPath(), "build_ssh", sshModel.getId() + ".sh");
			sshExecTemplateInputStream = ResourceUtil.getStream("classpath:/bin/execTemplate.sh");
			String sshExecTemplate = IoUtil.readUtf8(sshExecTemplateInputStream);
			StringBuilder stringBuilder = new StringBuilder(sshExecTemplate);
			for (String s : command) {
				stringBuilder.append(s).append(StrUtil.LF);
			}
			Charset charset = sshModel.getCharsetT();
			FileUtil.writeString(stringBuilder.toString(), buildSsh, charset);
			//
			session = getSessionByModel(sshModel);
			// 上传文件
			sftp = new Sftp(session);
			String home = sftp.home();
			String path = home + "/.jpom/";
			String destFile = path + IdUtil.fastSimpleUUID() + ".sh";
			sftp.mkDirs(path);
			sftp.upload(destFile, buildSsh);

			// 执行命令
			String exec, error;
			try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
				exec = JschUtil.exec(session, "sh " + destFile, charset, stream);
				error = new String(stream.toByteArray(), charset);
				if (StrUtil.isNotEmpty(error)) {
					error = " 错误：" + error;
				}
			} finally {
				try {
					sftp.delFile(destFile);
				} catch (Exception ignored) {
				}
			}
			return exec + error;
		} finally {
			IoUtil.close(sftp);
			IoUtil.close(sshExecTemplateInputStream);
			JschUtil.close(session);
		}
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
			session = getSessionByModel(sshModel);
			return exec(session, sshModel.getCharsetT(), command);
		} finally {
			JschUtil.close(session);
		}
	}

	private String exec(Session session, Charset charset, String command) throws IOException, JSchException {
		ChannelExec channel = null;
		try {
			channel = (ChannelExec) JschUtil.createChannel(session, ChannelType.EXEC);
			// 添加环境变量
			channel.setCommand(ServerExtConfigBean.getInstance().getSshInitEnv() + " && " + command);
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
	 */
	public void uploadDir(SshModel sshModel, String remotePath, File desc) {
		Session session = null;
		ChannelSftp channel = null;
		try {
			session = getSessionByModel(sshModel);
			channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
			Sftp sftp = new Sftp(channel, sshModel.getCharsetT());
			sftp.syncUpload(desc, remotePath);
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
	 * @throws FileNotFoundException io
	 * @throws SftpException         sftp
	 */
	public void download(SshModel sshModel, String remoteFile, File save) throws FileNotFoundException, SftpException {
		Session session = null;
		ChannelSftp channel = null;
		OutputStream output = null;
		try {
			session = getSessionByModel(sshModel);
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
