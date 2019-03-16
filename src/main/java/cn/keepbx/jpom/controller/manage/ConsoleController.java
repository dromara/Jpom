package cn.keepbx.jpom.controller.manage;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.model.ProjectInfoModel;
import cn.keepbx.jpom.model.UserModel;
import cn.keepbx.jpom.service.manage.CommandService;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

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

    @Resource
    private CommandService commandService;

    /**
     * 管理项目
     *
     * @return page
     */
    @RequestMapping(value = "console", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String console(String id) throws IOException {
        ProjectInfoModel projectInfoModel = null;
        try {
            projectInfoModel = projectInfoService.getItem(id);
        } catch (IOException e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        if (projectInfoModel != null) {
            UserModel userName = getUser();
            setAttribute("projectInfo", projectInfoModel);
            setAttribute("userInfo", userName.getUserMd5Key());
            String logSize = getLogSize(id);
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

    @RequestMapping(value = "logSize", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String logSize(String id) throws IOException {
        String info = getLogSize(id);
        if (info != null) {
            return JsonMessage.getString(200, "ok", info);
        }
        return JsonMessage.getString(500, "获取日志大小失败");
    }


    @RequestMapping(value = "resetLog", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String resetLog(String id) {
        ProjectInfoModel pim;
        try {
            pim = projectInfoService.getItem(id);
            String msg = commandService.execCommand(CommandService.CommandOp.backupLog, pim);
            if (msg.contains("ok")) {
                return JsonMessage.getString(200, "重置成功");
            }
            return JsonMessage.getString(201, "重置失败：" + msg);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return JsonMessage.getString(500, "重置日志失败");
        }
    }

    private String getLogSize(String id) {
        ProjectInfoModel pim;
        try {
            pim = projectInfoService.getItem(id);
            if (pim == null) {
                return null;
            }
        } catch (IOException e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return null;
        }
        String logSize = null;
        File file = new File(pim.getLog());
        if (file.exists()) {
            long fileSize = file.length();
            logSize = FileUtil.readableFileSize(fileSize);
        }
        return logSize;
    }

    @RequestMapping(value = "export.html", method = RequestMethod.GET)
    @ResponseBody
    public String export(String id) {
        try {
            ProjectInfoModel pim = projectInfoService.getItem(id);
            File file = new File(pim.getLog());
            if (!file.exists()) {
                return JsonMessage.getString(400, "没有日志文件:" + file.getPath());
            }
            HttpServletResponse response = getResponse();
            // 设置强制下载不打开
            response.setContentType("application/force-download");
            // 设置文件名
            response.addHeader("Content-Disposition", "attachment;fileName=" + file.getName());
            OutputStream os = response.getOutputStream();
            byte[] bytes = IoUtil.readBytes(new FileInputStream(file));
            IoUtil.write(os, false, bytes);
            os.flush();
            os.close();
            return "ok";
        } catch (IOException e) {
            DefaultSystemLog.ERROR().error("删除文件异常", e);
        }
        return JsonMessage.getString(500, "导出失败");
    }
}
