/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.script;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.func.assets.server.ScriptLibraryServer;
import org.dromara.jpom.model.data.CommandExecLogModel;
import org.dromara.jpom.model.script.ScriptExecuteLogModel;
import org.dromara.jpom.model.script.ScriptModel;
import org.dromara.jpom.service.h2db.BaseGlobalOrWorkspaceService;
import org.dromara.jpom.system.ExtConfigBean;
import org.dromara.jpom.util.CommandUtil;
import org.dromara.jpom.util.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;

/**
 * @author bwcx_jzy
 * @since 2022/1/19
 */
@Service
public class ScriptExecuteLogServer extends BaseGlobalOrWorkspaceService<ScriptExecuteLogModel> {

    private final ScriptLibraryServer scriptLibraryServer;
    private final JpomApplication jpomApplication;

    public ScriptExecuteLogServer(ScriptLibraryServer scriptLibraryServer,
                                  JpomApplication jpomApplication) {
        this.scriptLibraryServer = scriptLibraryServer;
        this.jpomApplication = jpomApplication;
    }

    /**
     * 创建执行记录
     *
     * @param scriptModel 脚本
     * @param type        执行类型
     * @return 对象
     */
    public ScriptExecuteLogModel create(ScriptModel scriptModel, int type) {
        return this.create(scriptModel, type, scriptModel.getWorkspaceId());
    }


    /**
     * 创建执行记录
     *
     * @param scriptModel 脚本
     * @param type        执行类型
     * @return 对象
     */
    public ScriptExecuteLogModel create(ScriptModel scriptModel, int type, String workspaceId) {
        ScriptExecuteLogModel scriptExecuteLogModel = new ScriptExecuteLogModel();
        scriptExecuteLogModel.setScriptId(scriptModel.getId());
        scriptExecuteLogModel.setScriptName(scriptModel.getName());
        scriptExecuteLogModel.setTriggerExecType(type);
        scriptExecuteLogModel.setWorkspaceId(workspaceId);
        super.insert(scriptExecuteLogModel);
        return scriptExecuteLogModel;
    }

    /**
     * 修改执行状态
     *
     * @param id     ID
     * @param status 状态
     */
    public void updateStatus(String id, CommandExecLogModel.Status status) {
        this.updateStatus(id, status, null);
    }

    /**
     * 修改执行状态
     *
     * @param id       ID
     * @param status   状态
     * @param exitCode 退出码
     */
    public void updateStatus(String id, CommandExecLogModel.Status status, Integer exitCode) {
        ScriptExecuteLogModel model = new ScriptExecuteLogModel();
        model.setId(id);
        model.setExitCode(exitCode);
        model.setStatus(status.getCode());
        this.updateById(model);
    }

    /**
     * 加载脚本文件
     *
     * @param scriptModel 脚本对象
     * @return file
     */
    public File toExecLogFile(ScriptModel scriptModel) {
        InputStream templateInputStream = ExtConfigBean.getConfigResourceInputStream("/exec/template." + CommandUtil.SUFFIX);
        String defaultTemplate = IoUtil.readUtf8(templateInputStream);
        String context = defaultTemplate + scriptModel.getContext();
        // 替换全局变量
        context = scriptLibraryServer.referenceReplace(context);
        //
        String dataPath = jpomApplication.getDataPath();
        File scriptFile = FileUtil.file(dataPath, Const.SCRIPT_RUN_CACHE_DIRECTORY, StrUtil.format("{}.{}", IdUtil.fastSimpleUUID(), CommandUtil.SUFFIX));
        FileUtils.writeScript(context, scriptFile, ExtConfigBean.getConsoleLogCharset());
        return scriptFile;
    }


    @Override
    protected void executeClearImpl(int h2DbLogStorageCount) {
        super.autoLoopClear("createTimeMillis", h2DbLogStorageCount, null, scriptExecuteLogModel -> {
            File logFile = ScriptModel.logFile(scriptExecuteLogModel.getScriptId(), scriptExecuteLogModel.getId());
            boolean fastDel = CommandUtil.systemFastDel(logFile);
            return !fastDel;
        });
    }

    @Override
    protected String[] clearTimeColumns() {
        return super.clearTimeColumns();
    }
}
