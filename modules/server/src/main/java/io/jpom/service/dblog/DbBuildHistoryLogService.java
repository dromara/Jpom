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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpStatus;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.build.BuildUtil;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.model.log.BuildHistoryLog;
import io.jpom.service.h2db.BaseWorkspaceService;
import io.jpom.system.extconf.BuildExtConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * 构建历史db
 *
 * @author bwcx_jzy
 * @since 2019/7/20
 */
@Service
@Slf4j
public class DbBuildHistoryLogService extends BaseWorkspaceService<BuildHistoryLog> {

    private final BuildInfoService buildService;
    private final BuildExtConfig buildExtConfig;

    public DbBuildHistoryLogService(BuildInfoService buildService,
                                    BuildExtConfig buildExtConfig) {
        this.buildService = buildService;
        this.buildExtConfig = buildExtConfig;
    }

//	/**
//	 * 更新状态
//	 *
//	 * @param logId  记录id
//	 * @param status 状态
//	 */
//	public void updateLog(String logId, BuildStatus status) {
//		if (logId == null) {
//			return;
//		}
//		BuildHistoryLog buildHistoryLog = new BuildHistoryLog();
//		buildHistoryLog.setId(logId);
//		buildHistoryLog.setStatus(status.getCode());
//		if (status != BuildStatus.PubIng) {
//			// 结束
//			buildHistoryLog.setEndTime(SystemClock.now());
//		}
//		this.update(buildHistoryLog);
//	}

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
        this.update(buildHistoryLog);
    }

    /**
     * 清理文件并删除记录
     *
     * @param logId 记录id
     * @return json
     */
    public JsonMessage<String> deleteLogAndFile(String logId) {
        BuildHistoryLog buildHistoryLog = getByKey(logId);
        return this.deleteLogAndFile(buildHistoryLog);
    }

    /**
     * 清理文件并删除记录
     *
     * @param buildHistoryLog 构建记录
     * @return json
     */
    public JsonMessage<String> deleteLogAndFile(BuildHistoryLog buildHistoryLog) {
        if (buildHistoryLog == null) {
            return new JsonMessage<>(405, "没有对应构建记录");
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
        return new JsonMessage<>(200, "删除成功", count + "");
    }

    @Override
    public void insert(BuildHistoryLog buildHistoryLog) {
        super.insert(buildHistoryLog);
        // 清理单个
        int buildItemMaxHistoryCount = buildExtConfig.getItemMaxHistoryCount();
        super.autoLoopClear("startTime", buildItemMaxHistoryCount,
            entity -> entity.set("buildDataId", buildHistoryLog.getBuildDataId()),
            buildHistoryLog1 -> {
                JsonMessage<String> jsonMessage = this.deleteLogAndFile(buildHistoryLog1);
                if (jsonMessage.getCode() != HttpStatus.HTTP_OK) {
                    log.warn("{} {} {}", buildHistoryLog1.getBuildName(), buildHistoryLog1.getBuildNumberId(), jsonMessage);
                    return false;
                }
                return true;
            });
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
                if (jsonMessage.getCode() != HttpStatus.HTTP_OK) {
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
}
