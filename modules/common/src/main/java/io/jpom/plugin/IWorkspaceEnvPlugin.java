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
package io.jpom.plugin;

import io.jpom.common.Const;

import java.util.Map;

/**
 * 工作空间环境变量 插件
 *
 * @author bwcx_jzy
 * @since 2022/8/30
 */
public interface IWorkspaceEnvPlugin extends IDefaultPlugin {

    String PLUGIN_NAME = "workspace-Env";

    /**
     * 转化 工作空间环境变量
     *
     * @param parameter 插件的参数
     * @param key       参数中的key
     * @return 转化后的
     * @throws Exception 异常
     */
    default String convertRefEnvValue(Map<String, Object> parameter, String key) throws Exception {
        String workspaceId = (String) parameter.get(Const.WORKSPACEID_REQ_HEADER);
        return this.convertRefEnvValue(workspaceId, (String) parameter.get(key));
    }

    /**
     * 转化 工作空间环境变量
     *
     * @param workspaceId 工作空间
     * @param value       值
     * @return 如果存在值，则返回环境变量值。不存在则返回原始值
     * @throws Exception 异常
     */
    default String convertRefEnvValue(String workspaceId, String value) throws Exception {
        return (String) PluginFactory.getPlugin(PLUGIN_NAME).execute("convert", "workspaceId", workspaceId, "value", value);
    }
}
