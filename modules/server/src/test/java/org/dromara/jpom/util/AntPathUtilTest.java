/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 差异构建路径匹配测试
 *
 * @author bwcx_jzy
 * @since 2026/11/2
 */
public class AntPathUtilTest {

    @Test
    public void testDirectoryRule() {
        Assertions.assertTrue(AntPathUtil.matchAny("/modules/server/src/A.java", "/modules/server"));
        Assertions.assertTrue(AntPathUtil.matchAny("/pom.xml", "/pom.xml"));
        Assertions.assertFalse(AntPathUtil.matchAny("/modules/agent/src/A.java", "/modules/server"));
    }

    @Test
    public void testIncludeAndExclude() {
        Assertions.assertTrue(AntPathUtil.matchDiffPath("/modules/server/src/A.java", "/modules/server", null));
        Assertions.assertFalse(AntPathUtil.matchDiffPath("/README.md", "/modules/server", null));
        Assertions.assertFalse(AntPathUtil.matchDiffPath("/docs/a.md", null, "/docs"));
        Assertions.assertTrue(AntPathUtil.matchDiffPath("/src/a.java", "**/*.java", "/docs"));
        Assertions.assertFalse(AntPathUtil.matchDiffPath("/src/a.md", "**/*.java", null));
    }

    @Test
    public void testChinesePath() {
        Assertions.assertTrue(AntPathUtil.matchDiffPath("/文档/说明.md", "/文档", null));
        Assertions.assertTrue(AntPathUtil.matchDiffPath("/文档/说明.md", null, "/docs"));
    }
}
