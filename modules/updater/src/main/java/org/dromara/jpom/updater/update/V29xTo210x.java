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
package org.dromara.jpom.updater.update;

import cn.hutool.core.util.StrUtil;
import freemarker.template.Configuration;
import org.dromara.jpom.updater.utils.FreeMarkerUtil;
import org.dromara.jpom.updater.utils.YamlUtil;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

/**
 * 2.9.x-to-2.10.x 升级工具
 *
 * <pre>
 *     参考文档：https://jpom.top/pages/upgrade/2.9.x-to-2.10.x/
 * </pre>
 *
 * @author hjk
 * @since 12/19/2022
 */
public interface V29xTo210x {

    String EXT_CONFIG_YML = "extConfig.yml";

    String EXT_CONFIG_YML_PATH = EXT_CONFIG_YML;

    String APPLICATION_YML = "application.yml";

    String APPLICATION_YML_PATH = "conf/" + APPLICATION_YML;

    static void showUpdateTips() {
        System.out.println("0. Execute all the following command");
        System.out.println("1. Backup data");
        System.out.println("2. Combine configuration");
    }

    interface Agent {

        static void updateConfig(File extConfigYmlFile) {
            if (!extConfigYmlFile.exists()) {
                System.out.println(StrUtil.format("extConfigYmlFile: {} is not exist", extConfigYmlFile.getAbsolutePath()));
                return;
            }
            Yaml customYaml = YamlUtil.createCustomYaml();
            try (FileInputStream fileInputStream = new FileInputStream(extConfigYmlFile)) {
                Map<String, Object> root = customYaml.load(fileInputStream);

                ClassLoader classLoader = V29xTo210x.class.getClassLoader();
                URL resource = classLoader.getResource("agent/application.yml");
                String agentTemplateFile = resource.getPath();
                Configuration customConfiguration = FreeMarkerUtil.createCustomConfiguration(new File(classLoader.getResource("").getPath()));
                customConfiguration.getTemplate(agentTemplateFile);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    interface Server {

        static void updateConfig(File extConfigYmlFile) {
            return;
        }
    }


}
