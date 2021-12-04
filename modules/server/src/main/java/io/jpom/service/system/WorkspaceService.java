package io.jpom.service.system;

import cn.jiangzeyin.common.DefaultSystemLog;
import io.jpom.model.data.WorkspaceModel;
import io.jpom.service.h2db.BaseDbService;
import org.springframework.stereotype.Service;

/**
 * @author bwcx_jzy
 * @since 2021/12/3
 */
@Service
public class WorkspaceService extends BaseDbService<WorkspaceModel> {

	/**
	 * 检查初始化 默认的工作空间
	 */
	public void checkInitDefault() {
		WorkspaceModel workspaceModel = super.getByKey(WorkspaceModel.DEFAULT_ID);
		if (workspaceModel != null) {
			return;
		}
		WorkspaceModel defaultWorkspace = new WorkspaceModel();
		defaultWorkspace.setId(WorkspaceModel.DEFAULT_ID);
		defaultWorkspace.setName("默认");
		defaultWorkspace.setDescription("系统默认的工作空间,不能删除");
		super.insert(defaultWorkspace);
		DefaultSystemLog.getLog().info("init created default workspace");
	}
}
