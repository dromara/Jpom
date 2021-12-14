/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 码之科技工作室
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
package io.jpom.system;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.script.ScriptUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.system.db.DbConfig;
import org.eclipse.jgit.api.Git;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import java.util.concurrent.TimeUnit;

/**
 * 外部配置文件
 *
 * @author jiangzeyin
 * @date 2019/3/04
 */
@Configuration
public class ServerExtConfigBean implements DisposableBean {

	/**
	 * 系统最多能创建多少用户
	 */
	@Value("${user.maxCount:10}")
	public int userMaxCount;
	/**
	 * 用户连续登录失败次数，超过此数将自动不再被允许登录，零是不限制
	 */
	@Value("${user.alwaysLoginError:5}")
	public int userAlwaysLoginError;

	/**
	 * 当ip连续登录失败，锁定对应IP时长，单位毫秒
	 */
	@Value("${user.ipErrorLockTime:60*60*5*1000}")
	private String ipErrorLockTime;
	private long ipErrorLockTimeValue = -1;
	/**
	 * 日志记录最大条数
	 */
	@Value("${db.logStorageCount:100000}")
	private int h2DbLogStorageCount;

	/**
	 * 数据库账号、默认为 jpom
	 */
	@Value("${db.userName:}")
	private String dbUserName;

	/**
	 * 数据库密码、默认为 jpom
	 */
	@Value("${db.userPwd:}")
	private String dbUserPwd;

	/**
	 * 缓存大小
	 * <p>
	 * http://www.h2database.com/html/features.html#cache_settings
	 */
	@Value("${db.cacheSize:}")
	private DataSize cacheSize;

	/**
	 * 自动全量备份数据库间隔天数 小于等于 0，不自动备份
	 */
	@Value("${db.autoBackupIntervalDay:1}")
	private Integer autoBackupIntervalDay;

	/**
	 * 自动备份保留天数 小于等于 0，不自动删除自动备份数据
	 */
	@Value("${db.autoBackupReserveDay:5}")
	private Integer autoBackupReserveDay;

	/**
	 * author Hotstrip
	 * 是否开启 web 访问数据库
	 *
	 * @see <a href=http://${ip}:${port}/h2-console>http://${ip}:${port}/h2-console</a>
	 */
	@Value("${spring.h2.console.enabled:false}")
	private boolean h2ConsoleEnabled;

	/**
	 * 服务端api token,长度要求大于等于6位，字母数字符号组合
	 */
	@Value("${jpom.authorize.token:}")
	private String authorizeToken;

	/**
	 * 登录token失效时间(单位：小时),默认为24
	 */
	@Value("${jpom.authorize.expired:24}")
	private int authorizeExpired;

	/**
	 * 登录token失效后自动续签时间（单位：分钟），默认为60，
	 */
	@Value("${jpom.authorize.renewal:60}")
	private int authorizeRenewal;

	/**
	 * 登录token 加密的key 长度建议控制到 16位
	 */
	@Value("${jpom.authorize.key:}")
	private String authorizeKey;

	/**
	 * 构建最多保存多少份历史记录
	 */
	@Value("${build.maxHistoryCount:1000}")
	private int buildMaxHistoryCount;

	/**
	 * 每一项构建最多保存的历史份数
	 */
	@Value("${build.itemMaxHistoryCount:50}")
	private int buildItemMaxHistoryCount;

	@Value("${build.checkDeleteCommand:true}")
	private Boolean buildCheckDeleteCommand;

	/**
	 * ssh 中执行命令 初始化的环境变量
	 */
	@Value("${ssh.initEnv:}")
	private String sshInitEnv;

	/**
	 * 上传文件的超时时间 单位秒,最短5秒中
	 */
	@Value("${node.uploadFileTimeOut:300}")
	private int uploadFileTimeOut;

	/**
	 * 前端接口 超时时间 单位秒
	 */
	@Value("${jpom.webApiTimeout:20}")
	private int webApiTimeout;

	/**
	 * 系统名称
	 */
	@Value("${jpom.name:}")
	private String name;

	/**
	 * 系统副名称（标题） 建议4个汉字以内
	 */
	@Value("${jpom.subTitle:}")
	private String subTitle;

	/**
	 * 登录页标题
	 */
	@Value("${jpom.loginTitle:}")
	private String loginTitle;

	/**
	 * logo 文件路径
	 */
	@Value("${jpom.logoFile:}")
	private String logoFile;

	/**
	 * 获取上传文件超时时间
	 *
	 * @return 返回毫秒
	 */
	public int getUploadFileTimeOut() {
		return Math.max(this.uploadFileTimeOut, 5) * 1000;
	}

	public String getSshInitEnv() {
		return StrUtil.emptyToDefault(this.sshInitEnv, "source /etc/profile && source ~/.bash_profile && source ~/.bashrc");
	}

	public String getAuthorizeToken() {
		return authorizeToken;
	}

	public long getIpErrorLockTime() {
		if (this.ipErrorLockTimeValue == -1) {
			String str = StrUtil.emptyToDefault(this.ipErrorLockTime, "60*60*5*1000");
			this.ipErrorLockTimeValue = Convert.toLong(ScriptUtil.eval(str), TimeUnit.HOURS.toMillis(5));
		}
		return this.ipErrorLockTimeValue;
	}

	public int getH2DbLogStorageCount() {
		return h2DbLogStorageCount;
	}

	public int getBuildMaxHistoryCount() {
		return buildMaxHistoryCount;
	}

	public int getBuildItemMaxHistoryCount() {
		return buildItemMaxHistoryCount;
	}

	public int getAuthorizeExpired() {
		return authorizeExpired;
	}

	public int getAuthorizeRenewal() {
		return authorizeRenewal;
	}

	public String getDbUserName() {
		return StrUtil.emptyToDefault(this.dbUserName, DbConfig.DEFAULT_USER_OR_AUTHORIZATION);
	}

	public String getDbUserPwd() {
		return StrUtil.emptyToDefault(this.dbUserPwd, DbConfig.DEFAULT_USER_OR_AUTHORIZATION);
	}

	public boolean isH2ConsoleEnabled() {
		return h2ConsoleEnabled;
	}

	public byte[] getAuthorizeKey() {
		return StrUtil.emptyToDefault(this.authorizeKey, "KZQfFBJTW2v6obS1").getBytes();
	}

	/**
	 * 数据缓存大小，默认10m,
	 * <p>
	 * SELECT * FROM INFORMATION_SCHEMA.SETTINGS WHERE NAME = 'info.CACHE_MAX_SIZE'
	 *
	 * @return dataSize
	 */
	public DataSize getCacheSize() {
		if (cacheSize == null) {
			cacheSize = DataSize.ofMegabytes(10);
		}
		return cacheSize;
	}

	public boolean getBuildCheckDeleteCommand() {
		return buildCheckDeleteCommand != null && buildCheckDeleteCommand;
	}

	/**
	 * 最小值 10秒
	 *
	 * @return 超时时间（单位秒）
	 */
	public int getWebApiTimeout() {
		return Math.max(this.webApiTimeout, 10);
	}

	public String getName() {
		return StrUtil.emptyToDefault(name, "Jpom项目管理系统");
	}

	public String getSubTitle() {
		return StrUtil.emptyToDefault(subTitle, "项目管理");
	}

	public String getLoginTitle() {
		return StrUtil.emptyToDefault(loginTitle, "登录JPOM");
	}

	public String getLogoFile() {
		return logoFile;
	}

	public Integer getAutoBackupIntervalDay() {
		return autoBackupIntervalDay;
	}

	public Integer getAutoBackupReserveDay() {
		return autoBackupReserveDay;
	}

	/**
	 * 单例
	 *
	 * @return this
	 */
	public static ServerExtConfigBean getInstance() {
		return SpringUtil.getBean(ServerExtConfigBean.class);
	}

	@Override
	public void destroy() throws Exception {
		try {
			Git.shutdown();
		} catch (Exception e) {
			Console.error(e.getMessage());
		}
	}
}
