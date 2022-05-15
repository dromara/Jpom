package io.jpom.service.outgiving;

import io.jpom.model.outgiving.LogReadModel;
import io.jpom.model.outgiving.OutGivingModel;
import io.jpom.model.outgiving.OutGivingNodeProject;
import io.jpom.service.h2db.BaseWorkspaceService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2022/5/15
 */
@Service
public class LogReadServer extends BaseWorkspaceService<LogReadModel> {


    public void checkNodeProject(String nodeId, String projectId, HttpServletRequest request) {
        // 检查节点分发
        List<LogReadModel> outGivingModels = super.listByWorkspace(request);
        if (outGivingModels != null) {
            boolean match = outGivingModels.stream().anyMatch(outGivingModel -> outGivingModel.checkContains(nodeId, projectId));
            Assert.state(!match, "当前项目存在日志阅读，不能直接删除");
        }
    }

    public boolean checkNode(String nodeId, HttpServletRequest request) {
        List<LogReadModel> list = super.listByWorkspace(request);
        if (list == null || list.isEmpty()) {
            return false;
        }
        for (LogReadModel outGivingModel : list) {
            List<LogReadModel.Item> items = outGivingModel.nodeProjectList();
            if (items != null) {
                for (LogReadModel.Item item : items) {
                    if (item.getNodeId().equals(nodeId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
