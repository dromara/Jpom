/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.service;

import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.dialect.DialectUtil;
import org.dromara.jpom.pipeline.model.PipelineInfoModel;
import org.dromara.jpom.service.ITriggerToken;
import org.dromara.jpom.service.h2db.BaseWorkspaceService;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 流水线配置服务
 *
 * @author jpom
 * @since 2.11.16
 */
@Service
public class PipelineInfoService extends BaseWorkspaceService<PipelineInfoModel> implements ITriggerToken {

    @Override
    public String typeName() {
        return getTableName();
    }

    @Override
    public boolean exists(String dataId) {
        return this.getByKey(dataId) != null;
    }

    /**
     * 分组列表（当前工作空间内去重）
     * <p>
     * 与在线构建、仓库、SSH、FTP、项目同一套「虚拟逻辑分组」语义：分组不是独立管理的实体，
     * 而是已有记录分组字段的去重结果，新增分组随记录保存自动出现（无需单独建分组表）。
     * </p>
     * <p>
     * 必须覆盖父类：{@link BaseWorkspaceService#listGroup(HttpServletRequest)} 查的是 {@code group} 列，
     * 而流水线的分组字段是 {@code groupName}（见 {@link PipelineInfoModel}）。
     * </p>
     *
     * @param request 请求
     * @return 分组名称列表（不含空分组：没填分组的记录不产生下拉候选项）
     */
    @Override
    public List<String> listGroup(HttpServletRequest request) {
        String workspaceId = getCheckUserWorkspace(request);
        String group = DialectUtil.wrapField("groupName");
        String sql = String.format("select %s from %s where workspaceId=? group by %s", group, getTableName(), group);
        List<String> list = super.listGroupByName(sql, "groupName", workspaceId);
        list.removeIf(StrUtil::isBlank);
        return list;
    }

    @Override
    public void updateTriggerToken(String id, String triggerToken) {
        PipelineInfoModel update = new PipelineInfoModel();
        update.setId(id);
        update.setTriggerToken(triggerToken);
        this.updateById(update);
    }

    /**
     * 分配下一个运行编号（供产物目录与运行记录使用）
     *
     * @param id 流水线 ID
     * @return 新编号
     */
    public int nextRunNumber(String id) {
        PipelineInfoModel item = this.getByKey(id);
        int next = (item == null || item.getRunNumber() == null ? 0 : item.getRunNumber()) + 1;
        PipelineInfoModel update = new PipelineInfoModel();
        update.setId(id);
        update.setRunNumber(next);
        this.updateById(update);
        return next;
    }

    /**
     * 更新运行状态
     *
     * @param id     流水线 ID
     * @param status 状态码
     * @param msg    说明
     * @param runId  运行记录 ID
     */
    public void updateRunStatus(String id, Integer status, String msg, String runId) {
        PipelineInfoModel update = new PipelineInfoModel();
        update.setId(id);
        update.setStatus(status);
        update.setStatusMsg(msg);
        update.setLastRunId(runId);
        this.updateById(update);
    }
}
