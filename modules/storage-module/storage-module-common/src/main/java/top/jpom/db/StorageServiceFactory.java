package top.jpom.db;

import cn.hutool.core.exceptions.CheckedUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Singleton;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ServiceLoaderUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import cn.hutool.db.ds.DSFactory;
import io.jpom.system.ExtConfigBean;
import io.jpom.system.JpomRuntimeException;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import top.jpom.h2db.TableName;

import java.io.File;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据存储服务
 *
 * @author bwcx_jzy
 * @since 2023/1/5
 */
@Slf4j
public class StorageServiceFactory {

    private static final String DB = "db";
    /**
     * 当前运行模式
     */
    private static DbExtConfig.Mode mode;

    /**
     * 配置当前数据库 模式
     *
     * @param mode mode
     */
    public static void setMode(DbExtConfig.Mode mode) {
        StorageServiceFactory.mode = mode;
    }

    public static DbExtConfig.Mode getMode() {
        return mode;
    }

    /**
     * 将数据迁移到当前环境
     */
    public static void migrateH2ToNow(DbExtConfig dbExtConfig, String h2Url, String h2User, String h2Pass) {
        log.info("开始迁移 h2 数据到 {}", dbExtConfig.getMode());
        try {
            IStorageService h2StorageService = doCreateStorageService(DbExtConfig.Mode.H2);
            boolean hasDbData = h2StorageService.hasDbData();
            if (!hasDbData) {
                throw new JpomRuntimeException("没有 h2 数据信息不用迁移");
            }
            DSFactory h2DsFactory = h2StorageService.create(dbExtConfig, h2Url, h2User, h2Pass);
            h2DsFactory.getDataSource();
            log.info("成功连接 H2 ");
            IStorageService nowStorageService = doCreateStorageService(dbExtConfig.getMode());
            DSFactory nowDsFactory = nowStorageService.create(dbExtConfig, null, null, null);
            nowDsFactory.getDataSource();
            log.info("成功连接 {} {}", dbExtConfig.getMode(), dbExtConfig.getUrl());
            Set<Class<?>> classes = ClassUtil.scanPackageByAnnotation("io.jpom", TableName.class);
            classes = classes.stream().filter(aClass -> {
                TableName tableName = aClass.getAnnotation(TableName.class);
                DbExtConfig.Mode[] modes = tableName.modes();
                if (ArrayUtil.isEmpty(modes)) {
                    return true;
                }
                return ArrayUtil.contains(modes, dbExtConfig.getMode());
            }).collect(Collectors.toSet());
            log.info("准备迁移数据");
            int total = 0;
            for (Class<?> aClass : classes) {
                total += migrateH2ToNowItem(aClass, h2DsFactory, nowDsFactory);
            }
            log.info("迁移完成,累计迁移 {} 条数据", total);
            String dbFiles = h2StorageService.deleteDbFiles();
            log.info("自动备份 h2 数据库文件,备份文件位于：{}", dbFiles);
        } catch (Exception e) {
            throw Lombok.sneakyThrow(e);
        }
    }

    private static int migrateH2ToNowItem(Class<?> aClass, DSFactory h2DsFactory, DSFactory mysqlDsFactory) throws SQLException {
        TableName tableName = aClass.getAnnotation(TableName.class);
        log.info("开始迁移 {} {}", tableName.name(), tableName.value());
        int total = 0;
        while (true) {
            Entity where = Entity.create(tableName.value());
            PageResult<Entity> pageResult;
            Db db = Db.use(h2DsFactory.getDataSource());
            Page page = new Page(1, 200);
            try {
                pageResult = db.page(where, page);
            } catch (Exception e) {
                throw Lombok.sneakyThrow(e);
            }
            if (pageResult.isEmpty()) {
                break;
            }
            total += pageResult.size();
            Db db2 = Db.use(mysqlDsFactory.getDataSource());
            db2.insert(pageResult);
            // 删除数据
            Entity deleteWhere = Entity.create(tableName.value());
            deleteWhere.set("id", pageResult.stream().map(entity -> entity.getStr("id")).collect(Collectors.toList()));
            db.del(deleteWhere);
        }
        log.info("{} 迁移成功 {} 条数据", tableName.name(), total);
        return total;
    }

    /**
     * 加载 本地已经执行的记录
     *
     * @return sha1 log
     * @author bwcx_jzy
     */
    public static Set<String> loadExecuteSqlLog() {
        File localPath = dbLocalPath();
        File file = FileUtil.file(localPath, "execute.init.sql.log");
        if (!FileUtil.isFile(file)) {
            // 不存在或者是文件夹
            FileUtil.del(file);
            return new LinkedHashSet<>();
        }
        List<String> strings = FileUtil.readLines(file, CharsetUtil.CHARSET_UTF_8);
        return new LinkedHashSet<>(strings);
    }

    /**
     * 获取数据库保存路径
     *
     * @return 默认本地数据目录下面的 db 目录
     * @author bwcx_jzy
     */
    public static File dbLocalPath() {
        return FileUtil.file(ExtConfigBean.getPath(), DB);
    }

    /**
     * 清除执行记录
     */
    public static void clearExecuteSqlLog() {
        File localPath = dbLocalPath();
        File file = FileUtil.file(localPath, "execute.init.sql.log");
        FileUtil.del(file);
    }

    /**
     * 保存本地已经执行的记录
     *
     * @author bwcx_jzy
     */
    public static void saveExecuteSqlLog(Set<String> logs) {
        File localPath = dbLocalPath();
        File file = FileUtil.file(localPath, "execute.init.sql.log");
        FileUtil.writeUtf8Lines(logs, file);
    }

    /**
     * 获得单例的 IStorageService
     *
     * @return 单例的 IStorageService
     */
    public static IStorageService get() {
        Assert.notNull(mode, "当前数据库模式未知");
        return Singleton.get(IStorageService.class.getName(), (CheckedUtil.Func0Rt<IStorageService>) () -> doCreateStorageService(mode));
    }


    /**
     * 根据用户引入的拼音引擎jar，自动创建对应的拼音引擎对象<br>
     * 推荐创建的引擎单例使用，此方法每次调用会返回新的引擎
     *
     * @return {@code EngineFactory}
     */
    private static IStorageService doCreateStorageService(DbExtConfig.Mode mode) {
        final List<IStorageService> storageServiceList = ServiceLoaderUtil.loadList(IStorageService.class);
        if (storageServiceList != null) {
            for (IStorageService storageService : storageServiceList) {
                if (storageService.mode() == mode) {
                    return storageService;
                }
            }
        }
        throw new RuntimeException("No Jpom Storage " + mode + " jar found ! Please add one of it to your project !");
    }
}
