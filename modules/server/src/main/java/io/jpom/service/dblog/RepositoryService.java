package io.jpom.service.dblog;

import io.jpom.model.data.RepositoryModel;
import io.jpom.service.h2db.BaseDbService;
import org.springframework.stereotype.Service;

/**
 * @author Hotstrip
 * Repository service
 */
@Service
public class RepositoryService extends BaseDbService<RepositoryModel> {

	@Override
	public void insert(RepositoryModel repositoryModelReq) {
		super.insert(repositoryModelReq);
	}

	@Override
	public int updateById(RepositoryModel info) {
		return super.updateById(info);
	}
}
