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

import cn.hutool.core.thread.ExecutorBuilder;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.configuration.BuildExtConfig;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadPoolExecutor;

@Service
@Slf4j
public class BuildExecutorPoolService {
    /**
     * 构建线程池
     */
    private volatile ThreadPoolExecutor threadPoolExecutor;
    private final BuildExtConfig buildExtConfig;

    public BuildExecutorPoolService(BuildExtConfig buildExtConfig) {
        this.buildExtConfig = buildExtConfig;
    }

    public ThreadPoolExecutor getThreadPoolExecutor() {
        this.initPool();
        return threadPoolExecutor;
    }

    public void execute(Runnable command) {
        this.initPool();
        threadPoolExecutor.execute(command);
    }

    /**
     * 创建构建线程池
     */
    private void initPool() {
        if (threadPoolExecutor == null) {
            synchronized (BuildExecutorPoolService.class) {
                if (threadPoolExecutor == null) {
                    ExecutorBuilder executorBuilder = ExecutorBuilder.create();
                    int poolSize = buildExtConfig.getPoolSize();
                    if (poolSize > 0) {
                        executorBuilder.setCorePoolSize(poolSize).setMaxPoolSize(poolSize);
                    }
                    executorBuilder.useArrayBlockingQueue(Math.max(buildExtConfig.getPoolWaitQueue(), 1));
                    executorBuilder.setHandler(new ThreadPoolExecutor.DiscardPolicy() {
                        @Override
                        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                            if (r instanceof BuildExecuteManage) {
                                // 取消任务
                                BuildExecuteManage buildExecuteManage = (BuildExecuteManage) r;
                                buildExecuteManage.rejectedExecution();
                            } else {
                                log.warn(I18nMessageUtil.get("i18n.build_thread_pool_rejected_task.3bad"), r.getClass());
                            }
                        }
                    });
                    threadPoolExecutor = executorBuilder.build();
                    JpomApplication.register("build", threadPoolExecutor);
                }
            }
        }
    }
}
