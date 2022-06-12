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
package io.jpom.system;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ClassUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.net.URL;
import java.util.List;

/**
 * 动态读取外部配置文件
 *
 * @author jiangzeyin
 * @since 2019/3/5
 */
public class ExtConfigEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        {
            YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();
            Resource resource = ExtConfigBean.getResource();
            try {
                List<PropertySource<?>> propertySources = yamlPropertySourceLoader.load(ExtConfigBean.FILE_NAME, resource);
                propertySources.forEach(propertySource -> environment.getPropertySources().addLast(propertySource));
            } catch (Exception e) {
                Console.error(e.getMessage());
            }
        }
        {
            // 兼容一些全局默认配置属性
            List<URL> resources = ClassUtil.getResources("bin/extConfigDefault.yml");
            if (resources.isEmpty()) {
                resources.add(ClassUtil.getResourceURL("bin/extConfigDefault.yml"));
            }
            for (int i = 0; i < resources.size(); i++) {
                URL resource = resources.get(i);
                Resource extConfigDefault = new UrlResource(resource);
                if (extConfigDefault.exists()) {
                    YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();
                    try {
                        List<PropertySource<?>> propertySources = yamlPropertySourceLoader.load("extConfigDefault" + i + ".yml", extConfigDefault);
                        propertySources.forEach(propertySource -> environment.getPropertySources().addLast(propertySource));
                    } catch (Exception e) {
                        Console.error(e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER - 1;
    }
}
