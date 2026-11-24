/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.build;

import cn.hutool.core.exceptions.ValidateException;
import org.dromara.jpom.IDockerConfigPlugin;
import org.dromara.jpom.TestI18nSupport;
import org.dromara.jpom.func.assets.server.MachineDockerServer;
import org.dromara.jpom.service.docker.DockerInfoService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Docker 构建 DSL fromTag 白名单校验（VL-001）回归测试
 * <p>
 * fromTag 会参与按标签查询 docker 节点，必须在保存构建配置时限制字符集，
 * 不允许引号、括号、注释符、分号、空格等可被拼接进 SQL/表达式的内容。
 *
 * @author jpom
 * @since 2026/11/18
 */
public class DockerYmlDslFromTagTest {

    @BeforeAll
    public static void setUp() {
        TestI18nSupport.init();
    }

    @Test
    public void testRejectSqlInjectionPayload() {
        String payload = "x') OR updatexml(1,concat(0x7e,(select database())),1) -- ";
        assertThrows(ValidateException.class, () -> DockerYmlDsl.validateFromTag(payload));
    }

    @Test
    public void testRejectUnsafeCharacters() {
        assertThrows(ValidateException.class, () -> DockerYmlDsl.validateFromTag("openjdk:8'"));
        assertThrows(ValidateException.class, () -> DockerYmlDsl.validateFromTag("openjdk\"8"));
        assertThrows(ValidateException.class, () -> DockerYmlDsl.validateFromTag("a;rm -rf /"));
        assertThrows(ValidateException.class, () -> DockerYmlDsl.validateFromTag("a b"));
        assertThrows(ValidateException.class, () -> DockerYmlDsl.validateFromTag("<img src=x>"));
        assertThrows(ValidateException.class, () -> DockerYmlDsl.validateFromTag("-leading"));
    }

    @Test
    public void testAcceptNormalTags() {
        assertDoesNotThrow(() -> DockerYmlDsl.validateFromTag("centos7"));
        assertDoesNotThrow(() -> DockerYmlDsl.validateFromTag("openjdk:8"));
        assertDoesNotThrow(() -> DockerYmlDsl.validateFromTag("registry.example.com/library/openjdk:17-jdk"));
        assertDoesNotThrow(() -> DockerYmlDsl.validateFromTag("my_tag.v1"));
    }

    /**
     * 保存构建配置（check）阶段就拒绝非法 fromTag，且不触发任何 docker 查询
     */
    @Test
    public void testCheckRejectsInvalidFromTagBeforeQuery() {
        DockerYmlDsl dockerYmlDsl = new DockerYmlDsl();
        dockerYmlDsl.setRunsOn("centos7");
        dockerYmlDsl.setFromTag("x') OR 1=1 -- ");
        Map<String, Object> step = Collections.singletonMap("run", "echo 1");
        dockerYmlDsl.setSteps(Collections.singletonList(step));

        DockerInfoService dockerInfoService = mock(DockerInfoService.class);
        MachineDockerServer machineDockerServer = mock(MachineDockerServer.class);
        IDockerConfigPlugin plugin = mock(IDockerConfigPlugin.class);

        assertThrows(ValidateException.class,
            () -> dockerYmlDsl.check(dockerInfoService, machineDockerServer, "DEFAULT", plugin));
        verifyNoInteractions(dockerInfoService, machineDockerServer);
    }

    /**
     * 正常 fromTag 不影响后续校验流程（steps 里没有 run 时的既有错误仍然抛出）
     */
    @Test
    public void testCheckAcceptsNormalFromTag() {
        DockerYmlDsl dockerYmlDsl = new DockerYmlDsl();
        dockerYmlDsl.setRunsOn("centos7");
        dockerYmlDsl.setFromTag("centos7:8");
        dockerYmlDsl.setSteps(Collections.singletonList(Collections.singletonMap("run", "echo 1")));

        assertDoesNotThrow(() -> dockerYmlDsl.check(mock(DockerInfoService.class), mock(MachineDockerServer.class),
            "DEFAULT", mock(IDockerConfigPlugin.class)));
    }
}
