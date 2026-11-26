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

import static org.junit.jupiter.api.Assertions.*;

/**
 * 「启动时把空分组修复为默认分组」SQL 的行为级回归测试。
 * <p>
 * 背景：{@link BaseDbService#repairGroupFiled()} 只覆盖旧字段 {@code group}，而机器资产、流水线把分组字段
 * 改名为 {@code groupName} 后没有对应的启动修复，老数据里的 NULL / 空串分组永远留在库里（dev 实测
 * 37 条流水线里有 12 条 groupName 为空）。修复必须同时覆盖两种字段，且 SQL 的 {@code is null} 与
 * {@code = ''} 两个条件缺一不可：
 * </p>
 * <ul>
 *   <li>只判 {@code = ''} → NULL 分组不被修复，反证用例立即失败；</li>
 *   <li>只判 {@code is null} → 空串分组不被修复，反证用例立即失败；</li>
 *   <li>字段名不带包裹器 → 命中数据库保留字（{@code group}）直接语法错误，断言失败。</li>
 * </ul>
 *
 * @author jpom
 */
public class BaseDbServiceGroupRepairSqlTest {

    /**
     * 注入简单的字段包裹器，避免单测依赖 Spring 初始化的数据库方言。
     */
    private static String sql(String tableName, String fieldName) {
        return BaseDbService.buildRepairGroupSql(tableName, fieldName, field -> "`" + field + "`");
    }

    @Test
    public void testOldGroupFieldSql() {
        String sql = sql("BUILD_INFO", "group");
        assertEquals("update BUILD_INFO set `group` =? where `group` is null or `group` = ''", sql);
    }

    @Test
    public void testNewGroupNameFieldSql() {
        String sql = sql("PIPELINE_INFO", "groupName");
        assertEquals("update PIPELINE_INFO set `groupName` =? where `groupName` is null or `groupName` = ''", sql);
    }

    /**
     * 反证：NULL 与空串两个条件必须同时在 where 里
     */
    @Test
    public void testBothNullAndEmptyAreRepaired() {
        for (String field : new String[]{"group", "groupName"}) {
            String sql = sql("T", field);
            assertTrue(sql.contains("`" + field + "` is null"), "缺少 is null 条件：" + sql);
            assertTrue(sql.contains("`" + field + "` = ''"), "缺少空串条件：" + sql);
            assertTrue(sql.indexOf(" or ") > sql.indexOf("where "), "两个条件必须是 or 关系：" + sql);
        }
    }

    /**
     * 反证：字段名必须经过包裹器（不包裹 {@code group} 在多数数据库里是保留字）
     */
    @Test
    public void testFieldWrapperApplied() {
        String raw = BaseDbService.buildRepairGroupSql("T", "group", field -> field);
        String wrapped = sql("T", "group");
        assertFalse(raw.contains("`"), "未注入包裹器时不应出现包裹字符：" + raw);
        assertNotEquals(raw, wrapped, "注入包裹器后 SQL 必须变化，否则说明包裹器没被使用");
    }
}
