/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.build.version;

import org.junit.Assert;
import org.junit.Test;

/**
 * 版本快照保留个数归一：0 / 空必须回落到默认值
 * <p>
 * 背景（2026-11-19 群反馈「构建版本回滚没有记录」）：构建配置打开「版本快照」但保留个数留空（默认 0）时，
 * 服务端把 0 透传给插件端，插件端按「未启用」处理，于是发布成功却一条版本记录都不写，
 * 快速回滚入口点进去永远没有记录。本用例锁住「0 不能透传」这条契约。
 *
 * @author jpom
 * @since 2026/11/19
 */
public class BuildVersionServiceKeepCountTest {

    @Test
    public void emptyOrZeroKeepCountFallsBackToDefault() {
        Assert.assertEquals(3, BuildVersionService.DEFAULT_VERSION_KEEP_COUNT);
        Assert.assertEquals(BuildVersionService.DEFAULT_VERSION_KEEP_COUNT, BuildVersionService.resolveKeepCount(null));
        Assert.assertEquals("保留个数为 0 时不能原样透传，否则插件端判定为未启用", BuildVersionService.DEFAULT_VERSION_KEEP_COUNT,
            BuildVersionService.resolveKeepCount(0));
        Assert.assertEquals(BuildVersionService.DEFAULT_VERSION_KEEP_COUNT, BuildVersionService.resolveKeepCount(-3));
    }

    @Test
    public void positiveKeepCountKept() {
        Assert.assertEquals(1, BuildVersionService.resolveKeepCount(1));
        Assert.assertEquals(5, BuildVersionService.resolveKeepCount(5));
    }
}
