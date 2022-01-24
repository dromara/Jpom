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

import io.jpom.model.BaseIdModel;

import java.util.List;

/**
 * 需要启动定时任务的 服务接口
 *
 * @author bwcx_jzy
 * @since 2021/12/23
 */
public interface ICron<T extends BaseIdModel> {

	/**
	 * 查询启动中的 所有定时任务 列表
	 *
	 * @return list
	 */
	List<T> queryStartingList();

	/**
	 * 启动所有的定时任务
	 *
	 * @return 启动成功的任务数
	 */
	default int startCron() {
		List<T> startingList = this.queryStartingList();
		if (startingList == null) {
			return 0;
		}
		return (int) startingList.stream().map(ICron.this::checkCron).filter(aBoolean -> aBoolean).count();
	}

	/**
	 * 检查是否启动定时
	 *
	 * @param data bean
	 * @return true 开启定时、false 关闭定时
	 */
	boolean checkCron(T data);
}
