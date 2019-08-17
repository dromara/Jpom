package cn.keepbx.jpom.service.node;

import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.model.data.OutGivingModel;
import cn.keepbx.jpom.model.data.OutGivingNodeProject;
import cn.keepbx.jpom.system.ServerConfigBean;
import cn.keepbx.permission.BaseDynamicService;
import cn.keepbx.plugin.ClassFeature;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

    @Override
    public void addItem(OutGivingModel outGivingModel) {
        // 不保存临时数据
        JSONObject jsonObject = outGivingModel.toJson();
        jsonObject.remove("tempCacheMap");
        OutGivingModel newData = jsonObject.toJavaObject(OutGivingModel.class);
        newData.setTempCacheMap(null);
        // 保存
        super.addItem(outGivingModel);
    }

    @Override
    public void updateItem(OutGivingModel outGivingModel) {
        // 不保存临时数据
        JSONObject jsonObject = outGivingModel.toJson();
        jsonObject.remove("tempCacheMap");
        OutGivingModel newData = jsonObject.toJavaObject(OutGivingModel.class);
        newData.setTempCacheMap(null);
        super.updateItem(outGivingModel);
    }

    public boolean checkNode(String nodeId) throws IOException {
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
