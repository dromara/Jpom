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
package io.jpom.service.dblog;

import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.db.Entity;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.build.BuildExecuteService;
import io.jpom.common.BaseServerController;
import io.jpom.cron.CronUtils;
import io.jpom.cron.ICron;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.enums.BuildReleaseMethod;
import io.jpom.model.enums.BuildStatus;
import io.jpom.service.IStatusRecover;
import io.jpom.service.h2db.BaseGroupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 构建 service 新版本，数据从数据库里面加载
 *
 * @author Hotstrip
 * @since 2021-08-10
 **/
@Service
@Slf4j
public class BuildInfoService extends BaseGroupService<BuildInfoModel> implements ICron<BuildInfoModel>, IStatusRecover {

    /**
     * 更新状态
     *
     * @param id          ID
     * @param buildStatus to Status
     */
    public void updateStatus(String id, BuildStatus buildStatus) {
        BuildInfoModel buildInfoModel = new BuildInfoModel();
        buildInfoModel.setId(id);
        buildInfoModel.setStatus(buildStatus.getCode());
        this.update(buildInfoModel);
    }

    @Override
    public void insert(BuildInfoModel buildInfoModel) {
        super.insert(buildInfoModel);
        this.checkCron(buildInfoModel);
    }

    @Override
    public int updateById(BuildInfoModel info, HttpServletRequest request) {
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
            String taskId = "build:" + delByKey;
            CronUtils.remove(taskId);
        }
        return delByKey;
    }

    /**
     * 开启定时构建任务
     */
    @Override
    public List<BuildInfoModel> queryStartingList() {
        String sql = "select * from " + super.getTableName() + " where autoBuildCron is not null and autoBuildCron <> ''";
        return super.queryList(sql);
    }

    @Override
    public int statusRecover() {
        // 恢复异常数据
        String updateSql = "update " + super.getTableName() + " set status=? where status=? or status=?";
        return super.execute(updateSql, BuildStatus.No.getCode(), BuildStatus.Ing.getCode(), BuildStatus.PubIng.getCode());
    }

    /**
     * 检查定时任务 状态
     *
     * @param buildInfoModel 构建信息
     */
    @Override
    public boolean checkCron(BuildInfoModel buildInfoModel) {
        String id = buildInfoModel.getId();
        String taskId = "build:" + id;
        String autoBuildCron = buildInfoModel.getAutoBuildCron();
        if (StrUtil.isEmpty(autoBuildCron)) {
            CronUtils.remove(taskId);
            return false;
        }
        log.debug("start build cron {} {} {}", id, buildInfoModel.getName(), autoBuildCron);
        CronUtils.upsert(taskId, autoBuildCron, new CronTask(id, autoBuildCron));
        return true;
    }

    private static class CronTask implements Task {

        private final String buildId;
        private final String autoBuildCron;

        public CronTask(String buildId, String autoBuildCron) {
            this.buildId = buildId;
            this.autoBuildCron = autoBuildCron;
        }

        @Override
        public void execute() {
            BuildExecuteService buildExecuteService = SpringUtil.getBean(BuildExecuteService.class);
            try {
                BaseServerController.resetInfo(UserModel.EMPTY);
                buildExecuteService.start(this.buildId, null, null, 2, "auto build:" + this.autoBuildCron);
            } finally {
                BaseServerController.removeEmpty();
            }
        }
    }


    /**
     * 判断是否存在 节点关联
     *
     * @param nodeId 节点ID
     * @return true 关联
     */
    public boolean checkNode(String nodeId, HttpServletRequest request) {
        Entity entity = new Entity();
        entity.set("releaseMethod", BuildReleaseMethod.Project.getCode());
        String workspaceId = this.getCheckUserWorkspace(request);
        entity.set("workspaceId", workspaceId);
        entity.set("releaseMethodDataId", StrUtil.format(" like '{}:%'", nodeId));
        return super.exists(entity);
    }

    /**
     * 判断是否存在 发布关联
     *
     * @param dataId        数据ID
     * @param releaseMethod 发布方法
     * @param request       请求对象
     * @return true 关联
     */
    public boolean checkReleaseMethodByLike(String dataId, HttpServletRequest request, BuildReleaseMethod releaseMethod) {
        Entity entity = new Entity();
        entity.set("releaseMethod", releaseMethod.getCode());
        String workspaceId = this.getCheckUserWorkspace(request);
        entity.set("workspaceId", workspaceId);
        entity.set("releaseMethodDataId", StrUtil.format(" like '%{}%'", dataId));
        return super.exists(entity);
    }

    /**
     * 判断是否存在 发布关联
     *
     * @param dataId        数据ID
     * @param request       请求对象
     * @param releaseMethod 发布方法
     * @return true 关联
     */
    public boolean checkReleaseMethod(String dataId, HttpServletRequest request, BuildReleaseMethod releaseMethod) {
        BuildInfoModel buildInfoModel = new BuildInfoModel();
        String workspaceId = this.getCheckUserWorkspace(request);
        buildInfoModel.setWorkspaceId(workspaceId);
        buildInfoModel.setReleaseMethodDataId(dataId);
        buildInfoModel.setReleaseMethod(releaseMethod.getCode());
        return super.exists(buildInfoModel);
    }
}
