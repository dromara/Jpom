package cn.keepbx.jpom.service.node;

import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.system.ConfigBean;
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
        JSONObject jsonObject = getJSONObject(ConfigBean.NODE);
        JSONArray jsonArray = formatToArray(jsonObject);
        return jsonArray.toJavaList(NodeModel.class);
    }

    @Override
    public NodeModel getItem(String id) {
        return getJsonObjectById(ConfigBean.NODE, id, NodeModel.class);
    }

    @Override
    public void addItem(NodeModel userModel) {
        // 保存
        saveJson(ConfigBean.NODE, userModel.toJson());
    }

    @Override
    public boolean updateItem(NodeModel userModel) throws Exception {
        updateJson(ConfigBean.NODE, userModel.toJson());
        return true;
    }
}
