package cn.keepbx.jpom.service.node;

import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.system.ServerConfigBean;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author jiangzeyin
 * @date 2019/4/16
 */
@Service
public class NodeService extends BaseOperService<NodeModel> {

    @Override
    public List<NodeModel> list() {
        JSONObject jsonObject = getJSONObject(ServerConfigBean.NODE);
        JSONArray jsonArray = formatToArray(jsonObject);
        return jsonArray.toJavaList(NodeModel.class);
    }

    @Override
    public NodeModel getItem(String id) {
        return getJsonObjectById(ServerConfigBean.NODE, id, NodeModel.class);
    }

    @Override
    public void addItem(NodeModel userModel) {
        // 保存
        saveJson(ServerConfigBean.NODE, userModel.toJson());
    }

    @Override
    public boolean updateItem(NodeModel userModel) throws Exception {
        updateJson(ServerConfigBean.NODE, userModel.toJson());
        return true;
    }

    public boolean deleteItem(String id) {
        try {
            deleteJson(ServerConfigBean.NODE, id);
            return true;
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return false;
    }
}
