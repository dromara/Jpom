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

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 运行中的构建/发布执行体登记表（取消能力的唯一入口）
 * <p>
 * 历史上的取消只查 {@code BuildExecuteManage.BUILD_MANAGE_MAP}：排队中、重发/回滚中的发布、
 * 服务重启后残留的任务都查不到，控制器却照样把状态写成「已取消」——于是界面取消成功、进程还在跑。
 * 这里把「谁在跑、跑在哪个阶段、占着哪些进程、能不能从线程池队列里摘掉」统一登记：
 * <ul>
 *     <li>取消 = 先登记取消意图（幂等），再执行 {@link CancelHook}，最后按 {@link ProcessTree} 的结果落状态；</li>
 *     <li>队列中的任务通过 {@link ThreadPoolExecutor#remove(Runnable)} 摘除，避免「已取消又跑起来」；</li>
 *     <li>执行体找不到时返回 {@link CancelOutcome#NOT_FOUND}，调用方不得再写「已取消」。</li>
 * </ul>
 *
 * @author jpom
 * @since 2.11.16
 */
@Slf4j
public final class ExecutionRegistry {

    /**
     * 取消动作
     */
    @FunctionalInterface
    public interface CancelHook {
        /**
         * 执行取消（终止进程、通知节点等）
         *
         * @param reason 取消原因
         * @return true 表示确认已停止（进程树已退出 / 节点已确认）
         */
        boolean cancel(String reason);
    }

    /**
     * 取消请求结果
     */
    public enum CancelOutcome {
        /**
         * 本次第一次受理
         */
        REQUESTED,
        /**
         * 之前已受理（幂等）
         */
        ALREADY_REQUESTED,
        /**
         * 没有找到执行体：调用方必须据此走「异常关闭」而不是「已取消」
         */
        NOT_FOUND
    }

    /**
     * 执行体句柄
     */
    public static final class Handle {
        private final String key;
        private final String name;
        private final CancelHook hook;
        private volatile String stage = StrUtil.EMPTY;
        private volatile Process process;
        private volatile boolean cancelRequested;
        private volatile boolean stopped;
        private volatile Runnable queuedTask;
        private volatile ThreadPoolExecutor queuedPool;

        Handle(String key, String name, CancelHook hook) {
            this.key = key;
            this.name = name;
            this.hook = hook;
        }

        public String getKey() {
            return key;
        }

        public String getName() {
            return name;
        }

        public String getStage() {
            return stage;
        }

        public boolean isCancelRequested() {
            return cancelRequested;
        }

        public boolean isStopped() {
            return stopped;
        }

        /**
         * 记录当前阶段（用于取消失败时告诉用户卡在哪）
         *
         * @param stage 阶段名
         */
        public void stage(String stage) {
            this.stage = StrUtil.nullToEmpty(stage);
        }

        /**
         * 登记当前正在执行的进程（发布命令等也需要登记，否则「发布中」取消不到）
         *
         * @param process 进程
         */
        public void attachProcess(Process process) {
            this.process = process;
        }

        /**
         * 解除进程登记
         */
        public void detachProcess() {
            this.process = null;
        }

        /**
         * @return 当前登记的进程（可能为 null）
         */
        public Process getProcess() {
            return process;
        }

        /**
         * 登记排队中的任务（用于从线程池队列摘除）
         *
         * @param pool 线程池
         * @param task 任务
         */
        public void queued(ThreadPoolExecutor pool, Runnable task) {
            this.queuedPool = pool;
            this.queuedTask = task;
        }

        /**
         * 标记执行体已停止（收尾时调用，避免把已结束的当成在跑）
         */
        public void stop() {
            this.stopped = true;
        }
    }

    private static final Map<String, Handle> HANDLES = new ConcurrentHashMap<>();

    private ExecutionRegistry() {
    }

    /**
     * 登记执行体（同一 key 重复登记时后写覆盖）
     *
     * @param key  构建 id / 运行 id
     * @param name 展示名
     * @param hook 取消动作
     * @return 句柄
     */
    public static Handle register(String key, String name, CancelHook hook) {
        Handle handle = new Handle(key, name, hook);
        HANDLES.put(key, handle);
        return handle;
    }

    /**
     * 移除登记
     *
     * @param key key
     */
    public static void unregister(String key) {
        if (StrUtil.isNotEmpty(key)) {
            HANDLES.remove(key);
        }
    }

    /**
     * 取句柄
     *
     * @param key key
     * @return 句柄（不存在为空）
     */
    public static Optional<Handle> get(String key) {
        return Optional.ofNullable(HANDLES.get(key));
    }

    /**
     * @param key key
     * @return 是否已登记取消意图
     */
    public static boolean isCancelRequested(String key) {
        return get(key).map(Handle::isCancelRequested).orElse(false);
    }

    /**
     * 请求取消（幂等）
     * <p>
     * 顺序很关键：先置「取消意图」（执行线程据此在阶段边界/起跑前退出），再从队列摘除或执行取消动作，
     * 最后按实际结果由调用方决定落「已取消」还是「异常关闭」。
     * </p>
     *
     * @param key    key
     * @param reason 取消原因
     * @return 取消结果；{@link CancelOutcome#NOT_FOUND} 表示没有执行体
     */
    public static CancelOutcome requestCancel(String key, String reason) {
        Handle handle = HANDLES.get(key);
        if (handle == null) {
            return CancelOutcome.NOT_FOUND;
        }
        if (handle.cancelRequested) {
            return CancelOutcome.ALREADY_REQUESTED;
        }
        handle.cancelRequested = true;
        // 排队中的任务：直接从线程池队列摘除，避免「已取消又跑起来」
        Runnable queuedTask = handle.queuedTask;
        ThreadPoolExecutor pool = handle.queuedPool;
        if (queuedTask != null && pool != null && pool.remove(queuedTask)) {
            handle.stopped = true;
            log.info("构建任务在队列中被摘除（未起跑）：key={}", key);
        }
        try {
            handle.hook.cancel(StrUtil.emptyToDefault(reason, "manual cancel"));
        } catch (Exception e) {
            log.warn("执行取消动作失败：key={}", key, e);
        }
        return CancelOutcome.REQUESTED;
    }

    /**
     * 当前登记数量（诊断/测试用）
     *
     * @return 数量
     */
    public static int size() {
        return HANDLES.size();
    }

    /**
     * 清空登记（仅测试用）
     */
    static void clearForTest() {
        HANDLES.clear();
    }
}
