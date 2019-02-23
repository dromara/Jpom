package cn.keepbx.jpom.controller.manage;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.controller.BaseController;
import cn.keepbx.jpom.model.ProjectInfoModel;
import cn.keepbx.jpom.service.UserService;
import cn.keepbx.jpom.service.manage.CommandService;
import cn.keepbx.jpom.service.manage.ManageService;
import cn.keepbx.jpom.service.oss.OssManagerService;
import com.alibaba.fastjson.JSONArray;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;

/**
 * @author jiangzeyin
 * @date 2018/9/29
 */
@Controller
@RequestMapping(value = "/manage/")
public class BuildController extends BaseController {

    @Resource
    private OssManagerService ossManagerService;
    @Resource
    private ManageService manageService;
    @Resource
    private CommandService commandService;
    @Resource
    private UserService userService;

    @RequestMapping(value = "build", method = RequestMethod.GET)
    public String build(String id) throws IOException {
        ProjectInfoModel projectInfoModel = manageService.getProjectInfo(id);
        if (projectInfoModel != null && StrUtil.isNotEmpty(projectInfoModel.getBuildTag())) {
            JSONArray jsonArray = ossManagerService.list(projectInfoModel.getBuildTag());
            setAttribute("array", jsonArray);
            setAttribute("id", id);
        }
        return "manage/build";
    }

    @RequestMapping(value = "build_download", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String buildDownload(String id, String key) {
        try {
            ProjectInfoModel projectInfoModel = manageService.getProjectInfo(id);
            if (projectInfoModel == null) {
                return "error";
            }
            return "redirect:" + ossManagerService.getUrl(key);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("获取下载地址失败", e);
            return "error";
        }
    }

    @RequestMapping(value = "build_install", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String buildInstall(String id, String key) throws Exception {
        boolean manager = userService.isManager(id, getUserName());
        if (!manager) {
            return JsonMessage.getString(400, "你没有对应操作权限操作!");
        }
        ProjectInfoModel projectInfoModel = manageService.getProjectInfo(id);
        if (projectInfoModel == null) {
            return JsonMessage.getString(400, "没有对应项目");
        }
        if (StrUtil.isEmpty(projectInfoModel.getBuildTag())) {
            return JsonMessage.getString(400, "项目还不支持构建");
        }
        File file = ossManagerService.download(key);
        if (!file.exists()) {
            return JsonMessage.getString(500, "下载远程文件失败");
        }
        File lib = new File(projectInfoModel.getLib());
        FileUtil.mkdir(lib);
        if (!FileUtil.clean(lib)) {
            return JsonMessage.getString(500, "清楚旧lib失败");
        }
        ZipUtil.unzip(file, lib);
        // 修改使用状态
        ProjectInfoModel modify = new ProjectInfoModel();
        modify.setId(projectInfoModel.getId());
        modify.setUseLibDesc("build");
        manageService.updateProject(modify);
        String result = commandService.execCommand(CommandService.CommandOp.restart, projectInfoModel);
        if (result.contains(CommandService.RUNING_TAG)) {
            return JsonMessage.getString(200, "安装成功，已自动重启");
        }
        return JsonMessage.getString(505, "安装失败：" + result);
    }
}
