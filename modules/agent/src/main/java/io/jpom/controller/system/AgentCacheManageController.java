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
package io.jpom.controller.system;

import cn.hutool.core.io.FileUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseAgentController;
import io.jpom.common.JpomManifest;
import io.jpom.common.commander.AbstractProjectCommander;
import io.jpom.cron.CronUtils;
import io.jpom.model.system.WorkspaceEnvVarModel;
import io.jpom.plugin.PluginFactory;
import io.jpom.service.system.AgentWorkspaceEnvVarService;
import io.jpom.socket.AgentFileTailWatcher;
import io.jpom.system.ConfigBean;
import io.jpom.util.CommandUtil;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.Map;

/**
 * 缓存管理
 *
 * @author bwcx_jzy
 * @since 2019/7/20
 */
@RestController
@RequestMapping(value = "system")
public class AgentCacheManageController extends BaseAgentController {

    private final AgentWorkspaceEnvVarService agentWorkspaceEnvVarService;

    public AgentCacheManageController(AgentWorkspaceEnvVarService agentWorkspaceEnvVarService) {
        this.agentWorkspaceEnvVarService = agentWorkspaceEnvVarService;
    }

    /**
     * 缓存信息
     *
     * @return json
     */
    @PostMapping(value = "cache", produces = MediaType.APPLICATION_JSON_VALUE)
    public String cache() {
        JSONObject jsonObject = new JSONObject();
        //
        ConfigBean instance = ConfigBean.getInstance();
        File file = instance.getTempPath();
        String fileSize = FileUtil.readableFileSize(FileUtil.size(file));
        jsonObject.put("fileSize", fileSize);
        jsonObject.put("dataSize", FileUtil.readableFileSize(instance.dataSize()));
        File oldJarsPath = JpomManifest.getOldJarsPath();
        jsonObject.put("oldJarsSize", FileUtil.readableFileSize(FileUtil.size(oldJarsPath)));
        //
        jsonObject.put("pidName", AbstractProjectCommander.PID_JPOM_NAME.size());
        jsonObject.put("pidPort", AbstractProjectCommander.PID_PORT.size());

        int oneLineCount = AgentFileTailWatcher.getOneLineCount();
        jsonObject.put("readFileOnLineCount", oneLineCount);
        jsonObject.put("taskList", CronUtils.list());
        jsonObject.put("pluginSize", PluginFactory.size());
        //
        WorkspaceEnvVarModel item = agentWorkspaceEnvVarService.getItem(getWorkspaceId());
        if (item != null) {
            Map<String, WorkspaceEnvVarModel.WorkspaceEnvVarItemModel> varData = item.getVarData();
            if (varData != null) {
                jsonObject.put("envVarKeys", varData.keySet());
            }
        }
        //
        return JsonMessage.getString(200, "ok", jsonObject);
    }

    /**
     * 清空缓存
     *
     * @param type 缓存类型
     * @return json
     */
    @RequestMapping(value = "clearCache", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String clearCache(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "类型错误") String type) {
        switch (type) {
            case "pidPort":
                AbstractProjectCommander.PID_PORT.clear();
                break;
            case "pidName":
                AbstractProjectCommander.PID_JPOM_NAME.clear();
                break;
            case "oldJarsSize": {
                File oldJarsPath = JpomManifest.getOldJarsPath();
                boolean clean = CommandUtil.systemFastDel(oldJarsPath);
                Assert.state(!clean, "清空旧版本重新包失败");
                break;
            }
            case "fileSize": {
                File tempPath = ConfigBean.getInstance().getTempPath();
                boolean clean = CommandUtil.systemFastDel(tempPath);
                Assert.state(!clean, "清空文件缓存失败");
                break;
            }
            default:
                return JsonMessage.getString(405, "没有对应类型：" + type);

        }
        return JsonMessage.getString(200, "清空成功");
    }
}
