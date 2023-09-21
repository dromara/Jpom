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
package org.dromara.jpom.controller.build;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import org.dromara.jpom.build.BuildUtil;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.validator.ValidatorConfig;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.model.EnvironmentMapBuilder;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.BuildInfoModel;
import org.dromara.jpom.model.log.BuildHistoryLog;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.service.dblog.BuildInfoService;
import org.dromara.jpom.service.dblog.DbBuildHistoryLogService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
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
    public void downloadFile(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String logId, HttpServletRequest request, HttpServletResponse response) {
        BuildHistoryLog buildHistoryLog = dbBuildHistoryLogService.getByKey(logId, request, false);
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
        List<BuildHistoryLog> buildHistoryLogs = dbBuildHistoryLogService.listByBean(historyLog, false);
        BuildHistoryLog first = CollUtil.getFirst(buildHistoryLogs);
        this.downloadFile(first, response);
    }

    private void downloadFile(BuildHistoryLog buildHistoryLog, HttpServletResponse response) {
        if (buildHistoryLog == null) {
            ServletUtil.write(response, JsonMessage.getString(404, "构建记录不存在"), MediaType.APPLICATION_JSON_VALUE);
            return;
        }
        EnvironmentMapBuilder environmentMapBuilder = buildHistoryLog.toEnvironmentMapBuilder();
        boolean tarGz = environmentMapBuilder.getBool(BuildUtil.USE_TAR_GZ, false);
        File resultDirFile = BuildUtil.getHistoryPackageFile(buildHistoryLog.getBuildDataId(), buildHistoryLog.getBuildNumberId(), buildHistoryLog.getResultDirFile());
        File dirPackage = BuildUtil.loadDirPackage(buildHistoryLog.getBuildDataId(), buildHistoryLog.getBuildNumberId(), resultDirFile, tarGz, (aBoolean, file) -> file);
        ServletUtil.write(response, dirPackage);
    }


    @RequestMapping(value = "/build/history/download_log.html", method = RequestMethod.GET)
    @Feature(method = MethodFeature.DOWNLOAD)
    public void downloadLog(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String logId, HttpServletRequest request, HttpServletResponse response) {
        BuildHistoryLog buildHistoryLog = dbBuildHistoryLogService.getByKey(logId, request);
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
    public IJsonMessage<PageResultDto<BuildHistoryLog>> historyList() {
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
    public IJsonMessage<String> delete(@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据")) String logId) {
        List<String> strings = StrUtil.splitTrim(logId, StrUtil.COMMA);
        for (String itemId : strings) {
            BuildHistoryLog buildHistoryLog = dbBuildHistoryLogService.getByKey(itemId, getRequest());
            IJsonMessage<String> jsonMessage = dbBuildHistoryLogService.deleteLogAndFile(buildHistoryLog);
            if (!jsonMessage.success()) {
                return jsonMessage;
            }
        }
        return new JsonMessage<>(200, "删除成功");
    }
}
