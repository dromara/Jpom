package cn.keepbx.jpom.util;

import cn.hutool.core.util.StrUtil;

/**
 * main 方法运行参数工具
 *
 * @author jiangzeyin
 * @date 2019/4/7
 */
public class ArgsUtil {
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
}
