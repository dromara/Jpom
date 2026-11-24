/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.ftp;

import org.dromara.jpom.TestI18nSupport;
import org.dromara.jpom.func.assets.server.MachineFtpServer;
import org.dromara.jpom.model.enums.BuildReleaseMethod;
import org.dromara.jpom.service.dblog.BuildInfoService;
import org.dromara.jpom.service.node.ftp.FtpService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FTP 资产删除守卫：被构建引用时必须拦住，且守卫要查「FTP」发布方式而不是复制粘贴来的 SSH
 *
 * @author bwcx_jzy
 * @since 2026/11/23
 */
public class FtpControllerDeleteGuardTest {

    @BeforeAll
    public static void initI18n() {
        TestI18nSupport.init();
    }

    private FtpController controller(BuildInfoService buildInfoService, FtpService ftpService) {
        return new FtpController(ftpService, mock(MachineFtpServer.class), buildInfoService);
    }

    @Test
    public void deleteBlockedWhenBuildReferencesFtp() {
        BuildInfoService buildInfoService = mock(BuildInfoService.class);
        FtpService ftpService = mock(FtpService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(buildInfoService.checkReleaseMethodTarget("f1", request, BuildReleaseMethod.Ftp)).thenReturn(true);
        FtpController controller = this.controller(buildInfoService, ftpService);
        // Spring 的 Assert.state 抛 IllegalStateException
        Assertions.assertThrows(IllegalStateException.class, () -> controller.del("f1", request));
        verify(ftpService, never()).delByKey(anyString(), any(HttpServletRequest.class));
        // 改回 BuildReleaseMethod.Ssh 时这条 verify 会失败（FTP 引用再也不会命中）
        verify(buildInfoService).checkReleaseMethodTarget("f1", request, BuildReleaseMethod.Ftp);
    }

    @Test
    public void deleteAllowedWhenNoBuildReferences() {
        BuildInfoService buildInfoService = mock(BuildInfoService.class);
        FtpService ftpService = mock(FtpService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(buildInfoService.checkReleaseMethodTarget("f1", request, BuildReleaseMethod.Ftp)).thenReturn(false);
        FtpController controller = this.controller(buildInfoService, ftpService);
        controller.del("f1", request);
        verify(ftpService).delByKey("f1", request);
    }
}
