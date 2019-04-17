package cn.keepbx.jpom.controller.node;

import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.common.BaseNodeController;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.system.JpomManifest;
import cn.keepbx.jpom.service.node.NodeService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

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
public class NodeIndexController extends BaseNodeController {

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
        JpomManifest jpomManifest = NodeForward.requestData(getNode(), NodeUrl.Info, getRequest(), JpomManifest.class);
        setAttribute("jpomManifest", jpomManifest);
        return "node/index";
    }

    @RequestMapping(value = "edit.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String edit(String id) {
        setAttribute("type", "add");
        if (StrUtil.isNotEmpty(id)) {
            NodeModel nodeModel = nodeService.getItem(id);
            if (nodeModel != null) {
                setAttribute("item", nodeModel);
                setAttribute("type", "edit");
            }
        }
        return "node/edit";
    }
}
