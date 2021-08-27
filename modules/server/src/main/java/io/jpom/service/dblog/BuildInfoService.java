package io.jpom.service.dblog;

import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.build.BuildInfoManage;
import io.jpom.common.Const;
import io.jpom.model.BaseEnum;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.model.data.BuildModel;
import io.jpom.model.data.RepositoryModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.enums.BuildReleaseMethod;
import io.jpom.model.enums.BuildStatus;
import io.jpom.service.h2db.BaseDbService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 构建 service 新版本，数据从数据库里面加载
 *
 * @author Hotstrip
 * @date 2021-08-10
 **/
@Service
public class BuildInfoService extends BaseDbService<BuildInfoModel> {

	@Resource
	private RepositoryService repositoryService;

	/**
	 * load date group by group name
	 *
	 * @return list
	 */
	public List<String> listGroup() {
		String sql = "select `GROUP` from " + getTableName() + " where 1=1 group by `GROUP`";
		List<Entity> list = super.query(sql);
		// 筛选字段
		return list.stream()
				.filter(entity -> StringUtils.hasLength(String.valueOf(entity.get(Const.GROUP_STR))))
				.flatMap(entity -> Stream.of(String.valueOf(entity.get(Const.GROUP_STR))))
				.distinct()
				.collect(Collectors.toList());
	}

	/**
	 * start build
	 *
	 * @param buildInfoModel 构建信息
	 * @param userModel      用户信息
	 * @return json
	 */
	public String start(BuildInfoModel buildInfoModel, UserModel userModel) {
		// load repository
		RepositoryModel repositoryModel = repositoryService.getByKey(buildInfoModel.getRepositoryId());
		if (null == repositoryModel) {
			return JsonMessage.getString(404, "仓库信息不存在");
		}
		BuildInfoManage.create(buildInfoModel, repositoryModel, userModel);
		return JsonMessage.getString(200, "开始构建中", buildInfoModel.getBuildId());
	}

	/**
	 * check status
	 *
	 * @param status 状态吗
	 * @return 错误消息
	 */
	public String checkStatus(Integer status) {
		if (status == null) {
			return null;
		}
		BuildStatus nowStatus = BaseEnum.getEnum(BuildStatus.class, status);
		Objects.requireNonNull(nowStatus);
		if (BuildStatus.Ing == nowStatus ||
				BuildStatus.PubIng == nowStatus) {
			return JsonMessage.getString(501, "当前还在：" + nowStatus.getDesc());
		}
		return null;
	}

	/**
	 * 判断是否存在 节点和项目关联
	 *
	 * @param nodeId    节点ID
	 * @param projectId 项目ID
	 * @return true 关联
	 */
	public boolean checkNodeProjectId(String nodeId, String projectId) {
		BuildInfoModel buildInfoModel = new BuildInfoModel();
		buildInfoModel.setReleaseMethodDataId(nodeId + ":" + projectId);
		buildInfoModel.setReleaseMethod(BuildReleaseMethod.Project.getCode());
		return super.exists(buildInfoModel);
	}

	/**
	 * 判断是否存在 节点关联
	 *
	 * @param nodeId 节点ID
	 * @return true 关联
	 */
	public boolean checkNode(String nodeId) {
		Entity entity = new Entity();
		entity.set("releaseMethod", BuildReleaseMethod.Project.getCode());
		entity.set("releaseMethodDataId", StrUtil.format(" like '{}:%'", nodeId));
		return super.exists(entity);
	}

	/**
	 * 判断是否存在 分发关联
	 *
	 * @param outGivingId 分发ID
	 * @return true 关联
	 */
	public boolean checkOutGiving(String outGivingId) {
		BuildInfoModel buildInfoModel = new BuildInfoModel();
		buildInfoModel.setReleaseMethodDataId(outGivingId);
		buildInfoModel.setReleaseMethod(BuildReleaseMethod.Outgiving.getCode());
		return super.exists(buildInfoModel);
	}
}
