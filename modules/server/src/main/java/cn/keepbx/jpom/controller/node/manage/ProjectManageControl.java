package cn.keepbx.jpom.controller.node.manage;

import cn.keepbx.jpom.common.BaseNodeController;
import cn.keepbx.jpom.common.Role;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.common.interceptor.ProjectPermission;
import cn.keepbx.jpom.common.interceptor.UrlPermission;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
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
public class ProjectManageControl extends BaseNodeController {

    @Resource
    private ProjectInfoService projectInfoService;

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
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_GetProjectInfo).toString();
    }


    /**
     * 删除项目
     *
     * @return json
     */
    @RequestMapping(value = "deleteProject", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @ProjectPermission
    @UrlPermission(Role.Manage)
    public String deleteProject() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_DeleteProject).toString();

    }
}
