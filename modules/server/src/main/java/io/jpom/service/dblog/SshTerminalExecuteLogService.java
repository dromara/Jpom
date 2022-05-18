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
package io.jpom.service.dblog;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.util.StrUtil;
import io.jpom.common.BaseServerController;
import io.jpom.model.data.SshModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.log.SshTerminalExecuteLog;
import io.jpom.service.h2db.BaseWorkspaceService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


/**
 * ssh 终端执行日志
 *
 * @author jiangzeyin
 * @since 2021/08/04
 */
@Service
public class SshTerminalExecuteLogService extends BaseWorkspaceService<SshTerminalExecuteLog> {

	@Override
	protected String[] clearTimeColumns() {
		return new String[]{"createTimeMillis"};
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
		if (sshItem == null) {
			return;
		}
		long optTime = SystemClock.now();
		try {
			BaseServerController.resetInfo(userInfo);
			List<SshTerminalExecuteLog> executeLogs = commands.stream().filter(StrUtil::isNotEmpty).map(s -> {
				SshTerminalExecuteLog sshTerminalExecuteLog = new SshTerminalExecuteLog();
				//sshTerminalExecuteLog.setId(IdUtil.fastSimpleUUID());
				sshTerminalExecuteLog.setSshId(sshItem.getId());
				sshTerminalExecuteLog.setSshName(sshItem.getName());
				sshTerminalExecuteLog.setWorkspaceId(sshItem.getWorkspaceId());
				sshTerminalExecuteLog.setCommands(s);
				sshTerminalExecuteLog.setRefuse(refuse);
				sshTerminalExecuteLog.setCreateTimeMillis(optTime);
				sshTerminalExecuteLog.setIp(ip);
				sshTerminalExecuteLog.setUserAgent(userAgent);
				//sshTerminalExecuteLog.setUserId(UserModel.getOptUserName(userInfo));
				return sshTerminalExecuteLog;
			}).collect(Collectors.toList());
			super.insert(executeLogs);
		} finally {
			BaseServerController.removeAll();
		}
	}
}
