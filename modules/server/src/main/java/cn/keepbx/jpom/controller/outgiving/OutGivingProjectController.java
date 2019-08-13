package cn.keepbx.jpom.controller.outgiving;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.common.interceptor.UrlPermission;
import cn.keepbx.jpom.model.BaseEnum;
import cn.keepbx.jpom.model.Role;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.OutGivingModel;
import cn.keepbx.jpom.model.data.OutGivingNodeProject;
import cn.keepbx.jpom.model.log.UserOperateLogV1;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
import cn.keepbx.jpom.service.node.NodeService;
import cn.keepbx.jpom.service.node.OutGivingServer;
import cn.keepbx.jpom.system.ConfigBean;
import cn.keepbx.jpom.system.ServerConfigBean;
import cn.keepbx.outgiving.OutGivingRun;
import cn.keepbx.util.StringUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
import java.util.function.Consumer;

/**
 * 分发文件管理
 *
 * @author jiangzeyin
 * @date 2019/4/21
 */
@Controller
@RequestMapping(value = "/outgiving")
public class OutGivingProjectController extends BaseServerController {
    @Resource
    private OutGivingServer outGivingServer;
    @Resource
    private NodeService nodeService;
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
                jsonObject.put("errorMsg", e.getMessage());
            }
            jsonObject.put("nodeId", outGivingNodeProject.getNodeId());
            jsonObject.put("projectId", outGivingNodeProject.getProjectId());
            jsonObject.put("nodeName", nodeModel.getName());
            if (projectInfo != null) {
                jsonObject.put("projectName", projectInfo.getString("name"));
            }
            jsonObject.put("projectStatus", false);
            jsonObject.put("outGivingStatus", outGivingNodeProject.getStatusMsg());
            jsonObject.put("outGivingResult", outGivingNodeProject.getResult());
            jsonObject.put("lastTime", outGivingNodeProject.getLastOutGivingTime());
            jsonArray.add(jsonObject);
        });
        return JsonMessage.getString(200, "", jsonArray);
    }


    @RequestMapping(value = "addOutgiving", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String addOutgiving(String id) throws IOException {

        JSONArray jsonArray = BaseEnum.toJSONArray(OutGivingModel.AfterOpt.class);
        setAttribute("afterOpt", jsonArray);
        //
        OutGivingModel outGivingModel = outGivingServer.getItem(id);
        setAttribute("outGivingModel", outGivingModel);
        return "outgiving/addOutgiving";
    }

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
    @UrlPermission(value = Role.ServerManager, optType = UserOperateLogV1.OptType.UploadOutGiving)
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
        OutGivingModel.AfterOpt afterOpt1 = BaseEnum.getEnum(OutGivingModel.AfterOpt.class, Convert.toInt(afterOpt, 0));
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
