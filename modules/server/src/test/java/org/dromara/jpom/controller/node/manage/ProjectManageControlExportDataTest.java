/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.node.manage;

import org.dromara.jpom.TestI18nSupport;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.service.dblog.BuildInfoService;
import org.dromara.jpom.service.dblog.DbBuildHistoryLogService;
import org.dromara.jpom.service.dblog.RepositoryService;
import org.dromara.jpom.service.monitor.MonitorService;
import org.dromara.jpom.service.node.ProjectInfoCacheService;
import org.dromara.jpom.service.outgiving.LogReadServer;
import org.dromara.jpom.service.outgiving.OutGivingServer;
import org.dromara.jpom.service.system.WhitelistDirectoryService;
import org.dromara.jpom.system.ServerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 项目导出接口工作空间强制条件回归（2026-11-08 审计 P1-2）。
 * <p>
 * {@code /node/manage/export-data} 旧实现取了 workspace 却未用于查询，客户端构造参数即可导出全部空间项目
 * （CSV 含项目 token）。修复后必须强制 {@code workspaceId:in = 当前空间,GLOBAL}，并剔除客户端同字段参数。
 *
 * @author jpom
 */
public class ProjectManageControlExportDataTest {

    private ProjectInfoCacheService projectInfoCacheService;
    private ProjectManageControl control;

    @BeforeEach
    public void setUp() {
        TestI18nSupport.init();
        projectInfoCacheService = mock(ProjectInfoCacheService.class);
        control = new ProjectManageControl(
            mock(OutGivingServer.class),
            mock(LogReadServer.class),
            mock(MonitorService.class),
            mock(BuildInfoService.class),
            mock(RepositoryService.class),
            projectInfoCacheService,
            mock(DbBuildHistoryLogService.class),
            mock(ServerConfig.class),
            mock(WhitelistDirectoryService.class));
    }

    private long countWorkspaceKeys(Map<String, String> paramMap) {
        return paramMap.keySet().stream()
            .filter(key -> key.replace("%", "").replace(":in", "").equalsIgnoreCase("workspaceId"))
            .count();
    }

    @Test
    public void testExportForcesWorkspaceAndRejectsClientOverride() throws Exception {
        when(projectInfoCacheService.getCheckUserWorkspace(any())).thenReturn("wsA");
        when(projectInfoCacheService.listPage(anyMap(), eq(false))).thenReturn(new PageResultDto<>(1, 10, 0));

        MockHttpServletRequest request = new MockHttpServletRequest();
        // 客户端伪造的同字段覆盖参数
        request.addParameter("workspaceId", "wsB");
        request.addParameter("workspaceId:in", "wsB");
        request.addParameter("%workspaceId%", "wsB");
        MockHttpServletResponse response = new MockHttpServletResponse();

        control.exportData(response, request);

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(projectInfoCacheService).listPage(captor.capture(), eq(false));
        Map<String, String> paramMap = captor.getValue();
        assertEquals("wsA,GLOBAL", paramMap.get("workspaceId:in"),
            "导出必须强制当前空间 + GLOBAL：" + paramMap);
        assertFalse(paramMap.containsKey("workspaceId"),
            "客户端 plain workspaceId 必须被剔除：" + paramMap);
        assertEquals(1, countWorkspaceKeys(paramMap),
            "工作空间条件必须唯一来自服务端：" + paramMap);
        assertEquals("1", paramMap.get("page"));
    }
}
