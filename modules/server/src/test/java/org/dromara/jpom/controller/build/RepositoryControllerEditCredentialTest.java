/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.build;

import org.dromara.jpom.TestI18nSupport;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.model.data.RepositoryModel;
import org.dromara.jpom.service.dblog.BuildInfoService;
import org.dromara.jpom.service.dblog.RepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 仓库编辑（/build/repository/edit）凭据回填的工作空间隔离回归。
 * <p>
 * 背景：编辑仓库时会把数据库里保存的 password/rsaPrv 回填进请求对象，随后 git 插件用请求里的
 * gitUrl 执行 {@code git ls-remote}。如果回填走的是非工作空间隔离的 {@code getByKey(id, false)}，
 * 工作空间 A 的 EDIT 用户只要知道工作空间 B 的仓库 id，就能让服务端把 B 的 git 密码以 HTTP Basic
 * 发到自己的地址（2026-11-19 外部安全报告）。因此回填必须只用 scoped 查询。
 * </p>
 *
 * @author jpom
 */
public class RepositoryControllerEditCredentialTest {

    private RepositoryService repositoryService;
    private RepositoryController controller;
    private MockHttpServletRequest request;

    @BeforeEach
    public void setUp() {
        TestI18nSupport.init();
        repositoryService = mock(RepositoryService.class);
        controller = new RepositoryController(repositoryService, mock(BuildInfoService.class));
        request = new MockHttpServletRequest();
    }

    /**
     * 其他工作空间的仓库 id：必须直接拒绝，且不得回填任何服务端凭据（否则会被发到调用者的 gitUrl）。
     */
    @Test
    public void testForeignRepositoryIdRejectedWithoutFillingCredentials() {
        RepositoryModel req = new RepositoryModel();
        req.setId("foreign-repo");
        req.setPassword("attacker-supplied");
        // scoped 查询（当前空间 + GLOBAL）查不到 => 属于其他工作空间
        when(repositoryService.getByKey("foreign-repo", request, false)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> controller.applyStoredCredentials(req, request));

        // 唯一允许的读取入口是带工作空间条件的查询
        verify(repositoryService).getByKey("foreign-repo", request, false);
        // 不允许回退到非隔离查询（修复前就是 getByKey(id, false)）
        verify(repositoryService, never()).getByKey(anyString(), anyBoolean());
        verify(repositoryService, never()).getByKey(anyString());
        verify(repositoryService, never()).getByKey(anyString(), any(HttpServletRequest.class));
        // 拒绝后请求对象保持原样：没有任何服务端凭据被带出去
        assertEquals("attacker-supplied", req.getPassword());
        assertNull(req.getRsaPrv());
    }

    /**
     * 本工作空间仓库：沿用 scoped 查询，且 fill=false 取到的密码/私钥要能正常回填（不能是脱敏后的空值）。
     */
    @Test
    public void testOwnRepositoryCredentialsFilledFromScopedQuery() {
        RepositoryModel stored = new RepositoryModel();
        stored.setId("my-repo");
        stored.setPassword("stored-pwd");
        stored.setRsaPrv("stored-rsa");
        when(repositoryService.getByKey("my-repo", request, false)).thenReturn(stored);

        RepositoryModel req = new RepositoryModel();
        req.setId("my-repo");
        controller.applyStoredCredentials(req, request);

        assertEquals("stored-pwd", req.getPassword());
        assertEquals("stored-rsa", req.getRsaPrv());
        verify(repositoryService, never()).getByKey(anyString(), anyBoolean());
    }

    /**
     * 请求里已显式填写的凭据优先，不被数据库值覆盖（编辑「修改密码」场景）。
     */
    @Test
    public void testRequestCredentialsWinOverStored() {
        RepositoryModel stored = new RepositoryModel();
        stored.setId("my-repo");
        stored.setPassword("old-pwd");
        stored.setRsaPrv("old-rsa");
        when(repositoryService.getByKey("my-repo", request, false)).thenReturn(stored);

        RepositoryModel req = new RepositoryModel();
        req.setId("my-repo");
        req.setPassword("new-pwd");
        controller.applyStoredCredentials(req, request);

        assertEquals("new-pwd", req.getPassword());
        assertEquals("old-rsa", req.getRsaPrv());
    }

    /**
     * GLOBAL 仓库按设计对所有工作空间可读，scoped 查询会命中，因此允许回填。
     */
    @Test
    public void testGlobalRepositoryStillResolved() {
        RepositoryModel stored = new RepositoryModel();
        stored.setId("global-repo");
        stored.setWorkspaceId(ServerConst.WORKSPACE_GLOBAL);
        stored.setPassword("global-pwd");
        when(repositoryService.getByKey("global-repo", request, false)).thenReturn(stored);

        RepositoryModel req = new RepositoryModel();
        req.setId("global-repo");
        controller.applyStoredCredentials(req, request);

        assertEquals("global-pwd", req.getPassword());
    }

    /**
     * 新增仓库（无 id）：不查库、不回填。
     */
    @Test
    public void testInsertDoesNotQueryRepository() {
        RepositoryModel req = new RepositoryModel();
        req.setPassword("new-pwd");
        controller.applyStoredCredentials(req, request);

        assertEquals("new-pwd", req.getPassword());
        verifyNoInteractions(repositoryService);
    }
}
