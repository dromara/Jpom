/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.node.ssh;

import org.dromara.jpom.model.data.CommandModel;
import org.dromara.jpom.model.data.SshModel;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.dromara.jpom.TestI18nSupport;

/**
 * 校验 SSH 命令批量执行入口按工作空间校验命令模板与 SSH 节点（issue #120）
 *
 * @author jpom
 */
public class SshCommandServiceExecuteBatchTest {

    /**
     * 使用 CALLS_REAL_METHODS 的 mock 执行真实的 executeBatch 逻辑，并注入 mock 的 SshService
     */
    private SshCommandService createService(SshService sshService) {
        TestI18nSupport.init();
        SshCommandService service = Mockito.mock(SshCommandService.class, Mockito.CALLS_REAL_METHODS);
        try {
            Field field = SshCommandService.class.getDeclaredField("sshService");
            field.setAccessible(true);
            field.set(service, sshService);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return service;
    }

    @Test
    public void testRejectForeignCommandId() {
        SshService sshService = mock(SshService.class);
        SshCommandService service = this.createService(sshService);
        HttpServletRequest request = new MockHttpServletRequest();
        doReturn(null).when(service).getByKey("foreign-command", request);
        assertThrows(IllegalArgumentException.class, () -> service.executeBatch("foreign-command", "", "ssh-1", request));
        verify(service).getByKey("foreign-command", request);
        // 不允许回退到无工作空间条件的 getByKey(id)
        verify(service, never()).getByKey("foreign-command");
    }

    @Test
    public void testRejectForeignSshNode() {
        SshService sshService = mock(SshService.class);
        SshCommandService service = this.createService(sshService);
        HttpServletRequest request = new MockHttpServletRequest();
        CommandModel commandModel = new CommandModel();
        doReturn(commandModel).when(service).getByKey("command-1", request);
        doReturn(null).when(sshService).getByKey("foreign-ssh", request);
        assertThrows(IllegalArgumentException.class, () -> service.executeBatch("command-1", "", "foreign-ssh", request));
        verify(sshService).getByKey("foreign-ssh", request);
    }

    @Test
    public void testAcceptCommandAndNodesInWorkspace() {
        SshService sshService = mock(SshService.class);
        SshCommandService service = this.createService(sshService);
        HttpServletRequest request = new MockHttpServletRequest();
        CommandModel commandModel = new CommandModel();
        doReturn(commandModel).when(service).getByKey("command-1", request);
        doReturn(new SshModel()).when(sshService).getByKey("ssh-1", request);
        doReturn("batch-ok").when(service).executeBatch(commandModel, "", "ssh-1", 0);
        assertEquals("batch-ok", service.executeBatch("command-1", "", "ssh-1", request));
    }
}
