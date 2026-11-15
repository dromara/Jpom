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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 校验通用分页查询的过滤参数名解析：
 * {@code field:in}（如 {@code workspaceId:in}）必须在字段白名单校验前剥离后缀，
 * 否则会被误判为非法字段而静默丢弃，导致工作空间隔离条件失效。
 *
 * @author jpom
 */
public class BaseDbServiceListPageFilterTest {

    @Test
    public void testInSuffixIsResolved() {
        assertEquals("workspaceId", BaseDbService.resolveFilterField("workspaceId:in"));
        assertEquals("staticDir", BaseDbService.resolveFilterField("staticDir:in"));
    }

    @Test
    public void testLikeWildcardIsResolved() {
        assertEquals("name", BaseDbService.resolveFilterField("%name%"));
        assertEquals("name", BaseDbService.resolveFilterField("%name"));
        assertEquals("name", BaseDbService.resolveFilterField("name%"));
    }

    @Test
    public void testTimeRangeSuffixIsResolved() {
        assertEquals("createTimeMillis", BaseDbService.resolveFilterField("createTimeMillis[0]"));
        assertEquals("createTimeMillis", BaseDbService.resolveFilterField("createTimeMillis[1]"));
    }

    @Test
    public void testNormalField() {
        assertEquals("id", BaseDbService.resolveFilterField("id"));
        assertEquals("createTimeMillis", BaseDbService.resolveFilterField("createTimeMillis"));
    }

    @Test
    public void testInjectionFieldIsRejected() {
        assertNull(BaseDbService.resolveFilterField("1=1 OR 1"));
        assertNull(BaseDbService.resolveFilterField("id) OR 1=1--"));
        assertNull(BaseDbService.resolveFilterField("name`"));
        assertNull(BaseDbService.resolveFilterField(""));
        assertNull(BaseDbService.resolveFilterField(null));
    }
}
