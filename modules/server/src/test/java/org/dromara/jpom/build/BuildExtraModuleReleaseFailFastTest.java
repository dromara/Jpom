/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.build;

import org.dromara.jpom.util.StringUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 多目标发布失败策略：老配置缺字段时按「任一目标失败即终止」处理
 *
 * @author bwcx_jzy
 * @since 2026/11/23
 */
public class BuildExtraModuleReleaseFailFastTest {

    @Test
    public void defaultIsFailFast() {
        BuildExtraModule module = new BuildExtraModule();
        Assertions.assertTrue(module.failFastOnRelease());
    }

    @Test
    public void explicitFalseKeepsPublishingRemainingTargets() {
        BuildExtraModule module = new BuildExtraModule();
        module.setReleaseFailFast(false);
        Assertions.assertFalse(module.failFastOnRelease());
    }

    @Test
    public void explicitTrueIsFailFast() {
        BuildExtraModule module = new BuildExtraModule();
        module.setReleaseFailFast(true);
        Assertions.assertTrue(module.failFastOnRelease());
    }

    @Test
    public void legacyJsonWithoutFieldIsFailFast() {
        // 线上老配置（2.11.14.5 及更早）的 extraData 没有 releaseFailFast 字段
        BuildExtraModule module = StringUtil.jsonConvert("{\"releaseMethod\":7,\"releaseMethodDataId\":\"p1,p2\"}",
            BuildExtraModule.class);
        Assertions.assertNotNull(module);
        Assertions.assertTrue(module.failFastOnRelease());
    }

    @Test
    public void jsonExplicitFalseKeepsContinue() {
        BuildExtraModule module = StringUtil.jsonConvert("{\"releaseFailFast\":false}", BuildExtraModule.class);
        Assertions.assertNotNull(module);
        Assertions.assertFalse(module.failFastOnRelease());
    }
}
