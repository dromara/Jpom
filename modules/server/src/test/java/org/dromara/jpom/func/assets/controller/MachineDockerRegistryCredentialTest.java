/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.assets.controller;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.jpom.DockerUtil;
import org.dromara.jpom.TestI18nSupport;
import org.dromara.jpom.func.assets.model.MachineDockerModel;
import org.dromara.jpom.func.assets.server.MachineDockerServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * docker 仓库凭据不外发的回归（2026-11-19 外部安全报告，CVSS 6.5）。
 * <p>
 * 修复前 {@code pullImageCmd} 对调用者传入的任意 repository 都附加资产自己保存的
 * {@code registryUsername/registryPassword}，工作空间用户指定自己的 registry 即可拿到资产登录。
 * 现在只有镜像与资产配置的仓库同域时才附带凭据，其它情况按匿名拉取。
 * <p>
 * 同时覆盖：资产列表/集群列表返回的 {@link MachineDockerModel} 不允许再序列化 {@code registryPassword}。
 *
 * @author jpom
 */
public class MachineDockerRegistryCredentialTest {

    @BeforeEach
    public void setUp() {
        TestI18nSupport.init();
    }

    @Test
    public void testSameRegistryKeepsCredential() {
        assertTrue(DockerUtil.registryMatches("127.0.0.1:5000/admin/secret:1.0", "http://127.0.0.1:5000"));
        assertTrue(DockerUtil.registryMatches("127.0.0.1:5000/admin/secret", "127.0.0.1:5000"));
        assertTrue(DockerUtil.registryMatches("registry.example.com/library/nginx", "https://registry.example.com/v2/"));
        assertTrue(DockerUtil.registryMatches("REGISTRY.example.com/library/nginx", "https://registry.example.com"));
        assertTrue(DockerUtil.registryMatches("localhost:5000/foo", "http://localhost:5000"));
        assertTrue(DockerUtil.registryMatches("library/nginx", "https://index.docker.io/v1/"));
        assertTrue(DockerUtil.registryMatches("nginx:1.25", "docker.io"));
    }

    @Test
    public void testForeignRegistryDropsCredential() {
        assertFalse(DockerUtil.registryMatches("attacker.test:5000/evil", "http://127.0.0.1:5000"));
        assertFalse(DockerUtil.registryMatches("registry.example.com/library/nginx", "https://registry.other.com"));
        // 只比较域名，协议/端口/路径不同不影响同域判定，但跨域必须拒绝
        assertFalse(DockerUtil.registryMatches("example.com.attacker.test/nginx", "https://example.com"));
    }

    @Test
    public void testEmptyRegistryUrlNeverSendsCredential() {
        assertFalse(DockerUtil.registryMatches("library/nginx", null));
        assertFalse(DockerUtil.registryMatches("library/nginx", ""));
        assertFalse(DockerUtil.registryMatches("registry.example.com/foo", "   "));
        assertFalse(DockerUtil.registryMatches("", "https://registry.example.com"));
        assertFalse(DockerUtil.registryMatches(null, "https://registry.example.com"));
    }

    /**
     * 列表接口不再回显密码，编辑表单留空时必须沿用原密码，否则保存一次资产就把仓库凭据清空
     */
    @Test
    public void testResolveRegistryPassword() {
        MachineDockerServer machineDockerServer = mock(MachineDockerServer.class);
        MachineDockerModel oldModel = new MachineDockerModel();
        oldModel.setId("asset-1");
        oldModel.setRegistryPassword("old-password");
        when(machineDockerServer.getByKey("asset-1", false)).thenReturn(oldModel);

        // 填写新密码：以请求为准
        MachineDockerModel newPasswordModel = new MachineDockerModel();
        newPasswordModel.setId("asset-1");
        newPasswordModel.setRegistryUrl("https://registry.example.com");
        newPasswordModel.setRegistryPassword("new-password");
        assertEquals("new-password", MachineDockerController.resolveRegistryPassword(newPasswordModel, machineDockerServer));

        // 留空：沿用原密码
        MachineDockerModel emptyPasswordModel = new MachineDockerModel();
        emptyPasswordModel.setId("asset-1");
        emptyPasswordModel.setRegistryUrl("https://registry.example.com");
        assertEquals("old-password", MachineDockerController.resolveRegistryPassword(emptyPasswordModel, machineDockerServer));

        // 没有仓库地址：不做回填
        MachineDockerModel noRegistryModel = new MachineDockerModel();
        noRegistryModel.setId("asset-1");
        noRegistryModel.setRegistryPassword(null);
        assertNull(MachineDockerController.resolveRegistryPassword(noRegistryModel, machineDockerServer));

        // 资产不存在：保持请求值
        MachineDockerModel missingModel = new MachineDockerModel();
        missingModel.setId("missing");
        missingModel.setRegistryUrl("https://registry.example.com");
        assertNull(MachineDockerController.resolveRegistryPassword(missingModel, machineDockerServer));
    }

    @Test
    public void testRegistryPasswordNotSerialized() throws Exception {
        MachineDockerModel model = new MachineDockerModel();
        model.setId("asset-1");
        model.setName("docker-1");
        model.setRegistryUsername("admin");
        model.setRegistryPassword("super-secret");
        model.setRegistryUrl("https://registry.example.com");

        String jackson = new ObjectMapper().writeValueAsString(model);
        assertFalse(jackson.contains("super-secret"), "Jackson 序列化不允许输出仓库密码：" + jackson);
        assertFalse(jackson.contains("registryPassword"), "响应里不应再出现 registryPassword 字段：" + jackson);
        // 其它字段照常返回，编辑表单仍可回填仓库地址与账号
        assertTrue(jackson.contains("registry.example.com"));
        assertTrue(jackson.contains("admin"));

        String fastjson = JSON.toJSONString(model);
        assertFalse(fastjson.contains("super-secret"), "fastjson2 序列化不允许输出仓库密码：" + fastjson);
        assertFalse(fastjson.contains("registryPassword"));
    }
}
