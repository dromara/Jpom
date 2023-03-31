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
 * 插件端配置
 *
 * @author jiangzeyin
 * @since 2019/4/16
 */
public class AgentConst {
    /**
     * 白名单文件
     */
    public static final String WHITELIST_DIRECTORY = "whitelistDirectory.json";
    /**
     * 项目数据文件
     */
    public static final String PROJECT = "project.json";

    /**
     * 项目回收文件
     */
//    public static final String PROJECT_RECOVER = "project_recover.json";
    /**
     * 证书文件
     */
    public static final String CERT = "cert.json";
    /**
     * 脚本管理数据文件
     */
    public static final String SCRIPT = "script.json";
    /**
     * 脚本管理执行记录数据文件
     */
    public static final String SCRIPT_LOG = "script_log.json";

    /**
     * nginx配置信息
     */
    public static final String NGINX_CONF = "nginx_conf.json";

    /**
     * 环境变量列表信息
     */
    public static final String WORKSPACE_ENV_VAR = "workspace_env_var.json";


}
