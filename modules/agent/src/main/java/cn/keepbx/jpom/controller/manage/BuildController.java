package cn.keepbx.jpom.controller.manage;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseAgentController;
import cn.keepbx.jpom.model.data.ProjectInfoModel;
import cn.keepbx.jpom.service.manage.ConsoleService;
import cn.keepbx.jpom.service.oss.OssManagerService;
import cn.keepbx.jpom.socket.CommandOp;
import com.alibaba.fastjson.JSONArray;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.File;

/**
 * 构建
 *
 * @author jiangzeyin
 * @date 2018/9/29
 */
@RestController
@RequestMapping(value = "/manage/")
public class BuildController extends BaseAgentController {

    @Resource
    private OssManagerService ossManagerService;
    @Resource
    private ConsoleService consoleService;

    @RequestMapping(value = "build_data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String build(String id) {
        ProjectInfoModel projectInfoModel = projectInfoService.getItem(id);
        JSONArray jsonArray = null;
        if (projectInfoModel != null && StrUtil.isNotEmpty(projectInfoModel.getBuildTag())) {
            jsonArray = ossManagerService.list(projectInfoModel.getBuildTag());
        }
        return JsonMessage.getString(200, "", jsonArray);
    }

    @RequestMapping(value = "build_download", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String buildDownload(String id, String key) {
        if (StrUtil.isEmpty(key)) {
            return JsonMessage.getString(401, "key 错误");
        }
        String url = null;
        try {
            ProjectInfoModel projectInfoModel = projectInfoService.getItem(id);
            if (projectInfoModel != null) {
                url = ossManagerService.getUrl(key).toString();
            }
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("获取下载地址失败", e);
        }
        return JsonMessage.getString(200, "", url);
    }

    @RequestMapping(value = "build_install", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
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
        String result = consoleService.execCommand(CommandOp.restart, projectInfoModel);
        return JsonMessage.getString(200, "安装成功，已自动重启,当前状态是：" + result);
    }
}
