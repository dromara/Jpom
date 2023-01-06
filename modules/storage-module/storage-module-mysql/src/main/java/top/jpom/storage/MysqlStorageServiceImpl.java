package top.jpom.storage;

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
    public DSFactory create(DbExtConfig dbExtConfig) {
        Setting setting = dbExtConfig.toSetting();
        Assert.hasText(dbExtConfig.getUrl(), "没有配置数据库连接");
        setting.set("url", dbExtConfig.getUrl());
        return DSFactory.create(setting);
    }

    @Override
    public DSFactory init(DbExtConfig dbExtConfig) {
        Assert.isNull(this.dsFactory, "不要重复初始化数据库");
        this.dsFactory = this.create(dbExtConfig);
        this.dbUrl = dbExtConfig.getUrl();
        return this.dsFactory;
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
