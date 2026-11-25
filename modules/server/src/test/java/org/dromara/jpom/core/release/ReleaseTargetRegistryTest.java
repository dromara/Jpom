/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.core.release;

import org.dromara.jpom.build.ReleaseManage;
import org.dromara.jpom.model.enums.BuildReleaseMethod;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 发布目标注册表测试：装配点完整性（新增发布方式漏注册会被这里拦住）
 *
 * @author jpom
 * @since 2.11.16
 */
public class ReleaseTargetRegistryTest {

    @Test
    public void everyReleaseMethodHasTarget() {
        for (BuildReleaseMethod method : BuildReleaseMethod.values()) {
            ReleaseTarget target = ReleaseTargetRegistry.get(method.getCode());
            Assertions.assertNotNull(target, "发布方式未注册实现：" + method.name() + "(" + method.getCode() + ")");
            Assertions.assertEquals(method.getCode(), target.method(), "实现声明的码值与注册码不一致：" + target.getClass());
        }
    }

    @Test
    public void unknownMethodReturnsNull() {
        Assertions.assertNull(ReleaseTargetRegistry.get(99), "未注册的发布方式必须返回 null，由调用方报「未实现」");
    }

    @Test
    public void duplicateRegistrationIsRejected() {
        ReleaseTargetRegistry.register(new ReleaseTarget() {
            @Override
            public int method() {
                return 100;
            }

            @Override
            public String release(ReleaseManage manage) {
                return null;
            }
        });
        Assertions.assertNotNull(ReleaseTargetRegistry.get(100));
        Assertions.assertThrows(IllegalStateException.class, () -> ReleaseTargetRegistry.register(new ReleaseTarget() {
            @Override
            public int method() {
                return 100;
            }

            @Override
            public String release(ReleaseManage manage) {
                return null;
            }
        }), "重复注册必须启动期暴露，而不是静默覆盖");
    }
}