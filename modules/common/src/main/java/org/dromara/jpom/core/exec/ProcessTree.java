/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.core.exec;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 进程树终止（共享内核）
 * <p>
 * 背景：历史实现只对 {@link Process#destroy()} 直接子进程下手，构建脚本 {@code sh build.sh} 下启动的
 * {@code mvn}/{@code java} 是孙进程，父 shell 退出后会被 init 收养继续运行，界面却已经显示「已取消」。
 * 这里把「整棵进程树必须消失」做成可复用、可测试的能力：
 * <ul>
 *     <li>Unix：优先按进程组终止（{@code kill -TERM -pgid} → 宽限 → {@code kill -KILL -pgid}），
 *     pgid 与 pid 不同（未建成进程组）时退化为「快照子孙进程再逐个终止」；</li>
 *     <li>Windows：{@code taskkill /PID x /T /F}；</li>
 *     <li>全程有界（宽限期内 100ms 轮询，最多一次补刀），不再出现历史上的 {@code while(true)} 自旋；
 *     返回结果明确告诉调用方「是否确认全部退出 / 还有哪些 pid 残留」。</li>
 * </ul>
 * <p>
 * 与 Go 插件端语义对齐：{@code go-agent/internal/builder/job.go} 的 Cancel 同样是 SIGTERM → 5s → SIGKILL，
 * 并在补刀前确认进程组仍在（避免 pgid 复用误杀）。
 *
 * @author jpom
 * @since 2.11.16
 */
@Slf4j
public final class ProcessTree {

    /**
     * 默认宽限时间：SIGTERM 之后等待进程自行退出的时间
     */
    public static final long DEFAULT_GRACE_MILLIS = 5000L;

    /**
     * 轮询步长
     */
    private static final long POLL_STEP_MILLIS = 100L;

    /**
     * 关闭开关（出现平台异常时可由运维显式关闭进程组能力，退回逐 pid 终止）
     */
    private static final String DISABLE_KEY = "jpom.process.tree.disabled";

    private static final boolean SETSID_AVAILABLE = ProcessProbe.UNIX && ProcessProbe.commandExists("setsid");

    private ProcessTree() {
    }

    /**
     * 是否支持把新进程放进独立进程组（Unix 且存在 setsid）
     *
     * @return true 支持
     */
    public static boolean supportsProcessGroup() {
        return SETSID_AVAILABLE && !Boolean.getBoolean(DISABLE_KEY);
    }

    /**
     * 给命令加上「新建进程组」前缀，便于后续按进程组整体终止
     * <p>
     * 仅 Unix + 存在 setsid 时生效；不加前缀时仍可通过递归枚举子孙进程终止（降级路径）。
     *
     * @param command 原始命令
     * @return 处理后的命令（不修改入参列表）
     */
    public static List<String> wrapNewProcessGroup(List<String> command) {
        if (CollUtil.isEmpty(command) || !supportsProcessGroup()) {
            return command;
        }
        if ("setsid".equals(FileUtil.getName(command.get(0)))) {
            return command;
        }
        List<String> wrapped = new ArrayList<>(command.size() + 1);
        wrapped.add("setsid");
        wrapped.addAll(command);
        return wrapped;
    }

    /**
     * 终止进程及其全部子孙进程
     *
     * @param process     进程（可为 null）
     * @param graceMillis 宽限时间（SIGTERM 之后等待多久再 KILL）
     * @return 终止结果（是否确认全部退出 + 残留 pid）
     */
    public static ProcessKillResult kill(Process process, long graceMillis) {
        if (process == null) {
            return new ProcessKillResult(true, Collections.emptyList(), false);
        }
        if (Boolean.getBoolean(DISABLE_KEY)) {
            return destroyOnly(process);
        }
        long pid = pidOf(process);
        if (pid <= 0) {
            return destroyOnly(process);
        }
        try {
            if (!ProcessProbe.UNIX) {
                return killWindows(process, pid);
            }
            return killUnix(process, pid, graceMillis <= 0 ? DEFAULT_GRACE_MILLIS : graceMillis);
        } catch (Exception e) {
            log.warn(I18nMessageUtil.get("i18n.process_tree_kill_failed.8a1d"), pid, e.getMessage());
            return destroyOnly(process);
        }
    }

    /**
     * 便捷入口：使用默认宽限时间
     *
     * @param process 进程
     * @return 终止结果
     */
    public static ProcessKillResult kill(Process process) {
        return kill(process, DEFAULT_GRACE_MILLIS);
    }

    // ------------------------------------------------------------------ 兼容入口（探针委托）

    /**
     * 取进程 pid（委托 {@link ProcessProbe}）
     *
     * @param process 进程
     * @return pid；取不到返回 -1
     */
    public static long pidOf(Process process) {
        return ProcessProbe.pidOf(process);
    }

    /**
     * 递归收集子孙进程 pid（委托 {@link ProcessProbe}）
     *
     * @param pid 父进程
     * @return 子孙 pid 列表（不含自身）
     */
    public static List<Long> descendants(long pid) {
        return ProcessProbe.descendants(pid);
    }

    /**
     * 判断 pid 是否存活（委托 {@link ProcessProbe}）
     *
     * @param pid pid
     * @return true 存活
     */
    public static boolean isAlive(long pid) {
        return ProcessProbe.isAlive(pid);
    }

    // ------------------------------------------------------------------ 终止策略

    private static ProcessKillResult killWindows(Process process, long pid) throws IOException, InterruptedException {
        Process killer = new ProcessBuilder("taskkill", "/PID", String.valueOf(pid), "/T", "/F")
            .redirectErrorStream(true)
            .start();
        if (!killer.waitFor(10, TimeUnit.SECONDS)) {
            killer.destroyForcibly();
        }
        process.destroyForcibly();
        boolean alive = ProcessProbe.isAlive(pid) || process.isAlive();
        return new ProcessKillResult(!alive, alive ? Collections.singletonList(pid) : Collections.emptyList(), true);
    }

    private static ProcessKillResult killUnix(Process process, long pid, long graceMillis) {
        // 先快照：父进程退出后子孙会被 init 收养，ppid 关系消失，必须提前收集
        Set<Long> tree = new LinkedHashSet<>();
        tree.add(pid);
        tree.addAll(ProcessProbe.descendants(pid));
        // 只按「快照到的 pid」逐个发信号，**不再按进程组发信号**。
        // 原因：实测（dev 2026-11-24）按组 kill 会把服务端 JVM 自己一起 SIGKILL 掉——
        // 脚本未必真的独立成组，pgid 判定一旦失准就是自杀。宁可少杀（漏网的 escaped 进程
        // 由「重启恢复 + 残留 pid 提示」兜底），也不能杀自己。
        for (Long child : tree) {
            if (child != pid) {
                ProcessProbe.signalPid(child, "TERM");
            }
        }
        process.destroy();
        boolean exited = awaitExit(tree, process, null, graceMillis);
        if (!exited) {
            for (Long child : tree) {
                ProcessProbe.signalPid(child, "KILL");
            }
            process.destroyForcibly();
            // 补刀后再给一次确认窗口（不再循环自旋）
            awaitExit(tree, process, null, 1000L);
        }
        // 补刀后再给 2s 容差：被杀的进程可能短暂处于「已死未回收」状态，/proc 还在，
        // 立刻判定失败会让上层把「已取消」误记成「异常关闭」。
        List<Long> remaining = new ArrayList<>();
        long deadline = System.currentTimeMillis() + 2000L;
        while (true) {
            remaining.clear();
            for (Long candidate : tree) {
                if (ProcessProbe.isAlive(candidate)) {
                    remaining.add(candidate);
                }
            }
            if (remaining.isEmpty() || System.currentTimeMillis() >= deadline) {
                break;
            }
            try {
                Thread.sleep(POLL_STEP_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return new ProcessKillResult(remaining.isEmpty(), remaining, false);
    }

    /**
     * 等待进程树退出
     *
     * @param tree         快照的进程树 pid
     * @param process      直接子进程
     * @param leaderPgid   进程组组长时的 pgid（非组长传 null）
     * @param timeoutMillis 等待上限
     * @return true 表示全部退出
     */
    private static boolean awaitExit(Set<Long> tree, Process process, Long leaderPgid, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + Math.max(timeoutMillis, POLL_STEP_MILLIS);
        while (System.currentTimeMillis() < deadline) {
            // 只有「进程组组长」才按进程组判断；否则按快照 pid 逐个确认，
            // 绝不能拿「JVM 所在进程组」当目标（那是别人，永远活着，会一直等到超时）。
            boolean allGone = leaderPgid != null ? !ProcessProbe.signalGroup(leaderPgid, "0") : !process.isAlive();
            if (allGone && noneAlive(tree)) {
                return true;
            }
            try {
                Thread.sleep(POLL_STEP_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private static boolean noneAlive(Set<Long> tree) {
        for (Long pid : tree) {
            if (ProcessProbe.isAlive(pid)) {
                return false;
            }
        }
        return true;
    }

    private static ProcessKillResult destroyOnly(Process process) {
        long pid = pidOf(process);
        process.destroy();
        if (process.isAlive()) {
            process.destroyForcibly();
            try {
                process.waitFor(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        boolean alive = process.isAlive();
        return new ProcessKillResult(!alive, alive && pid > 0 ? Collections.singletonList(pid) : Collections.emptyList(), false);
    }
}