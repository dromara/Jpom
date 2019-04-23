package cn.keepbx.jpom.service.node;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.util.IdUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.system.ServerConfigBean;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 节点管理
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
@Service
public class NodeService extends BaseOperService<NodeModel> {

    private static final TimedCache<String, List<NodeModel>> TIMED_CACHE = new TimedCache<>(TimeUnit.MINUTES.toMillis(5));

    @Override
    public List<NodeModel> list() {
        JSONObject jsonObject = getJSONObject(ServerConfigBean.NODE);
        JSONArray jsonArray = formatToArray(jsonObject);
        return jsonArray.toJavaList(NodeModel.class);
    }

    /**
     * 获取所有节点 和节点下面的项目
     *
     * @return list
     */
    public List<NodeModel> listAndProject() {
        List<NodeModel> nodeModels = this.list();
        Iterator<NodeModel> iterator = nodeModels.iterator();
        while (iterator.hasNext()) {
            NodeModel nodeModel = iterator.next();
            if (!nodeModel.isOpenStatus()) {
                iterator.remove();
                continue;
            }
            try {
                // 获取项目信息不需要状态
                JSONArray jsonArray = NodeForward.requestData(nodeModel, NodeUrl.Manage_GetProjectInfo, JSONArray.class, "notStatus", "true");
                if (jsonArray != null) {
                    nodeModel.setProjects(jsonArray);
                } else {
                    iterator.remove();
                }
            } catch (Exception e) {
                iterator.remove();
            }
        }
        return nodeModels;
    }

    public String cacheNodeList(List<NodeModel> list) {
        String reqId = IdUtil.fastUUID();
        TIMED_CACHE.put(reqId, list);
        return reqId;
    }

    /**
     * 获取页面编辑的节点信息
     *
     * @param id id
     * @return list
     */
    public List<NodeModel> getNodeModel(String id) {
        return TIMED_CACHE.get(id);
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
