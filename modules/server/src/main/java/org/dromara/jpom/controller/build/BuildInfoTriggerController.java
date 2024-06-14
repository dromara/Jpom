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
    private final TriggerTokenLogServer triggerTokenLogServer;

    public BuildInfoTriggerController(BuildInfoService buildInfoService,
                                      TriggerTokenLogServer triggerTokenLogServer) {
        this.buildInfoService = buildInfoService;
        this.triggerTokenLogServer = triggerTokenLogServer;
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
        BuildInfoModel item = buildInfoService.getByKey(id, request);
        UserModel user = getUser();
        BuildInfoModel updateInfo;
        if (StrUtil.isEmpty(item.getTriggerToken()) || StrUtil.isNotEmpty(rest)) {
            updateInfo = new BuildInfoModel();
            updateInfo.setId(id);
            updateInfo.setTriggerToken(triggerTokenLogServer.restToken(item.getTriggerToken(), buildInfoService.typeName(),
                item.getId(), user.getId()));
            buildInfoService.updateById(updateInfo);
        } else {
            updateInfo = item;
        }
        Map<String, String> map = this.getBuildToken(updateInfo, request);
        String string = I18nMessageUtil.get("i18n.reset_success.faa3");
        return JsonMessage.success(StrUtil.isEmpty(rest) ? "ok" : string, map);
    }

    private Map<String, String> getBuildToken(BuildInfoModel item, HttpServletRequest request) {
        String contextPath = UrlRedirectUtil.getHeaderProxyPath(request, ServerConst.PROXY_PATH);
        String url = ServerOpenApi.BUILD_TRIGGER_BUILD2.
            replace("{id}", item.getId()).
            replace("{token}", item.getTriggerToken());
        String triggerBuildUrl = String.format("/%s/%s", contextPath, url);
        Map<String, String> map = new HashMap<>(10);
        map.put("triggerBuildUrl", FileUtil.normalize(triggerBuildUrl));
        String batchTriggerBuildUrl = String.format("/%s/%s", contextPath, ServerOpenApi.BUILD_TRIGGER_BUILD_BATCH);
        map.put("batchTriggerBuildUrl", FileUtil.normalize(batchTriggerBuildUrl));
        //
        String batchBuildStatusUrl = String.format("/%s/%s", contextPath, ServerOpenApi.BUILD_TRIGGER_STATUS);
        map.put("batchBuildStatusUrl", FileUtil.normalize(batchBuildStatusUrl));
        String buildLogUrl = String.format("/%s/%s", contextPath, ServerOpenApi.BUILD_TRIGGER_LOG);
        map.put("buildLogUrl", FileUtil.normalize(buildLogUrl));
        map.put("id", item.getId());
        map.put("token", item.getTriggerToken());
        return map;
    }


//    /**
//     * reset new trigger url
//     *
//     * @param id id
//     * @return json
//     */
//    @RequestMapping(value = "/build/trigger/rest", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
//    @Feature(method = MethodFeature.EDIT)
//    public String triggerRest(String id) {
//        BuildInfoModel item = buildInfoService.getByKey(id, getRequest());
//        UserModel user = getUser();
//        BuildInfoModel updateInfo = new BuildInfoModel();
//        updateInfo.setId(id);
//        // new trigger url
//        updateInfo.setTriggerToken(triggerTokenLogServer.restToken(item.getTriggerToken(), buildInfoService.typeName(),
//            item.getId(), user.getId()));
//        buildInfoService.update(updateInfo);
//        Map<String, String> map = this.getBuildToken(updateInfo);
//        return JsonMessage.success( "重置成功", map);
//    }
}
