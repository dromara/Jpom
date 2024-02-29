/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package cn;

import org.apache.commons.exec.*;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;

/**
 * <a href="https://blog.51cto.com/u_75269/7968284">https://blog.51cto.com/u_75269/7968284</a>
 */
public class TestApacheExec {
    private ShutdownHookProcessDestroyer shutdownHookProcessDestroyer = new ShutdownHookProcessDestroyer();


    @Test
    public void test() throws IOException {


        CommandLine commandLine = CommandLine.parse("ping baidu.com");

        // 重定向stdout和stderr到文件
        PumpStreamHandler pumpStreamHandler = new PumpStreamHandler();

        // 超时终止：1秒
        ExecuteWatchdog executeWatchdog = ExecuteWatchdog.builder().setTimeout(Duration.ofSeconds(10)).get();

        // 创建执行器
        DefaultExecutor executor = DefaultExecutor.builder()
            .setExecuteStreamHandler(pumpStreamHandler)
            .get();
        executor.setProcessDestroyer(new ProcessDestroyer() {
            @Override
            public boolean add(Process process) {
                return shutdownHookProcessDestroyer.add(process);
            }

            @Override
            public boolean remove(Process process) {
                return shutdownHookProcessDestroyer.remove(process);
            }

            @Override
            public int size() {
                return shutdownHookProcessDestroyer.size();
            }
        });
        executor.setWatchdog(executeWatchdog);

        // 执行，打印退出码
        int exitValue = executor.execute(commandLine);
        System.out.println(exitValue);
    }
}
