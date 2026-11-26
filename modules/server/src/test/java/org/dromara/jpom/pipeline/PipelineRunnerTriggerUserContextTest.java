/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline;

import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.model.user.UserModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 流水线「触发上下文用户」的行为级回归测试。
 * <p>
 * 背景（2026-11-26 dev 实测缺陷）：流水线开放接口 GET /api/pipeline/{id}/{token} 用纯 token
 * （不带登录会话）调用时返回 code=405、msg=没有对应的用户——触发线程没有请求上下文，
 * PipelineRunner 插入运行记录时 BaseWorkspaceService#fillInsert 要按工作空间校验用户，
 * 直接抛 i18n.no_user.3b69，changelog 承诺的「开放接口可按 token 触发运行」实际不可用。
 * </p>
 * <p>
 * 本测试的断言点在 start() 的调用路径上（不是只测工具方法）：依赖全部为 null 时 start() 必然抛异常，
 * 但「进入 start 后是否先绑定上下文」可以在抛异常前观察到。把 start() 里的绑定去掉，
 * startBindsTriggerUserFromToken / startBindsSystemUserWhenNoUser 立即失败（反证已实测）。
 * </p>
 *
 * @author jpom
 */
public class PipelineRunnerTriggerUserContextTest {

    @BeforeEach
    public void clearContext() {
        BaseServerController.removeAll();
    }

    @AfterEach
    public void clearContextAfter() {
        BaseServerController.removeAll();
    }

    /**
     * 依赖故意为 null：只借 start() 的前半段验证上下文绑定顺序，异常在本用例中被忽略。
     */
    private PipelineRunner runnerWithoutDependencies() {
        return new PipelineRunner(null, null, null, null, null, null, null, null, null);
    }

    private void tryStart(PipelineRunner runner, UserModel user) {
        try {
            runner.start("e2e-pipeline", user, "触发上下文用例", 2);
        } catch (RuntimeException ignored) {
            // 依赖为 null 必然失败；本用例验证的是「抛异常前是否已绑定触发上下文」
        }
    }

    @Test
    public void startBindsTriggerUserFromToken() {
        UserModel userModel = new UserModel("e2e-trigger-user");
        tryStart(this.runnerWithoutDependencies(), userModel);
        UserModel current = BaseServerController.getUserByThreadLocal();
        // 原缺陷：这里是 null；运行记录 fillInsert 随后抛「没有对应的用户」
        assertNotNull(current, "开放接口触发进入 start() 后必须先绑定触发用户，否则写运行记录就会报「没有对应的用户」");
        assertEquals("e2e-trigger-user", current.getId(), "运行记录与审计应落在 token 所属用户，而不是系统身份");
    }

    @Test
    public void startBindsSystemUserWhenNoUser() {
        tryStart(this.runnerWithoutDependencies(), null);
        UserModel current = BaseServerController.getUserByThreadLocal();
        assertNotNull(current, "没有触发用户时必须绑定系统身份，而不是留空");
        assertSame(UserModel.EMPTY, current, "系统触发（无 token 用户）绑定 UserModel.EMPTY");
        assertEquals(UserModel.SYSTEM_ADMIN, current.getId(), "系统身份的 id 必须是 sys（工作空间校验放行）");
    }

    @Test
    public void keepExistingSessionUser() {
        UserModel sessionUser = new UserModel("e2e-session-user");
        BaseServerController.resetInfo(sessionUser);
        // 界面手动运行时会把 null 传下来（没有 token 用户），此时不能把登录用户覆盖成 sys
        PipelineRunner.bindTriggerUser(null);
        UserModel current = BaseServerController.getUserByThreadLocal();
        assertNotNull(current, "已有登录上下文时必须保留");
        assertEquals("e2e-session-user", current.getId(), "手工运行时沿用登录用户，不得改写成系统身份");
    }
}
