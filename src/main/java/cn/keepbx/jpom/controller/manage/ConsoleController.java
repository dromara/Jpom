package cn.keepbx.jpom.controller.manage;

import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.model.ProjectInfoModel;
import cn.keepbx.jpom.model.UserModel;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * 控制台
 *
 * @author jiangzeyin
 * @date 2018/9/28
 */
@Controller
@RequestMapping(value = "/manage/")
public class ConsoleController extends BaseController {

    @Resource
    private ProjectInfoService projectInfoService;


    /**
     * 管理项目
     *
     * @return page
     */
    @RequestMapping(value = "console", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String console(String id) {
        ProjectInfoModel projectInfoModel = projectInfoService.getItem(id);
        if (projectInfoModel != null) {
            UserModel userName = getUser();
            setAttribute("projectInfo", projectInfoModel);
            setAttribute("userInfo", userName.getUserMd5Key());
            String logSize = projectInfoService.getLogSize(id);
            setAttribute("logSize", logSize);
            setAttribute("manager", userName.isProject(id));

            //获取日志备份路径
            File logBack = projectInfoModel.getLogBack();
            if (logBack.exists() && logBack.isDirectory()) {
                setAttribute("logBack", true);
            }
        }
        return "manage/console";
    }



}
