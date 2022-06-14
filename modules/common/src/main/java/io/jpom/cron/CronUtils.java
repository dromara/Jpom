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
package io.jpom.cron;

import cn.hutool.core.date.SystemClock;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.Scheduler;
import cn.hutool.cron.TaskExecutor;
import cn.hutool.cron.TaskTable;
import cn.hutool.cron.listener.TaskListener;
import cn.hutool.cron.task.Task;
import com.alibaba.fastjson.JSONObject;
import io.jpom.system.ExtConfigBean;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private static class TaskStat {
        /**
         * 执行次数
         */
        private int executeCount;
        /**
         * 失败次数
         */
        private int failedCount;
        /**
         * 成功次数
         */
        private int succeedCount;
        /**
         * 最后执行时间
         */
        private Long lastExecuteTime;
    }

    /**
     * 开始
     */
    public static void start() {
        boolean matchSecond = ExtConfigBean.getInstance().getTimerMatchSecond();
        // 开启秒级
        CronUtil.setMatchSecond(matchSecond);
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
                    TaskStat taskStat = TASK_STAT.computeIfAbsent(executor.getCronTask().getId(), s -> new TaskStat());
                    taskStat.lastExecuteTime = SystemClock.now();
                    taskStat.executeCount++;
                }

                @Override
                public void onSucceeded(TaskExecutor executor) {
                    TaskStat taskStat = TASK_STAT.computeIfAbsent(executor.getCronTask().getId(), s -> new TaskStat());
                    taskStat.succeedCount++;
                }

                @Override
                public void onFailed(TaskExecutor executor, Throwable exception) {
                    TaskStat taskStat = TASK_STAT.computeIfAbsent(executor.getCronTask().getId(), s -> new TaskStat());
                    taskStat.failedCount++;
                    log.error("定时任务异常", exception);
                }
            });
        }
    }

    /**
     * 获取任务列表
     *
     * @return list
     */
    public static List<JSONObject> list() {
        Scheduler scheduler = CronUtil.getScheduler();
        TaskTable taskTable = scheduler.getTaskTable();
        List<String> ids = taskTable.getIds();
        return ids.stream().map(s -> {
            TaskStat taskStat = TASK_STAT.get(s);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("taskId", s);
            jsonObject.put("cron", scheduler.getPattern(s).toString());
            if (taskStat != null) {
                jsonObject.put("executeCount", taskStat.executeCount);
                jsonObject.put("failedCount", taskStat.failedCount);
                jsonObject.put("succeedCount", taskStat.succeedCount);
                jsonObject.put("lastExecuteTime", taskStat.lastExecuteTime);
            }
            return jsonObject;
        }).collect(Collectors.toList());
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
