/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.controller.manage.log;

import cn.hutool.core.io.FileUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseAgentController;
import io.jpom.common.commander.AbstractProjectCommander;
import io.jpom.model.data.NodeProjectInfoModel;
import io.jpom.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.List;

/**
 * @author jiangzeyin
 * @since 2019/4/17
 */
@RestController
@RequestMapping(value = "manage/log")
@Slf4j
public class LogBackController extends BaseAgentController {

    @RequestMapping(value = "logSize", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String logSize(String id, String copyId) {
        NodeProjectInfoModel nodeProjectInfoModel = getProjectInfoModel();
        JSONObject jsonObject = new JSONObject();
        //
        NodeProjectInfoModel.JavaCopyItem copyItem = nodeProjectInfoModel.findCopyItem(copyId);
        //获取日志备份路径
        File logBack = copyItem == null ? nodeProjectInfoModel.getLogBack() : nodeProjectInfoModel.getLogBack(copyItem);
        boolean logBackBool = logBack.exists() && logBack.isDirectory();
        jsonObject.put("logBack", logBackBool);
        String info = projectInfoService.getLogSize(nodeProjectInfoModel, copyItem);
        jsonObject.put("logSize", info);
        return JsonMessage.getString(200, "", jsonObject);
    }

    @RequestMapping(value = "resetLog", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String resetLog(String copyId) {
        NodeProjectInfoModel pim = getProjectInfoModel();
        NodeProjectInfoModel.JavaCopyItem copyItem = pim.findCopyItem(copyId);
        try {
            String msg = AbstractProjectCommander.getInstance().backLog(pim, copyItem);
            if (msg.contains("ok")) {
                return JsonMessage.getString(200, "重置成功");
            }
            return JsonMessage.getString(201, "重置失败：" + msg);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return JsonMessage.getString(500, "重置日志失败");
        }
    }

    @RequestMapping(value = "logBack_delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String clear(String name, String copyId) {
        Assert.hasText(name, "没有对应到文件");
        NodeProjectInfoModel pim = getProjectInfoModel();
        NodeProjectInfoModel.JavaCopyItem copyItem = pim.findCopyItem(copyId);
        File logBack = copyItem == null ? pim.getLogBack() : pim.getLogBack(copyItem);
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
    public String download(String key, String copyId) {
        Assert.hasText(key, "请选择对应到文件");
        try {
            NodeProjectInfoModel pim = getProjectInfoModel();
            NodeProjectInfoModel.JavaCopyItem copyItem = pim.findCopyItem(copyId);
            File logBack = copyItem == null ? pim.getLogBack() : pim.getLogBack(copyItem);
            if (logBack.exists() && logBack.isDirectory()) {
                logBack = FileUtil.file(logBack, key);
                ServletUtil.write(getResponse(), logBack);
            } else {
                return "没有对应文件";
            }
        } catch (Exception e) {
            log.error("下载文件异常", e);
        }
        return "下载失败。请刷新页面后重试";
    }

    @RequestMapping(value = "logBack", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String console(String copyId) {
        // 查询项目路径
        NodeProjectInfoModel pim = getProjectInfoModel();
        NodeProjectInfoModel.JavaCopyItem copyItem = pim.findCopyItem(copyId);
        JSONObject jsonObject = new JSONObject();

        File logBack = copyItem == null ? pim.getLogBack() : pim.getLogBack(copyItem);
        if (logBack.exists() && logBack.isDirectory()) {
            File[] filesAll = logBack.listFiles();
            if (filesAll != null) {
                List<JSONObject> jsonArray = FileUtils.parseInfo(filesAll, true, null);
                jsonObject.put("array", jsonArray);
            }
        }
        jsonObject.put("id", pim.getId());
        jsonObject.put("logPath", copyItem == null ? pim.getLog() : pim.getLog(copyItem));
        jsonObject.put("logBackPath", logBack.getAbsolutePath());
        return JsonMessage.getString(200, "", jsonObject);
    }

    @RequestMapping(value = "export.html", method = RequestMethod.GET)
    @ResponseBody
    public String export(String copyId) {
        NodeProjectInfoModel pim = getProjectInfoModel();
        NodeProjectInfoModel.JavaCopyItem copyItem = pim.findCopyItem(copyId);
        File file = copyItem == null ? new File(pim.getLog()) : pim.getLog(copyItem);
        if (!file.exists()) {
            return JsonMessage.getString(400, "没有日志文件:" + file.getPath());
        }
        HttpServletResponse response = getResponse();
        ServletUtil.write(response, file);
        return JsonMessage.getString(200, "");
    }
}
