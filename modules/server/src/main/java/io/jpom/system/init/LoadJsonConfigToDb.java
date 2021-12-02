package io.jpom.system.init;

import cn.hutool.core.io.FileUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSON;
import io.jpom.model.data.MailAccountModel;
import io.jpom.model.data.SystemIpConfigModel;
import io.jpom.service.system.SystemParametersServer;
import io.jpom.system.ConfigBean;
import io.jpom.system.ServerConfigBean;
import io.jpom.util.JsonFileUtil;

import java.io.File;

/**
 * json 配置转存 h2
 *
 * @author bwcx_jzy
 * @since 2021/12/2
 */
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
			DefaultSystemLog.getLog().info("{} mv to {}", FileUtil.getAbsolutePath(file), FileUtil.getAbsolutePath(backupOldData));
		} catch (Exception e) {
			DefaultSystemLog.getLog().error("load ip config error ", e);
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
			DefaultSystemLog.getLog().info("{} mv to {}", FileUtil.getAbsolutePath(file), FileUtil.getAbsolutePath(backupOldData));
		} catch (Exception e) {
			DefaultSystemLog.getLog().error("load mail config error ", e);
		}
	}
}
