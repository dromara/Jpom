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

import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.task.Task;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.common.BaseServerController;
import io.jpom.cron.CronUtils;
import io.jpom.cron.ICron;
import io.jpom.model.data.UserModel;
import io.jpom.model.script.ScriptExecuteLogModel;
import io.jpom.model.script.ScriptModel;
import io.jpom.service.h2db.BaseWorkspaceService;
import io.jpom.socket.ScriptProcessBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2022/1/19
 */
@Service
@Slf4j
public class ScriptServer extends BaseWorkspaceService<ScriptModel> implements ICron<ScriptModel> {

    @Override
    public List<ScriptModel> queryStartingList() {
        String sql = "select * from " + super.getTableName() + " where autoExecCron is not null and autoExecCron <> ''";
        return super.queryList(sql);
    }

    @Override
    public void insert(ScriptModel scriptModel) {
        super.insert(scriptModel);
        this.checkCron(scriptModel);
    }

    @Override
    public int updateById(ScriptModel info, HttpServletRequest request) {
        int update = super.updateById(info, request);
        if (update > 0) {
            this.checkCron(info);
        }
        return update;
    }

    @Override
    public int delByKey(String keyValue, HttpServletRequest request) {
        int delByKey = super.delByKey(keyValue, request);
        if (delByKey > 0) {
            String taskId = "server_script:" + keyValue;
            CronUtils.remove(taskId);
        }
        return delByKey;
    }

    /**
     * 检查定时任务 状态
     *
     * @param scriptModel 构建信息
     */
    @Override
    public boolean checkCron(ScriptModel scriptModel) {
        String id = scriptModel.getId();
        String taskId = "server_script:" + id;
        String autoExecCron = scriptModel.getAutoExecCron();
        if (StrUtil.isEmpty(autoExecCron)) {
            CronUtils.remove(taskId);
            return false;
        }
        log.debug("start script cron {} {} {}", id, scriptModel.getName(), autoExecCron);
        CronUtils.upsert(taskId, autoExecCron, new CronTask(id));
        return true;
    }


    /**
     * 将服务端 脚本信息同步到其他工作空间
     *
     * @param ids            多给节点ID
     * @param nowWorkspaceId 当前的工作空间ID
     * @param workspaceId    同步到哪个工作空间
     */
    public void syncToWorkspace(String ids, String nowWorkspaceId, String workspaceId) {
        StrUtil.splitTrim(ids, StrUtil.COMMA)
            .forEach(id -> {
                ScriptModel data = super.getByKey(id, false, entity -> entity.set("workspaceId", nowWorkspaceId));
                Assert.notNull(data, "没有对应到脚本信息");
                //
                ScriptModel where = new ScriptModel();
                where.setWorkspaceId(workspaceId);
                where.setName(data.getName());
                ScriptModel exits = super.queryByBean(where);
                if (exits == null) {
                    // 不存在则添加 信息
                    data.setId(null);
                    data.setWorkspaceId(workspaceId);
                    data.setCreateTimeMillis(null);
                    data.setModifyTimeMillis(null);
                    data.setNodeIds(null);
                    data.setModifyUser(null);
                    super.insert(data);
                } else {
                    // 修改信息
                    ScriptModel update = new ScriptModel();
                    update.setId(exits.getId());
                    update.setContext(data.getContext());
                    update.setDefArgs(data.getDefArgs());
                    update.setDescription(data.getDescription());
                    update.setAutoExecCron(data.getAutoExecCron());
                    super.updateById(update);
                }
            });
    }

    private static class CronTask implements Task {

        private final String id;

        public CronTask(String id) {
            this.id = id;
        }

        @Override
        public void execute() {
            try {
                BaseServerController.resetInfo(UserModel.EMPTY);
                ScriptServer nodeScriptServer = SpringUtil.getBean(ScriptServer.class);
                ScriptModel scriptServerItem = nodeScriptServer.getByKey(id);
                if (scriptServerItem == null) {
                    return;
                }
                // 创建记录
                ScriptExecuteLogServer execLogServer = SpringUtil.getBean(ScriptExecuteLogServer.class);
                ScriptExecuteLogModel nodeScriptExecLogModel = execLogServer.create(scriptServerItem, 1);
                // 执行
                ScriptProcessBuilder.create(scriptServerItem, nodeScriptExecLogModel.getId(), scriptServerItem.getDefArgs());
            } catch (Exception e) {
                log.error("触发自动执行命令模版异常", e);
            } finally {
                BaseServerController.removeEmpty();
            }
        }
    }
}
