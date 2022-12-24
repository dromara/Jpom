package top.jpom.transform;

/**
 * 消息转换服务
 *
 * @author bwcx_jzy
 * @since 2022/12/24
 */
public interface TransformServer {

    <T> T transform(String data);
}
