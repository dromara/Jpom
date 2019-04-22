package cn.keepbx.jpom.controller.node;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.model.Role;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.common.interceptor.UrlPermission;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.model.data.UserOperateLogV1;
import cn.keepbx.jpom.model.system.JpomManifest;
import cn.keepbx.jpom.service.node.NodeService;
import cn.keepbx.jpom.service.user.UserService;
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
public class NodeEditController extends BaseServerController {

    @Resource
    private NodeService nodeService;
    @Resource
    private UserService userService;

    @RequestMapping(value = "edit.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String edit(String id) {
        setAttribute("type", "add");
        if (StrUtil.isNotEmpty(id)) {
            UserModel userModel = getUser();
            NodeModel nodeModel = nodeService.getItem(id);
            if (nodeModel != null && userModel.isSystemUser()) {
                setAttribute("item", nodeModel);
                setAttribute("type", "edit");
            }
        }
        return "node/edit";
    }

    @RequestMapping(value = "save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @UrlPermission(value = Role.System, optType = UserOperateLogV1.OptType.EditNode)
    @ResponseBody
    public String save(NodeModel model, String type) throws Exception {
        if ("add".equalsIgnoreCase(type)) {
            return addNode(model);
        } else {
            return updateNode(model);
        }
    }

    private String addNode(NodeModel nodeModel) {
        if (!Validator.isGeneral(nodeModel.getId(), 2, 20)) {
            return JsonMessage.getString(405, "节点id不能为空并且2-20（英文字母 、数字和下划线）");
        }
        if (nodeService.getItem(nodeModel.getId()) != null) {
            return JsonMessage.getString(405, "节点id已经存在啦");
        }
        String error = checkData(nodeModel);
        if (error != null) {
            return error;
        }
        JpomManifest jpomManifest = NodeForward.requestData(nodeModel, NodeUrl.Info, getRequest(), JpomManifest.class);
        if (jpomManifest == null) {
            return JsonMessage.getString(204, "节点连接失败，请检查节点是否在线");
        }
        nodeService.addItem(nodeModel);
        return JsonMessage.getString(200, "操作成功");
    }

    private String updateNode(NodeModel nodeModel) throws Exception {
        NodeModel exit = nodeService.getItem(nodeModel.getId());
        if (exit == null) {
            return JsonMessage.getString(405, "节点不存在");
        }
        String error = checkData(nodeModel);
        if (error != null) {
            return error;
        }
        nodeService.updateItem(nodeModel);
        return JsonMessage.getString(200, "操作成功");
    }

    private String checkData(NodeModel nodeModel) {
        if (StrUtil.isEmpty(nodeModel.getName())) {
            return JsonMessage.getString(405, "节点名称 不能为空");
        }
        List<NodeModel> list = nodeService.list();
        if (list != null) {
            for (NodeModel model : list) {
                if (model.getUrl().equalsIgnoreCase(nodeModel.getUrl()) && !model.getId().equalsIgnoreCase(nodeModel.getId())) {
                    return JsonMessage.getString(405, "已经存在相同的节点地址啦");
                }
            }
        }
        return null;
    }

    /**
     * 删除节点
     *
     * @param id 节点id
     * @return json
     */
    @RequestMapping(value = "del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @UrlPermission(value = Role.System, optType = UserOperateLogV1.OptType.DelNode)
    @ResponseBody
    public String save(String id) {
        boolean flag = nodeService.deleteItem(id);
        if (!flag) {
            return JsonMessage.getString(405, "删除失败");
        }
        // 删除授权
        List<UserModel> list = userService.list();
        if (list != null) {
            list.forEach(userModel -> {
                userModel.removeNodeRole(id);
                userService.updateItem(userModel);
            });
        }
        return JsonMessage.getString(200, "操作成功");
    }
}
