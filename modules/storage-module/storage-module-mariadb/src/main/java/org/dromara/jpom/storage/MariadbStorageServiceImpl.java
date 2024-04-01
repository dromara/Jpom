/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.storage;

import cn.hutool.core.lang.Opt;
import cn.hutool.db.ds.DSFactory;
import cn.hutool.setting.Setting;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.db.DbExtConfig;
import org.dromara.jpom.db.IStorageService;
import org.dromara.jpom.system.JpomRuntimeException;
import org.springframework.util.Assert;

/**
 * @author bwcx_jzy
 * @since 2024/4/1
 */
@Slf4j
public class MariadbStorageServiceImpl implements IStorageService {

    private String dbUrl;
    private DSFactory dsFactory;

    @Override
    public String dbUrl() {
        Assert.hasText(this.dbUrl, "还没有初始化数据库");
        return dbUrl;
    }

    @Override
    public int getFetchSize() {
        return Integer.MIN_VALUE;
    }

    @Override
    public DbExtConfig.Mode mode() {
        return DbExtConfig.Mode.MARIADB;
    }

    @Override
    public DSFactory init(DbExtConfig dbExtConfig) {
        Assert.isNull(this.dsFactory, "不要重复初始化数据库");
        Assert.hasText(dbExtConfig.getUrl(), "没有配置数据库连接");
        Setting setting = dbExtConfig.toSetting();
        this.dsFactory = DSFactory.create(setting);
        this.dbUrl = dbExtConfig.getUrl();
        return this.dsFactory;
    }

    @Override
    public DSFactory create(DbExtConfig dbExtConfig, String url, String user, String pass) {
        Setting setting = this.createSetting(dbExtConfig, url, user, pass);
        return DSFactory.create(setting);
    }

    @Override
    public Setting createSetting(DbExtConfig dbExtConfig, String url, String user, String pass) {
        String url2 = Opt.ofBlankAble(url).orElse(dbExtConfig.getUrl());
        String user2 = Opt.ofBlankAble(user).orElse(dbExtConfig.getUserName());
        String pass2 = Opt.ofBlankAble(pass).orElse(dbExtConfig.getUserPwd());
        Setting setting = dbExtConfig.toSetting();
        setting.set("user", user2);
        setting.set("pass", pass2);
        setting.set("url", url2);
        return setting;
    }

    public DSFactory getDsFactory() {
        Assert.notNull(this.dsFactory, "还没有初始化数据库");
        return dsFactory;
    }


    @Override
    public JpomRuntimeException warpException(Exception e) {
        return new JpomRuntimeException("数据库异常", e);
    }


    @Override
    public void close() throws Exception {
        log.info("mysql db destroy");
        if (this.dsFactory != null) {
            dsFactory.destroy();
            this.dsFactory = null;
        }
    }
}
