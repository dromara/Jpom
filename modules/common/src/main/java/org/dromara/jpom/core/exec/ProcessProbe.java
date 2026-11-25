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

import cn.hutool.core.util.StrUtil;
import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 进程探针：pid 获取、子孙进程枚举、存活判断、信号发送（JDK 8 实现）
 * <p>
 * 与 {@link ProcessTree} 的分工：本类只做「读」与「发信号」，不决定终止策略——
 * 策略（进程组还是逐 pid、宽限期多久、何时补刀）全部在 {@link ProcessTree}，
 * 这样策略可测、探针可在不同平台替换。
 * <p>
 * 平台差异：
 * <ul>
 *     <li>pid：JDK 8 的 {@code java.lang.UNIXProcess} 反射字段 {@code pid}，Windows 为 {@code handle}；</li>
 *     <li>子孙枚举：Linux 读 {@code /proc/<pid>/task/<tid>/children}（procfs 文件 size 恒为 0，
 *     必须按字节读，不能用按长度判断的封装），其它 Unix 回退 {@code ps -eo pid=,ppid=}；</li>
 *     <li>存活：Linux 直接看 {@code /proc/<pid>} 是否存在，其它平台用 {@code kill -0}。</li>
 * </ul>
 *
 * @author jpom
 * @since 2.11.16
 */
@Slf4j
final class ProcessProbe {

    private static final OsInfo OS_INFO = SystemUtil.getOsInfo();
    static final boolean UNIX = !OS_INFO.isWindows();
    static final boolean LINUX = OS_INFO.isLinux();

    private ProcessProbe() {
    }

    /**
     * 取进程 pid
     *
     * @param process 进程
     * @return pid；取不到返回 -1
     */
    static long pidOf(Process process) {
        if (process == null) {
            return -1;
        }
        Object value = readField(process, "pid");
        if (value == null) {
            value = readField(process, "handle");
        }
        return value instanceof Number ? ((Number) value).longValue() : -1;
    }

    private static Object readField(Process process, String name) {
        try {
            Field field = process.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(process);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 递归收集子孙进程 pid
     *
     * @param pid 父进程
     * @return 子孙 pid 列表（不含自身）
     */
    static List<Long> descendants(long pid) {
        Set<Long> result = new LinkedHashSet<>();
        collect(pid, result);
        result.remove(pid);
        return new ArrayList<>(result);
    }

    private static void collect(long pid, Set<Long> result) {
        for (Long child : childrenOf(pid)) {
            if (result.add(child)) {
                collect(child, result);
            }
        }
    }

    private static List<Long> childrenOf(long pid) {
        if (LINUX) {
            List<Long> children = childrenFromProc(pid);
            if (children != null) {
                return children;
            }
        }
        return childrenFromPs(pid);
    }

    /**
     * Linux：读 /proc/&lt;pid&gt;/task/&lt;tid&gt;/children
     *
     * @return 子进程列表；目录不存在返回 null（调用方回退 ps）
     */
    private static List<Long> childrenFromProc(long pid) {
        File[] tasks = new File("/proc/" + pid + "/task").listFiles();
        if (tasks == null) {
            return null;
        }
        Set<Long> children = new LinkedHashSet<>();
        for (File task : tasks) {
            File childrenFile = new File(task, "children");
            if (!childrenFile.isFile()) {
                continue;
            }
            try {
                // procfs 文件 size 恒为 0，必须按字节读
                String text = new String(Files.readAllBytes(childrenFile.toPath()), StandardCharsets.UTF_8).trim();
                if (StrUtil.isEmpty(text)) {
                    continue;
                }
                for (String item : StrUtil.splitTrim(text, ' ')) {
                    try {
                        children.add(Long.parseLong(item));
                    } catch (NumberFormatException ignored) {
                        // 忽略异常行
                    }
                }
            } catch (Exception ignored) {
                // 进程刚退出时文件可能消失
            }
        }
        return new ArrayList<>(children);
    }

    /**
     * 通用 Unix 回退：解析 ps 的 pid/ppid 关系
     */
    private static List<Long> childrenFromPs(long pid) {
        List<Long> result = new ArrayList<>();
        for (String line : readCommand("ps", "-eo", "pid=,ppid=")) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length < 2) {
                continue;
            }
            try {
                if (Long.parseLong(parts[1]) == pid) {
                    result.add(Long.parseLong(parts[0]));
                }
            } catch (NumberFormatException ignored) {
                // 跳过表头/异常行
            }
        }
        return result;
    }

    /**
     * 取进程组 id
     *
     * @param pid 进程
     * @return pgid；取不到返回 null
     */
    static Long processGroupId(long pid) {
        return readCommand("ps", "-o", "pgid=", "-p", String.valueOf(pid)).stream()
            .map(String::trim).filter(StrUtil::isNotEmpty).findFirst()
            .map(value -> {
                try {
                    return Long.parseLong(value);
                } catch (NumberFormatException e) {
                    return null;
                }
            }).orElse(null);
    }

    /**
     * 取当前 JVM 所在进程组 id（用于安全护栏：绝不能对「自己所在的进程组」发信号）
     *
     * @return pgid；取不到返回 null
     */
    static Long selfGroupId() {
        try {
            String name = ManagementFactory.getRuntimeMXBean().getName();
            String pid = StrUtil.subBefore(name, "@", false);
            return processGroupId(Long.parseLong(pid));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 判断 pid 是否存活
     *
     * @param pid pid
     * @return true 存活
     */
    static boolean isAlive(long pid) {
        if (pid <= 0) {
            return false;
        }
        if (LINUX) {
            return new File("/proc/" + pid).exists();
        }
        if (!UNIX) {
            return false;
        }
        return exitCode("kill", "-0", String.valueOf(pid)) == 0;
    }

    /**
     * 向整个进程组发信号
     *
     * @param pgid   进程组
     * @param signal 信号（TERM/KILL/0）
     * @return true 表示命令执行成功（信号 0 时代表组仍存活）
     */
    static boolean signalGroup(long pgid, String signal) {
        long self = selfPid();
        if (pgid <= 0 || pgid == self) {
            log.warn("拒绝向进程组 {} 发送 {}（非法目标或自身）", pgid, signal);
            return false;
        }
        boolean ok = exitCode("kill", "-" + signal, "-" + pgid) == 0;
        log.info("signalGroup pgid={} signal={} ok={} selfPid={}", pgid, signal, ok, self);
        return ok;
    }

    /**
     * 当前 JVM 的 pid
     *
     * @return pid；取不到返回 -1
     */
    static long selfPid() {
        try {
            String name = ManagementFactory.getRuntimeMXBean().getName();
            return Long.parseLong(StrUtil.subBefore(name, "@", false));
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 向单个进程发信号
     *
     * @param pid    pid
     * @param signal 信号
     * @return true 表示命令执行成功
     */
    static boolean signalPid(long pid, String signal) {
        long self = selfPid();
        if (pid <= 1 || pid == self) {
            log.warn("拒绝向 pid {} 发送 {}（非法目标或自身）", pid, signal);
            return false;
        }
        boolean ok = exitCode("kill", "-" + signal, String.valueOf(pid)) == 0;
        log.info("signalPid pid={} signal={} ok={} selfPid={}", pid, signal, ok, self);
        return ok;
    }

    private static int exitCode(String... command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return -1;
            }
            return process.exitValue();
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static List<String> readCommand(String... command) {
        List<String> lines = new ArrayList<>();
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            StringBuilder builder = new StringBuilder();
            try (InputStream inputStream = process.getInputStream()) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    builder.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
                }
            }
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return lines;
            }
            for (String line : builder.toString().split("\\R")) {
                if (StrUtil.isNotBlank(line)) {
                    lines.add(line);
                }
            }
        } catch (IOException | InterruptedException ignored) {
            if (ignored instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return lines;
    }

    /**
     * 是否存在指定系统命令
     *
     * @param command 命令名
     * @return true 存在
     */
    static boolean commandExists(String command) {
        return exitCode("sh", "-c", "command -v " + command) == 0;
    }
}