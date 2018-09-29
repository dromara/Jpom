package cn.jiangzeyin.controller.manage;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.BaseController;
import cn.jiangzeyin.model.ProjectInfoModel;
import cn.jiangzeyin.service.manage.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * Created by jiangzeyin on 2018/9/29.
 */
@Controller
@RequestMapping(value = "/manage/")
public class EditProjectController extends BaseController {
    @Resource
    private ManageService manageService;

    @RequestMapping(value = "editProject", method = RequestMethod.GET)
    public String editProject(String id) throws IOException {
        ProjectInfoModel projectInfo = manageService.getProjectInfo(id);
        setAttribute("item", projectInfo);
        return "manage/editProject";
    }

    @RequestMapping(value = "saveProject", method = RequestMethod.POST)
    @ResponseBody
    public String saveProject(ProjectInfoModel projectInfo) throws IOException {
        String id = projectInfo.getId();
        ProjectInfoModel exits = manageService.getProjectInfo(id);
        if (exits == null) {
            return addProject(projectInfo);
        }
        return updateProject(projectInfo);
    }

    private String addProject(ProjectInfoModel projectInfo) {
        String id = projectInfo.getId();
        if (StrUtil.isEmpty(id)) {
            return JsonMessage.getString(400, "项目id不能为空");
        }
        try {
            ProjectInfoModel exitsModel = manageService.getProjectInfo(id);
            if (exitsModel != null) {
                return JsonMessage.getString(400, "id已经存在");
            }
            projectInfo.setCreateTime(DateUtil.now());
            manageService.saveProject(projectInfo);
            return JsonMessage.getString(200, "新增成功！");
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return JsonMessage.getString(500, e.getMessage());
        }
    }


    private String updateProject(ProjectInfoModel projectInfo) {
        try {
            manageService.updateProject(projectInfo);
            return JsonMessage.getString(200, "修改成功");
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return JsonMessage.getString(500, e.getMessage());
        }
    }
}
