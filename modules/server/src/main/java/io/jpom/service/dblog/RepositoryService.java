package io.jpom.service.dblog;

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
}
