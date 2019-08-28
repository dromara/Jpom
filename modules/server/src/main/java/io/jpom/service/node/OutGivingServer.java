package io.jpom.service.node;

import com.alibaba.fastjson.JSONArray;
import io.jpom.common.BaseOperService;
import io.jpom.model.data.OutGivingModel;
import io.jpom.model.data.OutGivingNodeProject;
import io.jpom.permission.BaseDynamicService;
import io.jpom.plugin.ClassFeature;
import io.jpom.system.ServerConfigBean;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分发管理
 *
 * @author jiangzeyin
 * @date 2019/4/21
 */
@Service
public class OutGivingServer extends BaseOperService<OutGivingModel> implements BaseDynamicService {

    public OutGivingServer() {
        super(ServerConfigBean.OUTGIVING);
    }

    public boolean checkNode(String nodeId) {
        List<OutGivingModel> list = list();
        if (list == null || list.isEmpty()) {
            return false;
        }
        for (OutGivingModel outGivingModel : list) {
            List<OutGivingNodeProject> outGivingNodeProjectList = outGivingModel.getOutGivingNodeProjectList();
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
    public JSONArray listToArray(String dataId) {
        return (JSONArray) JSONArray.toJSON(this.list());
    }

    @Override
    public List<OutGivingModel> list() {
        return (List<OutGivingModel>) filter(super.list(), ClassFeature.OUTGIVING);
    }
}
