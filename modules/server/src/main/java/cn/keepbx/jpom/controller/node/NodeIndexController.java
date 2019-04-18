package cn.keepbx.jpom.controller.node;

import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.system.JpomManifest;
import cn.keepbx.jpom.service.node.NodeService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
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
    public String index(String nodeId) {
        NodeModel nodeModel = nodeService.getItem(nodeId);
        if (nodeModel != null) {
            setAttribute("node", nodeModel);
        }
        List<NodeModel> nodeModels = nodeService.list();
        setAttribute("array", nodeModels);
        //
        JsonMessage jsonMessage = NodeForward.request(getNode(), getRequest(), NodeUrl.Info);
        JpomManifest jpomManifest = NodeForward.toObj(jsonMessage, JpomManifest.class);
        setAttribute("jpomManifest", jpomManifest);
        setAttribute("installed", jsonMessage.getCode() == 200);
        return "node/index";
    }

    @RequestMapping(value = "node_status", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String nodeStatus() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Status).toString();
    }
}
