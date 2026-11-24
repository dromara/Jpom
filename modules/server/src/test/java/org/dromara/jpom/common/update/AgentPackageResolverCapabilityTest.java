/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common.update;

import com.alibaba.fastjson2.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

/**
 * 构建节点能力解析回归（/info manifest.capabilities）。
 * <p>
 * 行为要求：
 * 1. 老插件端（无 capabilities 字段）解析为空集合，绝不能被当成“支持构建”；
 * 2. 引擎不是 Go 时即便声明了 build 也不能用（构建执行器只在 Go 插件端实现）；
 * 3. 大小写、空白要归一化。
 */
public class AgentPackageResolverCapabilityTest {

    private static JSONObject info(String engine, String... capabilities) {
        JSONObject manifest = new JSONObject();
        if (engine != null) {
            manifest.put("engine", engine);
        }
        if (capabilities.length > 0) {
            manifest.put("capabilities", capabilities);
        }
        JSONObject data = new JSONObject();
        data.put("manifest", manifest);
        return data;
    }

    @Test
    public void testLegacyAgentHasNoCapability() {
        JSONObject legacy = new JSONObject();
        legacy.put("manifest", new JSONObject());
        Assert.assertTrue(AgentPackageResolver.resolveCapabilities(legacy).isEmpty());
        Assert.assertFalse(AgentPackageResolver.hasCapability(legacy, AgentPackageResolver.CAPABILITY_BUILD));
    }

    @Test
    public void testResolveCapabilitiesNormalized() {
        Set<String> capabilities = AgentPackageResolver.resolveCapabilities(info("go", "build", " Build "));
        Assert.assertEquals(1, capabilities.size());
        Assert.assertTrue(capabilities.contains("build"));
        Assert.assertTrue(AgentPackageResolver.hasCapability(info("go", "build"), "BUILD"));
    }

    @Test
    public void testEngineParsedTogether() {
        Assert.assertEquals(AgentEngine.GO, AgentPackageResolver.resolveEngine(info("go", "build")));
        Assert.assertEquals(AgentEngine.JAVA, AgentPackageResolver.resolveEngine(info("java")));
        // 老 Go 插件端没有 engine，靠 jarFile 后缀兜底
        JSONObject legacyGo = new JSONObject();
        JSONObject manifest = new JSONObject();
        manifest.put("jarFile", "agent-linux-amd64");
        legacyGo.put("manifest", manifest);
        Assert.assertEquals(AgentEngine.GO, AgentPackageResolver.resolveEngine(legacyGo));
        Assert.assertTrue(AgentPackageResolver.resolveCapabilities(legacyGo).isEmpty());
    }
}
