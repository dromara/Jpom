package cn.keepbx.jpom.controller.node.manage;

import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;

/**
 * 控制台
 *
 * @author jiangzeyin
 * @date 2018/9/28
 */
@Controller
@RequestMapping(value = "/node/manage/")
public class ConsoleController extends BaseServerController {

    @Resource
    private ProjectInfoService projectInfoService;


    /**
     * 管理项目
     *
     * @return page
     */
    @RequestMapping(value = "console", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String console(String id) {
        JSONObject projectInfoModel = projectInfoService.getItem(getNode(), id);
        if (projectInfoModel != null) {
            setAttribute("projectInfo", projectInfoModel);
            JSONObject logSize = projectInfoService.getLogSize(getNode(), id);
            setAttribute("logSize", logSize);
            setAttribute("manager", true);
        }
        return "node/manage/console";
    }


}
