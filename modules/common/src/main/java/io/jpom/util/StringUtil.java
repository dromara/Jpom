package io.jpom.util;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;

import java.io.File;

/**
 * main 方法运行参数工具
 *
 * @author jiangzeyin
 * @date 2019/4/7
 */
public class StringUtil {

    /**
     * 支持的压缩包格式
     */
    public static final String[] PACKAGE_EXT = new String[]{"tar.bz2", "tar.gz", "tar", "bz2", "zip", "gz"};

    /**
     * 获取启动参数
     *
     * @param args 所有参数
     * @param name 参数名
     * @return 值
     */
    public static String getArgsValue(String[] args, String name) {
        if (args == null) {
            return null;
        }
        for (String item : args) {
            item = StrUtil.trim(item);
            if (item.startsWith("--" + name + "=")) {
                return item.substring(name.length() + 3);
            }
        }
        return null;
    }

    /**
     * id输入规则
     *
     * @param value 值
     * @param min   最短
     * @param max   最长
     * @return true
     */
    public static boolean isGeneral(CharSequence value, int min, int max) {
        String reg = "^[a-zA-Z0-9_-]{" + min + "," + max + "}$";
        return Validator.isMatchRegex(reg, value);
    }

    /**
     * 删除文件开始的路径
     *
     * @param file      要删除的文件
     * @param startPath 开始的路径
     * @param inName    是否返回文件名
     * @return /test/a.txt /test/  a.txt
     */
    public static String delStartPath(File file, String startPath, boolean inName) {
        String newWhitePath;
        if (inName) {
            newWhitePath = FileUtil.getAbsolutePath(file.getAbsolutePath());
        } else {
            newWhitePath = FileUtil.getAbsolutePath(file.getParentFile());
        }
        String itemAbsPath = FileUtil.getAbsolutePath(new File(startPath));
        itemAbsPath = FileUtil.normalize(itemAbsPath);
        newWhitePath = FileUtil.normalize(newWhitePath);
        String path = newWhitePath.substring(newWhitePath.indexOf(itemAbsPath) + itemAbsPath.length());
        path = FileUtil.normalize(path);
        if (path.startsWith(StrUtil.SLASH)) {
            path = path.substring(1);
        }
        return path;
    }

    /**
     * 获取jdk 中的tools jar文件路径
     *
     * @return file
     */
    public static File getToolsJar() {
        File file = new File(SystemUtil.getJavaRuntimeInfo().getHomeDir());
        return new File(file.getParentFile(), "lib/tools.jar");
    }

    /**
     * 指定时间的下一个刻度
     *
     * @return String
     */
    public static String getNextScaleTime(String time, Long millis) {
        DateTime dateTime = DateUtil.parseTime(time);
        if (millis == null) {
            millis = 30 * 1000L;
        }
        DateTime newTime = dateTime.offsetNew(DateField.SECOND, (int) (millis / 1000));
        return DateUtil.formatTime(newTime);
    }
}
