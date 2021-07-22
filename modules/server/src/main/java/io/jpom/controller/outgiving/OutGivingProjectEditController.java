package io.jpom.controller.outgiving;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpStatus;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.AfterOpt;
import io.jpom.model.BaseEnum;
import io.jpom.model.RunMode;
import io.jpom.model.data.*;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.node.OutGivingServer;
import io.jpom.service.system.ServerWhitelistServer;
import io.jpom.util.StringUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.*;

/**
 * 节点分发编辑项目
 *
 * @author jiangzeyin
 * @date 2019/4/22
 */
@Controller
@RequestMapping(value = "/outgiving")
@Feature(cls = ClassFeature.OUTGIVING)
public class OutGivingProjectEditController extends BaseServerController {
    @Resource
    private ServerWhitelistServer serverWhitelistServer;

    @Resource
    private OutGivingServer outGivingServer;

//    @RequestMapping(value = "editProject", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.EDIT)
//    public String editProject(String id) {
//        setAttribute("type", "add");
//        OutGivingModel outGivingModel;
//        if (StrUtil.isNotEmpty(id)) {
//            outGivingModel = outGivingServer.getItem(id);
//            if (outGivingModel != null) {
//                setAttribute("item", outGivingModel);
//                setAttribute("type", "edit");
//            }
//        }
//        // 运行模式
//        JSONArray runModes = (JSONArray) JSONArray.toJSON(RunMode.values());
//        runModes.remove(RunMode.File.name());
//        //
//        setAttribute("runModes", runModes);
//        //
//        JSONArray afterOpt = BaseEnum.toJSONArray(AfterOpt.class);
//        setAttribute("afterOpt", afterOpt);
//        // 权限
//        List<NodeModel> nodeModels = nodeService.list();
//        setAttribute("nodeModels", nodeModels);
//        //
//        String reqId = nodeService.cacheNodeList(nodeModels);
//        setAttribute("reqId", reqId);
//
//        // 白名单
//        List<String> jsonArray = serverWhitelistServer.getOutGiving();
//        setAttribute("whitelistDirectory", jsonArray);
//        return "outgiving/editProject";
//    }

    /**
     * 保存节点分发项目
     *
     * @param id   id
     * @param type 类型
     * @return json
     */
    @RequestMapping(value = "save_project", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @OptLog(UserOperateLogV1.OptType.SaveOutgivingProject)
    @Feature(method = MethodFeature.EDIT)
    public String save(String id, String type) {
        if ("add".equalsIgnoreCase(type)) {
            if (!StringUtil.isGeneral(id, 2, 20)) {
                return JsonMessage.getString(401, "分发id 不能为空并且长度在2-20（英文字母 、数字和下划线）");
            }
            return addOutGiving(id);
        } else {
            return updateGiving(id);
        }
    }

    /**
     * 删除分发项目
     *
     * @param id 项目id
     * @return json
     */
    @RequestMapping(value = "delete_project", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @OptLog(UserOperateLogV1.OptType.DeleteOutgivingProject)
    @Feature(method = MethodFeature.DEL)
    public String delete(String id) {
        OutGivingModel outGivingModel = outGivingServer.getItem(id);
        if (outGivingModel == null) {
            return JsonMessage.getString(200, "没有对应的分发项目");
        }
        if (!outGivingModel.isOutGivingProject()) {
            return JsonMessage.getString(405, "改项目不是节点分发项目,不能在此次删除");
        }
        UserModel userModel = getUser();
        List<OutGivingNodeProject> deleteNodeProject = outGivingModel.getOutGivingNodeProjectList();
        if (deleteNodeProject != null) {
            // 删除实际的项目
            for (OutGivingNodeProject outGivingNodeProject1 : deleteNodeProject) {
                NodeModel nodeModel = outGivingNodeProject1.getNodeData(true);
                JsonMessage<String> jsonMessage = deleteNodeProject(nodeModel, userModel, outGivingNodeProject1.getProjectId());
                if (jsonMessage.getCode() != HttpStatus.HTTP_OK) {
                    return JsonMessage.getString(406, nodeModel.getName() + "节点失败：" + jsonMessage.getMsg());
                }
            }
        }
        outGivingServer.deleteItem(id);
        return JsonMessage.getString(200, "删除成功");
    }

    private String addOutGiving(String id) {
        OutGivingModel outGivingModel = outGivingServer.getItem(id);
        if (outGivingModel != null) {
            return JsonMessage.getString(405, "分发id已经存在啦");
        }
        outGivingModel = new OutGivingModel();
        outGivingModel.setOutGivingProject(true);
        outGivingModel.setId(id);
        //
        String error = doData(outGivingModel, false);
        if (error != null) {
            return error;
        }
        outGivingServer.addItem(outGivingModel);
        error = saveNodeData(outGivingModel, false);
        if (error != null) {
            return error;
        }
        return JsonMessage.getString(200, "添加成功");
    }


    private String updateGiving(String id) {
        OutGivingModel outGivingModel = outGivingServer.getItem(id);
        if (outGivingModel == null) {
            return JsonMessage.getString(405, "没有找到对应的分发id");
        }
        String error = doData(outGivingModel, true);
        if (error != null) {
            return error;
        }
        outGivingServer.updateItem(outGivingModel);
        error = saveNodeData(outGivingModel, true);
        if (error != null) {
            return error;
        }
        return JsonMessage.getString(200, "修改成功");
    }

    /**
     * 保存节点项目数据
     *
     * @param outGivingModel 节点分发项目
     * @param edit           是否为编辑模式
     * @return 错误信息
     */
    private String saveNodeData(OutGivingModel outGivingModel, boolean edit) {
        Map<NodeModel, JSONObject> map = outGivingModel.getTempCacheMap();
        if (map == null) {
            if (!edit) {
                outGivingServer.deleteItem(outGivingModel.getId());
            }
            return JsonMessage.getString(405, "数据异常,请重新操作");
        }
        UserModel userModel = getUser();
        List<Map.Entry<NodeModel, JSONObject>> success = new ArrayList<>();
        boolean fail = false;
        try {
            Set<Map.Entry<NodeModel, JSONObject>> entries = map.entrySet();
            JsonMessage<String> jsonMessage;
            for (Map.Entry<NodeModel, JSONObject> entry : entries) {
                NodeModel nodeModel = entry.getKey();
                jsonMessage = sendData(nodeModel, userModel, entry.getValue(), true);
                if (jsonMessage.getCode() != HttpStatus.HTTP_OK) {
                    if (!edit) {
                        fail = true;
                        outGivingServer.deleteItem(outGivingModel.getId());
                    }
                    return JsonMessage.getString(406, nodeModel.getName() + "节点失败：" + jsonMessage.getMsg());
                }
                success.add(entry);
            }
        } catch (Exception e) {
            DefaultSystemLog.getLog().error("保存分发项目失败", e);
            if (!edit) {
                fail = true;
                outGivingServer.deleteItem(outGivingModel.getId());
            }
            return JsonMessage.getString(500, "保存节点数据失败:" + e.getMessage());
        } finally {
            if (fail) {
                try {
                    for (Map.Entry<NodeModel, JSONObject> entry : success) {
                        deleteNodeProject(entry.getKey(), userModel, outGivingModel.getId());
                    }
                } catch (Exception e) {
                    DefaultSystemLog.getLog().error("还原项目失败", e);
                }
            }
        }
        return null;
    }

    /**
     * 删除项目
     *
     * @param nodeModel 节点
     * @param userModel 用户
     * @param project   判断id
     * @return json
     */
    private JsonMessage<String> deleteNodeProject(NodeModel nodeModel, UserModel userModel, String project) {
        JSONObject data = new JSONObject();
        data.put("id", project);
        return NodeForward.request(nodeModel, NodeUrl.Manage_DeleteProject, userModel, data);
//        // 发起预检查数据
//        String url = nodeModel.getRealUrl(NodeUrl.Manage_DeleteProject);
//        HttpRequest request = HttpUtil.createPost(url);
//        // 授权信息
//        NodeForward.addUser(request, nodeModel, userModel);

//        request.form(data);
//        //
//        String body = request.execute()
//                .body();
//        return NodeForward.toJsonMessage(body);
    }

    /**
     * 创建项目管理的默认数据
     *
     * @param outGivingModel 分发实体
     * @param edit           是否为编辑模式
     * @return String为有异常
     */
    private Object getDefData(OutGivingModel outGivingModel, boolean edit) {
        JSONObject defData = new JSONObject();
        defData.put("id", outGivingModel.getId());
        defData.put("name", outGivingModel.getName());
        //
        // 运行模式
        String runMode = getParameter("runMode");
        RunMode runMode1 = RunMode.ClassPath;
        try {
            runMode1 = RunMode.valueOf(runMode);
        } catch (Exception ignored) {
        }
        defData.put("runMode", runMode1.name());
        if (runMode1 == RunMode.ClassPath || runMode1 == RunMode.JavaExtDirsCp) {
            String mainClass = getParameter("mainClass");
            defData.put("mainClass", mainClass);
        }
        if (runMode1 == RunMode.JavaExtDirsCp) {
            defData.put("javaExtDirsCp", getParameter("javaExtDirsCp"));
        }
        String whitelistDirectory = getParameter("whitelistDirectory");
        List<String> whitelistServerOutGiving = serverWhitelistServer.getOutGiving();
        if (!AgentWhitelist.checkPath(whitelistServerOutGiving, whitelistDirectory)) {
            return JsonMessage.getString(401, "请选择正确的项目路径,或者还没有配置白名单");
        }
        defData.put("whitelistDirectory", whitelistDirectory);
        String lib = getParameter("lib");
        defData.put("lib", lib);
        defData.put("group", "节点分发");
        if (edit) {
            // 编辑模式
            defData.put("edit", "on");
        }
        defData.put("previewData", true);
        return defData;
    }

    /**
     * 处理页面数据
     *
     * @param outGivingModel 分发实体
     * @param edit           是否为编辑模式
     * @return json
     */
    private String doData(OutGivingModel outGivingModel, boolean edit) {
        outGivingModel.setName(getParameter("name"));
        if (StrUtil.isEmpty(outGivingModel.getName())) {
            return JsonMessage.getString(405, "分发名称不能为空");
        }
        String reqId = getParameter("reqId");
        List<NodeModel> nodeModelList = nodeService.getNodeModel(reqId);
        if (nodeModelList == null) {
            return JsonMessage.getString(401, "当前页面请求超时");
        }
        //
        String afterOpt = getParameter("afterOpt");
        AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, Convert.toInt(afterOpt, 0));
        if (afterOpt1 == null) {
            return JsonMessage.getString(400, "请选择分发后的操作");
        }
        outGivingModel.setAfterOpt(afterOpt1.getCode());
        Object object = getDefData(outGivingModel, edit);
        if (object instanceof String) {
            return object.toString();
        }
        JSONObject defData = (JSONObject) object;
        UserModel userModel = getUser();
        //
        List<OutGivingModel> outGivingModels = outGivingServer.list();
        List<OutGivingNodeProject> outGivingNodeProjects = new ArrayList<>();
        OutGivingNodeProject outGivingNodeProject;
        //
        Iterator<NodeModel> iterator = nodeModelList.iterator();
        Map<NodeModel, JSONObject> cache = new HashMap<>(nodeModelList.size());
        while (iterator.hasNext()) {
            NodeModel nodeModel = iterator.next();
            String add = getParameter("add_" + nodeModel.getId());
            if (!nodeModel.getId().equals(add)) {
                iterator.remove();
                continue;
            }
            // 判断项目是否已经被使用过啦
            if (outGivingModels != null) {
                for (OutGivingModel outGivingModel1 : outGivingModels) {
                    if (outGivingModel1.getId().equalsIgnoreCase(outGivingModel.getId())) {
                        continue;
                    }
                    if (outGivingModel1.checkContains(nodeModel.getId(), outGivingModel.getId())) {
                        return JsonMessage.getString(405, "已经存在相同的分发项目:" + outGivingModel.getId());
                    }
                }
            }
            outGivingNodeProject = outGivingModel.getNodeProject(nodeModel.getId(), outGivingModel.getId());
            if (outGivingNodeProject == null) {
                outGivingNodeProject = new OutGivingNodeProject();
            }
            outGivingNodeProject.setNodeId(nodeModel.getId());
            outGivingNodeProject.setProjectId(outGivingModel.getId());
            outGivingNodeProjects.add(outGivingNodeProject);
            // 检查数据
            JSONObject allData = (JSONObject) defData.clone();
            String token = getParameter(StrUtil.format("{}_token", nodeModel.getId()));
            allData.put("token", token);
            String jvm = getParameter(StrUtil.format("{}_jvm", nodeModel.getId()));
            allData.put("jvm", jvm);
            String args = getParameter(StrUtil.format("{}_args", nodeModel.getId()));
            allData.put("args", args);
            // 项目副本
            String javaCopyIds = getParameter(StrUtil.format("{}_javaCopyIds", nodeModel.getId()));
            allData.put("javaCopyIds", javaCopyIds);
            if (StrUtil.isNotEmpty(javaCopyIds)) {
                String[] split = StrUtil.splitToArray(javaCopyIds, StrUtil.COMMA);
                for (String copyId : split) {
                    String copyJvm = getParameter(StrUtil.format("{}_jvm_{}", nodeModel.getId(), copyId));
                    String copyArgs = getParameter(StrUtil.format("{}_args_{}", nodeModel.getId(), copyId));
                    allData.put("jvm_" + copyId, copyJvm);
                    allData.put("args_" + copyId, copyArgs);
                }
            }
            JsonMessage<String> jsonMessage = sendData(nodeModel, userModel, allData, false);
            if (jsonMessage.getCode() != HttpStatus.HTTP_OK) {
                return JsonMessage.getString(406, nodeModel.getName() + "节点失败：" + jsonMessage.getMsg());
            }
            cache.put(nodeModel, allData);
        }
        // 删除已经删除的项目
        String error = deleteProject(outGivingModel, outGivingNodeProjects, userModel);
        if (error != null) {
            return error;
        }
        outGivingModel.setOutGivingNodeProjectList(outGivingNodeProjects);
        outGivingModel.setTempCacheMap(cache);
        return null;
    }

    /**
     * 删除已经删除过的项目
     *
     * @param outGivingModel        分发项目
     * @param outGivingNodeProjects 新的节点项目
     * @param userModel             用户
     * @return 错误信息
     */
    private String deleteProject(OutGivingModel outGivingModel, List<OutGivingNodeProject> outGivingNodeProjects, UserModel userModel) {
        if (outGivingNodeProjects.size() < 2) {
            return JsonMessage.getString(406, "至少选择两个节点及以上");
        }
        // 删除
        List<OutGivingNodeProject> deleteNodeProject = outGivingModel.getDelete(outGivingNodeProjects);
        if (deleteNodeProject != null) {
            JsonMessage jsonMessage;
            // 删除实际的项目
            for (OutGivingNodeProject outGivingNodeProject1 : deleteNodeProject) {
                NodeModel nodeModel = outGivingNodeProject1.getNodeData(true);
                jsonMessage = deleteNodeProject(nodeModel, userModel, outGivingNodeProject1.getProjectId());
                if (jsonMessage.getCode() != HttpStatus.HTTP_OK) {
                    return JsonMessage.getString(406, nodeModel.getName() + "节点失败：" + jsonMessage.getMsg());
                }
            }
        }
        return null;
    }

    private JsonMessage<String> sendData(NodeModel nodeModel, UserModel userModel, JSONObject data, boolean save) {
        if (save) {
            data.remove("previewData");
        }
        data.put("outGivingProject", true);
        // 发起预检查数据
        return NodeForward.request(nodeModel, NodeUrl.Manage_SaveProject, userModel, data);
    }
}
