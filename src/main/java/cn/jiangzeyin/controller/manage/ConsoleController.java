package cn.jiangzeyin.controller.manage;

import cn.hutool.crypto.SecureUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.controller.BaseController;
import cn.jiangzeyin.model.ProjectInfoModel;
import cn.jiangzeyin.service.manage.ManageService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * Created by jiangzeyin on 2018/9/28.
 */
@Controller
@RequestMapping(value = "/manage/")
public class ConsoleController extends BaseController {

    @Resource
    private ManageService manageService;

    /**
     * 管理项目
     *
     * @return page
     */
    @RequestMapping(value = "console", method = RequestMethod.GET)
    public String console(String id) {
        ProjectInfoModel pim = null;
        try {
            pim = manageService.getProjectInfo(id);
        } catch (IOException e) {
            DefaultSystemLog.LOG().error(e.getMessage(), e);
        }
        setAttribute("projectInfo", JSONObject.toJSONString(pim));
        String md5 = SecureUtil.md5(String.format("%s:%s", userName, userPwd));
        setAttribute("userInfo", md5);
        return "manage/console";
    }
}
