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
package org.dromara.jpom.func.openapi.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.ServerOpenApi;
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
        Assert.notNull(storageModel, "文件不存在");

        Assert.state(StrUtil.equals(token, storageModel.getTriggerToken()), "token错误,或者已经失效");
        //
        UserModel userModel = triggerTokenLogServer.getUserByToken(token, staticFileStorageService.typeName());
        //
        Assert.notNull(userModel, "token错误,或者已经失效:-1");
        File file = FileUtil.file(storageModel.getAbsolutePath());
        // 需要考虑文件名中存在非法字符
        String name = this.convertName(storageModel.getName(), storageModel.getExtName(), file.getName());
        //    解析断点续传相关信息
        long fileSize = FileUtil.size(file);
        long[] resolveRange = this.resolveRange(request, fileSize, storageModel.getId(), storageModel.getName(), response);
        this.download(file, fileSize, name, resolveRange, response);
    }
}
