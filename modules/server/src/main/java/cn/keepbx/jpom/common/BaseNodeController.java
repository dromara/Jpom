package cn.keepbx.jpom.common;

import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.service.node.NodeService;

import javax.annotation.Resource;

/**
 * @author jiangzeyin
 * @date 2019/4/16
 */
public abstract class BaseNodeController extends BaseController {
    @Resource
    protected NodeService nodeService;

    protected NodeModel getNode() {
        String nodeId = getParameter("nodeId");
        return nodeService.getItem(nodeId);
    }
}
