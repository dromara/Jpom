package io.jpom.service.user;

import io.jpom.model.data.UserBindWorkspaceModel;
import io.jpom.model.data.WorkspaceModel;
import io.jpom.service.h2db.BaseDbService;
import io.jpom.service.system.WorkspaceService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2021/12/4
 */
@Service
public class UserBindWorkspaceService extends BaseDbService<UserBindWorkspaceModel> {

	private final WorkspaceService workspaceService;

	public UserBindWorkspaceService(WorkspaceService workspaceService) {
		this.workspaceService = workspaceService;
	}

	/**
	 * 更新用户的工作空间信息
	 *
	 * @param userId    用户ID
	 * @param workspace 工作空间信息
	 */
	public void updateUserWorkspace(String userId, List<String> workspace) {
		Assert.notEmpty(workspace, "没有任何工作空间信息");
		List<UserBindWorkspaceModel> list = new HashSet<>(workspace).stream()
				// 过滤
				.filter(s -> workspaceService.exists(new WorkspaceModel(s)))
				.map(s -> {
					UserBindWorkspaceModel userBindWorkspaceModel = new UserBindWorkspaceModel();
					userBindWorkspaceModel.setWorkspaceId(s);
					userBindWorkspaceModel.setUserId(userId);
					userBindWorkspaceModel.setId(UserBindWorkspaceModel.getId(userId, s));
					return userBindWorkspaceModel;
				})
				.collect(Collectors.toList());
		// 删除之前的数据
		UserBindWorkspaceModel userBindWorkspaceModel = new UserBindWorkspaceModel();
		userBindWorkspaceModel.setUserId(userId);
		super.del(super.dataBeanToEntity(userBindWorkspaceModel));
		// 重新入库
		super.insert(list);
	}

	/**
	 * 查询用户绑定的工作空间
	 *
	 * @param userId 用户ID
	 * @return list
	 */
	public List<UserBindWorkspaceModel> listUserWorkspace(String userId) {
		UserBindWorkspaceModel userBindWorkspaceModel = new UserBindWorkspaceModel();
		userBindWorkspaceModel.setUserId(userId);
		return super.listByBean(userBindWorkspaceModel);
	}

	/**
	 * 判断对应的工作空间是否被用户绑定
	 *
	 * @param workspaceId 工作空间ID
	 * @return true 有用户绑定
	 */
	public boolean existsWorkspace(String workspaceId) {
		UserBindWorkspaceModel userBindWorkspaceModel = new UserBindWorkspaceModel();
		userBindWorkspaceModel.setWorkspaceId(workspaceId);
		return super.exists(userBindWorkspaceModel);
	}

	/**
	 * 查询用户绑定的工作空间
	 *
	 * @param userId 用户ID
	 * @return list
	 */
	public List<WorkspaceModel> listUserWorkspaceInfo(String userId) {
		UserBindWorkspaceModel userBindWorkspaceModel = new UserBindWorkspaceModel();
		userBindWorkspaceModel.setUserId(userId);
		List<UserBindWorkspaceModel> userBindWorkspaceModels = super.listByBean(userBindWorkspaceModel);
		Assert.notEmpty(userBindWorkspaceModels, "没有任何工作空间信息");
		List<String> collect = userBindWorkspaceModels.stream().map(UserBindWorkspaceModel::getWorkspaceId).collect(Collectors.toList());
		return workspaceService.listById(collect);
	}
}
