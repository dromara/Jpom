/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.system;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.event.ICacheTask;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.BaseAgentController;
import org.dromara.jpom.common.JpomManifest;
import org.dromara.jpom.common.commander.AbstractProjectCommander;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.configuration.AgentConfig;
import org.dromara.jpom.configuration.SystemConfig;
import org.dromara.jpom.cron.CronUtils;
import org.dromara.jpom.model.data.ScriptLibraryModel;
import org.dromara.jpom.model.system.WorkspaceEnvVarModel;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.service.script.NodeScriptExecLogServer;
import org.dromara.jpom.service.script.ScriptLibraryService;
import org.dromara.jpom.service.system.AgentWorkspaceEnvVarService;
import org.dromara.jpom.socket.AgentFileTailWatcher;
import org.dromara.jpom.util.CommandUtil;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * 缓存管理
 *
 * @author bwcx_jzy
 * @since 2019/7/20
 */
@RestController
@RequestMapping(value = "system")
public class AgentCacheManageController extends BaseAgentController implements ICacheTask {

    private final AgentWorkspaceEnvVarService agentWorkspaceEnvVarService;
    private final JpomApplication configBean;
    private final NodeScriptExecLogServer nodeScriptExecLogServer;
    private final SystemConfig systemConfig;
    private final ScriptLibraryService scriptLibraryService;

    private long dataSize;
    private long oldJarsSize;
    private long tempFileSize;

    public AgentCacheManageController(AgentWorkspaceEnvVarService agentWorkspaceEnvVarService,
                                      JpomApplication configBean,
                                      NodeScriptExecLogServer nodeScriptExecLogServer,
                                      AgentConfig agentConfig,
                                      ScriptLibraryService scriptLibraryService) {
        this.agentWorkspaceEnvVarService = agentWorkspaceEnvVarService;
        this.configBean = configBean;
        this.nodeScriptExecLogServer = nodeScriptExecLogServer;
        this.systemConfig = agentConfig.getSystem();
        this.scriptLibraryService = scriptLibraryService;
    }

    /**
     * 缓存信息
     *
     * @return json
     */
    @PostMapping(value = "cache", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONObject> cache() {
        JSONObject jsonObject = new JSONObject();
        //
        jsonObject.put("fileSize", this.tempFileSize);
        jsonObject.put("dataSize", this.dataSize);
        jsonObject.put("oldJarsSize", this.oldJarsSize);
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
        List<ScriptLibraryModel> scriptLibraryModels = scriptLibraryService.list();
        Map<String, String> scriptLibraryTagMap = scriptLibraryModels.stream()
            .collect(Collectors.toMap(ScriptLibraryModel::getTag, ScriptLibraryModel::getVersion));
        jsonObject.put("scriptLibraryTagMap", scriptLibraryTagMap);
        //
        jsonObject.put("dateTime", DateTime.now().toString());
        jsonObject.put("timeZoneId", TimeZone.getDefault().getID());
        // 待同步待日志数
        int size = nodeScriptExecLogServer.size();
        jsonObject.put("scriptExecLogSize", size);
        jsonObject.put("timerMatchSecond", systemConfig.isTimerMatchSecond());
        //
        return JsonMessage.success("", jsonObject);
    }

    /**
     * 清空缓存
     *
     * @param type 缓存类型
     * @return json
     */
    @RequestMapping(value = "clearCache", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<String> clearCache(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.type_error.395f") String type) {
        switch (type) {
            case "pidPort":
                AbstractProjectCommander.PID_PORT.clear();
                break;
            case "oldJarsSize": {
                File oldJarsPath = JpomManifest.getOldJarsPath();
                boolean clean = CommandUtil.systemFastDel(oldJarsPath);
                Assert.state(!clean, I18nMessageUtil.get("i18n.clear_old_version_package_failed.021c"));
                break;
            }
            case "fileSize": {
                File tempPath = configBean.getTempPath();
                boolean clean = CommandUtil.systemFastDel(tempPath);
                Assert.state(!clean, I18nMessageUtil.get("i18n.clear_file_cache_failed.5cd1"));
                break;
            }
            default:
                return new JsonMessage<>(405, I18nMessageUtil.get("i18n.no_type_specified.8c65") + type);

        }
        return JsonMessage.success(I18nMessageUtil.get("i18n.clear_success.2685"));
    }

    @Override
    public void refreshCache() {
        File file = configBean.getTempPath();
        this.tempFileSize = FileUtil.size(file);
        this.dataSize = configBean.dataSize();
        File oldJarsPath = JpomManifest.getOldJarsPath();
        this.oldJarsSize = FileUtil.size(oldJarsPath);
    }
}
