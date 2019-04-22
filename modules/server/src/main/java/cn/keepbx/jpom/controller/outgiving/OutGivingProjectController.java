package cn.keepbx.jpom.controller.outgiving;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.model.Role;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.common.interceptor.UrlPermission;
import cn.keepbx.jpom.model.BaseEnum;
import cn.keepbx.jpom.model.data.OutGivingNodeProject;
import cn.keepbx.jpom.model.data.OutGivingModel;
import cn.keepbx.jpom.model.data.UserOperateLogV1;
import cn.keepbx.jpom.service.node.OutGivingServer;
import cn.keepbx.jpom.system.ConfigBean;
import cn.keepbx.jpom.system.ServerConfigBean;
import com.alibaba.fastjson.JSONArray;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.List;

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

    @RequestMapping(value = "getProjectStatus", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String save() throws IOException {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_GetProjectStatus).toString();
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
    public String upload(String id, String afterOpt) throws IOException {
        OutGivingModel outGivingModel = outGivingServer.getItem(id);
        if (outGivingModel == null) {
            return JsonMessage.getString(400, "上传失败,没有找到对应的分发项目");
        }
        OutGivingModel.AfterOpt afterOpt1 = BaseEnum.getEnum(OutGivingModel.AfterOpt.class, Convert.toInt(afterOpt, 0));
        if (afterOpt1 == null) {
            return JsonMessage.getString(400, "请选择分发后的操作");
        }
        MultipartFileBuilder multipartFileBuilder = createMultipart();
        multipartFileBuilder
                .setInputStreamType("zip")
                .addFieldName("file")
                .setSavePath(ServerConfigBean.getInstance().getTempPath().getAbsolutePath());
        String path = multipartFileBuilder.save();
        File file = FileUtil.file(ConfigBean.getInstance().getDataPath(), ServerConfigBean.OUTGIVING_FILE, id + ".zip");
        FileUtil.move(FileUtil.file(path), file, true);
        //
        outGivingModel = outGivingServer.getItem(id);
        outGivingModel.setAfterOpt(afterOpt1.getCode());
        outGivingModel.start();
        outGivingServer.updateItem(outGivingModel);
        // 开启线程
        List<OutGivingNodeProject> outGivingNodeProjects = outGivingModel.getOutGivingNodeProjectList();
        outGivingNodeProjects.forEach(outGivingNodeProject -> ThreadUtil.execute(new OutGivingModel.OutGivingRun(id, outGivingNodeProject, file, afterOpt1, getUser())));
        return JsonMessage.getString(200, "上传成功");
    }
}
