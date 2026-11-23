/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.sshproject;

import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.model.data.SshProjectModel;
import org.dromara.jpom.model.log.BuildHistoryLog;
import org.dromara.jpom.model.sshproject.SshProjectExecuteResult;
import org.dromara.jpom.build.version.BuildVersionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.ResourceBundleMessageSource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SSH 项目版本快照：远端列表解析、保留个数归一、构建历史版本过滤。
 * <p>
 * 远端脚本用打桩输出验证解析与过滤，避免把「解析错位导致版本列表为空」这类问题留到界面才发现。
 *
 * @author bwcx_jzy
 * @since 2026/11/20
 */
public class SshProjectVersionServiceTest {

    @BeforeAll
    public static void initMessageSource() {
        GenericApplicationContext context = new GenericApplicationContext();
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setUseCodeAsDefaultMessage(true);
        context.getBeanFactory().registerSingleton("messageSource", messageSource);
        context.refresh();
        new SpringUtil().setApplicationContext(context);
    }

    private static SshProjectModel project() {
        SshProjectModel project = new SshProjectModel();
        project.setId("p1");
        project.setName("demo");
        project.setPath("/data/demo");
        project.setWorkspaceId("DEFAULT");
        project.setDslContent("description: d\n");
        return project;
    }

    private static SshProjectExecuteResult ok(String output) {
        SshProjectExecuteResult result = new SshProjectExecuteResult();
        result.setSuccess(true);
        result.setExitCode(0);
        result.setOutput(output);
        return result;
    }

    @Test
    public void resolveKeepCountFallsBackToDefault() {
        // 与构建侧同口径：0 / 空按默认 3，避免「开关打开但保留个数为 0」时静默不清理
        Assertions.assertEquals(SshProjectVersionService.DEFAULT_KEEP_COUNT, SshProjectVersionService.resolveKeepCount(null));
        Assertions.assertEquals(SshProjectVersionService.DEFAULT_KEEP_COUNT, SshProjectVersionService.resolveKeepCount(0));
        Assertions.assertEquals(SshProjectVersionService.DEFAULT_KEEP_COUNT, SshProjectVersionService.resolveKeepCount(-2));
        Assertions.assertEquals(8, SshProjectVersionService.resolveKeepCount(8));
    }

    @Test
    public void listParsesRemoteMetasAndSortsByCreateTime() {
        SshProjectExecutor executor = mock(SshProjectExecutor.class);
        String output = String.join("\n",
            "ROOT|/home/u/.jpom/versions/p1",
            "PROJECT|/data/demo",
            "PROJECT_FILES|2",
            "CURRENT|2",
            "META|1|{\"versionId\":\"2\",\"source\":\"build\",\"buildNumberId\":2,\"createTime\":2000,\"fileCount\":2,\"fileSize\":100,\"manifest\":\"m2\"}",
            "META|0|{\"versionId\":\"1\",\"source\":\"baseline\",\"buildNumberId\":1,\"createTime\":1000,\"fileCount\":1,\"fileSize\":10,\"manifest\":\"m1\"}",
            "JPOM_OK");
        when(executor.runScript(any(), anyString(), anyInt())).thenReturn(ok(output));
        SshProjectVersionService service = new SshProjectVersionService(executor, mock(SshProjectService.class));
        JSONObject list = service.list(project());
        Assertions.assertEquals("/home/u/.jpom/versions/p1", list.getString("path"));
        Assertions.assertEquals("2", list.getString("current"));
        Assertions.assertEquals(2, list.getIntValue("projectFiles"));
        Assertions.assertEquals(2, list.getJSONArray("list").size());
        // 新版本在前
        Assertions.assertEquals("2", list.getJSONArray("list").getJSONObject(0).getString("versionId"));
        Assertions.assertTrue(list.getJSONArray("list").getJSONObject(0).getBooleanValue("exist"));
        // 版本目录已丢失的版本要标出来（前端据此禁止切换与删除）
        Assertions.assertFalse(list.getJSONArray("list").getJSONObject(1).getBooleanValue("exist"));
        Assertions.assertTrue(list.getBooleanValue("hasVersions"));
    }

    @Test
    public void listByBuildLogOnlyKeepsBuildVersionsOfThatBuild() {
        SshProjectExecutor executor = mock(SshProjectExecutor.class);
        SshProjectService sshProjectService = mock(SshProjectService.class);
        when(sshProjectService.getByKey("p1")).thenReturn(project());
        String output = String.join("\n",
            "ROOT|/home/u/.jpom/versions/p1",
            "PROJECT|/data/demo",
            "PROJECT_FILES|2",
            "CURRENT|3",
            "META|1|{\"versionId\":\"3\",\"source\":\"build\",\"buildNumberId\":3,\"createTime\":3000}",
            "META|1|{\"versionId\":\"2\",\"source\":\"build\",\"buildNumberId\":2,\"createTime\":2000}",
            "META|1|{\"versionId\":\"auto-1\",\"source\":\"auto\",\"buildNumberId\":0,\"createTime\":4000}",
            "JPOM_OK");
        when(executor.runScript(any(), anyString(), anyInt())).thenReturn(ok(output));
        SshProjectVersionService service = new SshProjectVersionService(executor, sshProjectService);
        BuildHistoryLog log = new BuildHistoryLog();
        log.setBuildDataId("b1");
        log.setBuildNumberId(2);
        log.setReleaseMethod(7);
        log.setExtraData("{\"releaseMethod\":7,\"releaseMethodDataId\":\"p1\"}");
        JSONObject result = service.listByBuildLog(log);
        Assertions.assertEquals("sshProject", result.getString("targetType"));
        Assertions.assertTrue(result.getBooleanValue("reachable"));
        Assertions.assertEquals("demo", result.getString("projectName"));
        Assertions.assertEquals(1, result.getJSONArray("list").size());
        JSONObject item = result.getJSONArray("list").getJSONObject(0);
        Assertions.assertEquals("2", item.getString("versionId"));
        Assertions.assertEquals(1, item.getIntValue("status"));
    }

    @Test
    public void listByBuildLogReportsUnreachableMachine() {
        SshProjectExecutor executor = mock(SshProjectExecutor.class);
        SshProjectService sshProjectService = mock(SshProjectService.class);
        when(sshProjectService.getByKey("p1")).thenReturn(project());
        SshProjectExecuteResult fail = new SshProjectExecuteResult();
        fail.setSuccess(false);
        fail.setExitCode(-100);
        fail.setMessage("connect timeout");
        when(executor.runScript(any(), anyString(), anyInt())).thenReturn(fail);
        SshProjectVersionService service = new SshProjectVersionService(executor, sshProjectService);
        BuildHistoryLog log = new BuildHistoryLog();
        log.setBuildNumberId(1);
        log.setReleaseMethod(7);
        log.setExtraData("{\"releaseMethod\":7,\"releaseMethodDataId\":\"p1\"}");
        JSONObject result = service.listByBuildLog(log);
        // 目标机不可达：不抛异常打断弹窗，标记不可达由界面提示
        Assertions.assertFalse(result.getBooleanValue("reachable"));
        Assertions.assertEquals(0, result.getJSONArray("list").size());
    }

    @Test
    public void listByBuildLogMergesMultipleProjects() {
        // 发布方式＝SSH 项目支持多选：构建历史弹窗要把每个项目在该构建产生的版本都列出来
        SshProjectExecutor executor = mock(SshProjectExecutor.class);
        SshProjectService sshProjectService = mock(SshProjectService.class);
        SshProjectModel p2 = project();
        p2.setId("p2");
        p2.setName("demo2");
        when(sshProjectService.getByKey("p1")).thenReturn(project());
        when(sshProjectService.getByKey("p2")).thenReturn(p2);
        String output = String.join("\n",
            "ROOT|/home/u/.jpom/versions/p1",
            "PROJECT|/data/demo",
            "PROJECT_FILES|1",
            "CURRENT|2",
            "META|1|{\"versionId\":\"2\",\"source\":\"build\",\"buildNumberId\":2,\"buildDataId\":\"b1\",\"createTime\":2000}",
            "JPOM_OK");
        when(executor.runScript(any(), anyString(), anyInt())).thenReturn(ok(output));
        SshProjectVersionService service = new SshProjectVersionService(executor, sshProjectService);
        BuildHistoryLog log = new BuildHistoryLog();
        log.setBuildDataId("b1");
        log.setBuildNumberId(2);
        log.setReleaseMethod(7);
        log.setExtraData("{\"releaseMethod\":7,\"releaseMethodDataId\":\"p1,p2\"}");
        JSONObject result = service.listByBuildLog(log);
        Assertions.assertTrue(result.getBooleanValue("reachable"));
        Assertions.assertEquals(2, result.getJSONArray("projects").size());
        Assertions.assertEquals(2, result.getJSONArray("list").size());
        Assertions.assertEquals("p1", result.getJSONArray("list").getJSONObject(0).getString("projectId"));
        Assertions.assertEquals("p2", result.getJSONArray("list").getJSONObject(1).getString("projectId"));
    }

    @Test
    public void listByBuildLogSkipsProjectRemovedFromSelection() {
        // 多选里某个项目已被删除：其余项目照常列出，不因一个目标缺失把整个列表判成不可达
        SshProjectExecutor executor = mock(SshProjectExecutor.class);
        SshProjectService sshProjectService = mock(SshProjectService.class);
        when(sshProjectService.getByKey("p1")).thenReturn(project());
        when(sshProjectService.getByKey("gone")).thenReturn(null);
        String output = String.join("\n",
            "ROOT|/home/u/.jpom/versions/p1",
            "PROJECT|/data/demo",
            "PROJECT_FILES|1",
            "CURRENT|1",
            "META|1|{\"versionId\":\"1\",\"source\":\"build\",\"buildNumberId\":1,\"buildDataId\":\"b1\",\"createTime\":1000}",
            "JPOM_OK");
        when(executor.runScript(any(), anyString(), anyInt())).thenReturn(ok(output));
        SshProjectVersionService service = new SshProjectVersionService(executor, sshProjectService);
        BuildHistoryLog log = new BuildHistoryLog();
        log.setBuildDataId("b1");
        log.setBuildNumberId(1);
        log.setReleaseMethod(7);
        log.setExtraData("{\"releaseMethod\":7,\"releaseMethodDataId\":\"p1,gone\"}");
        JSONObject result = service.listByBuildLog(log);
        Assertions.assertTrue(result.getBooleanValue("reachable"));
        Assertions.assertEquals(1, result.getJSONArray("projects").size());
        Assertions.assertEquals(1, result.getJSONArray("list").size());
        Assertions.assertEquals("p1", result.getJSONArray("list").getJSONObject(0).getString("projectId"));
    }

    @Test
    public void sshProjectReleaseMethodDetection() {
        BuildHistoryLog log = new BuildHistoryLog();
        log.setReleaseMethod(7);
        Assertions.assertTrue(BuildVersionService.isSshProject(log));
        log.setReleaseMethod(2);
        Assertions.assertFalse(BuildVersionService.isSshProject(log));
        Assertions.assertFalse(BuildVersionService.isSshProject(null));
    }
}
