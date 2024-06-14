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

import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.extra.spring.SpringUtil;
import cn.keepbx.jpom.cron.ICron;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.cron.CronUtils;
import org.dromara.jpom.model.script.ScriptExecuteLogModel;
import org.dromara.jpom.model.script.ScriptModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.ITriggerToken;
import org.dromara.jpom.service.h2db.BaseGlobalOrWorkspaceService;
import org.dromara.jpom.socket.ServerScriptProcessBuilder;
import org.dromara.jpom.util.StringUtil;
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
public class ScriptServer extends BaseGlobalOrWorkspaceService<ScriptModel> implements ICron<ScriptModel>, ITriggerToken {

    @Override
    public List<ScriptModel> queryStartingList() {
        String sql = "select * from " + super.getTableName() + " where autoExecCron is not null and autoExecCron <> ''";
        return super.queryList(sql);
    }

    @Override
    public int insert(ScriptModel scriptModel) {
        int count = super.insert(scriptModel);
        this.checkCron(scriptModel);
        return count;
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
        autoExecCron = StringUtil.parseCron(autoExecCron);
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
                Assert.notNull(data, I18nMessageUtil.get("i18n.no_corresponding_script_info_or_global_script_selected.765b"));
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

    @Override
    public String typeName() {
        return getTableName();
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
                ServerScriptProcessBuilder.create(scriptServerItem, nodeScriptExecLogModel.getId(), scriptServerItem.getDefArgs());
            } finally {
                BaseServerController.removeEmpty();
            }
        }
    }
}
