/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.build;

import org.dromara.jpom.build.FileStorageReleaseTarget;
import org.dromara.jpom.core.release.ReleaseTarget;
import org.dromara.jpom.core.release.ReleaseTargetRegistry;
import org.dromara.jpom.model.enums.BuildReleaseMethod;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 文件中心发布方式测试：码值只追加、注册表可查、老码语义不变
 *
 * @author jpom
 * @since 2.11.16
 */
public class FileStorageReleaseTargetTest {

    @Test
    public void fileStorageIsRegisteredAsNewMethod() {
        Assertions.assertEquals(8, BuildReleaseMethod.FileStorage.getCode(), "文件中心必须是追加码 8");
        ReleaseTarget target = ReleaseTargetRegistry.get(BuildReleaseMethod.FileStorage.getCode());
        Assertions.assertNotNull(target, "文件中心必须注册进发布目标注册表");
        Assertions.assertTrue(target instanceof FileStorageReleaseTarget);
        Assertions.assertEquals(8, target.method());
    }

    @Test
    public void existingMethodsKeepTheirCodes() {
        Assertions.assertEquals(0, BuildReleaseMethod.No.getCode());
        Assertions.assertEquals(1, BuildReleaseMethod.Outgiving.getCode());
        Assertions.assertEquals(2, BuildReleaseMethod.Project.getCode());
        Assertions.assertEquals(3, BuildReleaseMethod.Ssh.getCode());
        Assertions.assertEquals(4, BuildReleaseMethod.LocalCommand.getCode());
        Assertions.assertEquals(5, BuildReleaseMethod.DockerImage.getCode());
        Assertions.assertEquals(6, BuildReleaseMethod.Ftp.getCode());
        Assertions.assertEquals(7, BuildReleaseMethod.SshProject.getCode());
        for (int i = 0; i <= 7; i++) {
            Assertions.assertNotNull(ReleaseTargetRegistry.get(i), "老发布方式 " + i + " 不能丢");
        }
    }
}
