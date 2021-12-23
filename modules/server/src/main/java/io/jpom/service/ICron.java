package io.jpom.service;

/**
 * @author bwcx_jzy
 * @since 2021/12/23
 */
public interface ICron {

	/**
	 * 启动所有的定时任务
	 *
	 * @return 启动成功的任务数
	 */
	int startCron();
}
