package io.jpom.controller.node.manage;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.alibaba.fastjson.JSONArray;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.OutGivingModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.build.BuildService;
import io.jpom.service.monitor.MonitorService;
import io.jpom.service.node.OutGivingServer;
import io.jpom.service.node.manage.ProjectInfoService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;

/**
 * 项目管理
 *
 * @author Administrator
 */
@Controller
@RequestMapping(value = "/node/manage/")
@Feature(cls = ClassFeature.PROJECT)
public class ProjectManageControl extends BaseServerController {

    @Resource
    private ProjectInfoService projectInfoService;
    @Resource
    private OutGivingServer outGivingServer;
    @Resource
    private MonitorService monitorService;
    @Resource
    private BuildService buildService;

//    /**
//     * 展示项目页面
//     *
//     * @return page
//     */
//    @RequestMapping(value = "projectInfo", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.LIST)
//    public String projectInfo() {
//        List<String> hashSet = projectInfoService.getAllGroup(getNode());
//        setAttribute("groups", hashSet);
//        return "node/manage/projectInfo";
//    }


//    @RequestMapping(value = "projectCopyLList.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.LIST)
//    public String projectInfoPage() {
//        return "node/manage/javaCopyList";
//    }


    /**
     * 展示项目页面
     */
    @RequestMapping(value = "project_copy_list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @Feature(method = MethodFeature.LIST)
    @ResponseBody
    public String projectCopyList() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_ProjectCopyList).toString();
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
     * 获取正在运行的项目的端口和进程id
     *
     * @return json
     */
    @RequestMapping(value = "getProjectCopyPort", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getProjectCopyPort() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_GetProjectCopyPort).toString();
    }

    /**
     * @author Hotstrip
     * get project group
     * 获取项目的分组信息
     * @return
     */
    @RequestMapping(value = "project-group-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String projectGroupList() {
        List<String> allGroup = projectInfoService.getAllGroup(getNode());
        return JsonMessage.getString(200, "success", allGroup);
    }

    /**
     * 查询所有项目
     *
     * @return json
     */
    @RequestMapping(value = "getProjectInfo", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.LIST)
    public String getProjectInfo() {
        NodeModel nodeModel = getNode();
        JSONArray jsonArray = projectInfoService.listAll(nodeModel, getRequest());
        return JsonMessage.getString(200, "ok", jsonArray);
    }

    /**
     * 删除项目
     *
     * @param id id
     * @return json
     */
    @RequestMapping(value = "deleteProject", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @OptLog(value = UserOperateLogV1.OptType.DelProject)
    @Feature(method = MethodFeature.DEL)
    public String deleteProject(@ValidatorItem(value = ValidatorRule.NOT_BLANK) String id, String copyId) {
        NodeModel nodeModel = getNode();
        if (StrUtil.isEmpty(copyId)) {
            // 检查节点分发
            List<OutGivingModel> outGivingModels = outGivingServer.list();
            if (outGivingModels != null) {
                for (OutGivingModel outGivingModel : outGivingModels) {
                    if (outGivingModel.checkContains(nodeModel.getId(), id)) {
                        return JsonMessage.getString(405, "当前项目存在节点分发，不能直接删除");
                    }
                }
            }
            //
            if (monitorService.checkProject(nodeModel.getId(), id)) {
                return JsonMessage.getString(405, "当前项目存在监控项，不能直接删除");
            }
            if (buildService.checkNodeProjectId(nodeModel.getId(), id)) {
                return JsonMessage.getString(405, "当前项目存在构建项，不能直接删除");
            }
        }
        return NodeForward.request(nodeModel, getRequest(), NodeUrl.Manage_DeleteProject).toString();
    }
}
