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

import org.dromara.jpom.model.data.RepositoryModel;
import org.dromara.jpom.service.dblog.RepositoryService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.dromara.jpom.TestI18nSupport;

/**
 * 校验构建分支列表接口：
 * <ol>
 *     <li>按工作空间查询仓库（issue #115，禁止跨空间访问）</li>
 *     <li>必须使用 fill=false 的查询：fill=true 会触发 {@code RepositoryService.fillSelectResult}
 *     把密码/私钥脱敏置空，交给 git 插件后变成空凭证（2.11.13 起「获取分支」认证失败回归）</li>
 * </ol>
 *
 * @author jpom
 */
public class BuildInfoControllerBranchListTest {

    private BuildInfoController createController(RepositoryService repositoryService) {
        TestI18nSupport.init();
        return new BuildInfoController(null, null, null, null, repositoryService, null, null, null, null, null, null, null);
    }

    @Test
    public void testRejectForeignRepositoryId() {
        RepositoryService repositoryService = mock(RepositoryService.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(repositoryService.getByKey("foreign-repo", request, false)).thenReturn(null);
        BuildInfoController controller = this.createController(repositoryService);
        assertThrows(IllegalArgumentException.class, () -> controller.branchList("foreign-repo", request));
        // 工作空间校验保留：fill=false 的查询同样带「当前空间 + GLOBAL」条件
        verify(repositoryService).getByKey("foreign-repo", request, false);
        // 不允许使用无工作空间条件的查询
        verify(repositoryService, never()).getByKey("foreign-repo", false);
        // 不允许使用 fill=true 的查询：会把仓库密码/私钥脱敏置空，导致 git 认证失败
        verify(repositoryService, never()).getByKey("foreign-repo", request);
        verify(repositoryService, never()).getByKey(anyString(), anyBoolean());
        verify(repositoryService, never()).getByKey(anyString());
    }

    /**
     * 当前工作空间（或全局共享）的仓库可以解析，确认修复不破坏正常流程。
     * 使用 SVN 仓库（repoType=1）让业务校验在进入 git 插件前失败，方便断言已越过工作空间查询。
     */
    @Test
    public void testWorkspaceRepositoryIdResolvedWithCredentials() {
        RepositoryService repositoryService = mock(RepositoryService.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        RepositoryModel repositoryModel = new RepositoryModel();
        repositoryModel.setRepoType(1);
        // fill=false 的查询会带上原始凭证，这里顺带守护「脱敏字段被置空」的回归
        repositoryModel.setPassword("token-value");
        repositoryModel.setRsaPrv("ssh-key-placeholder");
        when(repositoryService.getByKey("own-repo", request, false)).thenReturn(repositoryModel);
        BuildInfoController controller = this.createController(repositoryService);
        assertThrows(IllegalStateException.class, () -> controller.branchList("own-repo", request));
        verify(repositoryService).getByKey("own-repo", request, false);
        verify(repositoryService, never()).getByKey("own-repo", request);
    }
}
