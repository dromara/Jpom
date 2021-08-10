package io.jpom.service.dblog;

import cn.hutool.db.Entity;
import io.jpom.common.Const;
import io.jpom.model.data.RepositoryModel;
import org.springframework.stereotype.Service;

/**
 * @author Hotstrip
 * Repository service
 */
@Service
public class RepositoryService extends BaseDbLogService<RepositoryModel> {

	/**
	 * constructor, init table name and primary key
	 */
	public RepositoryService() {
		super(RepositoryModel.TABLE_NAME, RepositoryModel.class);
		setKey(Const.ID_STR);
	}

	/**
	 * update by id with data
	 * @param info
	 */
	public void updateById(RepositoryModel info) {
		Entity entity = new Entity(getTableName());
		entity.parseBean(info);

		Entity where = new Entity();
		where.set(Const.ID_STR, info.getId());
		update(entity, where);
	}

	/**
	 * delete by id
	 * @param id
	 */
	public void deleteById(String id) {
		Entity where = new Entity(getTableName());
		where.set("id", id);
		del(where);
	}
}
