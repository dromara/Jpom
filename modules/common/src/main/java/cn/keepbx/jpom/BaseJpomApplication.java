package cn.keepbx.jpom;

/**
 * Jpom
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
public abstract class BaseJpomApplication {
    protected static String[] args;

    /**
     * 获取程序命令行参数
     *
     * @return 数组
     */
    public static String[] getArgs() {
        return args;
    }
}
