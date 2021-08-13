package io.jpom.service.dblog;

import cn.hutool.core.date.LocalDateTimeUtil;
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
		if (null == repositoryModelReq.getModifyTime()) {
			repositoryModelReq.setModifyTime(LocalDateTimeUtil.format(LocalDateTimeUtil.now(), "YYYY-MM-dd HH:mm:ss"));
		}
		super.insert(repositoryModelReq);
	}

	@Override
	public void updateById(RepositoryModel info) {
		info.setModifyTime(LocalDateTimeUtil.format(LocalDateTimeUtil.now(), "YYYY-MM-dd HH:mm:ss"));
		super.updateById(info);
	}
}
