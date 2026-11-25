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

import cn.hutool.core.io.FileUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 进程树终止行为测试（取消构建「界面已取消、进程还在跑」的守卫）
 * <p>
 * 这里的用例是行为级反证：如果 {@link ProcessTree#kill(Process)} 退回历史上的
 * 「只 destroy 直接子进程」实现，{@code killsGrandchildProcesses} 会失败——构建脚本下的
 * {@code mvn}/{@code java} 属于孙进程，父 shell 退出后仍然存活。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
public class ProcessTreeTest {

    @Test
    public void killsGrandchildProcesses() throws Exception {
        File dir = FileUtil.mkdir(FileUtil.getTmpDirPath() + "/jpom-process-tree-test");
        File pidFile = new File(dir, "grandchild.pid");
        FileUtil.del(pidFile);
        // 外层 sh 起一个内层 sh（孙进程），内层写入自己的 pid 后长睡；外层等待
        Process process = new ProcessBuilder("sh", "-c",
            "sh -c 'echo $$ > " + pidFile.getAbsolutePath() + "; sleep 300' & wait")
            .redirectErrorStream(true)
            .start();
        long grandchild = awaitPid(pidFile, 5000);
        Assertions.assertTrue(grandchild > 0, "孙进程未启动");
        Assertions.assertTrue(ProcessTree.isAlive(grandchild), "孙进程应处于存活状态");

        ProcessKillResult result = ProcessTree.kill(process, 3000L);

        Assertions.assertTrue(result.isSuccess(), "进程树应全部退出：" + result);
        Assertions.assertFalse(process.isAlive(), "直接子进程仍存活");
        Assertions.assertFalse(ProcessTree.isAlive(grandchild), "孙进程仍存活（历史 bug 复现）");
    }

    @Test
    public void killIsIdempotentForExitedProcess() throws Exception {
        Process process = new ProcessBuilder("sh", "-c", "exit 0").start();
        Assertions.assertTrue(process.waitFor(10, TimeUnit.SECONDS));
        long start = System.currentTimeMillis();
        ProcessKillResult result = ProcessTree.kill(process, 2000L);
        long cost = System.currentTimeMillis() - start;
        Assertions.assertTrue(result.isSuccess(), "已退出进程应直接判定成功：" + result);
        Assertions.assertTrue(cost < 3000, "已退出进程不应触发长时间等待，实际耗时 " + cost + "ms");
    }

    @Test
    public void killIgnoresNullProcess() {
        ProcessKillResult result = ProcessTree.kill(null, 1000L);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.getRemainingPids().isEmpty());
    }

    @Test
    public void wrapNewProcessGroupIsIdempotent() {
        List<String> command = Arrays.asList("bash", "/tmp/demo.sh", "restart");
        List<String> wrapped = ProcessTree.wrapNewProcessGroup(command);
        if (!ProcessTree.supportsProcessGroup()) {
            Assertions.assertEquals(command, wrapped, "不支持进程组时应原样返回");
            return;
        }
        Assertions.assertEquals("setsid", wrapped.get(0));
        List<String> again = ProcessTree.wrapNewProcessGroup(wrapped);
        Assertions.assertEquals(wrapped, again, "重复包装不应叠加 setsid");
        Assertions.assertEquals(command, wrapped.subList(1, wrapped.size()), "其余命令参数必须保持不变");
    }

    @Test
    public void groupWrappedCommandCanBeKilledAsWholeGroup() throws Exception {
        if (!ProcessTree.supportsProcessGroup()) {
            return;
        }
        File dir = FileUtil.mkdir(FileUtil.getTmpDirPath() + "/jpom-process-tree-test");
        File pidFile = new File(dir, "group-child.pid");
        FileUtil.del(pidFile);
        List<String> command = Arrays.asList("sh", "-c",
            "sh -c 'echo $$ > " + pidFile.getAbsolutePath() + "; sleep 300' & wait");
        Process process = new ProcessBuilder(ProcessTree.wrapNewProcessGroup(command))
            .redirectErrorStream(true)
            .start();
        long child = awaitPid(pidFile, 5000);
        Assertions.assertTrue(child > 0, "进程组内子进程未启动");

        ProcessKillResult result = ProcessTree.kill(process, 3000L);

        Assertions.assertTrue(result.isSuccess(), "进程应整体退出：" + result);
        // 注意：为了不把服务端自己杀掉，2.11.16 起**不再按进程组发信号**，只对快照到的 pid 逐个终止，
        // 因此这里不再断言 isGroupKilled。
        Assertions.assertFalse(ProcessTree.isAlive(child), "子进程仍存活");
    }

    private long awaitPid(File pidFile, long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (pidFile.isFile()) {
                String text = FileUtil.readString(pidFile, StandardCharsets.UTF_8).trim();
                if (!text.isEmpty()) {
                    try {
                        return Long.parseLong(text.split("\\s+")[0]);
                    } catch (NumberFormatException ignored) {
                        // 继续等待写入完成
                    }
                }
            }
            Thread.sleep(50);
        }
        return -1;
    }
}