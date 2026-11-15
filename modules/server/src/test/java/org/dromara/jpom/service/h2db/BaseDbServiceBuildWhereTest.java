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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 通用分页查询条件构建的行为级回归测试。
 * <p>
 * 背景：2.11.13 在 {@code listPage} 中给字段名加白名单时，先校验后剥离 {@code :in} 后缀，
 * 导致服务端注入的 {@code workspaceId:in} 被静默丢弃，所有工作空间列表跨空间串数据。
 * 旧测试只覆盖 {@link BaseDbService#resolveFilterField(String)}，改回旧写法仍会全绿；
 * 因此这里直接断言 {@link BaseDbService#buildWhere(Map, java.util.function.Function)} 的产物，
 * {@code :in} 一旦被丢弃/降级为普通条件，本测试立即失败。
 *
 * @author jpom
 */
public class BaseDbServiceBuildWhereTest {

    /**
     * 注入简单的字段包裹器，避免单测依赖 Spring 初始化的数据库方言。
     */
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

    private static Condition condition(Entity where, String field) {
        Object value = where.get(field);
        assertNotNull(value, "缺少查询条件字段：" + field + "，实际：" + where);
        assertTrue(value instanceof Condition, "字段 " + field + " 应为 Condition，实际：" + value);
        return (Condition) value;
    }

    /**
     * 2.11.13 回归用例：workspaceId:in 必须在白名单校验前剥离并保留为集合条件。
     */
    @Test
    public void testWorkspaceIdInFilterIsKept() {
        Entity where = build(mapOf("workspaceId:in", "ws1,GLOBAL"));
        Object value = where.get("`workspaceId`");
        assertNotNull(value, "workspaceId:in 条件被丢弃（2.11.13 回归）：" + where);
        assertTrue(value instanceof Collection, "workspaceId:in 应构建为集合 IN 条件，实际：" + value);
        assertEquals(Arrays.asList("ws1", "GLOBAL"), new ArrayList<>((Collection<?>) value));
        assertFalse(where.containsKey("`workspaceId:in`"), "不应生成带 :in 后缀的非法字段名");
        assertTrue(where.containsKey("`workspaceId`"));
    }

    /**
     * staticDir:in 同类集合过滤（静态文件存储空间隔离依赖它）。
     */
    @Test
    public void testStaticDirInFilterIsKept() {
        Entity where = build(mapOf("staticDir:in", "/data/a,/data/b"));
        Object value = where.get("`staticDir`");
        assertNotNull(value, "staticDir:in 条件被丢弃：" + where);
        assertTrue(value instanceof Collection);
        assertEquals(Arrays.asList("/data/a", "/data/b"), new ArrayList<>((Collection<?>) value));
    }

    @Test
    public void testNormalEqualityUsesBoundCondition() {
        Entity where = build(mapOf("name", "abc"));
        Condition condition = condition(where, "`name`");
        // 生产代码以“已包裹的字段名”构造 Condition（与 DialectUtil.wrapField 行为一致）
        assertEquals("`name`", condition.getField());
        assertEquals("=", condition.getOperator());
        assertEquals("abc", condition.getValue());
    }

    @Test
    public void testLikeFilters() {
        Condition contains = condition(build(mapOf("%name%", "abc")), "`name`");
        assertEquals("like", contains.getOperator());
        assertEquals("%abc%", contains.getValue());

        Condition starts = condition(build(mapOf("name%", "abc")), "`name`");
        assertEquals("like", starts.getOperator());
        assertEquals("abc%", starts.getValue());

        Condition ends = condition(build(mapOf("%name", "abc")), "`name`");
        assertEquals("like", ends.getOperator());
        assertEquals("%abc", ends.getValue());
    }

    @Test
    public void testTimeRangeByBracketSuffix() {
        Entity where = build(mapOf(
            "createTimeMillis[0]", "2026-11-01 00:00:00",
            "createTimeMillis[1]", "2026-11-02 23:59:59"));
        Condition start = condition(where, "`createTimeMillis`");
        assertEquals(">=", start.getOperator());
        assertTrue(start.getValue() instanceof Number);
        Condition end = condition(where, "`createTimeMillis` ");
        assertEquals("<=", end.getOperator());
        assertTrue(end.getValue() instanceof Number);
    }

    @Test
    public void testTimeRangeByTilde() {
        Entity where = build(mapOf("modifyTimeMillis", "2026-11-01 00:00:00~2026-11-02 23:59:59"));
        assertEquals(">=", condition(where, "`modifyTimeMillis`").getOperator());
        assertEquals("<=", condition(where, "`modifyTimeMillis` ").getOperator());
    }

    /**
     * 注入防护不能回退：非法字段名、空值都必须被忽略。
     */
    @Test
    public void testInvalidFieldNamesAreIgnored() {
        Map<String, String> paramMap = mapOf(
            "1=1 OR 1", "x",
            "name`", "x",
            "id) OR 1=1--", "x",
            "name", "");
        Entity where = build(paramMap);
        assertTrue(where.isEmpty(), "非法/空值参数不应生成任何条件，实际：" + where);
    }

    /**
     * 非时间字段带 [0]/[1] 后缀不能生成非法列名（审计 P3）：应被忽略，而不是拼出 `foo[0]` 导致 500。
     * 时间字段的 [0]/[1] 范围过滤由 {@link #testTimeRangeByBracketSuffix()} 覆盖。
     */
    @Test
    public void testNonTimeBracketSuffixIsIgnored() {
        Entity where = build(mapOf("foo[0]", "start", "foo[1]", "end"));
        assertTrue(where.isEmpty(), "非时间字段的 [0]/[1] 后缀应被忽略，实际：" + where);
    }

    /**
     * 防止后续把 :in 再改回「校验前不剥离后缀」的旧实现：一旦丢弃，下面的断言会失败。
     */
    @Test
    public void testRegressionGuardForInSuffixParsing() {
        assertEquals("workspaceId", BaseDbService.resolveFilterField("workspaceId:in"));
        Entity where = build(mapOf("workspaceId:in", "ws1,GLOBAL"));
        assertTrue(where.get("`workspaceId`") instanceof Collection, "workspaceId:in 必须构建为集合条件：" + where);
    }
}
