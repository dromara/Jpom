package org.dromara.jpom.func.assets.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 机器资产敏感字段脱敏：authorizeToken 等价插件端通讯口令、jpomPassword 是机器登录口令，
 * 任何下发到浏览器的路径（机器列表、机器信息页、节点列表里的 machineNodeData、下拉搜索）都必须清空。
 */
class MachineNodeSensitiveFieldsTest {

    private MachineNodeModel build() {
        MachineNodeModel model = new MachineNodeModel();
        model.setId("m1");
        model.setName("node-1");
        model.setAuthorizeToken("token-should-not-leak");
        model.setJpomPassword("pwd-should-not-leak");
        return model;
    }

    @Test
    void clearSensitiveNullsBothFields() {
        MachineNodeModel model = build();
        Assertions.assertEquals("token-should-not-leak", model.getAuthorizeToken(), "前置：测试数据必须带凭据，否则用例无效");
        model.clearSensitive();
        Assertions.assertNull(model.getAuthorizeToken(), "authorizeToken 必须被清空");
        Assertions.assertNull(model.getJpomPassword(), "jpomPassword 必须被清空");
    }

    @Test
    void reverseProofWithoutClearSensitiveLeaks() {
        // 反证：不清空时两个字段仍在——证明上面的断言不是恒真
        MachineNodeModel model = build();
        Assertions.assertNotNull(model.getAuthorizeToken());
        Assertions.assertNotNull(model.getJpomPassword());
    }
}
