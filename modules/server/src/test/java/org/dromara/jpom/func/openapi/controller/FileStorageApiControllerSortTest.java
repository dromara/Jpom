/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.openapi.controller;

import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验 file-storage 下载接口的 sort 参数只允许模型真实字段，防止 ORDER BY 注入。
 *
 * @author jpom
 */
public class FileStorageApiControllerSortTest {

    @Test
    public void testSafeModelField() {
        assertTrue(FileStorageApiController.isSafeModelField("createTimeMillis"));
        assertTrue(FileStorageApiController.isSafeModelField("name"));
        assertTrue(FileStorageApiController.isSafeModelField("aliasCode"));
        // @PropIgnore 字段不是数据库字段
        assertFalse(FileStorageApiController.isSafeModelField("exists"));
        // 非法标识符/函数/注入片段
        assertFalse(FileStorageApiController.isSafeModelField("zzz(1)"));
        assertFalse(FileStorageApiController.isSafeModelField("id) OR 1=1--"));
        assertFalse(FileStorageApiController.isSafeModelField("1abc"));
        assertFalse(FileStorageApiController.isSafeModelField(""));
        assertFalse(FileStorageApiController.isSafeModelField(null));
    }

    @Test
    public void testParseOrders() {
        List<Order> orders = FileStorageApiController.parseOrders("createTimeMillis:desc,name:asc");
        assertEquals(2, orders.size());
        assertEquals("createTimeMillis", orders.get(0).getField());
        assertEquals(Direction.DESC, orders.get(0).getDirection());
        assertEquals("name", orders.get(1).getField());
        assertEquals(Direction.ASC, orders.get(1).getDirection());
    }

    @Test
    public void testParseOrdersIgnoresInjection() {
        List<Order> orders = FileStorageApiController.parseOrders("zzz(1):asc,createTimeMillis:desc");
        assertEquals(1, orders.size());
        assertEquals("createTimeMillis", orders.get(0).getField());
        assertEquals(Direction.DESC, orders.get(0).getDirection());
    }

    @Test
    public void testParseOrdersDefaultDirection() {
        List<Order> orders = FileStorageApiController.parseOrders("createTimeMillis");
        assertEquals(1, orders.size());
        assertEquals(Direction.DESC, orders.get(0).getDirection());
    }

    @Test
    public void testParseOrdersEmpty() {
        assertTrue(FileStorageApiController.parseOrders(null).isEmpty());
        assertTrue(FileStorageApiController.parseOrders("  ").isEmpty());
    }
}
