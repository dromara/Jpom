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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.dromara.jpom.TestI18nSupport;

/**
 * 校验构建分支列表接口按工作空间查询仓库（issue #115）
 *
 * @author jpom
 */
public class BuildInfoControllerBranchListTest {

    private BuildInfoController createController(RepositoryService repositoryService) {
        TestI18nSupport.init();
        return new BuildInfoController(null, null, null, null, repositoryService, null, null, null, null, null);
    }

    @Test
    public void testRejectForeignRepositoryId() {
        RepositoryService repositoryService = mock(RepositoryService.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(repositoryService.getByKey("foreign-repo", request)).thenReturn(null);
        BuildInfoController controller = this.createController(repositoryService);
        assertThrows(IllegalArgumentException.class, () -> controller.branchList("foreign-repo", request));
        verify(repositoryService).getByKey("foreign-repo", request);
        // 不允许回退到无工作空间条件的 getByKey(id, false)
        verify(repositoryService, never()).getByKey("foreign-repo", false);
    }

    /**
     * 当前工作空间（或全局共享）的仓库可以解析，确认修复不破坏正常流程。
     * 使用 SVN 仓库（repoType=1）让业务校验在进入 git 插件前失败，方便断言已越过工作空间查询。
     */
    @Test
    public void testWorkspaceRepositoryIdResolved() {
        RepositoryService repositoryService = mock(RepositoryService.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        RepositoryModel repositoryModel = new RepositoryModel();
        repositoryModel.setRepoType(1);
        when(repositoryService.getByKey("own-repo", request)).thenReturn(repositoryModel);
        BuildInfoController controller = this.createController(repositoryService);
        assertThrows(IllegalStateException.class, () -> controller.branchList("own-repo", request));
        verify(repositoryService).getByKey("own-repo", request);
    }
}
