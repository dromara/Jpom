/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.dblog;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 构建发布目标匹配：SSH 项目多选后 releaseMethodDataId 是逗号分隔列表，
 * 「被构建引用的项目禁止删除」必须以成员精确匹配，不能整体等值、也不能子串命中
 *
 * @author bwcx_jzy
 * @since 2026/11/23
 */
public class BuildInfoReleaseTargetMatcherTest {

    @Test
    public void singleTargetMatches() {
        Assertions.assertTrue(BuildInfoService.containsReleaseTarget("p1", "p1"));
    }

    @Test
    public void multiTargetMatchesAnyMember() {
        Assertions.assertTrue(BuildInfoService.containsReleaseTarget("p1,p2", "p1"));
        Assertions.assertTrue(BuildInfoService.containsReleaseTarget("p1,p2", "p2"));
    }

    @Test
    public void whitespaceAroundSeparatorIsIgnored() {
        Assertions.assertTrue(BuildInfoService.containsReleaseTarget(" p1 , p2 ", "p2"));
    }

    @Test
    public void lookalikeIdIsNotMatched() {
        // 子串匹配（like '%p1%'）会把 p11/pp1 当成同一个目标，删除守卫必须按成员精确比对
        Assertions.assertFalse(BuildInfoService.containsReleaseTarget("p11,pp1", "p1"));
    }

    @Test
    public void blankValuesNeverMatch() {
        Assertions.assertFalse(BuildInfoService.containsReleaseTarget(null, "p1"));
        Assertions.assertFalse(BuildInfoService.containsReleaseTarget("", "p1"));
        Assertions.assertFalse(BuildInfoService.containsReleaseTarget("p1", null));
        Assertions.assertFalse(BuildInfoService.containsReleaseTarget("p1", " "));
        Assertions.assertFalse(BuildInfoService.containsReleaseTarget("p1", ""));
    }
}
