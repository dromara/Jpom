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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.build.BuildExecuteManage;
import org.dromara.jpom.build.BuildExecuteService;
import org.dromara.jpom.build.BuildUtil;
import org.dromara.jpom.build.ResultDirFileAction;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorConfig;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.model.BaseEnum;
import org.dromara.jpom.model.data.BuildInfoModel;
import org.dromara.jpom.model.enums.BuildStatus;
import org.dromara.jpom.model.log.BuildHistoryLog;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.service.dblog.BuildInfoService;
import org.dromara.jpom.service.dblog.DbBuildHistoryLogService;
import org.dromara.jpom.util.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Objects;
import java.util.Optional;

/**
 * new build info manage controller
 * ` *
 *
 * @author Hotstrip
 * @since 2021-08-23
 */
@RestController
@Feature(cls = ClassFeature.BUILD)
public class BuildInfoManageController extends BaseServerController {

    private final BuildInfoService buildInfoService;
    private final DbBuildHistoryLogService dbBuildHistoryLogService;
    private final BuildExecuteService buildExecuteService;

    public BuildInfoManageController(BuildInfoService buildInfoService,
                                     DbBuildHistoryLogService dbBuildHistoryLogService,
                                     BuildExecuteService buildExecuteService) {
        this.buildInfoService = buildInfoService;
        this.dbBuildHistoryLogService = dbBuildHistoryLogService;
        this.buildExecuteService = buildExecuteService;
    }

    /**
     * 开始构建
     *
     * @param id id
     * @return json
     */
    @RequestMapping(value = "/build/manage/start", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public IJsonMessage<Integer> start(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.no_data.1ac0") String id,
                                       String buildRemark,
                                       String resultDirFile,
                                       String branchName,
                                       String branchTagName,
                                       String checkRepositoryDiff,
                                       String projectSecondaryDirectory,
                                       String buildEnvParameter,
                                       String dispatchSelectProject,
                                       HttpServletRequest request) {
        BuildInfoModel item = buildInfoService.getByKey(id, request);
        Assert.notNull(item, I18nMessageUtil.get("i18n.no_data_found.4ffb"));
        // 更新数据
        BuildInfoModel update = new BuildInfoModel();
        Opt.ofBlankAble(resultDirFile).ifPresent(s -> {
            ResultDirFileAction parse = ResultDirFileAction.parse(s);
            parse.check();
            update.setResultDirFile(s);
        });
        Opt.ofBlankAble(branchName).ifPresent(update::setBranchName);
        Opt.ofBlankAble(branchTagName).ifPresent(update::setBranchTagName);
        Opt.ofBlankAble(projectSecondaryDirectory).ifPresent(s -> {
            FileUtils.checkSlip(s, e -> new IllegalArgumentException(I18nMessageUtil.get("i18n.second_level_directory_cannot_skip_levels.c9fb") + e.getMessage()));
            //
            String extraData = item.getExtraData();
            JSONObject jsonObject = JSONObject.parseObject(extraData);
            jsonObject.put("projectSecondaryDirectory", s);
            update.setExtraData(jsonObject.toString());
        });
        // 会存在清空的情况
        update.setBuildEnvParameter(Optional.ofNullable(buildEnvParameter).orElse(StrUtil.EMPTY));
        update.setId(id);
        buildInfoService.updateById(update);
        // userModel
        UserModel userModel = getUser();
        Object[] parametersEnv = StrUtil.isNotEmpty(dispatchSelectProject) ? new Object[]{"dispatchSelectProject", dispatchSelectProject} : new Object[]{};
        // 执行构建
        return buildExecuteService.start(item.getId(), userModel, null, 0, buildRemark, checkRepositoryDiff, parametersEnv);
    }

    /**
     * 取消构建
     *
     * @param id id
     * @return json
     */
    @RequestMapping(value = "/build/manage/cancel", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public IJsonMessage<String> cancel(@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.no_data.1ac0")) String id, HttpServletRequest request) {
        BuildInfoModel item = buildInfoService.getByKey(id, request);
        Objects.requireNonNull(item, I18nMessageUtil.get("i18n.no_data_found.4ffb"));
        String checkStatus = buildExecuteService.checkStatus(item);
        BuildStatus nowStatus = BaseEnum.getEnum(BuildStatus.class, item.getStatus());
        Objects.requireNonNull(nowStatus);
        if (checkStatus == null) {
            return JsonMessage.success(I18nMessageUtil.get("i18n.status_not_in_progress.f410") + nowStatus.getDesc());
        }
        boolean status = BuildExecuteManage.cancelTaskById(item.getId());
        if (!status) {
            // 缓存中可能不存在数据,还是需要执行取消
            buildInfoService.updateStatus(id, BuildStatus.Cancel, I18nMessageUtil.get("i18n.manual_cancel.8464"));
        }
        return JsonMessage.success(I18nMessageUtil.get("i18n.cancel_success.285f"));
    }

    /**
     * 重新发布
     *
     * @param logId logId
     * @return json
     */
    @RequestMapping(value = "/build/manage/reRelease", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public IJsonMessage<Integer> reRelease(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.no_data.1ac0") String logId,
                                           HttpServletRequest request) {
        String workspaceId = dbBuildHistoryLogService.getCheckUserWorkspace(request);
        BuildHistoryLog buildHistoryLog = dbBuildHistoryLogService.getByKey(logId, false, entity -> entity.set("workspaceId", workspaceId));
        Objects.requireNonNull(buildHistoryLog, I18nMessageUtil.get("i18n.no_corresponding_build_record.b3b2"));
        BuildInfoModel item = buildInfoService.getByKey(buildHistoryLog.getBuildDataId(), request);
        Objects.requireNonNull(item, I18nMessageUtil.get("i18n.no_data_found.4ffb"));
        int buildId = buildExecuteService.rollback(buildHistoryLog, item, getUser());
        return JsonMessage.success(I18nMessageUtil.get("i18n.republishing.131d"), buildId);
    }

    /**
     * 获取构建的日志
     *
     * @param id      id
     * @param buildId 构建编号
     * @param line    需要获取的行号
     * @return json
     */
    @RequestMapping(value = "/build/manage/get-now-log", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<JSONObject> getNowLog(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.no_data.1ac0") String id,
                                              @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "i18n.no_build_id.a0b8") int buildId,
                                              @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "i18n.line_number_error.c65d") int line,
                                              HttpServletRequest request) {
        BuildInfoModel item = buildInfoService.getByKey(id, request);
        Assert.notNull(item, I18nMessageUtil.get("i18n.no_data_found.4ffb"));
        Assert.state(buildId <= item.getBuildId(), I18nMessageUtil.get("i18n.no_build_record_found.76f4"));

        BuildHistoryLog buildHistoryLog = new BuildHistoryLog();
        buildHistoryLog.setBuildDataId(id);
        buildHistoryLog.setBuildNumberId(buildId);
        BuildHistoryLog queryByBean = dbBuildHistoryLogService.queryByBean(buildHistoryLog);
        Assert.notNull(queryByBean, I18nMessageUtil.get("i18n.no_build_history.39f7"));

        File file = BuildUtil.getLogFile(item.getId(), buildId);
        Assert.state(FileUtil.isFile(file), I18nMessageUtil.get("i18n.log_file_does_not_exist_or_error.a0e7"));

        if (!file.exists()) {
            if (buildId == item.getBuildId()) {
                return new JsonMessage<>(201, I18nMessageUtil.get("i18n.no_log_file.bacf"));
            }
            return new JsonMessage<>(300, I18nMessageUtil.get("i18n.log_file_does_not_exist.f6c6"));
        }
        JSONObject data = FileUtils.readLogFile(file, line);
        // 运行中
        Integer status = queryByBean.getStatus();
        data.put("run", buildExecuteService.checkStatus(item) != null);
        data.put("logId", queryByBean.getId());
        data.put("status", status);
        data.put("statusMsg", queryByBean.getStatusMsg());
        // 构建中
        //data.put("buildRun", status == BuildStatus.Ing.getCode());
        return JsonMessage.success("", data);
    }
}
