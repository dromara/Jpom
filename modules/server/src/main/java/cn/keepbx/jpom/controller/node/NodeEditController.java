package cn.keepbx.jpom.controller.node;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.common.Role;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.common.interceptor.UrlPermission;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.system.JpomManifest;
import cn.keepbx.jpom.service.node.NodeService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 节点管理
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
@RestController
@RequestMapping(value = "/node")
public class NodeEditController extends BaseController {

    @Resource
    private NodeService nodeService;

    @RequestMapping(value = "save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String save(NodeModel model, String type) throws Exception {
        if ("add".equalsIgnoreCase(type)) {
            return addNode(model);
        } else {
            nodeService.updateItem(model);
        }
        return JsonMessage.getString(200, "操作成功");
    }

    private String addNode(NodeModel nodeModel) {
        if (StrUtil.isEmpty(nodeModel.getId())) {
            return JsonMessage.getString(405, "节点id 不能为空");
        }
        if (StrUtil.isEmpty(nodeModel.getName())) {
            return JsonMessage.getString(405, "节点名称 不能为空");
        }
        if (nodeService.getItem(nodeModel.getId()) != null) {
            return JsonMessage.getString(405, "节点id已经存在啦");
        }
        JpomManifest jpomManifest = NodeForward.requestData(nodeModel, NodeUrl.Info, getRequest(), JpomManifest.class);
        if (jpomManifest == null) {
            return JsonMessage.getString(204, "节点连接失败，请检查节点是否在线");
        }
        nodeService.addItem(nodeModel);
        return JsonMessage.getString(200, "操作成功");
    }

    @RequestMapping(value = "del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @UrlPermission(Role.System)
    public String save(String id) {
        boolean flag = nodeService.deleteItem(id);
        if (!flag) {
            return JsonMessage.getString(405, "删除失败");
        }
        return JsonMessage.getString(200, "操作成功");
    }
}
