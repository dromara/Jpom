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
package io.jpom.system.init;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.db.Entity;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.Const;
import io.jpom.model.data.*;
import io.jpom.model.log.MonitorNotifyLog;
import io.jpom.model.outgiving.OutGivingModel;
import io.jpom.service.dblog.DbMonitorNotifyLogService;
import io.jpom.service.monitor.MonitorService;
import io.jpom.service.node.NodeService;
import io.jpom.service.node.ssh.SshService;
import io.jpom.service.outgiving.OutGivingServer;
import io.jpom.service.system.SystemParametersServer;
import io.jpom.service.user.UserService;
import io.jpom.system.ConfigBean;
import io.jpom.system.ServerConfigBean;
import io.jpom.util.JsonFileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * json 配置转存 h2
 *
 * @author bwcx_jzy
 * @since 2021/12/2
 */
@Slf4j
public class LoadJsonConfigToDb {

	private LoadJsonConfigToDb() {
	}

	/**
	 * 静态内部类实现单例模式
	 */
	private static class LoadIpConfigToDbHolder {
		private static final LoadJsonConfigToDb INSTANCE = new LoadJsonConfigToDb();
	}

	public static LoadJsonConfigToDb getInstance() {
		return LoadJsonConfigToDb.LoadIpConfigToDbHolder.INSTANCE;
	}

	public void loadIpConfig() {
		File backupOldData = FileUtil.file(ConfigBean.getInstance().getDataPath(), "backup_old_data");
		// 读取 IP_CONFIG 文件内容
		File file = FileUtil.file(ConfigBean.getInstance().getDataPath(), ServerConfigBean.IP_CONFIG);
		if (!FileUtil.exist(file)) {
			return;
		}
		try {
			JSON json = JsonFileUtil.readJson(file.getAbsolutePath());
			SystemIpConfigModel systemIpConfigModel = json.toJavaObject(SystemIpConfigModel.class);
			if (systemIpConfigModel == null) {
				return;
			}
			SystemParametersServer parametersServer = SpringUtil.getBean(SystemParametersServer.class);
			parametersServer.upsert(SystemIpConfigModel.ID, systemIpConfigModel, SystemIpConfigModel.ID);
			// 将 json 文件转移到备份目录
			FileUtil.move(file, FileUtil.mkdir(backupOldData), true);
			log.info("{} mv to {}", FileUtil.getAbsolutePath(file), FileUtil.getAbsolutePath(backupOldData));
		} catch (Exception e) {
			log.error("load ip config error ", e);
		}
	}

	public void loadMailConfig() {
		File backupOldData = FileUtil.file(ConfigBean.getInstance().getDataPath(), "backup_old_data");
		// 读取 MAIL_CONFIG 文件内容
		File file = FileUtil.file(ConfigBean.getInstance().getDataPath(), ServerConfigBean.MAIL_CONFIG);
		if (!FileUtil.exist(file)) {
			return;
		}
		try {
			JSON json = JsonFileUtil.readJson(file.getAbsolutePath());
			MailAccountModel mailAccountModel = json.toJavaObject(MailAccountModel.class);
			if (mailAccountModel == null) {
				return;
			}
			SystemParametersServer parametersServer = SpringUtil.getBean(SystemParametersServer.class);
			parametersServer.upsert(MailAccountModel.ID, mailAccountModel, MailAccountModel.ID);
			// 将 json 文件转移到备份目录
			FileUtil.move(file, FileUtil.mkdir(backupOldData), true);
			log.info("{} mv to {}", FileUtil.getAbsolutePath(file), FileUtil.getAbsolutePath(backupOldData));
		} catch (Exception e) {
			log.error("load mail config error ", e);
		}
	}

	public void loadOutGivingWhitelistConfig() {
		File backupOldData = FileUtil.file(ConfigBean.getInstance().getDataPath(), "backup_old_data");
		// 读取 OUTGIVING_WHITELIST 文件内容
		File file = FileUtil.file(ConfigBean.getInstance().getDataPath(), ServerConfigBean.OUTGIVING_WHITELIST);
		if (!FileUtil.exist(file)) {
			return;
		}
		try {
			JSON json = JsonFileUtil.readJson(file.getAbsolutePath());
			ServerWhitelist serverWhitelist = json.toJavaObject(ServerWhitelist.class);
			if (serverWhitelist == null) {
				return;
			}
			SystemParametersServer parametersServer = SpringUtil.getBean(SystemParametersServer.class);
			String id = ServerWhitelist.workspaceId(Const.WORKSPACE_DEFAULT_ID);
			parametersServer.upsert(id, serverWhitelist, id);
			// 将 json 文件转移到备份目录
			FileUtil.move(file, FileUtil.mkdir(backupOldData), true);
			log.info("{} mv to {}", FileUtil.getAbsolutePath(file), FileUtil.getAbsolutePath(backupOldData));
		} catch (Exception e) {
			log.error("load mail config error ", e);
		}
	}


	public void loadUserInfo() {
		File backupOldData = FileUtil.file(ConfigBean.getInstance().getDataPath(), "backup_old_data");
		// 读取 USER 文件内容
		File file = FileUtil.file(ConfigBean.getInstance().getDataPath(), ServerConfigBean.USER);
		if (!FileUtil.exist(file)) {
			return;
		}
		try {
			JSON json = JsonFileUtil.readJson(file.getAbsolutePath());
			JSONArray jsonArray = JsonFileUtil.formatToArray((JSONObject) json);
			List<UserModel> userModels = jsonArray.toJavaList(UserModel.class);
			if (userModels == null) {
				return;
			}
			UserService userService = SpringUtil.getBean(UserService.class);
			userModels = userModels.stream().peek(userModel -> {
				//userModel.setRoles((Set<String>) null);
				userModel.setSystemUser(UserModel.SYSTEM_ADMIN.equals(userModel.getParent()) ? 1 : 0);
				//
				String salt = userService.generateSalt();
				userModel.setSalt(salt);
				userModel.setPassword(SecureUtil.sha1(userModel.getPassword() + salt));
			}).collect(Collectors.toList());

			userService.insert(userModels);
			// 将 json 文件转移到备份目录
			FileUtil.move(file, FileUtil.mkdir(backupOldData), true);
			log.info("{} mv to {}", FileUtil.getAbsolutePath(file), FileUtil.getAbsolutePath(backupOldData));
		} catch (Exception e) {
			log.error("load user info error ", e);
		}
	}

	public void loadNodeInfo() {
		File backupOldData = FileUtil.file(ConfigBean.getInstance().getDataPath(), "backup_old_data");
		// 读取 node 文件内容
		File file = FileUtil.file(ConfigBean.getInstance().getDataPath(), ServerConfigBean.NODE);
		if (!FileUtil.exist(file)) {
			return;
		}
		try {
			JSON json = JsonFileUtil.readJson(file.getAbsolutePath());
			JSONArray jsonArray = JsonFileUtil.formatToArray((JSONObject) json);
			List<NodeModel> nodeModels = jsonArray.toJavaList(NodeModel.class);
			if (nodeModels == null) {
				return;
			}
			nodeModels = nodeModels.stream().peek(nodeModel -> {
				//
				nodeModel.setProtocol(StrUtil.emptyToDefault(nodeModel.getProtocol(), "http"));
			}).collect(Collectors.toList());

			NodeService nodeService = SpringUtil.getBean(NodeService.class);
			nodeService.insert(nodeModels);
			// 将 json 文件转移到备份目录
			FileUtil.move(file, FileUtil.mkdir(backupOldData), true);
			log.info("{} mv to {}", FileUtil.getAbsolutePath(file), FileUtil.getAbsolutePath(backupOldData));
		} catch (Exception e) {
			log.error("load node error ", e);
		}
	}

	public void loadSshInfo() {
		File backupOldData = FileUtil.file(ConfigBean.getInstance().getDataPath(), "backup_old_data");
		// 读取 ssh 文件内容
		File file = FileUtil.file(ConfigBean.getInstance().getDataPath(), ServerConfigBean.SSH_LIST);
		if (!FileUtil.exist(file)) {
			return;
		}
		try {
			JSON json = JsonFileUtil.readJson(file.getAbsolutePath());
			JSONArray jsonArray = JsonFileUtil.formatToArray((JSONObject) json);
			List<SshModel> sshModels = jsonArray.toJavaList(SshModel.class);
			if (sshModels == null) {
				return;
			}
			SshService sshService = SpringUtil.getBean(SshService.class);
			sshService.insert(sshModels);
			// 将 json 文件转移到备份目录
			FileUtil.move(file, FileUtil.mkdir(backupOldData), true);
			log.info("{} mv to {}", FileUtil.getAbsolutePath(file), FileUtil.getAbsolutePath(backupOldData));
		} catch (Exception e) {
			log.error("load ssh error ", e);
		}
	}

	public void loadMonitorInfo() {
		File backupOldData = FileUtil.file(ConfigBean.getInstance().getDataPath(), "backup_old_data");
		// 读取 monitor 文件内容
		File file = FileUtil.file(ConfigBean.getInstance().getDataPath(), ServerConfigBean.MONITOR_FILE);
		if (!FileUtil.exist(file)) {
			return;
		}
		try {
			JSON json = JsonFileUtil.readJson(file.getAbsolutePath());
			JSONArray jsonArray = JsonFileUtil.formatToArray((JSONObject) json);
			List<MonitorModel> monitorModels = jsonArray.toJavaList(MonitorModel.class);
			if (monitorModels == null) {
				return;
			}

			MonitorService monitorService = SpringUtil.getBean(MonitorService.class);
			monitorService.insert(monitorModels);
			// 将 json 文件转移到备份目录
			FileUtil.move(file, FileUtil.mkdir(backupOldData), true);
			log.info("{} mv to {}", FileUtil.getAbsolutePath(file), FileUtil.getAbsolutePath(backupOldData));
		} catch (Exception e) {
			log.error("load monitor error ", e);
		}
	}


	public void loadOutgivinInfo() {
		File backupOldData = FileUtil.file(ConfigBean.getInstance().getDataPath(), "backup_old_data");
		// 读取 outgiving 文件内容
		File file = FileUtil.file(ConfigBean.getInstance().getDataPath(), ServerConfigBean.OUTGIVING);
		if (!FileUtil.exist(file)) {
			return;
		}
		try {
			JSON json = JsonFileUtil.readJson(file.getAbsolutePath());
			JSONArray jsonArray = JsonFileUtil.formatToArray((JSONObject) json);
			List<OutGivingModel> outGivingModels = jsonArray.toJavaList(OutGivingModel.class);
			if (outGivingModels == null) {
				return;
			}

			OutGivingServer outGivingServer = SpringUtil.getBean(OutGivingServer.class);
			outGivingServer.insert(outGivingModels);
			// 将 json 文件转移到备份目录
			FileUtil.move(file, FileUtil.mkdir(backupOldData), true);
			log.info("{} mv to {}", FileUtil.getAbsolutePath(file), FileUtil.getAbsolutePath(backupOldData));
		} catch (Exception e) {
			log.error("load OUTGIVING error ", e);
		}
	}

	/**
	 * 将 监控报警记录 里面但 logId 字段更新为 id
	 */
	public void convertMonitorLogField() {
		DbMonitorNotifyLogService monitorNotifyLogService = SpringUtil.getBean(DbMonitorNotifyLogService.class);
		String tableName = monitorNotifyLogService.getTableName();
		List<Entity> query = monitorNotifyLogService.query("select * from " + tableName + " order by createTime asc limit 1");
		Entity first = CollUtil.getFirst(query);
		if (first == null) {
			this.checkLogFiled(monitorNotifyLogService);
			return;
		}
		Object logId = first.get("logId");
		if (logId == null) {
			this.checkLogFiled(monitorNotifyLogService);
			return;
		}
		String sql = "update " + tableName + " set ID = LOGID where ID = '' and LOGID is not null  and LOGID <> '';";
		int execute = monitorNotifyLogService.execute(sql);
		if (execute > 0) {
			Console.log("convert monitor log field {}", execute);
		}
		// 标记包含旧字段
		MonitorNotifyLog.HAS_LOG_ID = true;
	}

	private void checkLogFiled(DbMonitorNotifyLogService monitorNotifyLogService) {
		//show COLUMNS from tablename
		List<Entity> query = monitorNotifyLogService.query("show COLUMNS from " + monitorNotifyLogService.getTableName());
		MonitorNotifyLog.HAS_LOG_ID = query.stream().anyMatch(entity -> {
			Object field = entity.get("field");
			return StrUtil.equalsIgnoreCase(StrUtil.toString(field), "LOGID");
		});
	}
}
