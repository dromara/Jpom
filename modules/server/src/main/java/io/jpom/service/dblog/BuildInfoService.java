package io.jpom.service.dblog;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.db.Entity;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.build.BuildInfoManage;
import io.jpom.common.Const;
import io.jpom.model.BaseEnum;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.model.data.BuildModel;
import io.jpom.model.data.RepositoryModel;
import io.jpom.model.data.UserModel;
import io.jpom.service.h2db.BaseDbService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
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
	 * 插入数据库
	 *
	 * @param info
	 * @return
	 */
	public int add(BuildInfoModel info) {
		// check id
		Assert.hasText(info.getId(), "不能执行：error");
		// def create time
		info.setCreateTimeMillis(ObjectUtil.defaultIfNull(info.getModifyTimeMillis(), SystemClock.now()));

		Entity entity = new Entity(getTableName());
		entity.parseBean(info, false, true);

		/**
		 * reset group field name
		 * group is key words
		 */
		if (null != info.getGroup()) {
			entity.remove(Const.GROUP_STR);
			entity.put(Const.GROUP_COLUMN_STR, info.getGroup());
		}

		return super.insert(entity);
	}

	/**
	 * update build info
	 *
	 * @param info data
	 * @return
	 */
	@Override
	public int update(BuildInfoModel info) {
		// check id
		Assert.hasText(info.getId(), "不能执行：error");
		// def modify time
		info.setModifyTimeMillis(ObjectUtil.defaultIfNull(info.getModifyTimeMillis(), SystemClock.now()));
		// remove create time
		info.setCreateTimeMillis(null);

		Entity entity = new Entity(getTableName());
		entity.parseBean(info, false, true);

		/**
		 * reset group field name
		 * group is key words
		 */
		if (null != info.getGroup()) {
			entity.remove(Const.GROUP_STR);
			entity.put(Const.GROUP_COLUMN_STR, info.getGroup());
		}

		Entity where = new Entity();
		where.set(Const.ID_STR, info.getId());
		return super.update(entity, where);
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
		BuildModel.Status nowStatus = BaseEnum.getEnum(BuildModel.Status.class, status);
		Objects.requireNonNull(nowStatus);
		if (BuildModel.Status.Ing == nowStatus ||
				BuildModel.Status.PubIng == nowStatus) {
			return JsonMessage.getString(501, "当前还在：" + nowStatus.getDesc());
		}
		return null;
	}
}
