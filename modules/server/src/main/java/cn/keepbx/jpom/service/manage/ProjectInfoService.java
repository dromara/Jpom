package cn.keepbx.jpom.service.manage;

import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.service.node.NodeService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 项目的简单操作
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
@Service
public class ProjectInfoService {

    @Resource
    private NodeService nodeService;

    public List<String> getAllGroup(NodeModel nodeModel) {
        return NodeForward.requestData(nodeModel, NodeUrl.Manage_GetProjectGroup, null, List.class);
    }

    public JSONObject getItem(NodeModel nodeModel, String id) {
        return NodeForward.requestData(nodeModel, NodeUrl.Manage_GetProjectItem, JSONObject.class, "id", id);
    }

    public JSONObject getLogSize(NodeModel nodeModel, String id) {
        return NodeForward.requestData(nodeModel, NodeUrl.Manage_Log_LogSize, JSONObject.class, "id", id);
    }

    public JSONObject getItem(String nodeId, String id) {
        NodeModel item = nodeService.getItem(id);
        return NodeForward.requestData(item, NodeUrl.Manage_GetProjectItem, JSONObject.class, "id", id);
    }
}
