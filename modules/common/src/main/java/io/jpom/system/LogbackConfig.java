package io.jpom.system;

import ch.qos.logback.core.PropertyDefinerBase;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.system.SystemUtil;
import org.springframework.util.Assert;

import java.io.File;

/**
 * @author bwcx_jzy
 * @since 2022/12/7
 */
public class LogbackConfig extends PropertyDefinerBase {

    public static String getPath() {
        String jpomLog = SystemUtil.get("JPOM_LOG");
        Assert.hasText(jpomLog, "没有配置 JPOM_LOG");
        return jpomLog;
    }

    @Override
    public String getPropertyValue() {
        String jpomLog = SystemUtil.get("JPOM_LOG");
        jpomLog = Opt.ofBlankAble(jpomLog).orElseGet(() -> {
            String locationPath = ClassUtil.getLocationPath(LogbackConfig.class);
            File file = FileUtil.file(FileUtil.getParent(locationPath, 2), "logs");
            String path = FileUtil.getAbsolutePath(file);
            System.out.println(path);
            return path;
        });
        SystemUtil.set("JPOM_LOG", jpomLog);
        return jpomLog;
    }
}
