/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.cron;

import cn.hutool.core.date.SystemClock;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.Scheduler;
import cn.hutool.cron.TaskExecutor;
import cn.hutool.cron.listener.TaskListener;
import cn.hutool.cron.pattern.CronPattern;
import cn.hutool.cron.task.CronTask;
import cn.hutool.cron.task.Task;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2019/7/12
 **/
@Slf4j
public class CronUtils {

    private static final Map<String, TaskStat> TASK_STAT = new ConcurrentHashMap<>(50);

    /**
     * 任务统计
     */
    public static class TaskStat {
        /**
         * 执行次数
         */
        private final AtomicInteger executeCount = new AtomicInteger(0);
        /**
         * 失败次数
         */
        private final AtomicInteger failedCount = new AtomicInteger(0);
        /**
         * 成功次数
         */
        private final AtomicInteger succeedCount = new AtomicInteger(0);
        /**
         * 最后执行时间
         */
        private Long lastExecuteTime;
        /**
         * 描述
         */
        private final String desc;

        public TaskStat(String desc) {
            this.desc = desc;
        }

        public void onStart() {
            this.lastExecuteTime = SystemClock.now();
            this.executeCount.incrementAndGet();
        }

        public void onSucceeded() {
            this.succeedCount.incrementAndGet();
        }

        public void onFailed(String tag, Throwable exception) {
            this.failedCount.incrementAndGet();
            log.error("定时任务异常 {}", tag, exception);
        }
    }

    /**
     * 开始
     */
    public static void start() {
        //
        Scheduler scheduler = CronUtil.getScheduler();
        //
        boolean started = scheduler.isStarted();
        if (started) {
            return;
        }
        synchronized (CronUtils.class) {
            started = scheduler.isStarted();
            if (started) {
                return;
            }
            CronUtil.start();
            scheduler.addListener(new TaskListener() {
                @Override
                public void onStart(TaskExecutor executor) {
                    CronTask cronTask = executor.getCronTask();
                    TaskStat taskStat = CronUtils.getTaskStat(cronTask.getId(), null);
                    taskStat.onStart();
                }

                @Override
                public void onSucceeded(TaskExecutor executor) {
                    CronTask cronTask = executor.getCronTask();
                    TaskStat taskStat = CronUtils.getTaskStat(cronTask.getId(), null);
                    taskStat.onSucceeded();
                }

                @Override
                public void onFailed(TaskExecutor executor, Throwable exception) {
                    CronTask cronTask = executor.getCronTask();
                    TaskStat taskStat = CronUtils.getTaskStat(cronTask.getId(), null);
                    taskStat.onFailed(cronTask.getId(), exception);
                }
            });
        }
    }

    /**
     * 获取任务统计
     *
     * @param id 任务id
     * @return 统计对象
     */
    public static TaskStat getTaskStat(String id, String desc) {
        return TASK_STAT.computeIfAbsent(id, s -> new TaskStat(desc));
    }

    /**
     * 获取任务列表
     *
     * @return list
     */
    public static List<JSONObject> list() {
        Scheduler scheduler = CronUtil.getScheduler();
        Set<Map.Entry<String, TaskStat>> entries = TASK_STAT.entrySet();
        return entries.stream()
            .map(entry -> {
                TaskStat taskStat = entry.getValue();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("taskId", entry.getKey());
                CronPattern pattern = scheduler.getPattern(entry.getKey());
                Optional.ofNullable(pattern).ifPresent(cronPattern -> jsonObject.put("cron", cronPattern.toString()));
                if (taskStat != null) {
                    jsonObject.put("executeCount", taskStat.executeCount.get());
                    jsonObject.put("failedCount", taskStat.failedCount.get());
                    jsonObject.put("succeedCount", taskStat.succeedCount.get());
                    jsonObject.put("lastExecuteTime", taskStat.lastExecuteTime);
                    jsonObject.put("desc", taskStat.desc);
                }
                return jsonObject;
            })
            .collect(Collectors.toList());
    }

    /**
     * 添加任务 已经存在则不添加
     *
     * @param id       任务ID
     * @param cron     表达式
     * @param supplier 创建任务回调
     */
    public static void add(String id, String cron, Supplier<Task> supplier) {
        Scheduler scheduler = CronUtil.getScheduler();
        Task task = scheduler.getTask(id);
        if (task != null) {
            return;
        }
        scheduler.schedule(id, cron, supplier.get());
        //
        CronUtils.start();
    }

    /**
     * 添加任务、自动去重
     *
     * @param id   任务ID
     * @param cron 表达式
     * @param task 任务作业
     */
    public static void upsert(String id, String cron, Task task) {
        Scheduler scheduler = CronUtil.getScheduler();
        Task schedulerTask = scheduler.getTask(id);
        if (schedulerTask != null) {
            CronUtil.remove(id);
        }
        // 创建任务
        CronUtil.schedule(id, cron, task);
        //
        CronUtils.start();
    }

    /**
     * 停止定时任务
     *
     * @param id ID
     */
    public static void remove(String id) {
        CronUtil.remove(id);
    }
}
