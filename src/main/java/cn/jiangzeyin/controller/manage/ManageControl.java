package cn.jiangzeyin.controller.manage;

import cn.jiangzeyin.common.base.AbstractBaseControl;
import cn.jiangzeyin.service.manage.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

@Controller
@RequestMapping(value = "/manage/")
public class ManageControl extends AbstractBaseControl {

    @Resource
    ManageService manageService;


    /**
     * 列出所有项目
     * @return
     */
    @RequestMapping(value = "projectInfo")
    public String projectInfo() {

        return "manage/projectInfo";
    }


    /**
     * 添加项目
     * @return
     */
    @RequestMapping(value = "addProject")
    @ResponseBody
    public String addProject() {


        return "";
    }

}
