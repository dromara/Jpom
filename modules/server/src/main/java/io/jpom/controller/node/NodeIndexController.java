package io.jpom.controller.node;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpStatus;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.JpomManifest;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.AgentFileModel;
import io.jpom.model.data.NodeModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.permission.SystemPermission;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.node.AgentFileService;
import io.jpom.service.node.ssh.SshService;
import io.jpom.system.ServerConfigBean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 节点管理
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
@Controller
@RequestMapping(value = "/node")
@Feature(cls = ClassFeature.NODE)
public class NodeIndexController extends BaseServerController {

    @Resource
    private SshService sshService;
    @Resource
    private AgentFileService agentFileService;

//    @RequestMapping(value = "list.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.LIST)
//    public String list(String group) {
//        List<NodeModel> nodeModels = this.listByGroup(group);
//        setAttribute("array", nodeModels);
//        // 获取所有的ssh 名称
//        JSONObject sshName = new JSONObject();
//        List<SshModel> sshModels = sshService.list();
//        if (sshModels != null) {
//            sshModels.forEach(sshModel -> sshName.put(sshModel.getId(), sshModel.getName()));
//        }
//        setAttribute("sshName", sshName);
//        // group
//        HashSet<String> allGroup = nodeService.getAllGroup();
//        setAttribute("groups", allGroup);
//        return "node/list";
//    }

    private List<NodeModel> listByGroup(String group) {
        List<NodeModel> nodeModels = nodeService.list();
        //
        if (nodeModels != null && StrUtil.isNotEmpty(group)) {
            // 筛选
            List<NodeModel> filterList = nodeModels.stream().filter(nodeModel -> StrUtil.equals(group, nodeModel.getGroup())).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(filterList)) {
                // 如果传入的分组找到了节点，就返回  否则返回全部
                nodeModels = filterList;
            }
        }
        return nodeModels;
    }


    @RequestMapping(value = "list_data.json", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    @ResponseBody
    public String listJson(String group) {
        List<NodeModel> nodeModels = this.listByGroup(group);
        return JsonMessage.getString(200, "", nodeModels);
    }


    @RequestMapping(value = "list_group.json", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    @ResponseBody
    public String listAllGroup() {
        HashSet<String> allGroup = nodeService.getAllGroup();
        return JsonMessage.getString(200, "", allGroup);
    }


//    @RequestMapping(value = "index.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    public String index() {
//        List<NodeModel> nodeModels = nodeService.list();
//        setAttribute("array", nodeModels);
//        //
//        JsonMessage<JpomManifest> jsonMessage = NodeForward.request(getNode(), getRequest(), NodeUrl.Info);
//        JpomManifest jpomManifest = jsonMessage.getData(JpomManifest.class);
//        setAttribute("jpomManifest", jpomManifest);
//        setAttribute("installed", jsonMessage.getCode() == 200);
//        UserModel userModel = getUser();
//        // 版本提示
//        if (!JpomManifest.getInstance().isDebug() && jpomManifest != null && userModel.isSystemUser()) {
//            JpomManifest thisInfo = JpomManifest.getInstance();
//            if (!StrUtil.equals(jpomManifest.getVersion(), thisInfo.getVersion())) {
//                setAttribute("tipUpdate", true);
//            }
//        }
//        return "node/index";
//    }

    @RequestMapping(value = "node_status", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.LIST)
    public String nodeStatus() {
        long timeMillis = System.currentTimeMillis();
        JSONObject jsonObject = NodeForward.requestData(getNode(), NodeUrl.Status, getRequest(), JSONObject.class);
        if (jsonObject == null) {
            return JsonMessage.getString(500, "获取信息失败");
        }
        JSONArray jsonArray = new JSONArray();
        jsonObject.put("timeOut", System.currentTimeMillis() - timeMillis);
        jsonArray.add(jsonObject);
        return JsonMessage.getString(200, "", jsonArray);
    }

    /**
     * @return
     * @author Hotstrip
     * load node project list
     * 加载节点项目列表
     */
    @RequestMapping(value = "node_project_list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String nodeProjectList() {
        List<NodeModel> nodeModels = nodeService.listAndProject();
        return JsonMessage.getString(200, "success", nodeModels);
    }

    @RequestMapping(value = "upload_agent", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @OptLog(UserOperateLogV1.OptType.UpdateSys)
    @SystemPermission
    public String uploadAgent() throws IOException {
        String saveDir = ServerConfigBean.getInstance().getAgentPath().getAbsolutePath();
        MultipartFileBuilder multipartFileBuilder = createMultipart();
        multipartFileBuilder
                .setFileExt("jar")
                .addFieldName("file")
                .setUseOriginalFilename(true)
                .setSavePath(saveDir);
        String path = multipartFileBuilder.save();
        // 基础检查
        JsonMessage<String> error = JpomManifest.checkJpomJar(path, "io.jpom.JpomAgentApplication", false);
        if (error.getCode() != HttpStatus.HTTP_OK) {
            FileUtil.del(path);
            return error.toString();
        }

        // 保存Agent文件
        String id = "agent";

        File file = new File(path);
        AgentFileModel agentFileModel = agentFileService.getItem(id);
        if (agentFileModel == null) {
            agentFileModel = new AgentFileModel();
            agentFileModel.setId(id);
            agentFileService.addItem(agentFileModel);
        }
        agentFileModel.setName(file.getName());
        agentFileModel.setSize(file.length());
        agentFileModel.setSavePath(path);
        agentFileModel.setVersion(error.getMsg());
        agentFileService.updateItem(agentFileModel);


        return JsonMessage.getString(200, "上传成功");
    }
}
