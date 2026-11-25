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

import org.dromara.jpom.model.enums.BuildStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 执行体登记与取消策略测试
 * <p>
 * 覆盖历史 bug 的两个关键面：①没有执行体时不能再写「已取消」；②排队中的任务取消后必须从队列摘除、
 * 不能「已取消又跑起来」；③只有确认进程退出才落 {@link BuildStatus#Cancel}。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
public class ExecutionRegistryTest {

    @AfterEach
    public void cleanup() {
        ExecutionRegistry.clearForTest();
    }

    @Test
    public void requestCancelWithoutHandleIsNotFound() {
        Assertions.assertEquals(ExecutionRegistry.CancelOutcome.NOT_FOUND,
            ExecutionRegistry.requestCancel("missing-1", "test"));
    }

    @Test
    public void requestCancelIsIdempotentAndRunsHookOnce() {
        AtomicInteger hookCount = new AtomicInteger();
        ExecutionRegistry.register("b1", "demo", reason -> {
            hookCount.incrementAndGet();
            return true;
        });
        Assertions.assertEquals(ExecutionRegistry.CancelOutcome.REQUESTED, ExecutionRegistry.requestCancel("b1", "test"));
        Assertions.assertEquals(ExecutionRegistry.CancelOutcome.ALREADY_REQUESTED, ExecutionRegistry.requestCancel("b1", "test"));
        Assertions.assertEquals(1, hookCount.get());
        Assertions.assertTrue(ExecutionRegistry.isCancelRequested("b1"));
    }

    @Test
    public void queuedTaskIsRemovedFromPoolAndNeverRuns() throws Exception {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        CountDownLatch blocker = new CountDownLatch(1);
        try {
            pool.execute(() -> {
                try {
                    blocker.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            });
            AtomicBoolean ran = new AtomicBoolean(false);
            AtomicBoolean hookCalled = new AtomicBoolean(false);
            Runnable queuedTask = () -> ran.set(true);
            ExecutionRegistry.Handle handle = ExecutionRegistry.register("b2", "demo", reason -> {
                hookCalled.set(true);
                return true;
            });
            handle.queued(pool, queuedTask);
            pool.execute(queuedTask);

            Assertions.assertEquals(ExecutionRegistry.CancelOutcome.REQUESTED, ExecutionRegistry.requestCancel("b2", "test"));

            blocker.countDown();
            pool.shutdown();
            Assertions.assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
            Assertions.assertFalse(ran.get(), "排队中被取消的任务不应再起跑");
            Assertions.assertTrue(hookCalled.get(), "取消动作仍应执行（用于杀进程/通知节点）");
        } finally {
            blocker.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    public void unregisterRemovesHandleAndStageIsTracked() {
        ExecutionRegistry.Handle handle = ExecutionRegistry.register("b3", "demo", reason -> true);
        handle.stage("release");
        handle.attachProcess(null);
        handle.detachProcess();
        Assertions.assertEquals("release", ExecutionRegistry.get("b3").map(ExecutionRegistry.Handle::getStage).orElse(null));
        ExecutionRegistry.unregister("b3");
        Assertions.assertFalse(ExecutionRegistry.get("b3").isPresent());
        Assertions.assertEquals(ExecutionRegistry.CancelOutcome.NOT_FOUND, ExecutionRegistry.requestCancel("b3", "test"));
    }

    @Test
    public void cancelStatusOnlyWhenStopConfirmed() {
        Assertions.assertEquals(BuildStatus.Cancel, CancelOutcomePolicy.resolve(true));
        Assertions.assertEquals(BuildStatus.AbnormalShutdown, CancelOutcomePolicy.resolve(false));
        Assertions.assertEquals(BuildStatus.CancelRequested, CancelOutcomePolicy.pending());
        Assertions.assertTrue(BuildStatus.CancelRequested.isProgress(), "取消中必须算进行中，否则会被重复启动");
    }
}
