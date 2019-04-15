package cn.keepbx.jpom.controller.manage;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.common.interceptor.ProjectPermission;
import cn.keepbx.jpom.model.ProjectInfoModel;
import cn.keepbx.jpom.service.manage.ConsoleService;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
import cn.keepbx.jpom.service.oss.OssManagerService;
import com.alibaba.fastjson.JSONArray;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.File;

/**
 * 构建
 *
 * @author jiangzeyin
 * @date 2018/9/29
 */
@Controller
@RequestMapping(value = "/manage/")
public class BuildController extends BaseController {

    @Resource
    private OssManagerService ossManagerService;
    @Resource
    private ProjectInfoService projectInfoService;
    @Resource
    private ConsoleService consoleService;

    @RequestMapping(value = "build", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String build(String id) {
        ProjectInfoModel projectInfoModel = projectInfoService.getItem(id);
        if (projectInfoModel != null && StrUtil.isNotEmpty(projectInfoModel.getBuildTag())) {
            JSONArray jsonArray = ossManagerService.list(projectInfoModel.getBuildTag());
            setAttribute("array", jsonArray);
            setAttribute("id", id);
        }
        return "manage/build";
    }

    @RequestMapping(value = "build_download", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String buildDownload(String id, String key) {
        if (!getUser().isProject(id)) {
            return "redirect:error";
        }
        try {
            ProjectInfoModel projectInfoModel = projectInfoService.getItem(id);
            if (projectInfoModel == null) {
                return "redirect:error";
            }
            return "redirect:" + ossManagerService.getUrl(key);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("获取下载地址失败", e);
            return "redirect:error";
        }
    }

    @RequestMapping(value = "build_install", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @ProjectPermission
    public String buildInstall(String key) throws Exception {
        ProjectInfoModel projectInfoModel = getProjectInfoModel();
        if (StrUtil.isEmpty(projectInfoModel.getBuildTag())) {
            return JsonMessage.getString(400, "项目还不支持构建");
        }
        File file = ossManagerService.download(key);
        if (!file.exists()) {
            return JsonMessage.getString(500, "下载远程文件失败");
        }
        File lib = new File(projectInfoModel.getLib());
        if (!FileUtil.clean(lib)) {
            return JsonMessage.getString(500, "覆盖清除旧lib失败");
        }
        ZipUtil.unzip(file, lib);
        // 修改使用状态
        projectInfoModel.setUseLibDesc("build");
        projectInfoService.updateItem(projectInfoModel);
        String result = consoleService.execCommand(ConsoleService.CommandOp.restart, projectInfoModel);
        return JsonMessage.getString(200, "安装成功，已自动重启,当前状态是：" + result);
    }
}
