/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.manage;

import cn.hutool.core.io.FileUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseAgentController;
import org.dromara.jpom.common.commander.ProjectCommander;
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
public class ProjectLogBackController extends BaseAgentController {

    private final ProjectCommander projectCommander;

    public ProjectLogBackController(ProjectCommander projectCommander) {
        this.projectCommander = projectCommander;
    }

    @RequestMapping(value = "logSize", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONObject> logSize(String id) {
        NodeProjectInfoModel nodeProjectInfoModel = getProjectInfoModel();
        JSONObject jsonObject = new JSONObject();
        //
        //获取日志备份路径
        File logBack = projectInfoService.resolveLogBack(nodeProjectInfoModel);
        boolean logBackBool = logBack.exists() && logBack.isDirectory();
        jsonObject.put("logBack", logBackBool);
        String info = this.getLogSize(nodeProjectInfoModel);
        jsonObject.put("logSize", info);
        return JsonMessage.success("", jsonObject);
    }

    /**
     * 查看项目控制台日志文件大小
     *
     * @param nodeProjectInfoModel 项目
     * @return 文件大小
     */
    private String getLogSize(NodeProjectInfoModel nodeProjectInfoModel) {
        if (nodeProjectInfoModel == null) {
            return null;
        }
        File file = projectInfoService.resolveAbsoluteLogFile(nodeProjectInfoModel);
        if (file.exists()) {
            long fileSize = file.length();
            if (fileSize <= 0) {
                return null;
            }
            return FileUtil.readableFileSize(fileSize);
        }
        return null;
    }

    @RequestMapping(value = "resetLog", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<String> resetLog() {
        NodeProjectInfoModel pim = getProjectInfoModel();
        try {
            String msg = projectCommander.backLog(pim);
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
    public IJsonMessage<String> clear(String name) {
        Assert.hasText(name, "没有对应到文件");
        NodeProjectInfoModel pim = getProjectInfoModel();
        File logBack = projectInfoService.resolveLogBack(pim);
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
    public void download(String key, HttpServletResponse response) {
        Assert.hasText(key, "请选择对应到文件");
        try {
            NodeProjectInfoModel pim = getProjectInfoModel();
            File logBack = projectInfoService.resolveLogBack(pim);
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
    public IJsonMessage<JSONObject> console() {
        // 查询项目路径
        NodeProjectInfoModel pim = getProjectInfoModel();

        JSONObject jsonObject = new JSONObject();
        NodeProjectInfoModel infoModel = projectInfoService.resolveModel(pim);
        File logBack = projectInfoService.resolveLogBack(pim, infoModel);
        if (logBack.exists() && logBack.isDirectory()) {
            File[] filesAll = logBack.listFiles();
            if (filesAll != null) {
                List<JSONObject> jsonArray = FileUtils.parseInfo(filesAll, true, null);
                jsonObject.put("array", jsonArray);
            }
        }
        jsonObject.put("id", pim.getId());
        jsonObject.put("logPath", projectInfoService.resolveAbsoluteLog(pim, infoModel));
        jsonObject.put("logBackPath", logBack.getAbsolutePath());
        return JsonMessage.success("", jsonObject);
    }

    @RequestMapping(value = "export", method = RequestMethod.GET)
    @ResponseBody
    public void export(HttpServletResponse response) {
        NodeProjectInfoModel pim = getProjectInfoModel();

        File file = projectInfoService.resolveAbsoluteLogFile(pim);
        if (!file.exists()) {
            ServletUtil.write(response, JsonMessage.getString(400, "没有日志文件:" + file.getPath()), MediaType.APPLICATION_JSON_VALUE);
            return;
        }
        ServletUtil.write(response, file);
    }
}
