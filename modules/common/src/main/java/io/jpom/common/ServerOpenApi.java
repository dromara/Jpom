package io.jpom.common;

/**
 * Server 开发接口api 列表
 *
 * @author bwcx_jzy
 * @date 2019/8/5
 */
public class ServerOpenApi {

    public static final String HEAD = "JPOM-TOKEN";

    /**
     * 用户的token
     */
    public static final String USER_TOKEN_HEAD = "JPOM-USER-TOKEN";

    /**
     * 存放token的http head
     */
    public static final String HTTP_HEAD_AUTHORIZATION = "Authorization";

    public static final String API = "/api/";

    public static final String UPDATE_NODE_INFO = API + "node/update";

    /**
     * 安装id
     */
    public static final String INSTALL_ID = API + "/installId";

    /**
     * 触发构建, 第一级构建id,第二级token
     */
    public static final String BUILD_TRIGGER_BUILD = API + "/build/{id}/{token}";
}
