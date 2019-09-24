package io.jpom.controller.manage.log;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseAgentController;
import io.jpom.common.commander.AbstractProjectCommander;
import io.jpom.model.data.ProjectInfoModel;
import io.jpom.util.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * @author jiangzeyin
 * @date 2019/4/17
 */
@RestController
@RequestMapping(value = "manage/log")
public class LogBackController extends BaseAgentController {

    @RequestMapping(value = "logSize", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String logSize(String id) {
        ProjectInfoModel projectInfoModel = getProjectInfoModel();
        //获取日志备份路径
        File logBack = projectInfoModel.getLogBack();
        JSONObject jsonObject = new JSONObject();
        boolean logBackBool = logBack.exists() && logBack.isDirectory();
        jsonObject.put("logBack", logBackBool);
        String info = projectInfoService.getLogSize(id);
        jsonObject.put("logSize", info);
        return JsonMessage.getString(200, "", jsonObject);
    }

    @RequestMapping(value = "resetLog", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String resetLog() {
        ProjectInfoModel pim = getProjectInfoModel();
        try {
            String msg = AbstractProjectCommander.getInstance().backLog(pim);
            if (msg.contains("ok")) {
                return JsonMessage.getString(200, "重置成功");
            }
            return JsonMessage.getString(201, "重置失败：" + msg);
        } catch (Exception e) {
            DefaultSystemLog.getLog().error(e.getMessage(), e);
            return JsonMessage.getString(500, "重置日志失败");
        }
    }

    @RequestMapping(value = "logBack_delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String clear(String name) {
        name = pathSafe(name);
        if (StrUtil.isEmpty(name)) {
            return JsonMessage.getString(405, "非法操作:" + name);
        }
        ProjectInfoModel pim = getProjectInfoModel();
        File logBack = pim.getLogBack();
        if (logBack.exists() && logBack.isDirectory()) {
            logBack = FileUtil.file(logBack, name);
            if (logBack.exists()) {
                FileUtil.del(logBack);
                return JsonMessage.getString(200, "删除成功");
            }
            return JsonMessage.getString(500, "没有对应文件");
        } else {
            return JsonMessage.getString(500, "没有对应文件夹");
        }
    }

    @RequestMapping(value = "logBack_download", method = RequestMethod.GET)
    public String download(String key) {
        key = pathSafe(key);
        if (StrUtil.isEmpty(key)) {
            return JsonMessage.getString(405, "非法操作");
        }
        try {
            ProjectInfoModel pim = getProjectInfoModel();
            File logBack = pim.getLogBack();
            if (logBack.exists() && logBack.isDirectory()) {
                logBack = FileUtil.file(logBack, key);
                ServletUtil.write(getResponse(), logBack);
            } else {
                return "没有对应文件";
            }
        } catch (Exception e) {
            DefaultSystemLog.getLog().error("下载文件异常", e);
        }
        return "下载失败。请刷新页面后重试";
    }

    @RequestMapping(value = "logBack", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String console() {
        // 查询项目路径
        ProjectInfoModel pim = getProjectInfoModel();
        JSONObject jsonObject = new JSONObject();

        File logBack = pim.getLogBack();
        if (logBack.exists() && logBack.isDirectory()) {
            File[] filesAll = logBack.listFiles();
            if (filesAll != null) {
                JSONArray jsonArray = FileUtils.parseInfo(filesAll, true, null);
                jsonObject.put("array", jsonArray);
            }
        }
        jsonObject.put("id", pim.getId());
        jsonObject.put("logPath", pim.getLog());
        jsonObject.put("logBackPath", logBack.getAbsolutePath());
        return JsonMessage.getString(200, "", jsonObject);
    }

    @RequestMapping(value = "export.html", method = RequestMethod.GET)
    @ResponseBody
    public String export() {
        ProjectInfoModel pim = getProjectInfoModel();
        File file = new File(pim.getLog());
        if (!file.exists()) {
            return JsonMessage.getString(400, "没有日志文件:" + file.getPath());
        }
        HttpServletResponse response = getResponse();
        ServletUtil.write(response, file);
        return JsonMessage.getString(200, "");
    }
}
