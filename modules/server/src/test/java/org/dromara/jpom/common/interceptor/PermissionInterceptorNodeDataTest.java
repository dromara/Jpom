/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common.interceptor;

import org.dromara.jpom.model.BaseNodeModel;
import org.dromara.jpom.model.user.UserBindWorkspaceModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 节点数据权限 fail-closed 回归（2026-11-08 审计 P3-4）。
 * <p>
 * 旧实现只在节点缓存命中时才校验数据所属工作空间，缓存未命中（节点数据未同步/已删除）直接放行（fail-open）。
 * 现在必须改为未命中即拒绝：无法确认数据归属时按无权限处理。
 *
 * @author jpom
 */
public class PermissionInterceptorNodeDataTest {

    private static UserBindWorkspaceModel.PermissionResult permission(boolean success) {
        return UserBindWorkspaceModel.PermissionResult.builder()
            .state(success ? UserBindWorkspaceModel.PermissionResultEnum.SUCCESS
                : UserBindWorkspaceModel.PermissionResultEnum.FAIL)
            .build();
    }

    /**
     * 核心回归：缓存未命中（data == null）时必须拒绝，不能 fail-open。
     */
    @Test
    public void testCacheMissIsDenied() {
        assertFalse(PermissionInterceptor.isNodeDataPermissionAllowed(null, null),
            "节点数据未命中必须 fail-closed 拒绝");
    }

    /**
     * 命中且对数据所属工作空间有权限：允许。
     */
    @Test
    public void testPermissionSuccessAllowed() {
        BaseNodeModel data = mock(BaseNodeModel.class);
        assertTrue(PermissionInterceptor.isNodeDataPermissionAllowed(data, permission(true)));
    }

    /**
     * 命中但对数据所属工作空间无权限：拒绝。
     */
    @Test
    public void testPermissionFailureDenied() {
        BaseNodeModel data = mock(BaseNodeModel.class);
        assertFalse(PermissionInterceptor.isNodeDataPermissionAllowed(data, permission(false)));
    }

    /**
     * 防御性断言：data 存在但权限结果缺失（不应发生）时同样拒绝。
     */
    @Test
    public void testMissingPermissionResultDenied() {
        BaseNodeModel data = mock(BaseNodeModel.class);
        assertFalse(PermissionInterceptor.isNodeDataPermissionAllowed(data, null));
    }

    /**
     * 权限结果成功但 data 为 null（理论上不会同时出现）也必须拒绝，避免调用方错误组合导致放行。
     */
    @Test
    public void testNullDataWithSuccessResultDenied() {
        UserBindWorkspaceModel.PermissionResult result = mock(UserBindWorkspaceModel.PermissionResult.class);
        when(result.isSuccess()).thenReturn(true);
        assertFalse(PermissionInterceptor.isNodeDataPermissionAllowed(null, result));
    }
}
