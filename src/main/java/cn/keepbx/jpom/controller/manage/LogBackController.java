package cn.keepbx.jpom.controller.manage;

import cn.hutool.core.io.FileUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.model.ProjectInfoModel;
import cn.keepbx.jpom.model.UserModel;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
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
 * 控制台日志备份管理
 *
 * @author jiangzeyin
 * @date 2019/3/7
 */
@Controller
@RequestMapping(value = "/manage/")
public class LogBackController extends BaseController {
    @Resource
    private ProjectInfoService projectInfoService;

    @RequestMapping(value = "logBack", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String console(String id) {
        try {
            // 查询项目路径
            ProjectInfoModel pim = projectInfoService.getProjectInfo(id);
            File logBack = pim.getLogBack();
            if (logBack.exists() && logBack.isDirectory()) {
                File[] filesAll = logBack.listFiles();
                if (filesAll != null) {
                    JSONArray jsonArray = ProjectFileControl.parseInfo(filesAll, true);
                    setAttribute("array", jsonArray);
                }
            }
            setAttribute("id", pim.getId());
        } catch (IOException e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return "manage/logBack";
    }

    @RequestMapping(value = "logBack_download", method = RequestMethod.GET)
    @ResponseBody
    public String download(String id, String key) {
        try {
            ProjectInfoModel pim = projectInfoService.getProjectInfo(id);
            File logBack = pim.getLogBack();
            if (logBack.exists() && logBack.isDirectory()) {
                logBack = new File(logBack, key);
                ServletUtil.write(getResponse(), logBack);
            } else {
                return "没有对应文件";
            }
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("下载文件异常", e);
        }
        return "下载失败。请刷新页面后重试";
    }


    @RequestMapping(value = "logBack_delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String clear(String id, String name) {
        UserModel userName = getUser();
        if (!userName.isProject(id)) {
            return JsonMessage.getString(400, "你没有对应操作权限操作!");
        }
        try {
            ProjectInfoModel pim = projectInfoService.getProjectInfo(id);
            File logBack = pim.getLogBack();
            if (logBack.exists() && logBack.isDirectory()) {
                logBack = new File(logBack, name);
                if (logBack.exists()) {
                    FileUtil.del(logBack);
                    return JsonMessage.getString(200, "删除成功");
                }
                return JsonMessage.getString(500, "没有对应文件");
            } else {
                return JsonMessage.getString(500, "没有对应文件夹");
            }
        } catch (IOException e) {
            DefaultSystemLog.ERROR().error("删除文件异常", e);
        }
        return JsonMessage.getString(500, "删除失败");
    }
}
