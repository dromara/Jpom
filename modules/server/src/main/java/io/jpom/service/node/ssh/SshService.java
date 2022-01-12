package io.jpom.service.node.ssh;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.*;
import cn.hutool.crypto.SecureUtil;
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
import org.springframework.util.Assert;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
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
			File rsaFile;
			String privateKey = sshModel.getPrivateKey();
			if (StrUtil.startWith(privateKey, URLUtil.FILE_URL_PREFIX)) {
				String rsaPath = StrUtil.removePrefix(privateKey, URLUtil.FILE_URL_PREFIX);
				rsaFile = FileUtil.file(rsaPath);
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
				File tempPath = ConfigBean.getInstance().getTempPath();
				String sshFile = StrUtil.emptyToDefault(sshModel.getId(), IdUtil.fastSimpleUUID());
				rsaFile = FileUtil.file(tempPath, "ssh", sshFile);
				FileUtil.writeString(privateKey, rsaFile, CharsetUtil.UTF_8);
			}
			Assert.state(FileUtil.isFile(rsaFile), "私钥文件不存在：" + FileUtil.getAbsolutePath(rsaFile));
			byte[] pas = null;
			if (StrUtil.isNotEmpty(sshModel.getPassword())) {
				pas = sshModel.getPassword().getBytes();
			}
			session = JschUtil.openSession(sshModel.getHost(), sshModel.getPort(), sshModel.getUser(), FileUtil.getAbsolutePath(rsaFile), pas);
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
	 * 获取 ssh 中的 Java 版本
	 *
	 * @param sshModel ssh
	 * @return true 存在运行中的
	 * @throws IOException   IO
	 * @throws JSchException jsch
	 */
	public String getSshJavaVersion(SshModel sshModel) throws IOException, JSchException {
		// 检查 java 环境
		String javaVersion = "java -version";
		List<String> command = this.execCommand(sshModel, javaVersion);
		String join = CollUtil.join(command, StrUtil.COMMA);
		return ReUtil.getGroup0("\"(.*?)\"", join);
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
		// 运行中
		List<String> result = this.execCommand(sshModel, ps);
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
		Charset charset = sshModel.getCharsetT();
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

	private List<String> execCommand(SshModel sshModel, String command) throws IOException, JSchException {
		Session session = null;
		ChannelExec channel = null;
		try {
			session = getSessionByModel(sshModel);
			channel = (ChannelExec) JschUtil.createChannel(session, ChannelType.EXEC);
			// 添加环境变量
			channel.setCommand(ServerExtConfigBean.getInstance().getSshInitEnv() + " && " + command);
			InputStream inputStream = channel.getInputStream();
			InputStream errStream = channel.getErrStream();
			channel.connect();
			Charset charset = sshModel.getCharsetT();
			// 运行中
			List<String> result = new ArrayList<>();
			IoUtil.readLines(inputStream, charset, (LineHandler) result::add);
			IoUtil.readLines(errStream, charset, (LineHandler) result::add);
			return result;
		} finally {
			JschUtil.close(channel);
			JschUtil.close(session);
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
