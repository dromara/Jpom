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

import cn.hutool.core.util.ReflectUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
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
