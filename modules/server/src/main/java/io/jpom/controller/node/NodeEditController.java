package io.jpom.controller.node;

import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.common.BaseServerController;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.data.NodeModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.build.BuildService;
import io.jpom.service.monitor.MonitorService;
import io.jpom.service.node.OutGivingServer;
import io.jpom.service.node.ssh.SshService;
import io.jpom.service.user.UserService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

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
    private UserService userService;
    @Resource
    private OutGivingServer outGivingServer;
    @Resource
    private MonitorService monitorService;
    @Resource
    private BuildService buildService;
    @Resource
    private SshService sshService;

//    @RequestMapping(value = "edit.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.EDIT)
//    public String edit(String id) {
//        setAttribute("type", "add");
//        if (StrUtil.isNotEmpty(id)) {
//            NodeModel nodeModel = nodeService.getItem(id);
//            if (nodeModel != null) {
//                setAttribute("item", nodeModel);
//                setAttribute("type", "edit");
//            }
//        }
//        // group
//        HashSet<String> allGroup = nodeService.getAllGroup();
//        setAttribute("groups", allGroup);
//        JSONArray sshList = sshService.listSelect(id);
//        setAttribute("sshList", sshList);
//        //监控周期
//        JSONArray cycleArray = Cycle.getAllJSONArray();
//        setAttribute("cycleArray", cycleArray);
//        return "node/edit";
//    }

    @RequestMapping(value = "save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @OptLog(UserOperateLogV1.OptType.EditNode)
    @ResponseBody
    @Feature(method = MethodFeature.EDIT)
    public String save(String type) {
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
    @OptLog(UserOperateLogV1.OptType.DelNode)
    @ResponseBody
    @Feature(method = MethodFeature.DEL)
    public String del(String id) {
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
        //        List<UserModel> list = userService.list();
        //        if (list != null) {
        //            list.forEach(userModel -> {
        //                userService.updateItem(userModel);
        //            });
        //        }
        return JsonMessage.getString(200, "操作成功");
    }
}
