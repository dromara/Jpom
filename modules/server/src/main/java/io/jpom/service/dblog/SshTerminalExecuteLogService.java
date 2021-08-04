package io.jpom.service.dblog;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.jpom.model.data.SshModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.log.SshTerminalExecuteLog;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * ssh 终端执行日志
 *
 * @author jiangzeyin
 * @date 2021/08/04
 */
@Service
public class SshTerminalExecuteLogService extends BaseDbLogService<SshTerminalExecuteLog> {

	public SshTerminalExecuteLogService() {
		super(SshTerminalExecuteLog.TABLE_NAME, SshTerminalExecuteLog.class);
		setKey("id");
	}

	/**
	 * 批量记录日志
	 *
	 * @param userInfo  操作的用户
	 * @param sshItem   ssh 对象
	 * @param ip        操作人的ip
	 * @param userAgent 浏览器标识
	 * @param commands  命令行
	 * @param refuse    是否拒绝执行
	 */
	public void batch(UserModel userInfo, SshModel sshItem, String ip, String userAgent, boolean refuse, List<String> commands) {
		long optTime = SystemClock.now();
		List<SshTerminalExecuteLog> executeLogs = commands.stream().filter(StrUtil::isNotEmpty).map(s -> {
			SshTerminalExecuteLog sshTerminalExecuteLog = new SshTerminalExecuteLog();
			sshTerminalExecuteLog.setId(IdUtil.fastSimpleUUID());
			if (sshItem != null) {
				sshTerminalExecuteLog.setSshId(sshItem.getId());
				sshTerminalExecuteLog.setSshName(sshItem.getName());
			}
			sshTerminalExecuteLog.setCommands(s);
			sshTerminalExecuteLog.setRefuse(refuse);
			sshTerminalExecuteLog.setOptTime(optTime);
			sshTerminalExecuteLog.setIp(ip);
			sshTerminalExecuteLog.setUserAgent(userAgent);
			sshTerminalExecuteLog.setUserId(UserModel.getOptUserName(userInfo));
			return sshTerminalExecuteLog;
		}).collect(Collectors.toList());
		super.insert(executeLogs);
	}
}
