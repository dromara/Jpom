package io.jpom.controller.outgiving;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.AfterOpt;
import io.jpom.model.BaseEnum;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.OutGivingModel;
import io.jpom.model.data.OutGivingNodeProject;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.outgiving.OutGivingRun;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.node.OutGivingServer;
import io.jpom.service.node.manage.ProjectInfoService;
import io.jpom.system.ConfigBean;
import io.jpom.system.ServerConfigBean;
import io.jpom.util.StringUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * 分发文件管理
 *
 * @author jiangzeyin
 * @date 2019/4/21
 */
@Controller
@RequestMapping(value = "/outgiving")
@Feature(cls = ClassFeature.OUTGIVING)
public class OutGivingProjectController extends BaseServerController {
    @Resource
    private OutGivingServer outGivingServer;
    @Resource
    private ProjectInfoService projectInfoService;

    @RequestMapping(value = "getProjectStatus", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getProjectStatus() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_GetProjectStatus).toString();
    }


    @RequestMapping(value = "getItemData.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getItemData(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "id error") String id) throws IOException {
        OutGivingModel outGivingServerItem = outGivingServer.getItem(id);
        Objects.requireNonNull(outGivingServerItem, "没有数据");
        List<OutGivingNodeProject> outGivingNodeProjectList = outGivingServerItem.getOutGivingNodeProjectList();
        JSONArray jsonArray = new JSONArray();
        outGivingNodeProjectList.forEach(outGivingNodeProject -> {
            NodeModel nodeModel = nodeService.getItem(outGivingNodeProject.getNodeId());
            JSONObject jsonObject = new JSONObject();
            JSONObject projectInfo = null;
            try {
                projectInfo = projectInfoService.getItem(nodeModel, outGivingNodeProject.getProjectId());
            } catch (Exception e) {
                jsonObject.put("errorMsg", "error " + e.getMessage());
            }

            jsonObject.put("nodeId", outGivingNodeProject.getNodeId());
            jsonObject.put("projectId", outGivingNodeProject.getProjectId());
            jsonObject.put("nodeName", nodeModel.getName());
            if (projectInfo != null) {
                jsonObject.put("projectName", projectInfo.getString("name"));
            }

            // set projectStatus property
            //NodeModel node = nodeService.getItem(outGivingNodeProject.getNodeId());
            // Project Status: data.pid > 0 means running
            JSONObject projectStatus = JsonMessage.toJson(200, "success");
            if (nodeModel.isOpenStatus()) {
                projectStatus = NodeForward.requestBySys(nodeModel, NodeUrl.Manage_GetProjectStatus, "id", outGivingNodeProject.getProjectId()).toJson();
            }
            if (projectStatus.getJSONObject("data") != null && projectStatus.getJSONObject("data").getInteger("pId") != null) {
                jsonObject.put("projectStatus", projectStatus.getJSONObject("data").getIntValue("pId") > 0);
            } else {
                jsonObject.put("projectStatus", false);
            }

            jsonObject.put("outGivingStatus", outGivingNodeProject.getStatusMsg());
            jsonObject.put("outGivingResult", outGivingNodeProject.getResult());
            jsonObject.put("lastTime", outGivingNodeProject.getLastOutGivingTime());
            jsonArray.add(jsonObject);
        });
        return JsonMessage.getString(200, "", jsonArray);
    }

//
//    @RequestMapping(value = "addOutgiving", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.UPLOAD)
//    public String addOutgiving(String id) {
//
//        JSONArray jsonArray = BaseEnum.toJSONArray(AfterOpt.class);
//        setAttribute("afterOpt", jsonArray);
//        //
//        OutGivingModel outGivingModel = outGivingServer.getItem(id);
//        setAttribute("outGivingModel", outGivingModel);
//        return "outgiving/addOutgiving";
//    }

    /**
     * 节点分发文件
     *
     * @param id       分发id
     * @param afterOpt 之后的操作
     * @return json
     * @throws IOException IO
     */
    @RequestMapping(value = "upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @OptLog(UserOperateLogV1.OptType.UploadOutGiving)
    @Feature(method = MethodFeature.UPLOAD)
    public String upload(String id, String afterOpt, String clearOld) throws IOException {
        OutGivingModel outGivingModel = outGivingServer.getItem(id);
        if (outGivingModel == null) {
            return JsonMessage.getString(400, "上传失败,没有找到对应的分发项目");
        }
        // 检查状态
        List<OutGivingNodeProject> outGivingNodeProjectList = outGivingModel.getOutGivingNodeProjectList();
        Objects.requireNonNull(outGivingNodeProjectList);
        for (OutGivingNodeProject outGivingNodeProject : outGivingNodeProjectList) {
            if (outGivingNodeProject.getStatus() == OutGivingNodeProject.Status.Ing.getCode()) {
                return JsonMessage.getString(400, "当前还在分发中,请等待分发结束");
            }
        }
        AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, Convert.toInt(afterOpt, 0));
        if (afterOpt1 == null) {
            return JsonMessage.getString(400, "请选择分发后的操作");
        }
        MultipartFileBuilder multipartFileBuilder = createMultipart();
        multipartFileBuilder
                .setFileExt(StringUtil.PACKAGE_EXT)
                .addFieldName("file")
                .setSavePath(ServerConfigBean.getInstance().getUserTempPath().getAbsolutePath());
        String path = multipartFileBuilder.save();
        //
        File src = FileUtil.file(path);
        File dest = null;
        for (String i : StringUtil.PACKAGE_EXT) {
            if (FileUtil.pathEndsWith(src, i)) {
                dest = FileUtil.file(ConfigBean.getInstance().getDataPath(), ServerConfigBean.OUTGIVING_FILE, id + "." + i);
                break;
            }
        }
        FileUtil.move(src, dest, true);
        //
        outGivingModel = outGivingServer.getItem(id);
        outGivingModel.setClearOld(Convert.toBool(clearOld, false));
        outGivingModel.setAfterOpt(afterOpt1.getCode());

        outGivingServer.updateItem(outGivingModel);
        // 开启
        OutGivingRun.startRun(outGivingModel.getId(), dest, getUser(), true);
        return JsonMessage.getString(200, "分发成功");
    }
}
