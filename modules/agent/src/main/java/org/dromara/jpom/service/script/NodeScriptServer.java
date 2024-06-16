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

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.extra.spring.SpringUtil;
import cn.keepbx.jpom.cron.ICron;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.AgentConst;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.cron.CronUtils;
import org.dromara.jpom.model.data.NodeScriptExecLogModel;
import org.dromara.jpom.model.data.NodeScriptModel;
import org.dromara.jpom.script.NodeScriptProcessBuilder;
import org.dromara.jpom.service.BaseWorkspaceOptService;
import org.dromara.jpom.system.ExtConfigBean;
import org.dromara.jpom.util.CommandUtil;
import org.dromara.jpom.util.FileUtils;
import org.dromara.jpom.util.StringUtil;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 脚本模板管理
 *
 * @author bwcx_jzy
 * @since 2019/4/24
 */
@Service
public class NodeScriptServer extends BaseWorkspaceOptService<NodeScriptModel> implements ICron<NodeScriptModel> {
    private final NodeScriptExecLogServer execLogServer;
    private final JpomApplication jpomApplication;
    private final ScriptLibraryService scriptLibraryService;

    public NodeScriptServer(NodeScriptExecLogServer execLogServer,
                            JpomApplication jpomApplication,
                            ScriptLibraryService scriptLibraryService) {
        super(AgentConst.SCRIPT);
        this.execLogServer = execLogServer;
        this.jpomApplication = jpomApplication;
        this.scriptLibraryService = scriptLibraryService;
    }

    @Override
    public void addItem(NodeScriptModel nodeScriptModel) {
        super.addItem(nodeScriptModel);
        this.checkCron(nodeScriptModel);
    }

    @Override
    public void updateItem(NodeScriptModel nodeScriptModel) {
        super.updateItem(nodeScriptModel);
        this.checkCron(nodeScriptModel);
    }

    /**
     * @param id 数据id
     * @see NodeScriptModel#logFile(String)
     */
    @Override
    public void deleteItem(String id) {
        NodeScriptModel nodeScriptModel = getItem(id);
        if (nodeScriptModel != null) {
            File file = nodeScriptModel.scriptPath();
            FileUtil.del(file);
        }
        super.deleteItem(id);
        String taskId = "script:" + id;
        CronUtils.remove(taskId);
    }

    public File toExecuteFile(NodeScriptModel nodeScriptModel) {
        String dataPath = jpomApplication.getDataPath();
        File scriptFile = FileUtil.file(dataPath, Const.SCRIPT_RUN_CACHE_DIRECTORY, StrUtil.format("{}.{}", IdUtil.fastSimpleUUID(), CommandUtil.SUFFIX));
        String context = nodeScriptModel.getContext();
        //
        context = scriptLibraryService.referenceReplace(context);
        FileUtils.writeScript(context, scriptFile, ExtConfigBean.getConsoleLogCharset());
        return scriptFile;
    }

    @Override
    public boolean checkCron(NodeScriptModel nodeScriptModel) {
        String id = "script:" + nodeScriptModel.getId();
        String autoExecCron = nodeScriptModel.getAutoExecCron();
        autoExecCron = StringUtil.parseCron(autoExecCron);
        if (StrUtil.isEmpty(autoExecCron)) {
            CronUtils.remove(id);
            return false;
        } else {
            CronUtils.upsert(id, autoExecCron, new CronTask(nodeScriptModel.getId()));
            return true;
        }
    }

    @Override
    public List<NodeScriptModel> queryStartingList() {
        return this.list();
    }

    private static class CronTask implements Task {
        private final String id;

        public CronTask(String id) {
            this.id = id;
        }

        @Override
        public void execute() {
            NodeScriptServer nodeScriptServer = SpringUtil.getBean(NodeScriptServer.class);
            NodeScriptModel scriptServerItem = nodeScriptServer.getItem(id);
            if (scriptServerItem == null) {
                return;
            }
            // 创建记录
            NodeScriptExecLogServer execLogServer = SpringUtil.getBean(NodeScriptExecLogServer.class);
            NodeScriptExecLogModel nodeScriptExecLogModel = new NodeScriptExecLogModel();
            nodeScriptExecLogModel.setId(IdUtil.fastSimpleUUID());
            nodeScriptExecLogModel.setCreateTimeMillis(SystemClock.now());
            nodeScriptExecLogModel.setScriptId(scriptServerItem.getId());
            nodeScriptExecLogModel.setScriptName(scriptServerItem.getName());
            nodeScriptExecLogModel.setWorkspaceId(scriptServerItem.getWorkspaceId());
            nodeScriptExecLogModel.setTriggerExecType(1);
            execLogServer.addItem(nodeScriptExecLogModel);
            // 执行
            NodeScriptProcessBuilder.create(scriptServerItem, nodeScriptExecLogModel.getId(), scriptServerItem.getDefArgs(), null);
        }
    }

    /**
     * 执行脚本
     *
     * @param scriptServerItem 脚本
     * @param type             类型
     * @param args             参数
     * @return 执行记录ID
     */
    public String execute(NodeScriptModel scriptServerItem, int type, String uerName, String workspaceId, String args, Map<String, String> paramMap) {
        NodeScriptExecLogModel nodeScriptExecLogModel = new NodeScriptExecLogModel();
        nodeScriptExecLogModel.setId(IdUtil.fastSimpleUUID());
        nodeScriptExecLogModel.setCreateTimeMillis(SystemClock.now());
        nodeScriptExecLogModel.setScriptId(scriptServerItem.getId());
        nodeScriptExecLogModel.setScriptName(scriptServerItem.getName());
        nodeScriptExecLogModel.setModifyUser(uerName);
        nodeScriptExecLogModel.setWorkspaceId(StrUtil.emptyToDefault(workspaceId, scriptServerItem.getWorkspaceId()));
        nodeScriptExecLogModel.setTriggerExecType(type);
        execLogServer.addItem(nodeScriptExecLogModel);
        String userArgs = StrUtil.emptyToDefault(args, scriptServerItem.getDefArgs());
        // 执行
        NodeScriptProcessBuilder.create(scriptServerItem, nodeScriptExecLogModel.getId(), userArgs, paramMap);
        return nodeScriptExecLogModel.getId();
    }
}
