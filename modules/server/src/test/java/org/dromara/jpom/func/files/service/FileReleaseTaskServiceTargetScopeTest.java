/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.files.service;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.TestI18nSupport;
import org.dromara.jpom.configuration.BuildExtConfig;
import org.dromara.jpom.db.DbExtConfig;
import org.dromara.jpom.db.StorageServiceFactory;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.data.SshModel;
import org.dromara.jpom.service.node.NodeService;
import org.dromara.jpom.service.node.ssh.SshService;
import org.dromara.jpom.service.script.ScriptServer;
import org.dromara.jpom.service.system.WorkspaceEnvVarService;
import org.dromara.jpom.system.ServerConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.context.support.GenericApplicationContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 文件发布任务目标解析的工作空间隔离回归（2026-11-20 外部安全报告 #6）。
 * <p>
 * 背景：{@code FileReleaseTaskService.addTask} 曾用 {@code sshService.exists(new SshModel(id))} /
 * {@code nodeService.exists(new NodeModel(id))} 只做存在性校验，没有工作空间条件。工作空间 A 的用户只要知道
 * 工作空间 B 的 node/SSH 主机 id，就能创建文件发布任务，让 before/after 脚本在 B 的主机上执行、文件落到 B 的
 * 发布目录（A 的节点/SSH 列表里看不到该主机，{@code /node/ssh_command/batch} 对同一 id 会拒绝）。
 * </p>
 *
 * @author jpom
 */
public class FileReleaseTaskServiceTargetScopeTest {

    private SshService sshService;
    private NodeService nodeService;
    private FileReleaseTaskService fileReleaseTaskService;

    @BeforeAll
    public static void initContext() {
        TestI18nSupport.init();
        // BaseDbCommonService 构造期会从 SpringUtil 取 DbExtConfig/方言，纯 Mockito 单测需要手工注册
        DbExtConfig dbExtConfig = mock(DbExtConfig.class);
        when(dbExtConfig.getMode()).thenReturn(DbExtConfig.Mode.H2);
        StorageServiceFactory.getInstance().setMode(DbExtConfig.Mode.H2);
        MessageSource messageSource = mock(MessageSource.class);
        when(messageSource.getMessage(anyString(), Mockito.any(), Mockito.any(Locale.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        GenericApplicationContext applicationContext = new GenericApplicationContext();
        applicationContext.getBeanFactory().registerSingleton("messageSource", messageSource);
        applicationContext.getBeanFactory().registerSingleton("dbExtConfig", dbExtConfig);
        applicationContext.refresh();
        new SpringUtil().setApplicationContext(applicationContext);
    }

    @BeforeEach
    public void setUp() {
        sshService = mock(SshService.class);
        nodeService = mock(NodeService.class);
        fileReleaseTaskService = new FileReleaseTaskService(sshService,
            mock(JpomApplication.class),
            mock(WorkspaceEnvVarService.class),
            nodeService,
            mock(BuildExtConfig.class),
            mock(ServerConfig.class),
            mock(FileStorageService.class),
            mock(StaticFileStorageService.class),
            mock(ScriptServer.class),
            mock(FileReleaseTaskTemplateService.class));
    }

    /**
     * 混合列表：只保留当前请求工作空间的目标，其他工作空间的 id 必须被剔除。
     */
    @Test
    public void testMixedTargetsKeepOnlyCurrentWorkspace() {
        when(nodeService.getByKeyAndWorkspace("node-mine", "ws-a")).thenReturn(new NodeModel());
        when(nodeService.getByKeyAndWorkspace("node-foreign", "ws-a")).thenReturn(null);

        List<String> list = fileReleaseTaskService.resolveTaskTargetIds(1, "node-mine,node-foreign", "ws-a");

        assertEquals(Collections.singletonList("node-mine"), list);
        // 只允许带工作空间条件的查询，不允许回退到非隔离查询
        verify(nodeService).getByKeyAndWorkspace("node-mine", "ws-a");
        verify(nodeService, never()).getByKey(anyString());
        verify(nodeService, never()).getByKey(anyString(), Mockito.anyBoolean());
        verify(nodeService, never()).exists(Mockito.any(NodeModel.class));
    }

    /**
     * 全部目标都属于其他工作空间：解析结果为空，调用方（addTask）据此拒绝创建任务。
     */
    @Test
    public void testAllForeignTargetsResolvedToEmpty() {
        when(nodeService.getByKeyAndWorkspace(anyString(), Mockito.eq("ws-a"))).thenReturn(null);

        List<String> list = fileReleaseTaskService.resolveTaskTargetIds(1, "node-b1,node-b2", "ws-a");

        assertTrue(list.isEmpty());
    }

    /**
     * SSH 目标同样按工作空间过滤（taskType=0）。
     */
    @Test
    public void testSshTargetsAreWorkspaceScoped() {
        when(sshService.getByKeyAndWorkspace("ssh-mine", "ws-a")).thenReturn(new SshModel());
        when(sshService.getByKeyAndWorkspace("ssh-foreign", "ws-a")).thenReturn(null);

        List<String> list = fileReleaseTaskService.resolveTaskTargetIds(0, "ssh-foreign,ssh-mine", "ws-a");

        assertEquals(Collections.singletonList("ssh-mine"), list);
        verify(sshService, never()).getByKey(anyString());
        verify(sshService, never()).exists(Mockito.any(SshModel.class));
    }

    /**
     * 不支持的任务类型直接拒绝，不再落入某一种目标的解析分支。
     */
    @Test
    public void testUnsupportedTaskTypeRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> fileReleaseTaskService.resolveTaskTargetIds(2, "any", "ws-a"));
        Mockito.verifyNoInteractions(nodeService, sshService);
    }

    /**
     * 已拼装发布目录的兜底校验：包含 .. 路径段直接拒绝（重新发布等不经过二级目录拼装的入口）。
     */
    @Test
    public void testAssertReleasePathRejectsTraversalSegments() {
        for (String path : Arrays.asList("/data/release/../..", "/data/release/../../tmp", "C:\\release\\..\\..\\tmp",
            "/data/release/..\\tmp")) {
            // 与控制器白名单校验一致，越界用 Assert.state 拒绝
            assertThrows(IllegalStateException.class, () -> FileReleaseTaskService.assertReleasePath(path), path);
        }
    }

    /**
     * 正常发布目录（含文件名里带点、父目录带点）不受影响。
     */
    @Test
    public void testAssertReleasePathAllowsNormalPaths() {
        for (String path : Arrays.asList("/data/release/app", "/data/release/app.v1.0", "/data/release..bak/app", "app/1.0.0")) {
            FileReleaseTaskService.assertReleasePath(path);
        }
    }

    /**
     * 空发布目录直接拒绝，避免落到当前工作目录。
     */
    @Test
    public void testAssertReleasePathRejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> FileReleaseTaskService.assertReleasePath(null));
        assertThrows(IllegalArgumentException.class, () -> FileReleaseTaskService.assertReleasePath(""));
    }
}
