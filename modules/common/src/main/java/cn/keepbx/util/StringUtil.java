package cn.keepbx.util;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;

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
        return Validator.isMactchRegex(reg, value);
    }
}
