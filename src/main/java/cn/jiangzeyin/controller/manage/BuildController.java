package cn.jiangzeyin.controller.manage;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.controller.BaseController;
import cn.jiangzeyin.model.ProjectInfoModel;
import cn.jiangzeyin.oss.OssManagerService;
import cn.jiangzeyin.service.manage.ManageService;
import com.alibaba.fastjson.JSONArray;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * Created by jiangzeyin on 2018/9/29.
 */
@Controller
@RequestMapping(value = "/manage/")
public class BuildController extends BaseController {

    @Resource
    private OssManagerService ossManagerService;

    @Resource
    private ManageService manageService;

    @RequestMapping(value = "build", method = RequestMethod.GET)
    public String console(String id) throws IOException {
        ProjectInfoModel projectInfoModel = manageService.getProjectInfo(id);
        if (projectInfoModel != null && StrUtil.isNotEmpty(projectInfoModel.getBuildTag())) {
            JSONArray jsonArray = ossManagerService.list(projectInfoModel.getBuildTag());
            setAttribute("array", jsonArray);
        }
        return "manage/build";
    }
}
