package cn.keepbx.jpom.controller.manage.recover;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.model.data.ProjectRecoverModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.service.manage.ProjectRecoverService;
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
@RequestMapping(value = "/manage/recover")
public class ProjectRecoverControl extends BaseController {

    @Resource
    private ProjectRecoverService projectRecoverService;

    /**
     * 展示项目页面
     *
     * @return page
     */
    @RequestMapping(value = "list.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String projectInfo() {
        UserModel userModel = getUser();
        if (userModel.isManage()) {
            List<ProjectRecoverModel> projectInfoModels = projectRecoverService.list();
            setAttribute("array", projectInfoModels);
        }
        return "manage/project_recover";
    }

    @RequestMapping(value = "data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String project(String id) throws IOException {
        if (StrUtil.isEmpty(id)) {
            return JsonMessage.getString(400, "项目id错误");
        }
        ProjectRecoverModel item = projectRecoverService.getItem(id);
        return JsonMessage.getString(200, "", item);
    }

}
