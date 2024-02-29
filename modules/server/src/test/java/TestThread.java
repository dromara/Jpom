/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
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
            executorService.submit((Callable<Object>) () -> {
                System.out.println(finalI);
                return finalI;
            });
        }

        ThreadUtil.sleep(10, TimeUnit.SECONDS);
    }

    @Override
    public String call() throws Exception {
        return null;
    }
}
