package cn.keepbx.jpom.controller.node;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.model.system.JpomManifest;
import cn.keepbx.jpom.service.node.NodeService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.InputStream;
import java.util.List;

/**
 * 节点管理
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
@Controller
@RequestMapping(value = "/node")
public class NodeIndexController extends BaseServerController {

    @Resource
    private NodeService nodeService;

    @RequestMapping(value = "list.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String list() {
        List<NodeModel> nodeModels = nodeService.list();
        setAttribute("array", nodeModels);
        return "node/list";
    }

    @RequestMapping(value = "index.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String index() {
        List<NodeModel> nodeModels = nodeService.list();
        setAttribute("array", nodeModels);
        //
        JsonMessage jsonMessage = NodeForward.request(getNode(), getRequest(), NodeUrl.Info);
        JpomManifest jpomManifest = NodeForward.toObj(jsonMessage, JpomManifest.class);
        setAttribute("jpomManifest", jpomManifest);
        setAttribute("installed", jsonMessage.getCode() == 200);
        UserModel userModel = getUser();
        // 版本提示
        if (!JpomManifest.getInstance().isDebug() && jpomManifest != null && userModel.isSystemUser()) {
            JpomManifest thisInfo = JpomManifest.getInstance();
            if (!StrUtil.equals(jpomManifest.getVersion(), thisInfo.getVersion())) {
                setAttribute("tipUpdate", true);
            }
        }
        return "node/index";
    }

    @RequestMapping(value = "node_status", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String nodeStatus() {
        long timeMillis = System.currentTimeMillis();
        JSONObject jsonObject = NodeForward.requestData(getNode(), NodeUrl.Status, getRequest(), JSONObject.class);
        if (jsonObject == null) {
            return JsonMessage.getString(500, "获取信息失败");
        }
        JSONArray jsonArray = new JSONArray();
        jsonObject.put("timeOut", System.currentTimeMillis() - timeMillis);
        jsonArray.add(jsonObject);
        return JsonMessage.getString(200, "", jsonArray);
    }
}
