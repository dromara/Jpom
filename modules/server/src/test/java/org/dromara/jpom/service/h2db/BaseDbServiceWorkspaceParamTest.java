/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.h2db;

import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.model.data.NodeModel;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * 非工作空间实体的 workspaceId 过滤参数剔除（回归：机器资产列表带 workspaceId 参数 500）
 *
 * @author jpom
 * @since 2.11.15
 */
public class BaseDbServiceWorkspaceParamTest {

    @Test
    public void removeWorkspaceParamForNonWorkspaceEntity() {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("workspaceId", "DEFAULT");
        paramMap.put("workspaceId:in", "a,b");
        paramMap.put("page", "1");
        paramMap.put("name", "abc");
        boolean removed = BaseDbService.removeNotExistWorkspaceParam(MachineNodeModel.class, paramMap);
        Assert.assertTrue(removed);
        Assert.assertFalse(paramMap.containsKey("workspaceId"));
        Assert.assertFalse(paramMap.containsKey("workspaceId:in"));
        Assert.assertEquals("1", paramMap.get("page"));
        Assert.assertEquals("abc", paramMap.get("name"));
    }

    @Test
    public void keepWorkspaceParamForWorkspaceEntity() {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("workspaceId", "DEFAULT");
        boolean removed = BaseDbService.removeNotExistWorkspaceParam(NodeModel.class, paramMap);
        Assert.assertFalse(removed);
        Assert.assertEquals("DEFAULT", paramMap.get("workspaceId"));
    }
}
