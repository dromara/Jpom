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
package io.jpom.func.openapi.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.*;
import cn.hutool.extra.servlet.ServletUtil;
import io.jpom.common.BaseJpomController;
import io.jpom.common.ServerOpenApi;
import io.jpom.common.interceptor.NotLogin;
import io.jpom.func.files.model.FileStorageModel;
import io.jpom.func.files.service.FileStorageService;
import io.jpom.model.user.UserModel;
import io.jpom.service.user.TriggerTokenLogServer;
import io.jpom.system.ServerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2023/3/17
 */
@RestController
@NotLogin
@Slf4j
public class FileStorageApiController extends BaseJpomController {

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

    @GetMapping(value = ServerOpenApi.FILE_STORAGE_DOWNLOAD, produces = MediaType.APPLICATION_JSON_VALUE)
    public void download(@PathVariable String id, @PathVariable String token,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        FileStorageModel storageModel = fileStorageService.getByKey(id);
        Assert.notNull(storageModel, "没有对应数据");
        Assert.state(StrUtil.equals(token, storageModel.getTriggerToken()), "token错误,或者已经失效");
        //
        UserModel userModel = triggerTokenLogServer.getUserByToken(token, fileStorageService.typeName());
        //
        Assert.notNull(userModel, "token错误,或者已经失效:-1");
        //
        File storageSavePath = serverConfig.fileStorageSavePath();
        File fileStorageFile = FileUtil.file(storageSavePath, storageModel.getPath());
        Assert.state(FileUtil.isFile(fileStorageFile), "文件已经不存在啦");
        long fileSize = FileUtil.size(fileStorageFile);
        // 需要考虑文件名中存在非法字符
        String name = ReUtil.replaceAll(storageModel.getName(), "[\\s\\\\/:\\*\\?\\\"<>\\|]", "");
        if (StrUtil.isEmpty(name)) {
            name = fileStorageFile.getName();
        } else {
            name += "." + storageModel.getExtName();
        }
        String contentType = ObjectUtil.defaultIfNull(FileUtil.getMimeType(name), "application/octet-stream");
        String charset = ObjectUtil.defaultIfNull(response.getCharacterEncoding(), CharsetUtil.UTF_8);
        response.setHeader("Content-Disposition", StrUtil.format("attachment;filename=\"{}\"",
            URLUtil.encode(name, CharsetUtil.charset(charset))));
        response.setContentType(contentType);
        //
        // 解析断点续传相关信息
        response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
        String range = ServletUtil.getHeader(request, HttpHeaders.RANGE, CharsetUtil.CHARSET_UTF_8);
        log.debug("下载文件 {} {} {}", storageModel.getId(), name, range);
        long fromPos = 0, toPos = 0, downloadSize;
        if (StrUtil.isEmpty(range)) {
            downloadSize = fileSize;
        } else {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            List<String> list = StrUtil.splitTrim(range, "=");
            String rangeByte = CollUtil.getLast(list);
            long[] split = StrUtil.splitToLong(rangeByte, "-");
            Assert.state(split != null, "range 传入的信息不正确");
            fromPos = split[0];
            if (split.length == 2) {
                toPos = split[1];
            }
            downloadSize = toPos > fromPos ? (int) (toPos - fromPos) : (int) (fileSize - fromPos);
        }
        response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(downloadSize));
        // Copy the stream to the response's output stream.
        OutputStream out = null;
        try (RandomAccessFile in = new RandomAccessFile(fileStorageFile, "r")) {
            out = response.getOutputStream();
            // 设置下载起始位置
            if (fromPos > 0) {
                in.seek(fromPos);
            }
            // 缓冲区大小
            int bufLen = (int) Math.min(downloadSize, IoUtil.DEFAULT_BUFFER_SIZE);
            byte[] buffer = new byte[bufLen];
            int num;
            int count = 0;
            // 当前写到客户端的大小
            while ((num = in.read(buffer)) != -1) {
                out.write(buffer, 0, num);
                count += num;
                //处理最后一段，计算不满缓冲区的大小
                if (downloadSize - count < bufLen) {
                    bufLen = (int) (downloadSize - count);
                    if (bufLen == 0) {
                        break;
                    }
                    buffer = new byte[bufLen];
                }
            }
            response.flushBuffer();
        } catch (Exception e) {
            log.error("数据下载失败", e);
        } finally {
            IoUtil.close(out);
        }
    }
}
