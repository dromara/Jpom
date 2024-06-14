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

import org.dromara.jpom.common.i18n.I18nMessageUtil;

import java.util.function.Supplier;

/**
 * @author Hotstrip
 * Const class
 */
public class Const {
    /**
     * 升级提示语
     */
    public static final Supplier<String> UPGRADE_MSG = () -> I18nMessageUtil.get("i18n.upgrade_duration_message.bab4");

    /**
     * 请求 header
     */
    public static final String WORKSPACE_ID_REQ_HEADER = "workspaceId";
    /**
     * 默认的工作空间
     */
    public static final String WORKSPACE_DEFAULT_ID = "DEFAULT";
    /**
     * 工作空间全局
     */
    public static final String WORKSPACE_GLOBAL = "GLOBAL";
    /**
     * 默认的分组名
     */
    public static final Supplier<String> DEFAULT_GROUP_NAME = () -> I18nMessageUtil.get("i18n.default_setting.18c6");
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

    /**
     * 第一次服务端安装信息
     */
    public static final String INSTALL = "INSTALL.json";
}
