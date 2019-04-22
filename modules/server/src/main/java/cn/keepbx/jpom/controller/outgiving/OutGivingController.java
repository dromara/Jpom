package cn.keepbx.jpom.controller.outgiving;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.Role;
import cn.keepbx.jpom.common.interceptor.UrlPermission;
import cn.keepbx.jpom.model.data.*;
import cn.keepbx.jpom.service.node.OutGivingServer;
import cn.keepbx.jpom.service.system.ServerWhitelistServer;
import cn.keepbx.jpom.util.JsonFileUtil;
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
public class OutGivingController extends BaseServerController {
    @Resource
    private OutGivingServer outGivingServer;

    @Resource
    private ServerWhitelistServer serverWhitelistServer;

    @RequestMapping(value = "list.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String list() throws IOException {
        List<OutGivingModel> outGivingModels = outGivingServer.list();
        setAttribute("array", outGivingModels);
        //
        List<NodeModel> nodeModels = nodeService.listAndProject();
        JSONObject jsonObject = new JSONObject();
        nodeModels.forEach(nodeModel -> {
            JSONObject jsonObject1 = nodeModel.toJson();
            JSONObject jsonObject2 = JsonFileUtil.arrayToObjById(nodeModel.getProjects());
            jsonObject1.put("projects", jsonObject2);
            jsonObject.put(nodeModel.getId(), jsonObject1);
        });
        setAttribute("nodeData", jsonObject);
        //
        ServerWhitelist serverWhitelist = serverWhitelistServer.getWhitelist();
        if (serverWhitelist != null) {
            List<String> whiteList = serverWhitelist.getOutGiving();
            String strWhiteList = AgentWhitelist.convertToLine(whiteList);
            setAttribute("whiteList", strWhiteList);
        }
        return "outgiving/list";
    }

    @RequestMapping(value = "edit.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
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
        if (userModel.isServerManager()) {
            List<NodeModel> nodeModels = nodeService.listAndProject();
            setAttribute("nodeModels", nodeModels);

            //
            String reqId = nodeService.cacheNodeList(nodeModels);
            setAttribute("reqId", reqId);

        }
        return "outgiving/edit";
    }

    @RequestMapping(value = "save", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @UrlPermission(value = Role.ServerManager, optType = UserOperateLogV1.OptType.SaveOutGiving)
    public String save(String type, String id) throws IOException {
        if ("add".equalsIgnoreCase(type)) {
            if (!Validator.isGeneral(id, 2, 20)) {
                return JsonMessage.getString(401, "分发id 不能为空并且长度在2-20");
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
        List<OutGivingModel.NodeProject> nodeProjects = new ArrayList<>();
        OutGivingModel.NodeProject nodeProject;
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
                    if (outGivingModel1.checkContains(trueProjectId)) {
                        return JsonMessage.getString(405, "已经存在相同的分发项目:" + trueProjectId);
                    }
                }
            }
            nodeProject = outGivingModel.getNodeProject(nodeModel.getId(), trueProjectId);
            if (nodeProject == null) {
                nodeProject = new OutGivingModel.NodeProject();
            }
            nodeProject.setNodeId(nodeModel.getId());
            nodeProject.setProjectId(trueProjectId);
            nodeProjects.add(nodeProject);
        }
        if (nodeProjects.size() < 2) {
            return JsonMessage.getString(405, "至少选择2个节点项目");
        }
        outGivingModel.setNodeProjectList(nodeProjects);
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
    @UrlPermission(value = Role.ServerManager, optType = UserOperateLogV1.OptType.DelOutGiving)
    public String del(String id) {
        boolean flag = outGivingServer.deleteItem(id);
        if (!flag) {
            return JsonMessage.getString(405, "删除失败");
        }
        return JsonMessage.getString(200, "操作成功");
    }
}
