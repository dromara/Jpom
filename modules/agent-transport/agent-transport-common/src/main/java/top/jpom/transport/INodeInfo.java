package top.jpom.transport;

/**
 * 节点通讯的 接口
 *
 * @author bwcx_jzy
 * @since 2022/12/23
 */
public interface INodeInfo {

    /**
     * 节点 url
     * <p>
     * HOST:PORT
     *
     * @return 节点 url
     */
    String url();

    /**
     * 协议
     *
     * @return http
     */
    String scheme();

    /**
     * 节点用户
     *
     * @return 用户
     */
    String user();

    /**
     * 节点密码
     *
     * @return 密码
     */
    String password();
}
