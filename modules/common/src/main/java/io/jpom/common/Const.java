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
 * @author Hotstrip
 * Const class
 */
public class Const {
    /**
     * 升级提示语
     */
    public static final String UPGRADE_MSG = "升级(重启)中大约需要30秒～2分钟左右";

    /**
     * 请求 header
     */
    public static final String WORKSPACE_ID_REQ_HEADER = "workspaceId";
    /**
     * 默认的工作空间
     */
    public static final String WORKSPACE_DEFAULT_ID = "DEFAULT";
    /**
     * 默认的分组名
     */
    public static final String DEFAULT_GROUP_NAME = "默认";
//    /**
//     * websocket 传输 agent 包 buffer size
//     */
//    public static final int DEFAULT_BUFFER_SIZE = 1024 * 1024;
    /**
     * id 最大长度
     */
    public static final int ID_MAX_LEN = 50;

    /**
     * 用户名header
     */
    public static final String JPOM_SERVER_USER_NAME = "Jpom-Server-UserName";

    public static final String JPOM_AGENT_AUTHORIZE = "Jpom-Agent-Authorize";

    public static final String DATA = "data";

    public static final int AUTHORIZE_ERROR = 900;
    /**
     * 脚本模板存放路径
     */
    public static final String SCRIPT_DIRECTORY = "script";
    /**
     * 脚本默认运行缓存执行文件路径，考虑 windows 文件被占用情况
     */
    public static final String SCRIPT_RUN_CACHE_DIRECTORY = "script_run_cache";
    /**
     * 授权信息
     */
    public static final String AUTHORIZE = "agent_authorize.json";

    /**
     * 程序升级信息文件
     */
    public static final String UPGRADE = "upgrade.json";
    public static final String RUN_JAR = "run.bin";
    /**
     * 远程版本信息
     */
    public static final String REMOTE_VERSION = "remote_version.json";

    public static final String FILE_NAME = "application.yml";

    /**
     *
     */
    public static final String SYSTEM_ID = "system";

    public static final String SOCKET_MSG_TAG = "JPOM_MSG";

}
