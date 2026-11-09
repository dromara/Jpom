/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.db;

import cn.hutool.db.Entity;
import cn.hutool.db.sql.Condition;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验查询 bean 转 Entity 时查询值统一使用占位符 Condition 绑定，
 * 不会被 Hutool 解析为 SQL 条件表达式（is not null / = null 等）。
 *
 * @author jpom
 */
public class BaseDbCommonServiceQueryEntityTest {

    private static Entity toQueryEntity(Object bean) {
        return BaseDbCommonService.dataBeanToQueryEntity("T_TEST", bean, field -> field);
    }

    @Test
    public void testConditionExpressionIsBoundAsLiteralValue() {
        QueryBean bean = new QueryBean();
        bean.setPassword("is not null");
        bean.setName("= null");
        bean.setDescription("> 0");
        Entity entity = toQueryEntity(bean);

        this.assertCondition(entity, "password", "is not null");
        this.assertCondition(entity, "name", "= null");
        this.assertCondition(entity, "description", "> 0");
    }

    @Test
    public void testNormalValueIsBoundAsLiteralValue() {
        QueryBean bean = new QueryBean();
        bean.setPassword("abc123");
        Entity entity = toQueryEntity(bean);

        this.assertCondition(entity, "password", "abc123");
    }

    @Test
    public void testCollectionAndConditionPassThrough() {
        List<String> ids = Arrays.asList("a", "b");
        Condition condition = new Condition("name", "=", "x");
        QueryBean bean = new QueryBean();
        bean.setIds(ids);
        bean.setCondition(condition);
        Entity entity = toQueryEntity(bean);

        assertSame(ids, entity.getObj("ids"));
        assertSame(condition, entity.getObj("condition"));
    }

    @Test
    public void testEmptyBean() {
        Entity entity = toQueryEntity(new QueryBean());
        assertTrue(entity.isEmpty());
    }

    @Test
    public void testKeyEditorApplied() {
        QueryBean bean = new QueryBean();
        bean.setPassword("p");
        Entity entity = BaseDbCommonService.dataBeanToQueryEntity("T_TEST", bean, String::toUpperCase);
        assertTrue(entity.containsKey("PASSWORD"));
        Condition condition = assertInstanceOf(Condition.class, entity.getObj("PASSWORD"));
        assertEquals("=", condition.getOperator());
        assertEquals("p", condition.getValue());
    }

    private void assertCondition(Entity entity, String field, Object expectedValue) {
        Condition condition = assertInstanceOf(Condition.class, entity.getObj(field));
        assertEquals("=", condition.getOperator());
        assertEquals(expectedValue, condition.getValue());
    }

    @SuppressWarnings("unused")
    public static class QueryBean {
        private String password;
        private String name;
        private String description;
        private List<String> ids;
        private Object condition;

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<String> getIds() {
            return ids;
        }

        public void setIds(List<String> ids) {
            this.ids = ids;
        }

        public Object getCondition() {
            return condition;
        }

        public void setCondition(Object condition) {
            this.condition = condition;
        }
    }
}
