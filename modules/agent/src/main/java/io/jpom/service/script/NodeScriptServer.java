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
package io.jpom.service.script;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.extra.spring.SpringUtil;
import io.jpom.common.AgentConst;
import io.jpom.cron.CronUtils;
import io.jpom.cron.ICron;
import io.jpom.model.data.NodeScriptExecLogModel;
import io.jpom.model.data.NodeScriptModel;
import io.jpom.script.ScriptProcessBuilder;
import io.jpom.service.BaseWorkspaceOptService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 脚本模板管理
 *
 * @author jiangzeyin
 * @since 2019/4/24
 */
@Service
public class NodeScriptServer extends BaseWorkspaceOptService<NodeScriptModel> implements ICron<NodeScriptModel> {
    private final NodeScriptExecLogServer execLogServer;

    public NodeScriptServer(NodeScriptExecLogServer execLogServer) {
        super(AgentConst.SCRIPT);
        this.execLogServer = execLogServer;
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

    @Override
    public boolean checkCron(NodeScriptModel nodeScriptModel) {
        String id = "script:" + nodeScriptModel.getId();
        if (StrUtil.isEmpty(nodeScriptModel.getAutoExecCron())) {
            CronUtils.remove(id);
            return false;
        } else {
            CronUtils.upsert(id, nodeScriptModel.getAutoExecCron(), new CronTask(nodeScriptModel.getId()));
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
            ScriptProcessBuilder.create(scriptServerItem, nodeScriptExecLogModel.getId(), scriptServerItem.getDefArgs(), null);
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
    public String execute(NodeScriptModel scriptServerItem, int type, String uerName, String args) {
        NodeScriptExecLogModel nodeScriptExecLogModel = new NodeScriptExecLogModel();
        nodeScriptExecLogModel.setId(IdUtil.fastSimpleUUID());
        nodeScriptExecLogModel.setCreateTimeMillis(SystemClock.now());
        nodeScriptExecLogModel.setScriptId(scriptServerItem.getId());
        nodeScriptExecLogModel.setScriptName(scriptServerItem.getName());
        nodeScriptExecLogModel.setModifyUser(uerName);
        nodeScriptExecLogModel.setWorkspaceId(scriptServerItem.getWorkspaceId());
        nodeScriptExecLogModel.setTriggerExecType(type);
        execLogServer.addItem(nodeScriptExecLogModel);
        String userArgs = StrUtil.emptyToDefault(args, scriptServerItem.getDefArgs());
        // 执行
        ScriptProcessBuilder.create(scriptServerItem, nodeScriptExecLogModel.getId(), userArgs, null);
        return nodeScriptExecLogModel.getId();
    }

    /**
     * 执行脚本
     *
     * @param scriptServerItem 脚本
     * @param type             类型
     * @param args             参数
     * @return 执行记录ID
     */
    public String execute(NodeScriptModel scriptServerItem, int type, String uerName, String args, Map<String, String> paramMap) {
        NodeScriptExecLogModel nodeScriptExecLogModel = new NodeScriptExecLogModel();
        nodeScriptExecLogModel.setId(IdUtil.fastSimpleUUID());
        nodeScriptExecLogModel.setCreateTimeMillis(SystemClock.now());
        nodeScriptExecLogModel.setScriptId(scriptServerItem.getId());
        nodeScriptExecLogModel.setScriptName(scriptServerItem.getName());
        nodeScriptExecLogModel.setModifyUser(uerName);
        nodeScriptExecLogModel.setWorkspaceId(scriptServerItem.getWorkspaceId());
        nodeScriptExecLogModel.setTriggerExecType(type);
        execLogServer.addItem(nodeScriptExecLogModel);
        String userArgs = StrUtil.emptyToDefault(args, scriptServerItem.getDefArgs());
        // 执行
        ScriptProcessBuilder.create(scriptServerItem, nodeScriptExecLogModel.getId(), userArgs, paramMap);
        return nodeScriptExecLogModel.getId();
    }
}
