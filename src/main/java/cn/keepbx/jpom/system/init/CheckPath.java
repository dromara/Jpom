package cn.keepbx.jpom.system.init;

import cn.hutool.core.io.FileUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import cn.keepbx.jpom.system.ExtConfigBean;

import java.io.File;

/**
 * @author jiangzeyin
 * @date 2019/3/26
 */
@PreLoadClass
public class CheckPath {

    @PreLoadMethod
    private static void init() {
        String path = ExtConfigBean.getInstance().getPath();
        File file = FileUtil.file(path);
        try {
            FileUtil.mkdir(file);
            file = FileUtil.createTempFile("jpom", ".temp", file, true);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("jpom 数据目录权限不足", e);
        }
        FileUtil.del(file);
    }
}
