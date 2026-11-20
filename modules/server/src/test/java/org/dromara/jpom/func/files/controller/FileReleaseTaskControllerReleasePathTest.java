/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.files.controller;

import cn.hutool.core.collection.CollUtil;
import org.dromara.jpom.TestI18nSupport;
import org.dromara.jpom.controller.outgiving.OutGivingWhitelistService;
import org.dromara.jpom.func.files.model.FileReleaseTaskLogModel;
import org.dromara.jpom.func.files.model.IFileStorage;
import org.dromara.jpom.func.files.service.FileReleaseTaskService;
import org.dromara.jpom.func.files.service.FileReleaseTaskTemplateService;
import org.dromara.jpom.model.data.ServerWhitelist;
import org.dromara.jpom.service.node.NodeService;
import org.dromara.jpom.service.script.ScriptServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 文件发布任务二级目录穿越回归（2026-11-20 外部安全报告 #6）。
 * <p>
 * 背景：{@code FileReleaseTaskController.addTask} 曾用
 * {@code FileUtil.normalize(releasePathParent + "/" + releasePathSecondary)} 拼发布目录，父目录有分发授权白名单
 * 校验，但二级目录没有包含校验，{@code releasePathSecondary=..} 可以把文件写到白名单目录之外（报告在宿主 /tmp 验证）。
 * 修复后二级目录走 {@code FileReleaseTaskService.resolveReleasePath}（{@code FileUtils.resolveSubFile} 包含校验）。
 * </p>
 *
 * @author jpom
 */
public class FileReleaseTaskControllerReleasePathTest {

    private static final String PARENT = "/data/release";

    private FileReleaseTaskService fileReleaseTaskService;
    private FileReleaseTaskTemplateService templateService;
    private FileReleaseTaskController controller;
    private MockHttpServletRequest request;

    @BeforeEach
    public void setUp() {
        TestI18nSupport.init();
        fileReleaseTaskService = mock(FileReleaseTaskService.class);
        templateService = mock(FileReleaseTaskTemplateService.class);
        OutGivingWhitelistService outGivingWhitelistService = mock(OutGivingWhitelistService.class);
        controller = new FileReleaseTaskController(fileReleaseTaskService, outGivingWhitelistService,
            mock(NodeService.class), mock(ScriptServer.class), templateService);
        request = new MockHttpServletRequest();
        ServerWhitelist whitelist = new ServerWhitelist();
        whitelist.setOutGiving(CollUtil.newArrayList(PARENT));
        when(outGivingWhitelistService.getServerWhitelistData(request)).thenReturn(whitelist);
        when(templateService.getCheckUserWorkspace(request)).thenReturn("ws-a");
    }

    /**
     * 二级目录越界：必须直接拒绝，且不创建任务、不执行任何脚本/上传。
     */
    @Test
    public void testTraversalSecondaryRejectedBeforeTaskCreation() {
        for (String secondary : new String[]{"..", "../..", "..\\..\\tmp", "/../../tmp", "app/../../.."}) {
            assertThrows(IllegalArgumentException.class,
                () -> controller.addTask("file-1", 1, "task", 1, "node-1", PARENT, secondary, null, null, null, request),
                "二级目录越界必须被拒绝: " + secondary);
        }
        verifyNoInteractions(fileReleaseTaskService);
    }

    /**
     * 正常二级目录：解析为父目录 + 二级目录，并原样传给任务创建与模板保存。
     */
    @Test
    public void testNormalSecondaryResolvedUnderParent() {
        when(fileReleaseTaskService.addTask(anyString(), anyInt(), anyString(), anyInt(), anyString(), anyString(),
            any(), any(), any(), eq(request))).thenReturn(mock(IFileStorage.class));

        controller.addTask("file-1", 1, "task", 1, "node-1", PARENT, "app/1.0", null, null, null, request);

        ArgumentCaptor<String> releasePath = ArgumentCaptor.forClass(String.class);
        verify(fileReleaseTaskService).addTask(eq("file-1"), eq(1), eq("task"), eq(1), eq("node-1"),
            releasePath.capture(), any(), any(), any(), eq(request));
        assertEquals("/data/release/app/1.0", releasePath.getValue());
        // 模板保存使用同一个已校验的发布目录，不再是手工拼装的字符串
        verify(templateService).add(any(), eq("ws-a"), any(), eq(1), any());
    }

    /**
     * 绝对路径二级目录不会跳出父目录：去掉前导分隔符后仍落在父目录内（与修复前的拼装结果一致）。
     */
    @Test
    public void testAbsoluteSecondaryStaysInsideParent() {
        assertEquals("/data/release/app", FileReleaseTaskService.resolveReleasePath(PARENT, "/app"));
        assertEquals("/data/release/app", FileReleaseTaskService.resolveReleasePath(PARENT + "/", "app"));
    }

    /**
     * 空二级目录拒绝（不拼成父目录本身）。
     */
    @Test
    public void testBlankSecondaryRejected() {
        assertThrows(IllegalArgumentException.class, () -> FileReleaseTaskService.resolveReleasePath(PARENT, ""));
        assertThrows(IllegalArgumentException.class, () -> FileReleaseTaskService.resolveReleasePath(PARENT, null));
    }

    /**
     * 重新发布复用父任务落库的目录：修复前创建的任务可能落在授权目录之外，再次发布必须拒绝。
     */
    @Test
    public void testReTaskRejectsStoredPathOutsideAuthorizedDir() {
        FileReleaseTaskLogModel parent = new FileReleaseTaskLogModel();
        parent.setId("parent-1");
        parent.setReleasePath("/tmp/outside");
        parent.setFileType(1);
        when(fileReleaseTaskService.getByKey("parent-1", request)).thenReturn(parent);

        assertThrows(IllegalStateException.class,
            () -> controller.reTask("file-1", "task", 1, "node-1", "parent-1", null, null, request));
        verify(fileReleaseTaskService, never()).addTask(anyString(), anyInt(), anyString(), anyInt(), anyString(),
            anyString(), any(), any(), any(), any());
    }

    /**
     * 重新发布：父任务目录仍在授权目录内时按原目录继续发布。
     */
    @Test
    public void testReTaskKeepsStoredPathInsideAuthorizedDir() {
        FileReleaseTaskLogModel parent = new FileReleaseTaskLogModel();
        parent.setId("parent-1");
        parent.setReleasePath(PARENT + "/app");
        parent.setFileType(1);
        when(fileReleaseTaskService.getByKey("parent-1", request)).thenReturn(parent);
        when(fileReleaseTaskService.addTask(anyString(), anyInt(), anyString(), anyInt(), anyString(), anyString(),
            any(), any(), any(), any())).thenReturn(mock(IFileStorage.class));

        controller.reTask("file-1", "task", 1, "node-1", "parent-1", null, null, request);

        verify(fileReleaseTaskService).addTask(eq("file-1"), eq(1), eq("task"), eq(1), eq("node-1"),
            eq("/data/release/app"), any(), any(), any(), eq(request));
    }

    /**
     * 授权目录是另一个目录的前缀时不能误判（/data/release-other 不属于 /data/release）。
     */
    @Test
    public void testAuthorizedDirPrefixNotConfused() {
        assertEquals(false, FileReleaseTaskService.isUnderAuthorizedDir(CollUtil.newArrayList(PARENT), "/data/release-other/app"));
        assertEquals(true, FileReleaseTaskService.isUnderAuthorizedDir(CollUtil.newArrayList(PARENT), "/data/release/app"));
        assertEquals(true, FileReleaseTaskService.isUnderAuthorizedDir(CollUtil.newArrayList(PARENT), PARENT));
        assertEquals(false, FileReleaseTaskService.isUnderAuthorizedDir(CollUtil.newArrayList(PARENT), "/data/release/../tmp"));
    }

    /**
     * 目录名里带点、带空格不受影响。
     */
    @Test
    public void testDottedDirectoryNamesAllowed() {
        assertEquals("/data/release/app.v1.0", FileReleaseTaskService.resolveReleasePath(PARENT, "app.v1.0"));
        assertEquals("/data/release/my app", FileReleaseTaskService.resolveReleasePath(PARENT, "my app"));
    }
}
