/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
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
import org.dromara.jpom.common.i18n.I18nMessageUtil;
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
    @RequestMapping(value = "/build/history/download_file", method = RequestMethod.GET)
    @Feature(method = MethodFeature.DOWNLOAD)
    public void downloadFile(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.no_data.1ac0") String logId, HttpServletRequest request, HttpServletResponse response) {
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
    public void downloadFile(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.no_data.1ac0") String buildId,
                             @ValidatorItem(ValidatorRule.NUMBERS) int buildNumberId,
                             HttpServletResponse response,
                             HttpServletRequest request) {
        String workspaceId = dbBuildHistoryLogService.getCheckUserWorkspace(request);
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
            ServletUtil.write(response, JsonMessage.getString(404, I18nMessageUtil.get("i18n.build_record_not_exist.8186")), MediaType.APPLICATION_JSON_VALUE);
            return;
        }
        EnvironmentMapBuilder environmentMapBuilder = buildHistoryLog.toEnvironmentMapBuilder();
        boolean tarGz = environmentMapBuilder.getBool(BuildUtil.USE_TAR_GZ, false);
        File resultDirFile = BuildUtil.getHistoryPackageFile(buildHistoryLog.getBuildDataId(), buildHistoryLog.getBuildNumberId(), buildHistoryLog.getResultDirFile());
        File dirPackage = BuildUtil.loadDirPackage(buildHistoryLog.getBuildDataId(), buildHistoryLog.getBuildNumberId(), resultDirFile, tarGz, (aBoolean, file) -> file);
        ServletUtil.write(response, dirPackage);
    }


    @RequestMapping(value = "/build/history/download_log", method = RequestMethod.GET)
    @Feature(method = MethodFeature.DOWNLOAD)
    public void downloadLog(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.no_data.1ac0") String logId, HttpServletRequest request, HttpServletResponse response) {
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
    public IJsonMessage<PageResultDto<BuildHistoryLog>> historyList(HttpServletRequest request) {
        PageResultDto<BuildHistoryLog> pageResultTemp = dbBuildHistoryLogService.listPage(request);
        pageResultTemp.each(buildHistoryLog -> {
            File file = BuildUtil.getHistoryPackageFile(buildHistoryLog.getBuildDataId(), buildHistoryLog.getBuildNumberId(), buildHistoryLog.getResultDirFile());
            buildHistoryLog.setHasFile(FileUtil.isNotEmpty(file));
            //
            File logFile = BuildUtil.getLogFile(buildHistoryLog.getBuildDataId(), buildHistoryLog.getBuildNumberId());
            buildHistoryLog.setHasLog(FileUtil.exist(logFile));
        });
        return JsonMessage.success(I18nMessageUtil.get("i18n.get_success.fb55"), pageResultTemp);
    }

    /**
     * 删除构建历史，支持批量删除，用逗号分隔
     *
     * @param logId id
     * @return json
     */
    @RequestMapping(value = "/build/history/delete_log.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<String> delete(@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.no_data.1ac0")) String logId, HttpServletRequest request) {
        List<String> strings = StrUtil.splitTrim(logId, StrUtil.COMMA);
        for (String itemId : strings) {
            BuildHistoryLog buildHistoryLog = dbBuildHistoryLogService.getByKey(itemId, request);
            IJsonMessage<String> jsonMessage = dbBuildHistoryLogService.deleteLogAndFile(buildHistoryLog);
            if (!jsonMessage.success()) {
                return jsonMessage;
            }
        }
        return new JsonMessage<>(200, I18nMessageUtil.get("i18n.delete_success.0007"));
    }
}
