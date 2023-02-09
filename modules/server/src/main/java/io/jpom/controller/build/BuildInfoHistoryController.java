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
package io.jpom.controller.build;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import io.jpom.build.BuildUtil;
import io.jpom.common.BaseServerController;
import io.jpom.common.JsonMessage;
import io.jpom.common.validator.ValidatorConfig;
import io.jpom.common.validator.ValidatorItem;
import io.jpom.common.validator.ValidatorRule;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.model.log.BuildHistoryLog;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.service.dblog.BuildInfoService;
import io.jpom.service.dblog.DbBuildHistoryLogService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import top.jpom.model.PageResultDto;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * new version for build info history controller
 *
 * @author Hotstrip
 * @since 2021-08-26
 */
@RestController
@Feature(cls = ClassFeature.BUILD_LOG)
public class BuildInfoHistoryController extends BaseServerController {

    private final BuildInfoService buildInfoService;
    private final DbBuildHistoryLogService dbBuildHistoryLogService;

    public BuildInfoHistoryController(BuildInfoService buildInfoService,
                                      DbBuildHistoryLogService dbBuildHistoryLogService) {
        this.buildInfoService = buildInfoService;
        this.dbBuildHistoryLogService = dbBuildHistoryLogService;
    }

    /**
     * 下载构建物
     *
     * @param logId 日志id
     */
    @RequestMapping(value = "/build/history/download_file.html", method = RequestMethod.GET)
    @Feature(method = MethodFeature.DOWNLOAD)
    public void downloadFile(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String logId, HttpServletResponse response) {
        BuildHistoryLog buildHistoryLog = dbBuildHistoryLogService.getByKey(logId, getRequest());
        this.downloadFile(buildHistoryLog, response);
    }

    /**
     * 下载构建物
     *
     * @param buildId       构建ID
     * @param buildNumberId 构建序号ID
     */
    @RequestMapping(value = "/build/history/download_file_by_build", method = RequestMethod.GET)
    @Feature(method = MethodFeature.DOWNLOAD)
    public void downloadFile(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String buildId,
                             @ValidatorItem(ValidatorRule.NUMBERS) int buildNumberId,
                             HttpServletResponse response) {
        String workspaceId = dbBuildHistoryLogService.getCheckUserWorkspace(getRequest());
        //
        BuildHistoryLog historyLog = new BuildHistoryLog();
        historyLog.setWorkspaceId(workspaceId);
        historyLog.setBuildDataId(buildId);
        historyLog.setBuildNumberId(buildNumberId);
        List<BuildHistoryLog> buildHistoryLogs = dbBuildHistoryLogService.listByBean(historyLog);
        BuildHistoryLog first = CollUtil.getFirst(buildHistoryLogs);
        this.downloadFile(first, response);
    }

    private void downloadFile(BuildHistoryLog buildHistoryLog, HttpServletResponse response) {
        if (buildHistoryLog == null) {
            ServletUtil.write(response, JsonMessage.getString(404, "构建记录不存在"), MediaType.APPLICATION_JSON_VALUE);
            return;
        }
        File resultDirFile = BuildUtil.getHistoryPackageFile(buildHistoryLog.getBuildDataId(), buildHistoryLog.getBuildNumberId(), buildHistoryLog.getResultDirFile());
        File dirPackage = BuildUtil.loadDirPackage(buildHistoryLog.getBuildDataId(), buildHistoryLog.getBuildNumberId(), resultDirFile, (aBoolean, file) -> file);
        ServletUtil.write(response, dirPackage);
    }


    @RequestMapping(value = "/build/history/download_log.html", method = RequestMethod.GET)
    @Feature(method = MethodFeature.DOWNLOAD)
    public void downloadLog(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String logId, HttpServletResponse response) {
        BuildHistoryLog buildHistoryLog = dbBuildHistoryLogService.getByKey(logId, getRequest());
        Objects.requireNonNull(buildHistoryLog);
        BuildInfoModel item = buildInfoService.getByKey(buildHistoryLog.getBuildDataId());
        Objects.requireNonNull(item);
        File logFile = BuildUtil.getLogFile(item.getId(), buildHistoryLog.getBuildNumberId());
        if (!FileUtil.exist(logFile)) {
            return;
        }
        if (logFile.isFile()) {
            ServletUtil.write(response, logFile);
        }
    }

    @RequestMapping(value = "/build/history/history_list.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<PageResultDto<BuildHistoryLog>> historyList() {
        PageResultDto<BuildHistoryLog> pageResultTemp = dbBuildHistoryLogService.listPage(getRequest());
        pageResultTemp.each(buildHistoryLog -> {
            File file = BuildUtil.getHistoryPackageFile(buildHistoryLog.getBuildDataId(), buildHistoryLog.getBuildNumberId(), buildHistoryLog.getResultDirFile());
            buildHistoryLog.setHasFile(FileUtil.isNotEmpty(file));
            //
            File logFile = BuildUtil.getLogFile(buildHistoryLog.getBuildDataId(), buildHistoryLog.getBuildNumberId());
            buildHistoryLog.setHasLog(FileUtil.exist(logFile));
        });
        return JsonMessage.success("获取成功", pageResultTemp);
    }

    /**
     * 删除构建历史，支持批量删除，用逗号分隔
     *
     * @param logId id
     * @return json
     */
    @RequestMapping(value = "/build/history/delete_log.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public JsonMessage<String> delete(@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据")) String logId) {
        List<String> strings = StrUtil.splitTrim(logId, StrUtil.COMMA);
        for (String itemId : strings) {
            BuildHistoryLog buildHistoryLog = dbBuildHistoryLogService.getByKey(itemId, getRequest());
            Objects.requireNonNull(buildHistoryLog);
            JsonMessage<String> jsonMessage = dbBuildHistoryLogService.deleteLogAndFile(buildHistoryLog);
            if (jsonMessage.getCode() != 200) {
                return jsonMessage;
            }
        }
        return new JsonMessage<>(200, "删除成功");
    }
}
