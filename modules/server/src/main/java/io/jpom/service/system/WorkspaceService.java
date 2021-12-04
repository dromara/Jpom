package io.jpom.service.system;

import cn.hutool.core.util.ClassUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.model.BaseWorkspaceModel;
import io.jpom.model.data.WorkspaceModel;
import io.jpom.service.h2db.BaseDbService;
import io.jpom.service.h2db.TableName;
import io.jpom.service.node.NodeService;
import org.springframework.stereotype.Service;

import java.util.Set;

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


	/**
	 * 将没有工作空间ID 的数据添加默认值
	 */
	public void convertNullWorkspaceId() {
		Set<Class<?>> classes = ClassUtil.scanPackage("io.jpom.model", BaseWorkspaceModel.class::isAssignableFrom);
		for (Class<?> aClass : classes) {
			TableName tableName = aClass.getAnnotation(TableName.class);
			if (tableName == null) {
				continue;
			}
			String sql = "update " + tableName.value() + " set workspaceId=? where workspaceId is null";
			NodeService nodeService = SpringUtil.getBean(NodeService.class);
			int execute = nodeService.execute(sql, WorkspaceModel.DEFAULT_ID);
			DefaultSystemLog.getLog().info("convertNullWorkspaceId {} {}", tableName.value(), execute);
		}

	}
}
