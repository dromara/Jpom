/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.db;

import cn.hutool.core.util.StrUtil;
import cn.hutool.db.sql.SqlLog;
import cn.hutool.setting.Setting;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

/**
 * 数据库配置
 *
 * @author bwcx_jzy
 * @since 2022/2/9
 */
@Configuration
@ConfigurationProperties(prefix = "jpom.db")
@Data
public class DbExtConfig implements InitializingBean {

    /**
     * 默认的账号或者密码
     */
    public static final String DEFAULT_USER_OR_AUTHORIZATION = "jpom";

    /**
     * SQL backup default directory name
     * 数据库备份默认目录名称
     */
    public static final String BACKUP_DIRECTORY_NAME = "backup";
    /**
     * 备份 SQL 文件 后缀
     */
    public static final String SQL_FILE_SUFFIX = ".sql";

    /**
     * 日志记录最大条数
     */
    private Integer logStorageCount = 10000;
    /**
     * 数据库默认
     */
    private Mode mode = Mode.H2;
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
     * <a href="http://www.h2database.com/html/features.html#cache_settings">http://www.h2database.com/html/features.html#cache_settings</a>
     */
    private DataSize cacheSize = DataSize.ofMegabytes(10);

    /**
     * 自动全量备份数据库间隔天数 小于等于 0，不自动备份
     */
    private Integer autoBackupIntervalDay = 1;

    /**
     * 自动备份保留天数 小于等于 0，不自动删除自动备份数据
     */
    private int autoBackupReserveDay = 5;

    private int maxActive = 100;

    private int initialSize = 10;

    private int maxWait = 10;

    private int minIdle = 1;
    /**
     * @see cn.hutool.db.sql.SqlLog#KEY_SHOW_SQL
     */
    private Boolean showSql = false;

    public String userName() {
        return StrUtil.emptyToDefault(this.userName, DbExtConfig.DEFAULT_USER_OR_AUTHORIZATION);
    }

    public String userPwd() {
        return StrUtil.emptyToDefault(this.userPwd, DbExtConfig.DEFAULT_USER_OR_AUTHORIZATION);
    }

    public Setting toSetting() {
        //
        Setting setting = new Setting();
        setting.set("user", this.userName());
        setting.set("pass", this.userPwd());
        setting.set("url", this.getUrl());
        // 配置连接池大小
        setting.set("maxActive", this.getMaxActive() + "");
        setting.set("initialSize", this.getInitialSize() + "");
        setting.set("maxWait", this.getMaxWait() + "");
        setting.set("minIdle", this.getMinIdle() + "");
        // 调试模式显示sql 信息
        if (this.getShowSql()) {

            setting.set(SqlLog.KEY_SHOW_SQL, "true");
			/*
			  @author Hotstrip
			  sql log only show when it's needed,
			  if you want to check init sql,
			  set the [sqlLevel] from [DEBUG] to [INFO]
			 */
            setting.set(SqlLog.KEY_SQL_LEVEL, "DEBUG");
            setting.set(SqlLog.KEY_SHOW_PARAMS, "true");
        }
        return setting;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        StorageServiceFactory.setMode(this.getMode());
    }

    public enum Mode {
        /**
         * h2
         */
        H2,
        /**
         * mysql
         */
        MYSQL,
        /**
         * postgresql
         */
        POSTGRESQL,
        /**
         * Mariadb
         */
        MARIADB
    }
}
