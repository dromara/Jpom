package cn.keepbx.jpom.common;

import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.service.node.NodeService;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @author jiangzeyin
 * @date 2019/4/16
 */
public abstract class BaseNodeController extends BaseController {
    @Resource
    protected NodeService nodeService;

    protected NodeModel getNode() {
        String nodeId = getParameter("nodeId");
        NodeModel nodeModel = nodeService.getItem(nodeId);
        Objects.requireNonNull(nodeModel);
        return nodeModel;
    }
}
