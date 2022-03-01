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
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.Digester;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.common.BaseServerController;
import io.jpom.common.ServerOpenApi;
import io.jpom.common.UrlRedirectUtil;
import io.jpom.common.interceptor.BaseJpomInterceptor;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.model.data.UserModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.service.dblog.BuildInfoService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

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

    /**
     * 填充的长度
     */
    public static final int BUILD_INFO_TRIGGER_TOKEN_FILL_LEN = 3;
    public static final int BUILD_INFO_TRIGGER_TOKEN_DIGEST_COUNT_MAX = 500;

    public BuildInfoTriggerController(BuildInfoService buildInfoService) {
        this.buildInfoService = buildInfoService;
    }

    /**
     * get a trigger url
     *
     * @param id id
     * @return json
     */
    @RequestMapping(value = "/build/trigger/url", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public String getTriggerUrl(String id) {
        BuildInfoModel item = buildInfoService.getByKey(id);
        if (StrUtil.isEmpty(item.getTriggerToken())) {
            item.setTriggerToken(this.createTriggerUrl());
            buildInfoService.update(item);
        }
        Map<String, String> map = this.getBuildToken(item);
        return JsonMessage.getString(200, "ok", map);
    }

    private Map<String, String> getBuildToken(BuildInfoModel item) {
        String contextPath = UrlRedirectUtil.getHeaderProxyPath(getRequest(), BaseJpomInterceptor.PROXY_PATH);
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
        map.put("id", item.getId());
        map.put("token", item.getTriggerToken());
        return map;
    }


    /**
     * reset new trigger url
     *
     * @param id id
     * @return json
     */
    @RequestMapping(value = "/build/trigger/rest", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public String triggerRest(String id) {
        BuildInfoModel item = buildInfoService.getByKey(id, getRequest());
        // new trigger url
        item.setTriggerToken(this.createTriggerUrl());
        buildInfoService.update(item);
        Map<String, String> map = this.getBuildToken(item);
        return JsonMessage.getString(200, "重置成功", map);
    }

    private String createTriggerUrl() {
        UserModel user = getUser();
        int randomInt = RandomUtil.randomInt(1, BUILD_INFO_TRIGGER_TOKEN_DIGEST_COUNT_MAX);
        String fill = StrUtil.fillBefore(randomInt + "", '0', BUILD_INFO_TRIGGER_TOKEN_FILL_LEN);
        String nowStr = new Digester(DigestAlgorithm.SHA256).setDigestCount(randomInt).digestHex(user.getId());
        return StrUtil.format("{}{}", fill, nowStr);
    }
}
