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
package org.dromara.jpom.controller.manage.log;

import cn.hutool.core.io.FileUtil;
import cn.hutool.extra.servlet.ServletUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseAgentController;
import org.dromara.jpom.common.JsonMessage;
import org.dromara.jpom.common.commander.AbstractProjectCommander;
import org.dromara.jpom.model.data.NodeProjectInfoModel;
import org.dromara.jpom.util.FileUtils;
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
 * @author bwcx_jzy
 * @since 2019/4/17
 */
@RestController
@RequestMapping(value = "manage/log")
@Slf4j
public class LogBackController extends BaseAgentController {

    @RequestMapping(value = "logSize", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<JSONObject> logSize(String id, String copyId) {
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
        return JsonMessage.success("", jsonObject);
    }

    @RequestMapping(value = "resetLog", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<String> resetLog(String copyId) {
        NodeProjectInfoModel pim = getProjectInfoModel();
        NodeProjectInfoModel.JavaCopyItem copyItem = pim.findCopyItem(copyId);
        try {
            String msg = AbstractProjectCommander.getInstance().backLog(pim, copyItem);
            if (msg.contains("ok")) {
                return JsonMessage.success("重置成功");
            }
            return new JsonMessage<>(201, "重置失败：" + msg);
        } catch (Exception e) {
            log.error("重置日志失败", e);
            return new JsonMessage<>(500, "重置日志失败");
        }
    }

    @RequestMapping(value = "logBack_delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<String> clear(String name, String copyId) {
        Assert.hasText(name, "没有对应到文件");
        NodeProjectInfoModel pim = getProjectInfoModel();
        NodeProjectInfoModel.JavaCopyItem copyItem = pim.findCopyItem(copyId);
        File logBack = copyItem == null ? pim.getLogBack() : pim.getLogBack(copyItem);
        if (logBack.exists() && logBack.isDirectory()) {
            logBack = FileUtil.file(logBack, name);
            if (logBack.exists()) {
                FileUtil.del(logBack);
                return JsonMessage.success("删除成功");
            }
            return new JsonMessage<>(500, "没有对应文件");
        } else {
            return new JsonMessage<>(500, "没有对应文件夹");
        }
    }

    @RequestMapping(value = "logBack_download", method = RequestMethod.GET)
    public void download(String key, String copyId, HttpServletResponse response) {
        Assert.hasText(key, "请选择对应到文件");
        try {
            NodeProjectInfoModel pim = getProjectInfoModel();
            NodeProjectInfoModel.JavaCopyItem copyItem = pim.findCopyItem(copyId);
            File logBack = copyItem == null ? pim.getLogBack() : pim.getLogBack(copyItem);
            if (logBack.exists() && logBack.isDirectory()) {
                logBack = FileUtil.file(logBack, key);
                ServletUtil.write(response, logBack);
            } else {
                ServletUtil.write(response, JsonMessage.getString(400, "没有对应文件:" + logBack.getPath()), MediaType.APPLICATION_JSON_VALUE);
            }
        } catch (Exception e) {
            log.error("下载文件异常", e);
            ServletUtil.write(response, JsonMessage.getString(400, "下载失败。请刷新页面后重试", e.getMessage()), MediaType.APPLICATION_JSON_VALUE);
        }
    }

    @RequestMapping(value = "logBack", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<JSONObject> console(String copyId) {
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
        return JsonMessage.success("", jsonObject);
    }

    @RequestMapping(value = "export.html", method = RequestMethod.GET)
    @ResponseBody
    public void export(String copyId, HttpServletResponse response) {
        NodeProjectInfoModel pim = getProjectInfoModel();
        NodeProjectInfoModel.JavaCopyItem copyItem = pim.findCopyItem(copyId);
        File file = copyItem == null ? new File(pim.getLog()) : pim.getLog(copyItem);
        if (!file.exists()) {
            ServletUtil.write(response, JsonMessage.getString(400, "没有日志文件:" + file.getPath()), MediaType.APPLICATION_JSON_VALUE);
            return;
        }
        ServletUtil.write(response, file);
    }
}
