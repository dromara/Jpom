/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.plugin;

import cn.keepbx.jpom.plugins.IPlugin;
import org.dromara.jpom.system.ExtConfigBean;

import java.io.InputStream;

/**
 * 插件模块接口
 *
 * @author bwcx_jzy
 * @since 2021/12/22
 */
public interface IDefaultPlugin extends IPlugin, AutoCloseable {

    /**
     * 获取配置文件流
     *
     * @param name 配置文件名称
     * @return InputStream
     */
    default InputStream getConfigResourceInputStream(String name) {
        return ExtConfigBean.tryGetConfigResourceInputStream(name);
    }
}
