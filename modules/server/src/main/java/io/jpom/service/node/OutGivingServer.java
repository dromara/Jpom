package io.jpom.service.node;

import io.jpom.model.data.OutGivingModel;
import io.jpom.model.data.OutGivingNodeProject;
import io.jpom.service.IStatusRecover;
import io.jpom.service.h2db.BaseWorkspaceService;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 分发管理
 *
 * @author jiangzeyin
 * @date 2019/4/21
 */
@Service
public class OutGivingServer extends BaseWorkspaceService<OutGivingModel> implements IStatusRecover {


	public boolean checkNode(String nodeId, HttpServletRequest request) {
		List<OutGivingModel> list = super.listByWorkspace(request);
		if (list == null || list.isEmpty()) {
			return false;
		}
		for (OutGivingModel outGivingModel : list) {
			List<OutGivingNodeProject> outGivingNodeProjectList = outGivingModel.outGivingNodeProjectList();
			if (outGivingNodeProjectList != null) {
				for (OutGivingNodeProject outGivingNodeProject : outGivingNodeProjectList) {
					if (outGivingNodeProject.getNodeId().equals(nodeId)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public int statusRecover() {
		// 恢复异常数据
		String updateSql = "update " + super.getTableName() + " set status=? where status=?";
		return super.execute(updateSql, OutGivingModel.Status.DONE.getCode(), OutGivingModel.Status.ING.getCode());
	}
}
