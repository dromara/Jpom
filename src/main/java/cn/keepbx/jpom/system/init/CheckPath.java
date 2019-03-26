package cn.keepbx.jpom.system.init;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import cn.keepbx.jpom.system.ExtConfigBean;

import java.io.File;
import java.io.IOException;

/**
 * 数据目录权限检查
 *
 * @author jiangzeyin
 * @date 2019/3/26
 */
@PreLoadClass
public class CheckPath {

    @PreLoadMethod
    private static void init() throws IOException {
        String path = ExtConfigBean.getInstance().getPath();
        File file = FileUtil.file(path);
        try {
            FileUtil.mkdir(file);
            file = FileUtil.createTempFile("jpom", ".temp", file, true);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(StrUtil.format("jpom 数据目录权限不足,目录位置：{},请检查当前用户是否有此目录权限", path), e);
            System.exit(-1);
        }
        FileUtil.del(file);
        DefaultSystemLog.LOG().info("Jpom外部配置文件路径：" + ExtConfigBean.getResource().getURL().toString());
    }
}
