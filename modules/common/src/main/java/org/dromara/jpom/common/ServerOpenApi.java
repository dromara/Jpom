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
package org.dromara.jpom.common;

/**
 * Server 开发接口api 列表
 *
 * @author bwcx_jzy
 * @since 2019/8/5
 */
public class ServerOpenApi {
    /**
     * 用户的token
     */
    public static final String USER_TOKEN_HEAD = "JPOM-USER-TOKEN";

    /**
     * 存放token的http head
     */
    public static final String HTTP_HEAD_AUTHORIZATION = "Authorization";

    public static final String API = "/api/";

    /**
     * 接收推送
     */
    public static final String RECEIVE_PUSH = API + "node/receive_push";

    public static final String PUSH_NODE_KEY = "--auto-push-to-server";
    /**
     * 触发构建(新), 第一级构建id,第二级token
     */
    public static final String BUILD_TRIGGER_BUILD2 = API + "build2/{id}/{token}";

    /**
     * 触发构建 批量触发
     */
    public static final String BUILD_TRIGGER_BUILD_BATCH = API + "build_batch";

    /**
     * 文件下载
     */
    public static final String FILE_STORAGE_DOWNLOAD = API + "file-storage/download/{id}/{token}";
    /**
     * 获取当前构建状态
     */
    public static final String BUILD_TRIGGER_STATUS = API + "build_status";

    /**
     * 获取当前构建日志
     */
    public static final String BUILD_TRIGGER_LOG = API + "build_log";

    /**
     * SSH 脚本执行, 第一级脚本id,第二级token
     */
    public static final String SSH_COMMAND_TRIGGER_URL = API + "ssh_command/{id}/{token}";

    /**
     * SSH 脚本执行 批量触发
     */
    public static final String SSH_COMMAND_TRIGGER_BATCH = API + "ssh_command_batch";

    /**
     * 服务端脚本执行, 第一级脚本id,第二级token
     */
    public static final String SERVER_SCRIPT_TRIGGER_URL = API + "server_script/{id}/{token}";

    /**
     * 服务端脚本执行 批量触发
     */
    public static final String SERVER_SCRIPT_TRIGGER_BATCH = API + "server_script_batch";

    /**
     * 插件端脚本执行, 第一级脚本id,第二级token
     */
    public static final String NODE_SCRIPT_TRIGGER_URL = API + "node_script/{id}/{token}";

    /**
     * 插件端脚本执行 批量触发
     */
    public static final String NODE_SCRIPT_TRIGGER_BATCH = API + "node_script_batch";

    /**
     * 项目触发器, 第一级项目id（服务端存储）,第二级token
     */
    public static final String SERVER_PROJECT_TRIGGER_URL = API + "project/{id}/{token}";

    /**
     * 项目触发器,批量触发
     */
    public static final String SERVER_PROJECT_TRIGGER_BATCH = API + "project_batch";

    /**
     * 环境变量, 第一级脚本id,第二级token
     */
    public static final String SERVER_ENV_VAR_TRIGGER_URL = API + "env-var/{id}/{token}";
}
