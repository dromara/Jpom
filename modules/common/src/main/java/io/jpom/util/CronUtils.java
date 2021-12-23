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
package io.jpom.util;

import cn.hutool.cron.CronUtil;
import cn.hutool.cron.Scheduler;
import cn.hutool.cron.TaskTable;
import cn.hutool.cron.task.Task;
import com.alibaba.fastjson.JSONObject;
import io.jpom.system.ExtConfigBean;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @date 2019/7/12
 **/
public class CronUtils {

	/**
	 * 开始
	 */
	public static void start() {
		boolean matchSecond = ExtConfigBean.getInstance().getTimerMatchSecond();
		// 开启秒级
		CronUtil.setMatchSecond(matchSecond);
		//
		Scheduler scheduler = CronUtil.getScheduler();
		if (!scheduler.isStarted()) {
			CronUtil.start();
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
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("taskId", s);
			jsonObject.put("cron", scheduler.getPattern(s).toString());
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
