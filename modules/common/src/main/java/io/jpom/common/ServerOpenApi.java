/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.common;

/**
 * Server 开发接口api 列表
 *
 * @author bwcx_jzy
 * @since 2019/8/5
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
     * 接收推送
     */
    public static final String RECEIVE_PUSH = API + "node/receive_push";

    public static final String PUSH_NODE_KEY = "--auto-push-to-server";

    /**
     * 安装id
     */
    public static final String INSTALL_ID = API + "/installId";

    /**
     * 触发构建(新), 第一级构建id,第二级token
     */
    public static final String BUILD_TRIGGER_BUILD2 = API + "/build2/{id}/{token}";

    /**
     * 触发构建 批量触发
     */
    public static final String BUILD_TRIGGER_BUILD_BATCH = API + "/build_batch";
    /**
     * 获取当前构建状态和日志
     */
    public static final String BUILD_TRIGGER_STATUS = API + "/build_status";
}
