/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.exception;

import cn.keepbx.jpom.IJsonMessage;
import org.dromara.jpom.TestI18nSupport;
import org.dromara.jpom.system.JpomRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 全局异常处理：数据库异常响应脱敏（不回显 SQL/列名/表名/驱动信息）的行为级回归。
 * <p>
 * 背景：列表接口传入未知字段过滤参数（如 {@code foo=1}）时，H2 会抛出
 * {@code Column "FOO" not found; SQL statement: SELECT ...}；旧 handler 对
 * {@link JpomRuntimeException} 直接返回 {@code e.getMessage()}，把完整 SQL 回显给客户端（信息暴露）。
 * 现在异常链含数据库异常时，服务端记录完整堆栈、响应仅返回通用文案；非数据库异常保持原行为。
 *
 * @author jpom
 */
public class BaseExceptionHandlerTest {

    /**
     * 模拟 H2 语法错误信息（含列名、表名、完整 SQL、错误码、驱动版本）
     */
    private static final String SQL_DETAIL = "Column \"FOO\" not found; SQL statement:\n"
        + "SELECT * FROM T_TEST WHERE FOO = ? [42S22-214]";

    private BaseExceptionHandler handler;

    @BeforeEach
    public void setUp() {
        TestI18nSupport.init();
        handler = new BaseExceptionHandler() {
        };
    }

    private MockHttpServletRequest request() {
        return new MockHttpServletRequest("POST", "/node/project_list");
    }

    private IJsonMessage<String> handle(Exception e) {
        return handler.defExceptionHandler(request(), e);
    }

    /**
     * 断言响应消息不含任何数据库细节（SQL/列/表/驱动）。
     */
    private void assertNoDatabaseDetails(String message) {
        assertNotNull(message, "响应消息不能为空");
        String lower = message.toLowerCase();
        assertFalse(lower.contains("sql"), "响应消息不得包含 SQL 关键字：" + message);
        assertFalse(lower.contains("column"), "响应消息不得包含列信息：" + message);
        assertFalse(lower.contains("select"), "响应消息不得包含 SQL 语句：" + message);
        assertFalse(lower.contains("not found"), "响应消息不得包含驱动错误细节：" + message);
        assertFalse(message.contains("FOO"), "响应消息不得包含列名：" + message);
        assertFalse(message.contains("T_TEST"), "响应消息不得包含表名：" + message);
        assertFalse(message.contains("42S22"), "响应消息不得包含数据库错误码：" + message);
        assertFalse(message.contains("42S22-214"), "响应消息不得包含驱动版本/错误码：" + message);
    }

    /**
     * 真实泄漏形态：H2StorageServiceImpl 把 SQL 详情拼进 JpomRuntimeException 的 message，cause 为 SQLException。
     */
    @Test
    public void testWrappedH2SqlExceptionEchoIsMasked() {
        JpomRuntimeException exception = new JpomRuntimeException("数据库异常：" + SQL_DETAIL,
            new SQLException(SQL_DETAIL, "42S22"));
        IJsonMessage<String> result = handle(exception);
        assertEquals(500, result.getCode());
        assertNoDatabaseDetails(result.getMsg());
        assertFalse(result.getMsg().contains("数据库异常"), "数据库异常消息不得透出：" + result.getMsg());
    }

    /**
     * 多层包装的异常链同样要识别（RuntimeException -> IllegalStateException -> SQLException）。
     */
    @Test
    public void testNestedSqlExceptionEchoIsMasked() {
        Exception exception = new RuntimeException("outer",
            new IllegalStateException("middle", new SQLException(SQL_DETAIL, "42S22")));
        IJsonMessage<String> result = handle(exception);
        assertEquals(500, result.getCode());
        assertNoDatabaseDetails(result.getMsg());
    }

    /**
     * 未包装的 SQLException 也不能回显。
     */
    @Test
    public void testPlainSqlExceptionEchoIsMasked() {
        IJsonMessage<String> result = handle(new SQLException(SQL_DETAIL, "42S22"));
        assertEquals(500, result.getCode());
        assertNoDatabaseDetails(result.getMsg());
    }

    /**
     * 非数据库原因的 JpomRuntimeException 保持原行为（message 原样返回）。
     */
    @Test
    public void testNonDatabaseJpomRuntimeExceptionKeepsMessage() {
        IJsonMessage<String> result = handle(new JpomRuntimeException("业务异常：项目不存在"));
        assertEquals(500, result.getCode());
        assertEquals("业务异常：项目不存在", result.getMsg());
    }

    /**
     * 普通运行时异常保持原行为（通用文案 + 原始 message）。
     */
    @Test
    public void testNormalRuntimeExceptionKeepsMessage() {
        IJsonMessage<String> result = handle(new RuntimeException("普通异常：参数不合法"));
        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("普通异常：参数不合法"), "普通异常 message 应保留：" + result.getMsg());
    }

    /**
     * 异常链识别 helper：SQLException 直接/嵌套/被 JpomRuntimeException 包装都算数据库异常；
     * 普通业务异常不算。
     */
    @Test
    public void testIsDatabaseException() {
        assertTrue(BaseExceptionHandler.isDatabaseException(new SQLException(SQL_DETAIL)));
        assertTrue(BaseExceptionHandler.isDatabaseException(new RuntimeException(new SQLException(SQL_DETAIL))));
        assertTrue(BaseExceptionHandler.isDatabaseException(new JpomRuntimeException("wrap", new SQLException(SQL_DETAIL))));
        assertTrue(BaseExceptionHandler.isDatabaseException(
            new RuntimeException("a", new IllegalStateException("b", new SQLException("c")))));
        assertFalse(BaseExceptionHandler.isDatabaseException(new JpomRuntimeException("业务异常")));
        assertFalse(BaseExceptionHandler.isDatabaseException(new RuntimeException("普通异常")));
        assertFalse(BaseExceptionHandler.isDatabaseException(null));
    }
}
