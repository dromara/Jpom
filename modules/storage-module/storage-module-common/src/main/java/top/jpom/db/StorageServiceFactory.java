package top.jpom.db;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Singleton;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ServiceLoaderUtil;
import io.jpom.system.ExtConfigBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
        return Singleton.get(IStorageService.class.getName(), StorageServiceFactory::doCreateStorageService);
    }


    /**
     * 根据用户引入的拼音引擎jar，自动创建对应的拼音引擎对象<br>
     * 推荐创建的引擎单例使用，此方法每次调用会返回新的引擎
     *
     * @return {@code EngineFactory}
     */
    private static IStorageService doCreateStorageService() {
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
