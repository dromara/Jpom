package cn.keepbx.jpom;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;

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

    private static Charset charset;

    /**
     * 获取程序命令行参数
     *
     * @return 数组
     */
    public static String[] getArgs() {
        return args;
    }

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
}
