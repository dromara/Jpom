/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.configuration;

import org.junit.Assert;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

/**
 * 插件端默认配置 {@code version-count}/{@code version-path} 测试。
 *
 * <p>版本快照属于资源消耗型功能，默认必须关闭（0），否则所有项目发布都会额外占用磁盘。
 *
 * @author Jpom
 * @since 2.11.15
 */
public class AgentDefaultVersionConfigTest {

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadProjectConfig(String resource) throws Exception {
        try (InputStream inputStream = AgentDefaultVersionConfigTest.class.getClassLoader().getResourceAsStream(resource)) {
            Assert.assertNotNull("缺少配置文件: " + resource, inputStream);
            Map<String, Object> root = new Yaml().load(inputStream);
            Map<String, Object> jpom = (Map<String, Object>) root.get("jpom");
            return (Map<String, Object>) jpom.get("project");
        }
    }

    @Test
    public void testVersionSnapshotDisabledByDefault() throws Exception {
        for (String resource : new String[]{"config_default/application.yml", "application.yml"}) {
            Map<String, Object> project = loadProjectConfig(resource);
            Assert.assertNotNull(resource, project);
            Assert.assertEquals(resource, 0, ((Number) project.get("version-count")).intValue());
            Assert.assertTrue(resource, project.containsKey("version-path"));
        }
    }
}
