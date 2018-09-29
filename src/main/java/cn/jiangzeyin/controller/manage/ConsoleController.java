package cn.jiangzeyin.controller.manage;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.crypto.SecureUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.BaseController;
import cn.jiangzeyin.model.ProjectInfoModel;
import cn.jiangzeyin.service.manage.ManageService;
import com.alibaba.fastjson.JSONObject;
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
        if (pim != null) {
            setAttribute("projectInfo", JSONObject.toJSONString(pim));
            String md5 = SecureUtil.md5(String.format("%s:%s", userName, userPwd));
            setAttribute("userInfo", md5);

            String logSize = null;

            File file = new File(pim.getLog());
            if (file.exists()) {
                logSize = FileUtil.readableFileSize(file);
            }
            setAttribute("logSize", logSize);
        }
        return "manage/console";
    }

    @RequestMapping(value = "export.html", method = RequestMethod.GET)
    @ResponseBody
    public String export(String id) {
        try {
            ProjectInfoModel pim = manageService.getProjectInfo(id);
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
