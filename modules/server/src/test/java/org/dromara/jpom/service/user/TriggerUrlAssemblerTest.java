/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.user;

import cn.keepbx.jpom.model.BaseIdModel;
import org.dromara.jpom.common.ServerOpenApi;
import org.dromara.jpom.service.ITriggerToken;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * 触发器统一收口点回归测试。
 * <p>
 * 事故背景：B-041 修「构建 / 脚本 / SSH 命令」三处触发器入口 id 不存在时的 500 空指针时发现，
 * 同一族 10 个入口用了 4 种不同的收口方式（`getByKeyOrThrow` / 服务层断言 / 控制器断言 / 下游断言），
 * 新增第 11 个入口必然再写一套。改为统一走 {@link TriggerUrlAssembler} 后，本测试守住三条不变量：
 * </p>
 * <ol>
 *     <li>查不到一律抛业务异常（全局处理器映射 405），不再把 null 交给调用方；</li>
 *     <li>token 首次生成与重置进同一段代码，且每次都写入审计表（restToken 只调用一次）；</li>
 *     <li>响应 key 与历史逐字一致（构建 4 个地址 + id/token、无 id 的全局 token）——回归时改 key 必须先改测试。</li>
 * </ol>
 *
 * @author jpom
 * @since 2026/11/25
 */
public class TriggerUrlAssemblerTest {

    private final TriggerTokenLogServer tokenLogServer = Mockito.mock(TriggerTokenLogServer.class);

    private final TriggerUrlAssembler assembler = new TriggerUrlAssembler(tokenLogServer);

    private MockHttpServletRequest request() {
        return new MockHttpServletRequest("POST", "/build/trigger/url");
    }

    /**
     * 查不到数据必须抛业务异常（响应 405），不得返回 null 让调用方解引用
     */
    @Test
    public void requireShouldThrowWhenMissing() {
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
            () -> assembler.require(() -> null, "没有对应数据"));
        Assertions.assertEquals("没有对应数据", exception.getMessage());
    }

    /**
     * 构建触发器：key 集合与历史逐字一致，token 首次生成时落审计表并写回实体
     */
    @Test
    public void issueShouldKeepBuildResponseKeys() {
        Mockito.when(tokenLogServer.restToken(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn("tk-new");
        FakeTokenService service = new FakeTokenService("BUILD_INFO");

        Map<String, String> map = assembler.issue(service, "build-1", null, "",
            "user-1", buildUrls(), request());

        Assertions.assertEquals("tk-new", map.get("token"));
        Assertions.assertEquals("build-1", map.get("id"));
        Assertions.assertEquals("/api/build2/build-1/tk-new", map.get("triggerBuildUrl"));
        Assertions.assertEquals("/api/build_batch", map.get("batchTriggerBuildUrl"));
        Assertions.assertEquals("/api/build_status", map.get("batchBuildStatusUrl"));
        Assertions.assertEquals("/api/build_log", map.get("buildLogUrl"));
        Assertions.assertEquals(
            Arrays.asList("triggerBuildUrl", "batchTriggerBuildUrl", "batchBuildStatusUrl", "buildLogUrl", "id", "token"),
            new ArrayList<>(map.keySet()));
        // token 写回只发生一次，且写回的值与响应一致
        Assertions.assertEquals(1, service.writeCount);
        Assertions.assertEquals("tk-new", service.lastWrittenToken);
        Mockito.verify(tokenLogServer, Mockito.times(1))
            .restToken(ArgumentMatchers.isNull(), ArgumentMatchers.eq("BUILD_INFO"), ArgumentMatchers.eq("build-1"), ArgumentMatchers.eq("user-1"));
    }

    /**
     * 已有 token 且不重置：不得生成新 token（否则用户已配好的地址会失效）
     */
    @Test
    public void issueShouldReuseTokenWithoutReset() {
        FakeTokenService service = new FakeTokenService("BUILD_INFO");
        Map<String, String> map = assembler.issue(service, "build-1", "tk-old", "",
            "user-1", buildUrls(), request());

        Assertions.assertEquals("tk-old", map.get("token"));
        Assertions.assertEquals("/api/build2/build-1/tk-old", map.get("triggerBuildUrl"));
        Assertions.assertEquals(0, service.writeCount);
        Mockito.verify(tokenLogServer, Mockito.never())
            .restToken(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    /**
     * 重置：必须重新生成并写回
     */
    @Test
    public void issueShouldResetTokenWhenRestNotEmpty() {
        Mockito.when(tokenLogServer.restToken(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn("tk-reset");
        FakeTokenService service = new FakeTokenService("BUILD_INFO");
        Map<String, String> map = assembler.issue(service, "build-1", "tk-old", "rest",
            "user-1", buildUrls(), request());

        Assertions.assertEquals("tk-reset", map.get("token"));
        Assertions.assertEquals(1, service.writeCount);
        Mockito.verify(tokenLogServer, Mockito.times(1))
            .restToken(ArgumentMatchers.eq("tk-old"), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    /**
     * 文件存储：别名地址只有配置了别名才输出（历史行为，不能因为统一收口点而多出空地址）
     */
    @Test
    public void issueShouldSkipAliasUrlWhenAliasEmpty() {
        Mockito.when(tokenLogServer.restToken(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn("tk-store");
        FakeTokenService service = new FakeTokenService("FILE_STORAGE");
        TriggerUrls urls = TriggerUrls.builder()
            .add("triggerDownloadUrl", ServerOpenApi.FILE_STORAGE_DOWNLOAD)
            .addAlias("triggerAliasDownloadUrl", ServerOpenApi.FILE_STORAGE_DOWNLOAD)
            .build();

        Map<String, String> noAlias = assembler.issue(service, "store-1", "tk-store", "", "user-1", urls, request(), null);
        Assertions.assertEquals("/api/file-storage/download/store-1/tk-store", noAlias.get("triggerDownloadUrl"));
        Assertions.assertFalse(noAlias.containsKey("triggerAliasDownloadUrl"));

        Map<String, String> withAlias = assembler.issue(service, "store-1", "tk-store", "", "user-1", urls, request(), "alias-1");
        Assertions.assertEquals("/api/file-storage/download/alias-1/tk-store", withAlias.get("triggerAliasDownloadUrl"));
    }

    /**
     * 系统备份这类全局 token：没有实体 id，响应只有地址与 token
     */
    @Test
    public void issueGlobalShouldNotEmitDataId() {
        ArrayList<String> saved = new ArrayList<>();
        Map<String, String> map = assembler.issueGlobal(null, "", saved::add,
            TriggerUrls.builder().add("triggerUrl", ServerOpenApi.BACKUP_TRIGGER_URL).withoutDataId().build(), request());

        Assertions.assertEquals(1, saved.size());
        Assertions.assertEquals(saved.get(0), map.get("token"));
        Assertions.assertFalse(map.containsKey("id"));
        Assertions.assertEquals("/api/backup-db/" + saved.get(0), map.get("triggerUrl"));

        // 已有 token 且不重置：不再调用 saver
        ArrayList<String> saved2 = new ArrayList<>();
        assembler.issueGlobal("tk-exists", "", saved2::add,
            TriggerUrls.builder().add("triggerUrl", ServerOpenApi.BACKUP_TRIGGER_URL).withoutDataId().build(), request());
        Assertions.assertTrue(saved2.isEmpty());
    }

    private TriggerUrls buildUrls() {
        return TriggerUrls.builder()
            .add("triggerBuildUrl", ServerOpenApi.BUILD_TRIGGER_BUILD2)
            .add("batchTriggerBuildUrl", ServerOpenApi.BUILD_TRIGGER_BUILD_BATCH)
            .add("batchBuildStatusUrl", ServerOpenApi.BUILD_TRIGGER_STATUS)
            .add("buildLogUrl", ServerOpenApi.BUILD_TRIGGER_LOG)
            .build();
    }

    /**
     * 记录 token 写回的服务替身
     */
    private static final class FakeTokenService implements ITriggerToken {
        private final String type;
        private int writeCount;
        private String lastWrittenToken;

        private FakeTokenService(String type) {
            this.type = type;
        }

        @Override
        public String typeName() {
            return type;
        }

        @Override
        public String getDataDesc() {
            return type;
        }

        @Override
        public boolean exists(String dataId) {
            return true;
        }

        @Override
        public BaseIdModel getByKey(String keyValue) {
            return null;
        }

        @Override
        public void updateTriggerToken(String id, String triggerToken) {
            this.writeCount++;
            this.lastWrittenToken = triggerToken;
        }
    }
}
