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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import io.jpom.build.BuildUtil;
import io.jpom.common.BaseServerController;
import io.jpom.model.PageResultDto;
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
    public void downloadFile(@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据")) String logId) {
        BuildHistoryLog buildHistoryLog = dbBuildHistoryLogService.getByKey(logId);
        if (buildHistoryLog == null) {
            return;
        }
        BuildInfoModel item = buildInfoService.getByKey(buildHistoryLog.getBuildDataId());
        if (item == null) {
            return;
        }
        File logFile = BuildUtil.getHistoryPackageFile(item.getId(), buildHistoryLog.getBuildNumberId(), buildHistoryLog.getResultDirFile());
        if (!FileUtil.exist(logFile)) {
            return;
        }
        if (logFile.isFile()) {
            ServletUtil.write(getResponse(), logFile);
        } else {
            File zipFile = BuildUtil.isDirPackage(logFile);
            assert zipFile != null;
            ServletUtil.write(getResponse(), zipFile);
        }
    }


    @RequestMapping(value = "/build/history/download_log.html", method = RequestMethod.GET)
    @Feature(method = MethodFeature.DOWNLOAD)
    public void downloadLog(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String logId) {
        BuildHistoryLog buildHistoryLog = dbBuildHistoryLogService.getByKey(logId);
        Objects.requireNonNull(buildHistoryLog);
        BuildInfoModel item = buildInfoService.getByKey(buildHistoryLog.getBuildDataId());
        Objects.requireNonNull(item);
        File logFile = BuildUtil.getLogFile(item.getId(), buildHistoryLog.getBuildNumberId());
        if (!FileUtil.exist(logFile)) {
            return;
        }
        if (logFile.isFile()) {
            ServletUtil.write(getResponse(), logFile);
        }
    }

    @RequestMapping(value = "/build/history/history_list.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public String historyList() {
        PageResultDto<BuildHistoryLog> pageResultTemp = dbBuildHistoryLogService.listPage(getRequest());
        pageResultTemp.each(buildHistoryLog -> {
            File file = BuildUtil.getHistoryPackageFile(buildHistoryLog.getBuildDataId(), buildHistoryLog.getBuildNumberId(), buildHistoryLog.getResultDirFile());
            buildHistoryLog.setHashFile(FileUtil.exist(file));
            //
            File logFile = BuildUtil.getLogFile(buildHistoryLog.getBuildDataId(), buildHistoryLog.getBuildNumberId());
            buildHistoryLog.setHasLog(FileUtil.exist(logFile));
        });
        return JsonMessage.getString(200, "获取成功", pageResultTemp);
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
