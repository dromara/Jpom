package io.jpom.common;

/**
 * Server 开发接口api 列表
 *
 * @author bwcx_jzy
 * @date 2019/8/5
 */
public class ServerOpenApi {

    public static final String HEAD = "JPOM-TOKEN";

    public static final String UPDATE_NODE_INFO = "/api/node/update";

    /**
     * 安装id
     */
    public static final String INSTALL_ID = "/api/installId";

    /**
     * 触发构建, 第一级构建id,第二级token
     */
    public static final String BUILD_TRIGGER_BUILD = "/api/build/{id}/{token}";
}
