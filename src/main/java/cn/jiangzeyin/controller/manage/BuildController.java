package cn.jiangzeyin.controller.manage;

import cn.jiangzeyin.controller.BaseController;
import cn.jiangzeyin.oss.OssManagerService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;

/**
 * Created by jiangzeyin on 2018/9/29.
 */
@Controller
@RequestMapping(value = "/manage/")
public class BuildController extends BaseController {

    @Resource
    private OssManagerService ossManagerService;

    @RequestMapping(value = "build", method = RequestMethod.GET)
    public String console(String id) {

        return "manage/build";
    }
}
