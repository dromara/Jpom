package io.jpom.service.h2db;

import cn.hutool.db.Entity;
import io.jpom.common.Const;
import io.jpom.model.BaseGroupModel;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 分组 server
 *
 * @author bwcx_jzy
 * @since 2022/1/5
 */
public abstract class BaseGroupService<T extends BaseGroupModel> extends BaseWorkspaceService<T> {


	/**
	 * load date group by group name
	 *
	 * @return list
	 */
	public List<String> listGroup(HttpServletRequest request) {
		String workspaceId = getCheckUserWorkspace(request);
		String sql = "select `GROUP` from " + getTableName() + " where workspaceId=? group by `GROUP`";
		List<Entity> list = super.query(sql, workspaceId);
		// 筛选字段
		return list.stream()
				.filter(entity -> StringUtils.hasLength(String.valueOf(entity.get(Const.GROUP_STR))))
				.flatMap(entity -> Stream.of(String.valueOf(entity.get(Const.GROUP_STR))))
				.distinct().collect(Collectors.toList());
	}

	/**
	 * 修护字段
	 */
	public void repairGroupFiled() {
		String sql = "update " + getTableName() + " set `GROUP`=? where `GROUP` is null or `GROUP`=''";
		super.execute(sql, "默认");
	}
}
