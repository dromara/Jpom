/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.dblog;

import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.model.data.RepositoryModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;

/**
 * 仓库读取脱敏行为：{@code fill=true} 的查询会把密码、私钥置空，只能用于返回前端的场景；
 * 需要把凭证交给 git 插件的内部路径（例如构建「获取分支」）必须用 {@code fill=false}。
 *
 * @author jpom
 */
public class RepositoryServiceFillSelectResultTest {

    private RepositoryService repositoryService;

    @BeforeEach
    public void setUp() {
        // 不触发 Spring 依赖，仅执行脱敏方法本身
        repositoryService = mock(RepositoryService.class);
        doCallRealMethod().when(repositoryService).fillSelectResult(any(RepositoryModel.class));
    }

    @Test
    public void testHideCredentialWhenFill() {
        RepositoryModel repositoryModel = new RepositoryModel();
        repositoryModel.setPassword("token-value");
        repositoryModel.setRsaPrv("ssh-key-placeholder");
        repositoryService.fillSelectResult(repositoryModel);
        assertNull(repositoryModel.getPassword(), "非环境变量引用的密码必须脱敏");
        assertNull(repositoryModel.getRsaPrv(), "私钥必须脱敏");
    }

    @Test
    public void testKeepWorkspaceEnvPassword() {
        RepositoryModel repositoryModel = new RepositoryModel();
        repositoryModel.setPassword(ServerConst.REF_WORKSPACE_ENV + "BUILD_TOKEN");
        repositoryModel.setRsaPrv("key");
        repositoryService.fillSelectResult(repositoryModel);
        assertEquals(ServerConst.REF_WORKSPACE_ENV + "BUILD_TOKEN", repositoryModel.getPassword(), "环境变量引用需要保留，前端才能展示");
        assertNull(repositoryModel.getRsaPrv());
    }
}
