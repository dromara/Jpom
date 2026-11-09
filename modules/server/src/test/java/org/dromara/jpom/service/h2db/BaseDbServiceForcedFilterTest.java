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

import cn.hutool.db.Entity;
import cn.hutool.db.sql.Condition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 服务端强制过滤条件与客户端同字段参数冲突的行为级回归（2026-11-08 审计 P1-1）。
 * <p>
 * 背景：服务端通过 {@code paramMap} 注入工作空间隔离条件（{@code workspaceId} / {@code workspaceId:in}），
 * 客户端可构造同字段但不同后缀的参数（{@code workspaceId:in} / {@code %workspaceId%} / 大小写变体），
 * 在 {@link BaseDbService#buildWhere(Map, java.util.function.Function)} 中映射为同一个数据库字段并覆盖
 * 服务端条件。{@link BaseDbService#putForcedFilter(Map, String, String)} 必须在注入前剔除这些客户端参数，
 * 保证最终查询条件唯一来自服务端。
 *
 * @author jpom
 */
public class BaseDbServiceForcedFilterTest {

    private static final String WORKSPACE_FIELD = "workspaceId";

    private static Entity build(Map<String, String> paramMap) {
        return BaseDbService.buildWhere(paramMap, field -> "`" + field + "`");
    }

    private static Map<String, String> mapOf(String... kv) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put(kv[i], kv[i + 1]);
        }
        return map;
    }

    /**
     * 统计 paramMap 中解析后为 workspaceId 的 key 数量（含 :in / % / [0] / 大小写变体）。
     */
    private static long countWorkspaceKeys(Map<String, String> paramMap) {
        return paramMap.keySet().stream()
            .filter(key -> WORKSPACE_FIELD.equalsIgnoreCase(BaseDbService.resolveFilterField(key)))
            .count();
    }

    /**
     * 服务端强制普通等值条件：客户端 :in 必须被剔除，最终只能是服务端的 `=` 条件。
     */
    @Test
    public void testForcedPlainFilterRejectsClientIn() {
        Map<String, String> paramMap = mapOf(
            "workspaceId:in", "wsB",
            "name", "demo");
        BaseDbService.putForcedFilter(paramMap, WORKSPACE_FIELD, "wsA");

        assertEquals(1, countWorkspaceKeys(paramMap), "同字段客户端参数应被剔除，仅保留服务端强制 key：" + paramMap);
        assertEquals("wsA", paramMap.get(WORKSPACE_FIELD));

        Entity where = build(paramMap);
        Object value = where.get("`workspaceId`");
        assertNotNull(value, "缺少 workspaceId 条件：" + where);
        assertInstanceOf(Condition.class, value, "强制普通条件应为 Condition 等值绑定，实际：" + value);
        Condition condition = (Condition) value;
        assertEquals("=", condition.getOperator());
        assertEquals("wsA", condition.getValue());
        assertNotNull(where.get("`name`"), "无关查询字段不应被丢弃");
    }

    /**
     * 服务端强制集合条件（当前空间 + GLOBAL）：客户端 plain / :in / % / 大小写变体都必须被剔除，
     * 最终只保留服务端注入的集合 IN，避免越权扩大数据范围。
     */
    @Test
    public void testForcedInFilterRejectsClientPlainLikeAndIn() {
        Map<String, String> paramMap = mapOf(
            "workspaceId", "wsA",
            "workspaceId:in", "wsB",
            "%workspaceId%", "wsC",
            "WorkspaceId:in", "wsD",
            "name", "demo");
        BaseDbService.putForcedFilter(paramMap, "workspaceId:in", "wsS,GLOBAL");

        assertEquals(1, countWorkspaceKeys(paramMap), "同字段客户端参数（含大小写/模糊/:in）应被剔除：" + paramMap);
        assertEquals("wsS,GLOBAL", paramMap.get("workspaceId:in"));
        assertFalse(paramMap.containsKey(WORKSPACE_FIELD));

        Entity where = build(paramMap);
        Object value = where.get("`workspaceId`");
        assertNotNull(value, "workspaceId:in 条件被丢弃：" + where);
        assertInstanceOf(Collection.class, value, "强制集合条件应为 IN 集合值，实际：" + value);
        assertEquals(Arrays.asList("wsS", "GLOBAL"), new ArrayList<>((Collection<?>) value));
        assertFalse(where.containsKey("`workspaceId:in`"), "不应生成带 :in 后缀的非法字段名：" + where);
    }

    /**
     * 无关查询参数（模糊查询、时间范围等）不应被强制过滤逻辑误删。
     */
    @Test
    public void testForcedFilterDoesNotTouchOtherFields() {
        Map<String, String> paramMap = mapOf(
            "name%", "demo",
            "createTimeMillis", "2026-11-01 00:00:00~2026-11-02 23:59:59");
        BaseDbService.putForcedFilter(paramMap, WORKSPACE_FIELD, "wsA");
        assertTrue(paramMap.containsKey("name%"));
        assertTrue(paramMap.containsKey("createTimeMillis"));
        assertEquals(1, countWorkspaceKeys(paramMap));
    }

    /**
     * 重复注入（如分页循环内每页调用）必须保持幂等，不会叠加客户端残留 key。
     */
    @Test
    public void testForcedFilterIsIdempotent() {
        Map<String, String> paramMap = mapOf("workspaceId:in", "wsB");
        BaseDbService.putForcedFilter(paramMap, WORKSPACE_FIELD, "wsA");
        BaseDbService.putForcedFilter(paramMap, WORKSPACE_FIELD, "wsA");
        assertEquals("wsA", paramMap.get(WORKSPACE_FIELD));
        assertEquals(1, paramMap.size(), "只应保留服务端强制 key：" + paramMap);
    }

    /**
     * 非法强制字段名属于编码错误，必须尽早暴露而不是静默注入。
     */
    @Test
    public void testInvalidForcedFieldRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> BaseDbService.putForcedFilter(new HashMap<>(), "bad field;drop table", "x"));
        assertThrows(IllegalArgumentException.class,
            () -> BaseDbService.putForcedFilter(new HashMap<>(), "", "x"));
    }
}
