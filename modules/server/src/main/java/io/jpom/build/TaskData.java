package io.jpom.build;

import io.jpom.model.data.BuildInfoModel;
import io.jpom.model.data.RepositoryModel;
import io.jpom.model.data.UserModel;
import lombok.Builder;

/**
 * @author bwcx_jzy
 * @since 2022/1/26
 */
@Builder
public class TaskData {

	protected final BuildInfoModel buildInfoModel;
	protected final RepositoryModel repositoryModel;
	protected final UserModel userModel;
	/**
	 * 延迟执行的时间（单位秒）
	 */
	protected final Integer delay;
	/**
	 * 触发类型
	 */
	protected final int triggerBuildType;
}
