/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
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
     * 静态文件下载
     */
    public static final String STATIC_FILE_STORAGE_DOWNLOAD = API + "file-storage/static/download/{id}/{token}";
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
