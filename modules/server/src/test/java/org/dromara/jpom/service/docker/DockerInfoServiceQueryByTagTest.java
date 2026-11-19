/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.docker;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.jpom.TestI18nSupport;
import org.dromara.jpom.db.DbExtConfig;
import org.dromara.jpom.db.StorageServiceFactory;
import org.dromara.jpom.model.docker.DockerInfoModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.GenericApplicationContext;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Docker 构建 fromTag 查询 SQL 注入（VL-001）回归测试
 * <p>
 * 修复前 {@code queryByTag} 把 tag 拼进 {@code instr(tags,'...')} 并 {@code setPlaceHolder(false)}，
 * 具备工作空间 BUILD/EDIT 权限的用户可通过 {@code fromTag} 突破 workspaceId 条件读取业务库。
 * 本测试要求 tag 只能作为绑定参数出现：改回字符串拼接的实现会让 {@code queryList} 不再被调用、SQL 中出现 payload，测试随即失败。
 *
 * @author jpom
 * @since 2026/11/18
 */
public class DockerInfoServiceQueryByTagTest {

    private static final String SQL_INJECTION_PAYLOAD = "x') OR updatexml(1,concat(0x7e,(select database())),1) -- ";

    @BeforeAll
    public static void setUp() {
        TestI18nSupport.init();
        DbExtConfig dbExtConfig = mock(DbExtConfig.class);
        when(dbExtConfig.getMode()).thenReturn(DbExtConfig.Mode.H2);
        // 方言与表名解析依赖存储单例，测试环境手工指定为 H2
        StorageServiceFactory.getInstance().setMode(DbExtConfig.Mode.H2);
        MessageSource messageSource = mock(MessageSource.class);
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        GenericApplicationContext applicationContext = new GenericApplicationContext();
        applicationContext.getBeanFactory().registerSingleton("messageSource", messageSource);
        applicationContext.getBeanFactory().registerSingleton("dbExtConfig", dbExtConfig);
        applicationContext.refresh();
        new SpringUtil().setApplicationContext(applicationContext);
    }

    @Test
    public void testFromTagPayloadIsBoundAsValue() {
        CapturingDockerInfoService dockerInfoService = new CapturingDockerInfoService();
        dockerInfoService.queryByTag("DEFAULT", SQL_INJECTION_PAYLOAD);

        assertNotNull(dockerInfoService.sql, "fromTag 查询必须走参数绑定（改回字符串拼接时本断言失败）");
        assertFalse(dockerInfoService.sql.contains("updatexml"), "SQL 中不得出现 fromTag 内容：" + dockerInfoService.sql);
        assertFalse(dockerInfoService.sql.contains(SQL_INJECTION_PAYLOAD), "SQL 中不得出现注入 payload：" + dockerInfoService.sql);
        assertTrue(dockerInfoService.sql.contains("instr(tags,?)"), "instr 的 tag 必须是占位符：" + dockerInfoService.sql);
        assertTrue(dockerInfoService.sql.contains("=?"), "workspaceId 必须使用占位符：" + dockerInfoService.sql);

        assertEquals(2, dockerInfoService.params.length);
        assertEquals("DEFAULT", dockerInfoService.params[0]);
        // StrUtil.wrap 会补齐首尾冒号，tag 仍整体作为绑定参数
        assertEquals(":" + SQL_INJECTION_PAYLOAD + ":", dockerInfoService.params[1]);
    }

    @Test
    public void testNormalTagIsBoundWithColonWrapper() {
        CapturingDockerInfoService dockerInfoService = new CapturingDockerInfoService();
        dockerInfoService.queryByTag("WORKSPACE_A", "centos7");

        assertNotNull(dockerInfoService.sql);
        assertFalse(dockerInfoService.sql.contains("centos7"), "正常 tag 也不允许拼进 SQL：" + dockerInfoService.sql);
        assertEquals("WORKSPACE_A", dockerInfoService.params[0]);
        assertEquals(":centos7:", dockerInfoService.params[1]);
    }

    @Test
    public void testQuoteInTagCannotBreakSql() {
        CapturingDockerInfoService dockerInfoService = new CapturingDockerInfoService();
        dockerInfoService.queryByTag("DEFAULT", "a' OR '1'='1");

        assertFalse(dockerInfoService.sql.contains("OR '1'='1"), "引号 payload 不得进入 SQL：" + dockerInfoService.sql);
        assertEquals(":a' OR '1'='1:", dockerInfoService.params[1]);
    }

    /**
     * 只捕获查询语句，不真正访问数据库
     */
    private static class CapturingDockerInfoService extends DockerInfoService {

        private String sql;
        private Object[] params;

        @Override
        public List<DockerInfoModel> queryList(String sql, Object... params) {
            this.sql = sql;
            this.params = params;
            return Collections.emptyList();
        }
    }
}
