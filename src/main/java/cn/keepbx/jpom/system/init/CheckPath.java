package cn.keepbx.jpom.system.init;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
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

    private static final String CLASS_NAME = "com.sun.tools.attach.VirtualMachine";

    @PreLoadMethod(1)
    private static void checkPath() throws IOException {
        String path = ExtConfigBean.getInstance().getPath();
        String extConfigPath = ExtConfigBean.getResource().getURL().toString();
        File file = FileUtil.file(path);
        try {
            FileUtil.mkdir(file);
            file = FileUtil.createTempFile("jpom", ".temp", file, true);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(StrUtil.format("Jpom创建数据目录失败,目录位置：{},请检查当前用户是否有此目录权限或修改配置文件：{}中的jpom.path为可创建目录的路径", path, extConfigPath), e);
            System.exit(-1);
        }
        FileUtil.del(file);
        DefaultSystemLog.LOG().info("Jpom外部配置文件路径：" + extConfigPath);
    }

    @PreLoadMethod(2)
    private static void checkToolsJar() {
        try {
            ClassUtil.loadClass(CLASS_NAME, false);
        } catch (Exception e) {
            File file = getToolsJar();
            DefaultSystemLog.ERROR().error("当前JDK中没有找到tools.jar,请检查当前JDK是否安装完整，文件完整路径是：" + file.getAbsolutePath(), e);
            System.exit(-1);
        }
    }

    private static File getToolsJar() {
        File file = new File(SystemUtil.getJavaRuntimeInfo().getHomeDir());
        return new File(file.getParentFile(), "lib/tools.jar");
    }
}
