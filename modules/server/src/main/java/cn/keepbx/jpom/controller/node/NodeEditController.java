package cn.keepbx.jpom.controller.node;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.interceptor.UrlPermission;
import cn.keepbx.jpom.model.Role;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.SshModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.model.log.UserOperateLogV1;
import cn.keepbx.jpom.service.build.BuildService;
import cn.keepbx.jpom.service.monitor.MonitorService;
import cn.keepbx.jpom.service.node.NodeService;
import cn.keepbx.jpom.service.node.OutGivingServer;
import cn.keepbx.jpom.service.node.ssh.SshService;
import cn.keepbx.jpom.service.user.UserService;
import cn.keepbx.plugin.ClassFeature;
import cn.keepbx.plugin.Feature;
import cn.keepbx.plugin.MethodFeature;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

/**
 * 节点管理
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
@Controller
@RequestMapping(value = "/node")
@Feature(cls = ClassFeature.NODE)
public class NodeEditController extends BaseServerController {

    @Resource
    private NodeService nodeService;
    @Resource
    private UserService userService;
    @Resource
    private OutGivingServer outGivingServer;
    @Resource
    private MonitorService monitorService;
    @Resource
    private BuildService buildService;
    @Resource
    private SshService sshService;

    @RequestMapping(value = "edit.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public String edit(String id) throws IOException {
        setAttribute("type", "add");
        if (StrUtil.isNotEmpty(id)) {
            UserModel userModel = getUser();
            NodeModel nodeModel = nodeService.getItem(id);
            if (nodeModel != null && userModel.isSystemUser()) {
                setAttribute("item", nodeModel);
                setAttribute("type", "edit");
            }
        }
        // 查询ssh
        List<SshModel> sshModels = sshService.list();
        List<NodeModel> list = nodeService.list();
        JSONArray sshList = new JSONArray();
        if (sshModels != null) {
            sshModels.forEach(sshModel -> {
                String sshModelId = sshModel.getId();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", sshModelId);
                jsonObject.put("name", sshModel.getName());
                if (list != null) {
                    for (NodeModel nodeModel : list) {
                        if (!StrUtil.equals(id, nodeModel.getId()) && StrUtil.equals(sshModelId, nodeModel.getSshId())) {
                            jsonObject.put("disabled", true);
                            break;
                        }
                    }
                }
                sshList.add(jsonObject);
            });
        }
        setAttribute("sshList", sshList);
        return "node/edit";
    }

    @RequestMapping(value = "save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @UrlPermission(value = Role.System, optType = UserOperateLogV1.OptType.EditNode)
    @ResponseBody
    @Feature(method = MethodFeature.EDIT)
    public String save(String type) throws Exception {
        NodeModel model = ServletUtil.toBean(getRequest(), NodeModel.class, true);
        if ("add".equalsIgnoreCase(type)) {
            return nodeService.addNode(model, getRequest());
        } else {
            return nodeService.updateNode(model);
        }
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
    @Feature(method = MethodFeature.DEL)
    public String del(String id) throws IOException {
        //  判断分发
        if (outGivingServer.checkNode(id)) {
            return JsonMessage.getString(400, "该节点存在分发项目，不能删除");
        }
        // 监控
        if (monitorService.checkNode(id)) {
            return JsonMessage.getString(400, "该节点存在监控项，不能删除");
        }
        if (buildService.checkNode(id)) {
            return JsonMessage.getString(400, "该节点存在构建项，不能删除");
        }
        nodeService.deleteItem(id);
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
