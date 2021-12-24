package io.jpom.service;

/**
 * 状态恢复接口
 *
 * @author bwcx_jzy
 * @since 2021/12/24
 */
public interface IStatusRecover {

	/**
	 * 状态恢复
	 *
	 * @return 恢复条数
	 */
	int statusRecover();
}
