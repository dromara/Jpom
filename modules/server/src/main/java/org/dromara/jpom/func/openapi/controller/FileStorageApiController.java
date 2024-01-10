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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.hutool.extra.servlet.ServletUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.ServerOpenApi;
import org.dromara.jpom.common.interceptor.NotLogin;
import org.dromara.jpom.func.files.model.FileStorageModel;
import org.dromara.jpom.func.files.service.FileStorageService;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.user.TriggerTokenLogServer;
import org.dromara.jpom.system.ServerConfig;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2023/3/17
 */
@RestController
@NotLogin
@Slf4j
public class FileStorageApiController extends BaseDownloadApiController {

    private final TriggerTokenLogServer triggerTokenLogServer;
    private final FileStorageService fileStorageService;
    private final ServerConfig serverConfig;

    public FileStorageApiController(TriggerTokenLogServer triggerTokenLogServer,
                                    FileStorageService fileStorageService,
                                    ServerConfig serverConfig) {
        this.triggerTokenLogServer = triggerTokenLogServer;
        this.fileStorageService = fileStorageService;
        this.serverConfig = serverConfig;
    }


    /**
     * 解析别名参数
     *
     * @param id      别名
     * @param token   token
     * @param sort    排序
     * @param request 请求
     * @return 数据
     */
    private FileStorageModel queryByAliasCode(String id, String token, String sort, HttpServletRequest request) {
        // 先验证 token 和 id 是否都存在
        {
            FileStorageModel data = new FileStorageModel();
            data.setAliasCode(id);
            data.setTriggerToken(token);
            Assert.state(fileStorageService.exists(data), "别名或者token错误,或者已经失效");
        }
        List<Order> orders = Opt.ofBlankAble(sort)
            .map(s -> StrUtil.splitTrim(s, StrUtil.COMMA))
            .map(strings ->
                strings.stream()
                    .map(s -> {
                        List<String> list = StrUtil.splitTrim(s, StrUtil.COLON);
                        Order order = new Order();
                        order.setField(CollUtil.getFirst(list));
                        String s1 = CollUtil.get(list, 1);
                        order.setDirection(ObjectUtil.defaultIfNull(Direction.fromString(s1), Direction.DESC));
                        return order;
                    })
                    .collect(Collectors.toList()))
            .orElse(Collections.emptyList());
        Map<String, String> paramMap = ServletUtil.getParamMap(request);
        FileStorageModel where = new FileStorageModel();
        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
            String key = entry.getKey();
            if (StrUtil.startWith(key, "filter_")) {
                key = StrUtil.removePrefix(key, "filter_");
                ReflectUtil.setFieldValue(where, key, entry.getValue());
            }
        }
        where.setAliasCode(id);
        List<FileStorageModel> fileStorageModels = fileStorageService.queryList(where, 1, orders.toArray(new Order[]{}));
        return CollUtil.getFirst(fileStorageModels);
    }

    @GetMapping(value = ServerOpenApi.FILE_STORAGE_DOWNLOAD, produces = MediaType.APPLICATION_JSON_VALUE)
    public void download(@PathVariable String id,
                         @PathVariable String token,
                         String sort,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        FileStorageModel storageModel = fileStorageService.getByKey(id);
        if (storageModel == null) {
            // 根据别名查询
            storageModel = this.queryByAliasCode(id, token, sort, request);
            Assert.notNull(storageModel, "没有对应数据");
        } else {
            Assert.state(StrUtil.equals(token, storageModel.getTriggerToken()), "token错误,或者已经失效");
        }
        //
        UserModel userModel = triggerTokenLogServer.getUserByToken(token, fileStorageService.typeName());
        //
        Assert.notNull(userModel, "token错误,或者已经失效:-1");
        //
        File storageSavePath = serverConfig.fileStorageSavePath();
        File fileStorageFile = FileUtil.file(storageSavePath, storageModel.getPath());
        // 需要考虑文件名中存在非法字符
        String name = this.convertName(storageModel.getName(), storageModel.getExtName(), fileStorageFile.getName());
        //    解析断点续传相关信息
        long fileSize = FileUtil.size(fileStorageFile);
        long[] resolveRange = this.resolveRange(request, fileSize, storageModel.getId(), storageModel.getName(), response);
        this.download(fileStorageFile, fileSize, name, resolveRange, response);
    }
}
