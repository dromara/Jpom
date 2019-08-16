package cn.keepbx.jpom.controller.outgiving;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.common.interceptor.OptLog;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.OutGivingModel;
import cn.keepbx.jpom.model.data.OutGivingNodeProject;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.model.log.UserOperateLogV1;
import cn.keepbx.jpom.service.build.BuildService;
import cn.keepbx.jpom.service.node.OutGivingServer;
import cn.keepbx.plugin.ClassFeature;
import cn.keepbx.plugin.Feature;
import cn.keepbx.plugin.MethodFeature;
import cn.keepbx.util.StringUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 分发控制
 *
 * @author jiangzeyin
 * @date 2019/4/20
 */
@Controller
@RequestMapping(value = "/outgiving")
@Feature(cls = ClassFeature.OUTGIVING)
public class OutGivingController extends BaseServerController {
    @Resource
    private OutGivingServer outGivingServer;
    @Resource
    private BuildService buildService;

    @RequestMapping(value = "list.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @Feature(method = MethodFeature.LIST)
    public String list() throws IOException {
        List<OutGivingModel> outGivingModels = outGivingServer.list();
        setAttribute("array", outGivingModels);
        return "outgiving/list";
    }

    @RequestMapping(value = "edit.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public String edit(String id) throws IOException {
        setAttribute("type", "add");
        if (StrUtil.isNotEmpty(id)) {
            OutGivingModel outGivingModel = outGivingServer.getItem(id);
            if (outGivingModel != null) {
                setAttribute("item", outGivingModel);
                setAttribute("type", "edit");
            }
        }
        UserModel userModel = getUser();

        List<NodeModel> nodeModels = nodeService.listAndProject();
        setAttribute("nodeModels", nodeModels);

        //
        String reqId = nodeService.cacheNodeList(nodeModels);
        setAttribute("reqId", reqId);

        return "outgiving/edit";
    }

    @RequestMapping(value = "save", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @OptLog(UserOperateLogV1.OptType.SaveOutGiving)
    @Feature(method = MethodFeature.EDIT)
    public String save(String type, String id) throws IOException {
        if ("add".equalsIgnoreCase(type)) {
            if (!StringUtil.isGeneral(id, 2, 20)) {
                return JsonMessage.getString(401, "分发id 不能为空并且长度在2-20（英文字母 、数字和下划线）");
            }
            return addOutGiving(id);
        } else {
            return updateGiving(id);
        }
    }

    private String addOutGiving(String id) throws IOException {
        OutGivingModel outGivingModel = outGivingServer.getItem(id);
        if (outGivingModel != null) {
            return JsonMessage.getString(405, "分发id已经存在啦");
        }
        outGivingModel = new OutGivingModel();
        outGivingModel.setId(id);
        //
        String error = doData(outGivingModel);
        if (error == null) {
            outGivingServer.addItem(outGivingModel);
            return JsonMessage.getString(200, "添加成功");
        }
        return error;
    }

    private String updateGiving(String id) throws IOException {
        OutGivingModel outGivingModel = outGivingServer.getItem(id);
        if (outGivingModel == null) {
            return JsonMessage.getString(405, "没有找到对应的分发id");
        }
        String error = doData(outGivingModel);
        if (error == null) {
            outGivingServer.updateItem(outGivingModel);
            return JsonMessage.getString(200, "修改成功");
        }
        return error;
    }

    private String doData(OutGivingModel outGivingModel) throws IOException {
        outGivingModel.setName(getParameter("name"));
        if (StrUtil.isEmpty(outGivingModel.getName())) {
            return JsonMessage.getString(405, "分发名称不能为空");
        }
        String reqId = getParameter("reqId");
        List<NodeModel> list = nodeService.getNodeModel(reqId);
        if (list == null) {
            return JsonMessage.getString(401, "页面请求超时");
        }
        List<OutGivingModel> outGivingModels = outGivingServer.list();
        //
        List<OutGivingNodeProject> outGivingNodeProjects = new ArrayList<>();
        OutGivingNodeProject outGivingNodeProject;
        for (NodeModel nodeModel : list) {
            String nodeIdProject = getParameter("node_" + nodeModel.getId());
            if (StrUtil.isEmpty(nodeIdProject)) {
                continue;
            }
            String trueProjectId = null;
            JSONArray jsonArray = nodeModel.getProjects();
            for (Object o : jsonArray) {
                JSONObject data = (JSONObject) o;
                String id1 = data.getString("id");
                if (id1.equals(nodeIdProject)) {
                    trueProjectId = nodeIdProject;
                    break;
                }
            }
            if (trueProjectId == null) {
                return JsonMessage.getString(405, "没有找到对应的项目id:" + nodeIdProject);
            }
            // 判断项目是否已经被使用过啦
            if (outGivingModels != null) {
                for (OutGivingModel outGivingModel1 : outGivingModels) {
                    if (outGivingModel1.getId().equalsIgnoreCase(outGivingModel.getId())) {
                        continue;
                    }
                    if (outGivingModel1.checkContains(nodeModel.getId(), trueProjectId)) {
                        return JsonMessage.getString(405, "已经存在相同的分发项目:" + trueProjectId);
                    }
                }
            }
            outGivingNodeProject = outGivingModel.getNodeProject(nodeModel.getId(), trueProjectId);
            if (outGivingNodeProject == null) {
                outGivingNodeProject = new OutGivingNodeProject();
            }
            outGivingNodeProject.setNodeId(nodeModel.getId());
            outGivingNodeProject.setProjectId(trueProjectId);
            outGivingNodeProjects.add(outGivingNodeProject);
        }
        if (outGivingNodeProjects.size() < 2) {
            return JsonMessage.getString(405, "至少选择2个节点项目");
        }
        outGivingModel.setOutGivingNodeProjectList(outGivingNodeProjects);
        return null;
    }

    /**
     * 删除分发信息
     *
     * @param id 分发id
     * @return json
     */
    @RequestMapping(value = "del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @OptLog(UserOperateLogV1.OptType.DelOutGiving)
    @Feature(method = MethodFeature.DEL)
    public String del(String id) throws IOException {
        // 判断构建
        if (buildService.checkOutGiving(id)) {
            return JsonMessage.getString(400, "当前分发存在构建项，不能删除");
        }
        OutGivingModel outGivingServerItem = outGivingServer.getItem(id);
        if (outGivingServerItem.isOutGivingProject()) {
            UserModel userModel = getUser();
            // 解除项目分发独立分发属性
            List<OutGivingNodeProject> outGivingNodeProjectList = outGivingServerItem.getOutGivingNodeProjectList();
            if (outGivingNodeProjectList != null) {
                outGivingNodeProjectList.forEach(outGivingNodeProject -> {
                    NodeModel item = nodeService.getItem(outGivingNodeProject.getNodeId());
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("id", outGivingNodeProject.getProjectId());
                    NodeForward.request(item, NodeUrl.Manage_ReleaseOutGiving, userModel, jsonObject);
                });
            }
        }
        outGivingServer.deleteItem(id);
        return JsonMessage.getString(200, "操作成功");
    }
}
