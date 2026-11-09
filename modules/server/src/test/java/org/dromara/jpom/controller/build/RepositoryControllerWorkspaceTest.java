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

import cn.keepbx.jpom.IJsonMessage;
import org.dromara.jpom.TestI18nSupport;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.service.dblog.BuildInfoService;
import org.dromara.jpom.service.dblog.RepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 仓库列表/导出接口工作空间隔离回归（2026-11-08 审计 P2-1 / P3）。
 *
 * @author jpom
 */
public class RepositoryControllerWorkspaceTest {

    private RepositoryService repositoryService;
    private RepositoryController controller;

    @BeforeEach
    public void setUp() {
        TestI18nSupport.init();
        repositoryService = mock(RepositoryService.class);
        controller = new RepositoryController(repositoryService, mock(BuildInfoService.class));
    }

    private long countWorkspaceKeys(Map<String, String> paramMap) {
        return paramMap.keySet().stream()
            .filter(key -> key.replace("%", "").replace(":in", "").equalsIgnoreCase("workspaceId"))
            .count();
    }

    /**
     * P2-1：仓库导出必须强制当前空间 + GLOBAL，客户端同字段参数（plain/:in/%）不得覆盖。
     */
    @Test
    public void testExportForcesWorkspaceAndRejectsClientOverride() throws Exception {
        when(repositoryService.getCheckUserWorkspace(any())).thenReturn("wsA");
        when(repositoryService.listPage(anyMap(), eq(false))).thenReturn(new PageResultDto<>(1, 10, 0));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("workspaceId", "wsB");
        request.addParameter("workspaceId:in", "wsB");
        request.addParameter("%workspaceId%", "wsB");
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.exportRepositoryList(request, response);

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(repositoryService).listPage(captor.capture(), eq(false));
        Map<String, String> paramMap = captor.getValue();
        assertEquals("wsA,GLOBAL", paramMap.get("workspaceId:in"),
            "仓库导出必须强制当前空间 + GLOBAL：" + paramMap);
        assertFalse(paramMap.containsKey("workspaceId"),
            "客户端 plain workspaceId 必须被剔除：" + paramMap);
        assertEquals(1, countWorkspaceKeys(paramMap),
            "工作空间条件必须唯一来自服务端：" + paramMap);
        assertEquals("1", paramMap.get("page"));
    }

    /**
     * P3：仓库分组下拉必须按当前工作空间过滤（listGroup(request)），不再返回全表分组。
     */
    @Test
    public void testListGroupUsesRequestWorkspace() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(repositoryService.listGroup(request)).thenReturn(Arrays.asList("g1", "g2"));

        IJsonMessage<List<String>> message = controller.getBuildGroupAll(request);

        assertNotNull(message);
        verify(repositoryService).listGroup(request);
    }
}
