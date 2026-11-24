/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.manage;

import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseAgentController;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.model.data.NodeProjectInfoModel;
import org.dromara.jpom.service.ProjectFileVersionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 项目文件版本快照（构建发布触发的完整版本，用于快速回滚）
 *
 * @author bwcx_jzy
 * @since 2026/11/18
 */
@RestController
@RequestMapping(value = "/manage/file/")
@Slf4j
public class ProjectFileVersionController extends BaseAgentController {

    private final ProjectFileVersionService projectFileVersionService;

    public ProjectFileVersionController(ProjectFileVersionService projectFileVersionService) {
        this.projectFileVersionService = projectFileVersionService;
    }

    /**
     * 能力探测
     *
     * @param id 项目ID
     * @return json
     */
    @PostMapping(value = "version-capability", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONObject> capability(String id) {
        NodeProjectInfoModel projectInfoModel = super.getProjectInfoModel(id);
        return JsonMessage.success(I18nMessageUtil.get("i18n.query_success.d72b"), projectFileVersionService.capability(projectInfoModel));
    }

    /**
     * 发布前准备（首次开启时生成基线版本）
     *
     * @param id            项目ID
     * @param buildNumberId 构建编号
     * @param keepCount     保留个数
     * @return json
     */
    @PostMapping(value = "version-begin", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONObject> begin(String id, Integer buildNumberId, Integer keepCount) {
        NodeProjectInfoModel projectInfoModel = super.getProjectInfoModel(id);
        return JsonMessage.success(I18nMessageUtil.get("i18n.query_success.d72b"), projectFileVersionService.begin(projectInfoModel, buildNumberId, keepCount));
    }

    /**
     * 发布成功后登记版本
     *
     * @param id            项目ID
     * @param buildNumberId 构建编号
     * @param buildDataId   构建配置ID
     * @param keepCount     保留个数
     * @return json
     */
    @PostMapping(value = "version-commit", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONObject> commit(String id, Integer buildNumberId, String buildDataId, Integer keepCount) {
        NodeProjectInfoModel projectInfoModel = super.getProjectInfoModel(id);
        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"), projectFileVersionService.commit(projectInfoModel, buildNumberId, buildDataId, keepCount));
    }

    /**
     * 版本列表
     *
     * @param id 项目ID
     * @return json
     */
    @PostMapping(value = "version-list", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONObject> list(String id) {
        NodeProjectInfoModel projectInfoModel = super.getProjectInfoModel(id);
        return JsonMessage.success(I18nMessageUtil.get("i18n.query_success.d72b"), projectFileVersionService.list(projectInfoModel));
    }

    /**
     * 切换版本（快速回滚）
     *
     * @param id              项目ID
     * @param versionId       目标版本
     * @param after           切换后操作（0 不操作，大于 0 重启）
     * @param snapshotCurrent 是否先把当前目录快照为安全版本
     * @param keepCount       保留个数
     * @return json
     */
    @PostMapping(value = "version-switch", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONObject> switchVersion(String id,
                                                  @ValidatorItem(msg = "i18n.version_id_required.0a3e") String versionId,
                                                  Integer after,
                                                  Boolean snapshotCurrent,
                                                  Integer keepCount) {
        NodeProjectInfoModel projectInfoModel = super.getProjectInfoModel(id);
        JSONObject jsonObject = projectFileVersionService.switchVersion(projectInfoModel, versionId, after, snapshotCurrent, keepCount);
        return JsonMessage.success(I18nMessageUtil.get("i18n.version_switch_success.2f70"), jsonObject);
    }

    /**
     * 删除版本
     *
     * @param id        项目ID
     * @param versionId 版本ID
     * @return json
     */
    @PostMapping(value = "version-delete", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<Object> delete(String id, @ValidatorItem(msg = "i18n.version_id_required.0a3e") String versionId) {
        NodeProjectInfoModel projectInfoModel = super.getProjectInfoModel(id);
        projectFileVersionService.deleteVersion(projectInfoModel, versionId);
        return JsonMessage.success(I18nMessageUtil.get("i18n.delete_success.0007"));
    }
}
