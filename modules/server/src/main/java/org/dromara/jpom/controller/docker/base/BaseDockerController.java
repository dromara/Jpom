/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.docker.base;

import org.dromara.jpom.common.BaseServerController;

import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2023/3/3
 */
public abstract class BaseDockerController extends BaseServerController {


    /**
     * 根据参数 id 获取 docker 信息
     *
     * @param id id
     * @return docker 信息
     */
    protected abstract Map<String, Object> toDockerParameter(String id);

}
