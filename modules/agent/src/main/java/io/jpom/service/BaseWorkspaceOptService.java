package io.jpom.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import io.jpom.common.BaseAgentController;
import io.jpom.model.data.BaseWorkspaceModel;

/**
 * @author bwcx_jzy
 * @since 2022/1/17
 */
public abstract class BaseWorkspaceOptService<T extends BaseWorkspaceModel> extends BaseOperService<T> {

	public BaseWorkspaceOptService(String fileName) {
		super(fileName);
	}

	/**
	 * 修改信息
	 *
	 * @param data 信息
	 */
	@Override
	public void updateItem(T data) {
		data.setModifyTime(DateUtil.now());
		String userName = BaseAgentController.getNowUserName();
		if (!StrUtil.DASHED.equals(userName)) {
			data.setModifyUser(userName);
		}
		super.updateItem(data);
	}
}
