package io.jpom.service.node.command;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import io.jpom.model.data.CommandModel;
import io.jpom.service.h2db.BaseDbService;
import io.jpom.service.node.ssh.SshService;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 命令管理
 *
 * @author : Arno
 * @since : 2021/12/6 22:11
 */
@Service
public class CommandService extends BaseDbService<CommandModel> {

	private final SshService sshService;

	public CommandService(SshService sshService) {
		this.sshService = sshService;
	}

	/**
	 * 批量执行命令
	 *
	 * @param model 命令信息
	 * @param nodes ssh节点
	 */
	public void executeBatch(CommandModel model, List<String> nodes) {
		for (String sshId : nodes) {
			this.execute(model.getCommand(), sshId);
		}
	}

	/**
	 * 执行命令
	 *
	 * @param command 命令内容
	 * @param sshId   ssh id
	 * @return 执行命令结果
	 */
	private String execute(String command, String sshId) {
		try {
			Session session = SshService.getSession(sshId);
			ChannelExec channel = (ChannelExec) JschUtil.createChannel(session, ChannelType.EXEC);
			channel.setCommand(StrUtil.bytes("source /etc/profile && " + command, StandardCharsets.UTF_8));
			channel.setInputStream(null);
			channel.setErrStream(System.err);
			channel.connect();
			InputStream in = channel.getInputStream();
			return IoUtil.read(in, StandardCharsets.UTF_8);
		} catch (Exception e) {
			DefaultSystemLog.getLog().error(sshId + " 节点执行命令异常", e);
		}
		return null;
	}


}
