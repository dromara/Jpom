package top.jpom.transport;

/**
 * @author bwcx_jzy
 * @since 2022/12/23
 */
public interface IUrlItem {

    /**
     * 请求路径
     *
     * @return path
     */
    String path();

    /**
     * 请求超时时间
     *
     * @return 超时时间
     */
    int timeout();
}
