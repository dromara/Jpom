package cn.keepbx.jpom;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;
import cn.keepbx.jpom.common.Type;

import java.nio.charset.Charset;

/**
 * Jpom
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
public abstract class BaseJpomApplication {

    public static final OsInfo OS_INFO = SystemUtil.getOsInfo();

    protected static String[] args;
    /**
     * 应用类型
     */
    private static Type appType;
    private static Charset charset;

    /**
     * 获取程序命令行参数
     *
     * @return 数组
     */
    public static String[] getArgs() {
        return args;
    }

    public BaseJpomApplication(Type appType) {
        BaseJpomApplication.appType = appType;
    }

    /**
     * 获取当前系统编码
     *
     * @return charset
     */
    public static Charset getCharset() {
        if (charset == null) {
            if (OS_INFO.isLinux()) {
                charset = CharsetUtil.CHARSET_UTF_8;
            } else {
                charset = CharsetUtil.CHARSET_GBK;
            }
        }
        return charset;
    }

    public static Type getAppType() {
        return appType;
    }
}
