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
import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.thread.ThreadUtil;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

/**
 * @author bwcx_jzy
 * @since 2022/5/13
 */
public class TestThread implements Callable<String> {

    @Test
    public void test1() {
        ThreadPoolExecutor build = ExecutorBuilder.create().setCorePoolSize(1)
            .setMaxPoolSize(1)
            .useArrayBlockingQueue(1).setHandler(new ThreadPoolExecutor.DiscardPolicy() {
                @Override
                public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                    if (r instanceof FutureTask) {
                        FutureTask<?> futureTask = (FutureTask<?>) r;
//                        System.out.println(futureTask.);
                    }
                }
            }).build();
        for (int i = 0; i <= 2; i++) {
            build.execute(() -> ThreadUtil.sleep(1, TimeUnit.MINUTES));
        }
        build.submit(this);
    }

    @Test
    public void test() throws ExecutionException, InterruptedException {
        ExecutorService executorService = ThreadUtil.newSingleExecutor();


        for (int i = 0; i < 5; i++) {
            int finalI = i;
            executorService.submit(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    System.out.println(finalI);
                    return finalI;
                }
            });
        }

        ThreadUtil.sleep(10, TimeUnit.SECONDS);
    }

    @Override
    public String call() throws Exception {
        return null;
    }
}
