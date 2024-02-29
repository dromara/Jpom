/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.transport;

import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2022/12/23
 */
public interface IUrlItem {

    /**
     * 请求路径
     *
     * @return path
     */
    String path();

    /**
     * 请求超时时间
     * 单位秒
     *
     * @return 超时时间
     */
    Integer timeout();

    /**
     * 当前工作空间id
     *
     * @return 工作空间
     */
    String workspaceId();

    /**
     * 请求类型
     *
     * @return contentType
     */
    DataContentType contentType();

    /**
     * 请求头
     *
     * @return 请求头
     */
    Map<String, String> header();
}
