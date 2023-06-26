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
package org.dromara.jpom.service.dblog;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.keepbx.jpom.event.ISystemTask;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.build.BuildExtraModule;
import org.dromara.jpom.build.BuildUtil;
import org.dromara.jpom.common.JsonMessage;
import org.dromara.jpom.model.BaseDbModel;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.BuildInfoModel;
import org.dromara.jpom.model.enums.BuildStatus;
import org.dromara.jpom.model.log.BuildHistoryLog;
import org.dromara.jpom.service.h2db.BaseWorkspaceService;
import org.dromara.jpom.system.extconf.BuildExtConfig;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 构建历史db
 *
 * @author bwcx_jzy
 * @since 2019/7/20
 */
@Service
@Slf4j
public class DbBuildHistoryLogService extends BaseWorkspaceService<BuildHistoryLog> implements ISystemTask {

    private final BuildInfoService buildService;
    private final BuildExtConfig buildExtConfig;

    public DbBuildHistoryLogService(BuildInfoService buildService,
                                    BuildExtConfig buildExtConfig) {
        this.buildService = buildService;
        this.buildExtConfig = buildExtConfig;
    }

    /**
     * 更新状态
     *
     * @param logId         记录id
     * @param resultDirFile 构建产物目录
     */
    public void updateResultDirFile(String logId, String resultDirFile) {
        if (logId == null || StrUtil.isEmpty(resultDirFile)) {
            return;
        }

        BuildHistoryLog buildHistoryLog = new BuildHistoryLog();
        buildHistoryLog.setId(logId);
        buildHistoryLog.setResultDirFile(resultDirFile);
        this.updateById(buildHistoryLog);
    }


    /**
     * 清理文件并删除记录
     *
     * @param buildHistoryLog 构建记录
     * @return json
     */
    public JsonMessage<String> deleteLogAndFile(BuildHistoryLog buildHistoryLog) {
        if (buildHistoryLog == null) {
            return JsonMessage.success("没有对应构建记录,忽略删除");
        }
        BuildInfoModel item = buildService.getByKey(buildHistoryLog.getBuildDataId());
        if (item != null) {
            File logFile = BuildUtil.getLogFile(item.getId(), buildHistoryLog.getBuildNumberId());
            if (logFile != null) {
                File dataFile = logFile.getParentFile();
                if (dataFile.exists()) {
                    boolean s = FileUtil.del(dataFile);
                    if (!s) {
                        return new JsonMessage<>(500, "清理文件失败");
                    }
                }
            }
        }
        int count = this.delByKey(buildHistoryLog.getId());
        return new JsonMessage<>(200, "删除成功", String.valueOf(count));
    }

    @Override
    protected void fillSelectResult(BuildHistoryLog data) {
        super.fillSelectResult(data);
        Optional.ofNullable(data).ifPresent(buildHistoryLog -> {
            // 不能返回环境变量的信息（存在隐私字段）
            buildHistoryLog.setBuildEnvCache(null);
        });

    }

    @Override
    public int insert(BuildHistoryLog buildHistoryLog) {
        int count = super.insert(buildHistoryLog);
        // 清理单个
        BuildExtraModule build = BuildExtraModule.build(buildHistoryLog);
        int resultKeepCount = ObjectUtil.defaultIfNull(build.getResultKeepCount(), 0);
        int buildItemMaxHistoryCount = buildExtConfig.getItemMaxHistoryCount();
        if (resultKeepCount > 0 || buildItemMaxHistoryCount > 0) {
            // 至少有一个配置
            int useCount;
            if (resultKeepCount > 0 && buildItemMaxHistoryCount > 0) {
                // 都配置过，使用最小值
                useCount = Math.min(resultKeepCount, buildItemMaxHistoryCount);
            } else {
                // 只配置了一处，使用最大值
                useCount = Math.max(resultKeepCount, buildItemMaxHistoryCount);
            }
            super.autoLoopClear("startTime", useCount, entity -> this.fillClearWhere(entity, buildHistoryLog.getBuildDataId()), this.predicate());
        }
        return count;
    }

    private Predicate<BuildHistoryLog> predicate() {
        return buildHistoryLog1 -> {
            JsonMessage<String> jsonMessage = this.deleteLogAndFile(buildHistoryLog1);
            if (!jsonMessage.success()) {
                log.warn("{} {} {}", buildHistoryLog1.getBuildName(), buildHistoryLog1.getBuildNumberId(), jsonMessage);
                return false;
            }
            return true;
        };
    }

    private void fillClearWhere(Entity entity, String buildDataId) {
        entity.set("buildDataId", buildDataId);
        // 清理单项构建历史保留个数只判断（构建结束、发布中、发布失败、发布失败）有效构建状态，避免无法保留有效构建历史
        entity.set("status", CollUtil.newArrayList(
            BuildStatus.Success.getCode(),
            BuildStatus.PubIng.getCode(),
            BuildStatus.PubSuccess.getCode(),
            BuildStatus.PubError.getCode()));
    }

    @Override
    protected void executeClearImpl(int count) {
        // 清理总数据
        int buildMaxHistoryCount = buildExtConfig.getMaxHistoryCount();
        int saveCount = Math.min(count, buildMaxHistoryCount);
        if (saveCount <= 0) {
            // 不清除
            return;
        }
        super.autoLoopClear("startTime", saveCount,
            null,
            buildHistoryLog1 -> {
                JsonMessage<String> jsonMessage = this.deleteLogAndFile(buildHistoryLog1);
                if (!jsonMessage.success()) {
                    log.warn("{} {} {}", buildHistoryLog1.getBuildName(), buildHistoryLog1.getBuildNumberId(), jsonMessage);
                    return false;
                }
                return true;
            });
    }

    @Override
    protected String[] clearTimeColumns() {
        return super.clearTimeColumns();
    }

    @Override
    public void executeTask() {
        List<BuildInfoModel> buildInfoModels = buildService.hasResultKeep();
        if (CollUtil.isEmpty(buildInfoModels)) {
            return;
        }
        for (BuildInfoModel buildInfoModel : buildInfoModels) {
            Integer resultKeepDay = buildInfoModel.getResultKeepDay();
            if (resultKeepDay == null || resultKeepDay <= 0) {
                continue;
            }
            log.debug("自动删除过期的构建历史相关文件：{} {}", buildInfoModel.getName(), resultKeepDay);
            Entity entity = Entity.create();
            this.fillClearWhere(entity, buildInfoModel.getId());
            DateTime date = DateTime.now();
            date = DateUtil.offsetDay(date, -resultKeepDay);
            date = DateUtil.beginOfDay(date);
            entity.set("startTime", "< " + date.getTime());
            while (true) {
                Page page = new Page(1, 50);
                page.addOrder(new Order("startTime", Direction.DESC));
                PageResultDto<BuildHistoryLog> pageResult = this.listPage(entity, page);
                if (pageResult.isEmpty()) {
                    break;
                }
                List<String> ids = pageResult.getResult()
                    .stream()
                    .filter(this.predicate())
                    .map(BaseDbModel::getId)
                    .collect(Collectors.toList());
                //
                this.delByKey(ids, null);
            }
        }
    }
}
