package cn.keepbx.jpom.controller.node.manage;

import cn.hutool.http.HttpStatus;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.common.interceptor.ProjectPermission;
import cn.keepbx.jpom.common.interceptor.UrlPermission;
import cn.keepbx.jpom.model.Role;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.OutGivingModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.model.data.UserOperateLogV1;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
import cn.keepbx.jpom.service.node.OutGivingServer;
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
 * 项目管理
 *
 * @author Administrator
 */
@Controller
@RequestMapping(value = "/node/manage/")
public class ProjectManageControl extends BaseServerController {

    @Resource
    private ProjectInfoService projectInfoService;
    @Resource
    private OutGivingServer outGivingServer;

    /**
     * 展示项目页面
     *
     * @return page
     */
    @RequestMapping(value = "projectInfo", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String projectInfo() {
        List<String> hashSet = projectInfoService.getAllGroup(getNode());
        setAttribute("groups", hashSet);
        return "node/manage/projectInfo";
    }

    /**
     * 获取正在运行的项目的端口和进程id
     *
     * @return json
     */
    @RequestMapping(value = "getProjectPort", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getProjectPort() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_GetProjectPort).toString();
    }

    /**
     * 查询所有项目
     *
     * @return json
     */
    @RequestMapping(value = "getProjectInfo", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getProjectInfo() {
        NodeModel nodeModel = getNode();
        JsonMessage jsonMessage = NodeForward.request(nodeModel, getRequest(), NodeUrl.Manage_GetProjectInfo);
        if (jsonMessage.getCode() == HttpStatus.HTTP_OK) {
            UserModel userModel = getUser();
            JSONArray jsonArray = NodeForward.toObj(jsonMessage, JSONArray.class);
            if (jsonArray != null) {
                JSONArray newArray = new JSONArray();
                jsonArray.forEach(o -> {
                    JSONObject jsonObject = (JSONObject) o;
                    String id = jsonObject.getString("id");
                    jsonObject.put("manager", userModel.isProject(nodeModel.getId(), id));
                    newArray.add(jsonObject);
                });
                jsonArray = newArray;
            }
            jsonMessage.setData(jsonArray);

        }
        return jsonMessage.toString();
    }


    /**
     * 删除项目
     *
     * @return json
     */
    @RequestMapping(value = "deleteProject", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @ProjectPermission(optType = UserOperateLogV1.OptType.DelProject)
    @UrlPermission(value = Role.NodeManage, optType = UserOperateLogV1.OptType.DelProject)
    public String deleteProject(String id) throws IOException {
        // 检查节点分发
        List<OutGivingModel> outGivingModels = outGivingServer.list();
        if (outGivingModels != null) {
            NodeModel nodeModel = getNode();
            for (OutGivingModel outGivingModel : outGivingModels) {
                if (outGivingModel.checkContains(nodeModel.getId(), id)) {
                    return JsonMessage.getString(405, "当前项目存在节点分发，不能直接删除");
                }
            }
        }
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_DeleteProject).toString();
    }
}
