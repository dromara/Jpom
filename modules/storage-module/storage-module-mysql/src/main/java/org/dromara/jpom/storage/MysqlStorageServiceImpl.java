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
 * @since 2023/1/5
 */
@Slf4j
public class MysqlStorageServiceImpl implements IStorageService {

    private String dbUrl;
    private DSFactory dsFactory;

    @Override
    public String dbUrl() {
        Assert.hasText(this.dbUrl, "还没有初始化数据库");
        return dbUrl;
    }

    @Override
    public DbExtConfig.Mode mode() {
        return DbExtConfig.Mode.MYSQL;
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
