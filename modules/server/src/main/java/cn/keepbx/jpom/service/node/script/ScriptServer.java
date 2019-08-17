package cn.keepbx.jpom.service.node.script;

import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.permission.BaseDynamicService;
import cn.keepbx.jpom.service.node.NodeService;
import cn.keepbx.plugin.ClassFeature;
import com.alibaba.fastjson.JSONArray;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author bwcx_jzy
 * @date 2019/8/16
 */
@Service
public class ScriptServer implements BaseDynamicService {

    @Resource
    private NodeService nodeService;

    @Override
    public JSONArray listToArray(String dataId) {
        NodeModel item = nodeService.getItem(dataId);
        return listToArray(item);
    }

    public JSONArray listToArray(NodeModel nodeModel) {
        JSONArray jsonArray = NodeForward.requestData(nodeModel, NodeUrl.Script_List, null, JSONArray.class);
        return filter(jsonArray, ClassFeature.SCRIPT);
    }
}
