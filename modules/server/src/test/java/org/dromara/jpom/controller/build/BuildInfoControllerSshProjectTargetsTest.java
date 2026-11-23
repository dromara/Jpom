/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.build;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * SSH 项目多选发布目标解析：去重、保持选择顺序、忽略多余空白/空值
 *
 * @author bwcx_jzy
 * @since 2026/11/23
 */
public class BuildInfoControllerSshProjectTargetsTest {

    @Test
    public void keepsSelectionOrderAndDedupes() {
        Assertions.assertEquals(Arrays.asList("p2", "p1"),
            new ArrayList<>(BuildInfoController.parseSshProjectTargets("p2,p1,p2")));
    }

    @Test
    public void trimsWhitespaceAndIgnoresEmptyEntries() {
        Assertions.assertEquals(Arrays.asList("p1", "p2"),
            new ArrayList<>(BuildInfoController.parseSshProjectTargets(" p1 ,, p2 ,")));
    }

    @Test
    public void blankValueYieldsEmptySet() {
        Assertions.assertTrue(BuildInfoController.parseSshProjectTargets(null).isEmpty());
        Assertions.assertTrue(BuildInfoController.parseSshProjectTargets("").isEmpty());
        Assertions.assertTrue(BuildInfoController.parseSshProjectTargets("  , ").isEmpty());
    }
}
