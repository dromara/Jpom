/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.openapi.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.ServerOpenApi;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.interceptor.NotLogin;
import org.dromara.jpom.func.files.model.StaticFileStorageModel;
import org.dromara.jpom.func.files.service.StaticFileStorageService;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.user.TriggerTokenLogServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * @author bwcx_jzy
 * @since 2023/12/28
 */
@RestController
@NotLogin
@Slf4j
public class StaticFileStorageApiController extends BaseDownloadApiController {

    private final TriggerTokenLogServer triggerTokenLogServer;
    private final StaticFileStorageService staticFileStorageService;

    public StaticFileStorageApiController(TriggerTokenLogServer triggerTokenLogServer,
                                          StaticFileStorageService staticFileStorageService) {
        this.triggerTokenLogServer = triggerTokenLogServer;
        this.staticFileStorageService = staticFileStorageService;
    }


    @GetMapping(value = ServerOpenApi.STATIC_FILE_STORAGE_DOWNLOAD, produces = MediaType.APPLICATION_JSON_VALUE)
    public void download(@PathVariable String id,
                         @PathVariable String token,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        StaticFileStorageModel storageModel = staticFileStorageService.getByKey(id);
        Assert.notNull(storageModel, I18nMessageUtil.get("i18n.file_not_found.d952"));

        Assert.state(StrUtil.equals(token, storageModel.getTriggerToken()), I18nMessageUtil.get("i18n.invalid_or_expired_token.bc43"));
        //
        UserModel userModel = triggerTokenLogServer.getUserByToken(token, staticFileStorageService.typeName());
        //
        Assert.notNull(userModel, I18nMessageUtil.get("i18n.token_invalid_or_expired.cb96"));
        File file = FileUtil.file(storageModel.getAbsolutePath());
        // 需要考虑文件名中存在非法字符
        String name = this.convertName(storageModel.getName(), storageModel.getExtName(), file.getName());
        //    解析断点续传相关信息
        long fileSize = FileUtil.size(file);
        long[] resolveRange = this.resolveRange(request, fileSize, storageModel.getId(), storageModel.getName(), response);
        this.download(file, fileSize, name, resolveRange, response);
    }
}
