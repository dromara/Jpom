/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.enums;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 发布方式枚举的编码是前后端契约（前端 releaseMethodMap 用数字），改动必须同步并保持只增不改。
 *
 * @author bwcx_jzy
 * @since 2026/11/20
 */
public class BuildReleaseMethodSshProjectTest {

    @Test
    public void sshProjectCodeIsStable() {
        Assertions.assertEquals(7, BuildReleaseMethod.SshProject.getCode());
        Assertions.assertEquals("SSH 项目", BuildReleaseMethod.SshProject.getDesc());
        // 既有编码不允许被挤占
        Assertions.assertEquals(3, BuildReleaseMethod.Ssh.getCode());
        Assertions.assertEquals(6, BuildReleaseMethod.Ftp.getCode());
    }
}
