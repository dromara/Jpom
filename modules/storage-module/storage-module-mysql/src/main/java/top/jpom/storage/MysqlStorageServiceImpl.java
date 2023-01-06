package top.jpom.storage;

import cn.hutool.core.lang.Opt;
import cn.hutool.db.ds.DSFactory;
import cn.hutool.setting.Setting;
import io.jpom.system.JpomRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import top.jpom.db.DbExtConfig;
import top.jpom.db.IStorageService;

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
        String url2 = Opt.ofBlankAble(url).orElse(dbExtConfig.getUrl());
        String user2 = Opt.ofBlankAble(user).orElse(dbExtConfig.getUserName());
        String pass2 = Opt.ofBlankAble(pass).orElse(dbExtConfig.getUserPwd());
        Setting setting = dbExtConfig.toSetting();
        setting.set("user", user2);
        setting.set("pass", pass2);
        setting.set("url", url2);
        return DSFactory.create(setting);
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
        log.info("h2 db destroy");
        if (this.dsFactory != null) {
            dsFactory.destroy();
            this.dsFactory = null;
        }
    }
}
