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
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.common.ServerOpenApi;
import org.dromara.jpom.common.UrlRedirectUtil;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.service.user.TriggerUrlAssembler;
import org.dromara.jpom.service.user.TriggerUrls;
import org.dromara.jpom.model.data.BuildInfoModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.service.dblog.BuildInfoService;
import org.dromara.jpom.service.user.TriggerTokenLogServer;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * new trigger controller for build
 *
 * @author Hotstrip
 * @since 2021-08-23
 */
@RestController
@Feature(cls = ClassFeature.BUILD)
public class BuildInfoTriggerController extends BaseServerController {

    private final BuildInfoService buildInfoService;
    private final TriggerUrlAssembler triggerUrlAssembler;

    public BuildInfoTriggerController(BuildInfoService buildInfoService,
                                      TriggerUrlAssembler triggerUrlAssembler) {
        this.buildInfoService = buildInfoService;
        this.triggerUrlAssembler = triggerUrlAssembler;
    }

    /**
     * get a trigger url
     *
     * @param id id
     * @return json
     */
    @RequestMapping(value = "/build/trigger/url", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<Map<String, String>> getTriggerUrl(String id, String rest, HttpServletRequest request) {
        BuildInfoModel item = triggerUrlAssembler.require(() -> buildInfoService.getByKey(id, request),
            I18nMessageUtil.get("i18n.no_data_found.4ffb"));
        Map<String, String> map = triggerUrlAssembler.issue(buildInfoService, item.getId(), item.getTriggerToken(), rest, getUser().getId(),
            TriggerUrls.builder()
                .add("triggerBuildUrl", ServerOpenApi.BUILD_TRIGGER_BUILD2)
                .add("batchTriggerBuildUrl", ServerOpenApi.BUILD_TRIGGER_BUILD_BATCH)
                .add("batchBuildStatusUrl", ServerOpenApi.BUILD_TRIGGER_STATUS)
                .add("buildLogUrl", ServerOpenApi.BUILD_TRIGGER_LOG)
                .build(), request);
        return JsonMessage.success(TriggerUrlAssembler.successMessage(rest), map);
    }

}
