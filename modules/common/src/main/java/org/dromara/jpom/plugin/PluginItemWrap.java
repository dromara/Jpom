/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.plugin;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.keepbx.jpom.plugins.IPlugin;
import cn.keepbx.jpom.plugins.PluginConfig;
import lombok.Getter;

/**
 * 插件端对象
 *
 * @author bwcx_jzy
 * @since 2021/12/24
 */
@Getter
public class PluginItemWrap {

    /**
     * 配置相关
     */
    private final PluginConfig pluginConfig;

    /**
     * 插件名
     */
    private final String name;

    /**
     * 插件类名
     */
    private final Class<? extends IPlugin> className;

    /**
     * 插件对象
     */
    private volatile IPlugin plugin;

    public PluginItemWrap(Class<? extends IPlugin> className) {
        this.className = className;
        this.pluginConfig = className.getAnnotation(PluginConfig.class);
        this.name = this.pluginConfig.name();
    }

    public IPlugin getPlugin() {
        if (plugin == null) {
            synchronized (className) {
                if (plugin == null) {
                    //
                    boolean nativeObject = this.pluginConfig.nativeObject();
                    if (nativeObject) {
                        plugin = ReflectUtil.newInstance(className);
                    } else {
                        plugin = SpringUtil.getBean(className);
                    }
                }
            }
        }
        return plugin;
    }
}
