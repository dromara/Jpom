/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.sshproject;

import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.SshProjectModel;
import org.dromara.jpom.model.sshproject.SshProjectExecuteResult;
import org.dromara.jpom.model.sshproject.SshProjectStatus;
import org.dromara.jpom.model.sshproject.SshProjectTemplate;
import org.dromara.jpom.service.sshproject.SshProjectLogService;
import org.dromara.jpom.service.sshproject.SshProjectService;
import org.dromara.jpom.service.sshproject.SshProjectTemplateService;
import org.dromara.jpom.service.sshproject.SshProjectVersionService;
import org.dromara.jpom.service.user.TriggerUrlAssembler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * SSH 项目接口契约测试：路由注册、参数绑定、分页参数兼容、响应结构。
 * <p>
 * 不依赖数据库与登录态（服务层 mock），补足装配级验证。
 *
 * @author bwcx_jzy
 * @since 2026/11/20
 */
public class SshProjectControllerTest {

    private SshProjectService sshProjectService;
    private SshProjectTemplateService sshProjectTemplateService;
    private SshProjectVersionService sshProjectVersionService;
    private MockMvc mockMvc;

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

    @BeforeEach
    public void setup() {
        sshProjectService = mock(SshProjectService.class);
        sshProjectTemplateService = mock(SshProjectTemplateService.class);
        sshProjectVersionService = mock(SshProjectVersionService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SshProjectController(sshProjectService, sshProjectTemplateService,
            mock(SshProjectLogService.class), sshProjectVersionService,
            mock(TriggerUrlAssembler.class))).build();
    }

    @Test
    public void templateListReturnsTemplates() throws Exception {
        SshProjectTemplate template = new SshProjectTemplate();
        template.setKey("spring-boot-jar");
        template.setName("Spring Boot Jar");
        when(sshProjectTemplateService.list()).thenReturn(Collections.singletonList(template));
        mockMvc.perform(post("/node/ssh-project/template-list"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].key").value("spring-boot-jar"));
    }

    @Test
    public void templateRenderReturnsDslContent() throws Exception {
        when(sshProjectTemplateService.render(eq("systemd"), any())).thenReturn("description: x\n");
        mockMvc.perform(post("/node/ssh-project/template-render")
                .param("templateKey", "systemd")
                .param("fieldsJson", "{}"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.dslContent").value("description: x\n"));
    }

    @Test
    public void saveDelegatesModelToService() throws Exception {
        mockMvc.perform(post("/node/ssh-project/save")
                .param("name", "demo")
                .param("path", "/data/demo")
                .param("dslContent", "description: d"))
            .andExpect(jsonPath("$.code").value(200));
        ArgumentCaptor<SshProjectModel> captor = ArgumentCaptor.forClass(SshProjectModel.class);
        verify(sshProjectService).saveItem(captor.capture(), any(HttpServletRequest.class));
        Assertions.assertEquals("demo", captor.getValue().getName());
        Assertions.assertEquals("/data/demo", captor.getValue().getPath());
    }

    @Test
    public void listAcceptsCurrentPageSizeAliases() throws Exception {
        when(sshProjectService.getCheckUserWorkspace(any(HttpServletRequest.class))).thenReturn("DEFAULT");
        when(sshProjectService.listPage(anyMap())).thenReturn(new PageResultDto<>(1, 10, 0));
        mockMvc.perform(post("/node/ssh-project/list")
                .param("current", "2")
                .param("pageSize", "5"))
            .andExpect(jsonPath("$.code").value(200));
        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(sshProjectService).listPage(captor.capture());
        Assertions.assertEquals("2", captor.getValue().get("page"));
        Assertions.assertEquals("5", captor.getValue().get("limit"));
        Assertions.assertEquals("DEFAULT", captor.getValue().get("workspaceId"));
    }

    @Test
    public void deleteDelegatesToService() throws Exception {
        mockMvc.perform(post("/node/ssh-project/delete").param("id", "abc"))
            .andExpect(jsonPath("$.code").value(200));
        verify(sshProjectService).deleteItem(eq("abc"), any(HttpServletRequest.class));
    }

    @Test
    public void operateReturnsStatusAfterExecution() throws Exception {
        SshProjectExecuteResult result = new SshProjectExecuteResult();
        result.setExitCode(0);
        result.setSuccess(true);
        result.setOutput("started: 123");
        result.setCost(12);
        SshProjectModel project = new SshProjectModel();
        project.setId("p1");
        project.setLastStatus("running");
        project.setLastPid(123);
        project.setLastPort("8080");
        when(sshProjectService.operate(eq("p1"), eq("start"), any(), any(HttpServletRequest.class))).thenReturn(result);
        when(sshProjectService.getItem(eq("p1"), any(HttpServletRequest.class))).thenReturn(project);
        mockMvc.perform(post("/node/ssh-project/operate").param("id", "p1").param("opt", "start"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.success").value(true))
            .andExpect(jsonPath("$.data.status").value("running"))
            .andExpect(jsonPath("$.data.pid").value(123));
    }

    @Test
    public void statusBatchReturnsMap() throws Exception {
        SshProjectModel project = new SshProjectModel();
        project.setId("p1");
        project.setLastStatus("running");
        SshProjectStatus status = new SshProjectStatus();
        status.setRunning(true);
        status.setPid(9);
        status.setPorts("80");
        status.setMessage("running:9:80");
        when(sshProjectService.getItem(eq("p1"), any(HttpServletRequest.class))).thenReturn(project);
        when(sshProjectService.refreshStatus(any(SshProjectModel.class), eq(true))).thenReturn(status);
        when(sshProjectService.getItem(any(), any(HttpServletRequest.class))).thenReturn(project);
        mockMvc.perform(post("/node/ssh-project/status").param("ids", "[\"p1\"]"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.p1.status").value("running"));
    }

    @Test
    public void logListContainsConsoleAndConfiguredFiles() throws Exception {
        SshProjectModel project = new SshProjectModel();
        project.setId("p1");
        project.setDslContent("description: d\nconfig:\n  log:\n    - name: 应用日志\n      path: logs/app.log\n");
        when(sshProjectService.getItem(eq("p1"), any(HttpServletRequest.class))).thenReturn(project);
        mockMvc.perform(post("/node/ssh-project/log-list").param("id", "p1"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].name").value("console"))
            .andExpect(jsonPath("$.data[1].name").value("应用日志"));
    }

    @Test
    public void versionListReturnsRemoteVersions() throws Exception {
        SshProjectModel project = new SshProjectModel();
        project.setId("p1");
        when(sshProjectService.getItem(eq("p1"), any(HttpServletRequest.class))).thenReturn(project);
        JSONObject data = new JSONObject();
        data.put("path", "/home/u/.jpom/versions/p1");
        data.put("current", "3");
        data.put("enabled", true);
        JSONArray list = new JSONArray();
        JSONObject item = new JSONObject();
        item.put("versionId", "3");
        item.put("source", "build");
        item.put("buildNumberId", 3);
        item.put("exist", true);
        list.add(item);
        data.put("list", list);
        when(sshProjectVersionService.list(any(SshProjectModel.class))).thenReturn(data);
        mockMvc.perform(post("/node/ssh-project/version-list").param("id", "p1"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.current").value("3"))
            .andExpect(jsonPath("$.data.list[0].versionId").value("3"))
            .andExpect(jsonPath("$.data.list[0].source").value("build"));
    }

    @Test
    public void versionSwitchPassesVersionIdAndRefreshesStatus() throws Exception {
        SshProjectModel project = new SshProjectModel();
        project.setId("p1");
        when(sshProjectService.getItem(eq("p1"), any(HttpServletRequest.class))).thenReturn(project);
        JSONObject switched = new JSONObject();
        switched.put("versionId", "2");
        switched.put("restarted", true);
        when(sshProjectVersionService.switchVersion(any(SshProjectModel.class), eq("2"), eq(1), eq(true), isNull()))
            .thenReturn(switched);
        mockMvc.perform(post("/node/ssh-project/version-switch")
                .param("id", "p1")
                .param("versionId", "2")
                .param("after", "1")
                .param("snapshotCurrent", "true"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.versionId").value("2"));
        verify(sshProjectVersionService).switchVersion(any(SshProjectModel.class), eq("2"), eq(1), eq(true), isNull());
        // 切换会改动项目目录，状态缓存必须回写
        verify(sshProjectService).refreshStatus(any(SshProjectModel.class), eq(true));
    }

    @Test
    public void versionDeleteDelegatesToService() throws Exception {
        SshProjectModel project = new SshProjectModel();
        project.setId("p1");
        when(sshProjectService.getItem(eq("p1"), any(HttpServletRequest.class))).thenReturn(project);
        mockMvc.perform(post("/node/ssh-project/version-delete").param("id", "p1").param("versionId", "2"))
            .andExpect(jsonPath("$.code").value(200));
        verify(sshProjectVersionService).deleteVersion(any(SshProjectModel.class), eq("2"));
    }

    @Test
    public void versionCommitCreatesManualSnapshot() throws Exception {
        SshProjectModel project = new SshProjectModel();
        project.setId("p1");
        when(sshProjectService.getItem(eq("p1"), any(HttpServletRequest.class))).thenReturn(project);
        JSONObject commit = new JSONObject();
        commit.put("versionId", "manual-1");
        when(sshProjectVersionService.commit(any(SshProjectModel.class), isNull(),
            eq(SshProjectVersionService.SOURCE_MANUAL), isNull(), isNull(), isNull())).thenReturn(commit);
        mockMvc.perform(post("/node/ssh-project/version-commit").param("id", "p1"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.versionId").value("manual-1"));
    }
}
