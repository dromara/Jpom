/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom;

import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.SystemClock;
import cn.keepbx.jpom.JpomAppType;
import cn.keepbx.jpom.Type;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.startup.CommandExecutor;
import org.dromara.jpom.util.StringUtil;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.ApplicationContext;

/**
 * jpom 启动类
 *
 * @author bwcx_jzy
 * @since 2017/9/14
 */
@SpringBootApplication(scanBasePackages = {"org.dromara.jpom"})
@ServletComponentScan(basePackages = {"org.dromara.jpom"})
@Slf4j
@JpomAppType(Type.Server)
public class JpomServerApplication {

    /**
     * 启动执行
     * <p>
     * --rest:ip_config 重置 IP 授权配置
     * <p>
     * --rest:load_init_db 重新加载数据库初始化操作
     * <p>
     * --rest:super_user_pwd 重置超级管理员密码
     * <p>
     * --recover:h2db 当 h2 数据出现奔溃无法启动需要执行恢复逻辑
     * <p>
     * --close:super_user_mfa 关闭超级管理员 mfa
     * <p>
     * --backup-h2 备份数据库
     * <p>
     * --import-h2-sql=/xxxx.sql 导入指定文件 sql
     * <p>
     * --replace-import-h2-sql=/xxxx.sql 替换导入指定文件 sql（会删除掉已经存的数据）
     * <p>
     * --transform-sql 转换 sql 内容(低版本兼容高版本),仅在导入 sql 文件时候生效：--import-h2-sql=/xxxx.sql、--replace-import-h2-sql=/xxxx.sql
     * <p>
     * --h2-migrate-mysql --h2-user=jpom --h2-pass=jpom  将 h2 数据库迁移到 mysql
     * <p>
     * --h2-migrate-postgresql --h2-user=jpom --h2-pass=jpom 将 h2 数据库迁移到 postgresql
     * <p>
     * --h2-migrate-mariadb --h2-user=jpom --h2-pass=jpom 将 h2 数据库迁移到 mariadb
     * <p>
     * --h2-migrate-dameng --h2-user=jpom --h2-pass=jpom 将 h2 数据库迁移到 dameng
     *
     * @param args 参数
     * @throws Exception 异常
     */
    public static void main(String[] args) throws Exception {
        long time = SystemClock.now();
        //
        SpringApplicationBuilder springApplicationBuilder = new SpringApplicationBuilder(JpomServerApplication.class);
        springApplicationBuilder.bannerMode(Banner.Mode.LOG);
        ApplicationContext applicationContext = springApplicationBuilder.run(args);

        // 使用命令执行器处理启动参数
        CommandExecutor commandExecutor = new CommandExecutor(applicationContext, args);
        commandExecutor.execute();

        //
        log.info(I18nMessageUtil.get("i18n.startup_duration.54fe"), StringUtil.formatBetween(SystemClock.now() - time, BetweenFormatter.Level.MILLISECOND));
    }
}
