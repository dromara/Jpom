package io.jpom.system.extconf;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.setting.yaml.YamlUtil;
import io.jpom.system.db.DbConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;

/**
 * 数据库配置
 *
 * @author bwcx_jzy
 * @since 2022/2/9
 */
@Configuration
@ConfigurationProperties(prefix = "db")
@Data
public class DbExtConfig {

	/**
	 * 日志记录最大条数
	 */
	private Integer logStorageCount = 10000;
	/**
	 * 数据库 url
	 */
	private String url;
	/**
	 * 数据库账号、默认为 jpom
	 */
	private String userName;

	/**
	 * 数据库密码、默认为 jpom
	 */
	private String userPwd;

	/**
	 * 缓存大小
	 * <p>
	 * http://www.h2database.com/html/features.html#cache_settings
	 */
	private DataSize cacheSize = DataSize.ofMegabytes(10);

	/**
	 * 自动全量备份数据库间隔天数 小于等于 0，不自动备份
	 */
	private Integer autoBackupIntervalDay = 1;

	/**
	 * 自动备份保留天数 小于等于 0，不自动删除自动备份数据
	 */
	private Integer autoBackupReserveDay = 5;

	private Integer maxActive = 100;

	private Integer initialSize = 10;

	private Integer maxWait = 10;

	private Integer minIdle = 1;

	public String getUserName() {
		return StrUtil.emptyToDefault(this.userName, DbConfig.DEFAULT_USER_OR_AUTHORIZATION);
	}

	public String getUserPwd() {
		return StrUtil.emptyToDefault(this.userPwd, DbConfig.DEFAULT_USER_OR_AUTHORIZATION);
	}


	public static DbExtConfig parse(String content) {
		ByteArrayInputStream inputStream = IoUtil.toStream(content, CharsetUtil.CHARSET_UTF_8);
		return parse(inputStream);
	}

	public static DbExtConfig parse(InputStream inputStream) {
		Dict dict = YamlUtil.load(inputStream, Dict.class);
		Object db = dict.get("db");
		StringWriter writer = new StringWriter();
		YamlUtil.dump(db, writer);
		ByteArrayInputStream byteArrayInputStream = IoUtil.toStream(writer.toString(), CharsetUtil.CHARSET_UTF_8);
		return YamlUtil.load(byteArrayInputStream, DbExtConfig.class);
	}
}
