/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.sshproject;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.func.assets.model.MachineSshModel;
import org.dromara.jpom.func.assets.server.MachineSshServer;
import org.dromara.jpom.model.data.DslYmlDto;
import org.dromara.jpom.model.data.SshModel;
import org.dromara.jpom.model.data.SshProjectModel;
import org.dromara.jpom.model.enums.BuildReleaseMethod;
import org.dromara.jpom.model.sshproject.SshProjectExecuteResult;
import org.dromara.jpom.model.sshproject.SshProjectStatus;
import org.dromara.jpom.service.dblog.BuildInfoService;
import org.dromara.jpom.service.ITriggerToken;
import org.dromara.jpom.service.h2db.BaseWorkspaceService;
import org.dromara.jpom.service.node.ssh.SshService;
import org.dromara.jpom.socket.ConsoleCommandOp;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * SSH 项目
 *
 * @author bwcx_jzy
 * @since 2026/11/20
 */
@Service
public class SshProjectService extends BaseWorkspaceService<SshProjectModel> implements ITriggerToken {

    private final SshProjectExecutor sshProjectExecutor;
    private final SshProjectExecLogService sshProjectExecLogService;
    private final BuildInfoService buildInfoService;
    private final MachineSshServer machineSshServer;
    private final SshService sshService;

    public SshProjectService(SshProjectExecutor sshProjectExecutor,
                             SshProjectExecLogService sshProjectExecLogService,
                             BuildInfoService buildInfoService,
                             MachineSshServer machineSshServer,
                             SshService sshService) {
        this.sshProjectExecutor = sshProjectExecutor;
        this.sshProjectExecLogService = sshProjectExecLogService;
        this.buildInfoService = buildInfoService;
        this.machineSshServer = machineSshServer;
        this.sshService = sshService;
    }

    /**
     * 获取项目（校验工作空间权限）
     *
     * @param id      id
     * @param request 请求
     * @return 项目
     */
    public SshProjectModel getItem(String id, HttpServletRequest request) {
        SshProjectModel model = this.getByKey(id, request);
        Assert.notNull(model, I18nMessageUtil.get("i18n.ssh_project_not_exist.5d18"));
        return model;
    }

    /**
     * 保存项目（新增/修改）
     * <p>
     * 只做结构校验，脚本执行语义留给运行期；避免保存时把用户脚本"执行"一遍。
     *
     * @param model   项目
     * @param request 请求
     */
    public void saveItem(SshProjectModel model, HttpServletRequest request) {
        Assert.hasText(model.getName(), I18nMessageUtil.get("i18n.ssh_project_name_required.7a13"));
        Assert.hasText(model.getPath(), I18nMessageUtil.get("i18n.ssh_project_path_required.4e28"));
        String path = StrUtil.trim(model.getPath());
        Assert.state(!StrUtil.contains(path, ".."), I18nMessageUtil.get("i18n.ssh_project_path_required.4e28"));
        String dslContent = StrUtil.trim(model.getDslContent());
        Assert.hasText(dslContent, I18nMessageUtil.get("i18n.ssh_project_dsl_required.9e52"));
        DslYmlDto dslYmlDto;
        try {
            dslYmlDto = DslYmlDto.build(dslContent);
        } catch (Exception e) {
            throw new IllegalArgumentException(I18nMessageUtil.get("i18n.ssh_project_dsl_invalid.7c31") + e.getMessage());
        }
        Assert.notNull(dslYmlDto, I18nMessageUtil.get("i18n.ssh_project_dsl_invalid.7c31"));
        model.setPath(path);
        model.setDslContent(dslContent);
        model.setName(StrUtil.trim(model.getName()));
        // 触发器 token 只允许通过触发器地址接口生成/重置（与项目、脚本、SSH 脚本等保持一致）
        model.setTriggerToken(null);
        String id = model.getId();
        if (StrUtil.isEmpty(id)) {
            this.insert(model);
            return;
        }
        SshProjectModel old = this.getItem(id, request);
        Assert.state(!old.running(), I18nMessageUtil.get("i18n.ssh_project_running_cannot_edit.1b44"));
        this.updateById(model, request);
    }

    /**
     * 启动/停止/重启/重载
     *
     * @param id      id
     * @param opt     操作
     * @param handler 输出回调（可为空）
     * @param request 请求
     * @return 执行结果
     */
    public SshProjectExecuteResult operate(String id, String opt, LineHandler handler, HttpServletRequest request) {
        SshProjectModel project = this.getItem(id, request);
        return this.execute(project, opt, handler);
    }

    /**
     * 执行操作（不校验请求上下文，供触发器/自动启动复用）
     *
     * @param project 项目
     * @param opt     操作
     * @param handler 输出回调
     * @return 结果
     */
    public SshProjectExecuteResult execute(SshProjectModel project, String opt, LineHandler handler) {
        ConsoleCommandOp op = EnumUtil.fromStringQuietly(ConsoleCommandOp.class, opt);
        Assert.notNull(op, I18nMessageUtil.get("i18n.ssh_project_operate_unsupported.3d18") + opt);
        Assert.state(op.isCanOpt(), I18nMessageUtil.get("i18n.ssh_project_operate_unsupported.3d18") + opt);
        SshProjectExecuteResult result = sshProjectExecutor.operate(project, op, handler);
        sshProjectExecLogService.record(project, op.name(), result);
        this.refreshStatus(project, true);
        return result;
    }

    /**
     * 刷新项目状态（可持久化）
     *
     * @param project 项目
     * @param save    是否写回数据库
     * @return 状态
     */
    public SshProjectStatus refreshStatus(SshProjectModel project, boolean save) {
        SshProjectStatus status;
        try {
            status = sshProjectExecutor.status(project);
        } catch (Exception e) {
            status = SshProjectStatus.stopped(e.getMessage());
        }
        project.setLastStatus(status.isRunning() ? "running" : "stopped");
        project.setLastPid(status.getPid());
        project.setLastPort(status.getPorts());
        project.setLastStatusMsg(StrUtil.maxLength(StrUtil.nullToEmpty(status.getMessage()), 400));
        project.setLastRefreshTime(System.currentTimeMillis());
        if (save) {
            SshProjectModel update = new SshProjectModel();
            update.setId(project.getId());
            update.setLastStatus(project.getLastStatus());
            update.setLastPid(project.getLastPid());
            update.setLastPort(project.getLastPort());
            update.setLastStatusMsg(project.getLastStatusMsg());
            update.setLastRefreshTime(project.getLastRefreshTime());
            this.updateById(update);
        }
        return status;
    }

    /**
     * 填充「机器 SSH」展示名：优先资产名，其次工作空间 SSH 记录名
     *
     * @param list 列表
     */
    public void fillSshName(List<SshProjectModel> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Map<String, String> cache = new HashMap<>();
        for (SshProjectModel project : list) {
            String name = null;
            String machineSshId = project.getMachineSshId();
            if (StrUtil.isNotEmpty(machineSshId)) {
                name = cache.computeIfAbsent("m:" + machineSshId, key -> {
                    MachineSshModel machine = machineSshServer.getByKey(machineSshId, false);
                    return machine == null ? "" : machine.getName();
                });
            }
            if (StrUtil.isEmpty(name) && StrUtil.isNotEmpty(project.getSshId())) {
                name = cache.computeIfAbsent("s:" + project.getSshId(), key -> {
                    SshModel sshModel = sshService.getByKey(project.getSshId());
                    return sshModel == null ? "" : sshModel.getName();
                });
            }
            project.setSshName(StrUtil.emptyToNull(name));
        }
    }

    /**
     * 查询需要自动启动的项目
     *
     * @return list
     */
    public List<SshProjectModel> listAutoStart() {
        SshProjectModel query = new SshProjectModel();
        query.setAutoStart(true);
        return this.listByBean(query, false);
    }

    @Override
    public String typeName() {
        return getTableName();
    }

    /**
     * 删除项目
     *
     * @param id      id
     * @param request 请求
     */
    public void deleteItem(String id, HttpServletRequest request) {
        SshProjectModel item = this.getItem(id, request);
        Assert.state(!item.running(), I18nMessageUtil.get("i18n.ssh_project_running_cannot_delete.6c07"));
        // 被在线构建引用时不允许删除（与 agent 项目一致）
        // 发布方式「SSH 项目」支持多选：releaseMethodDataId 是逗号分隔列表，必须按成员精确匹配
        boolean releaseMethod = buildInfoService.checkReleaseMethodTarget(id, request, BuildReleaseMethod.SshProject);
        Assert.state(!releaseMethod, I18nMessageUtil.get("i18n.ssh_project_has_build_items_cannot_delete.4f73"));
        this.delByKey(id, request);
    }
}
