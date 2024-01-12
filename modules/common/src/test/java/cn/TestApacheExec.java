/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package cn;

import cn.hutool.core.thread.GlobalThreadPool;
import cn.hutool.core.thread.ThreadUtil;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * <a href="https://blog.51cto.com/u_75269/7968284">https://blog.51cto.com/u_75269/7968284</a>
 */
public class TestApacheExec {

    @Test
    public void test() throws IOException {
        ThreadPoolExecutor executorService = (ThreadPoolExecutor) GlobalThreadPool.getExecutor();
        ThreadFactory threadFactory = executorService.getThreadFactory();

        CommandLine commandLine = CommandLine.parse("ping baidu.com");

        // 重定向stdout和stderr到文件
        PumpStreamHandler pumpStreamHandler = new PumpStreamHandler();
        // 超时终止：1秒
        ExecuteWatchdog executeWatchdog = ExecuteWatchdog.builder().setTimeout(Duration.ofSeconds(10)).get();

        // 创建执行器
        DefaultExecutor executor = DefaultExecutor.builder()
            .setExecuteStreamHandler(pumpStreamHandler)
            .get();
        executor.setWatchdog(executeWatchdog);

        // 执行，打印退出码
        int exitValue = executor.execute(commandLine);
        System.out.println(exitValue);
    }
}
