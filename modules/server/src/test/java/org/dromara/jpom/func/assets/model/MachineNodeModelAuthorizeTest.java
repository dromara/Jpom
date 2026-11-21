/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.assets.model;

import cn.hutool.crypto.SecureUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * 机器资产授权摘要优先级（反向隧道自动注册上报 authToken 后直接可用）
 *
 * @author jpom
 * @since 2.11.15
 */
public class MachineNodeModelAuthorizeTest {

    @Test
    public void preferTunnelAuthorizeToken() {
        MachineNodeModel model = new MachineNodeModel();
        model.setJpomUsername("jpomAgent");
        model.setJpomPassword("pwd");
        model.setAuthorizeToken("token-from-hello");
        Assert.assertEquals("token-from-hello", model.authorize());
    }

    @Test
    public void fallbackToSha1WhenTokenMissing() {
        MachineNodeModel model = new MachineNodeModel();
        model.setJpomUsername("jpomAgent");
        model.setJpomPassword("pwd");
        Assert.assertEquals(SecureUtil.sha1("jpomAgent@pwd"), model.authorize());
    }
}
