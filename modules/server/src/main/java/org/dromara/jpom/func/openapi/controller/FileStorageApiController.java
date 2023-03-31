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
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.NioUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.*;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.hutool.extra.servlet.ServletUtil;
import org.dromara.jpom.common.BaseJpomController;
import org.dromara.jpom.common.ServerOpenApi;
import org.dromara.jpom.common.interceptor.NotLogin;
import org.dromara.jpom.func.files.model.FileStorageModel;
import org.dromara.jpom.func.files.service.FileStorageService;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.user.TriggerTokenLogServer;
import org.dromara.jpom.system.ServerConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
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

    private long[] resolveRange(HttpServletRequest request, long fileSize, String id, String name, HttpServletResponse response) {
        String range = ServletUtil.getHeader(request, HttpHeaders.RANGE, CharsetUtil.CHARSET_UTF_8);
        log.debug("下载文件 {} {} {}", id, name, range);
        long fromPos = 0, toPos, downloadSize;
        if (StrUtil.isEmpty(range)) {
            downloadSize = fileSize;
        } else {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            List<String> list = StrUtil.splitTrim(range, "=");
            String rangeByte = CollUtil.getLast(list);
            //  Range: bytes=0-499 表示第 0-499 字节范围的内容
            //  Range: bytes=500-999 表示第 500-999 字节范围的内容
            //  Range: bytes=-500 表示最后 500 字节的内容
            //  Range: bytes=500- 表示从第 500 字节开始到文件结束部分的内容
            //  Range: bytes=0-0,-1 表示第一个和最后一个字节
            //  Range: bytes=500-600,601-999 同时指定几个范围
            Assert.state(!StrUtil.contains(rangeByte, StrUtil.COMMA), "不支持分片多端下载");
            // TODO 解析更多格式的 RANGE 请求头
            long[] split = StrUtil.splitToLong(rangeByte, StrUtil.DASHED);
            Assert.state(split != null, "range 传入的信息不正确");
            if (split.length == 2) {
                // Range: bytes=0-499 表示第 0-499 字节范围的内容
                toPos = split[1];
                fromPos = split[0];
            } else if (split.length == 1) {
                if (StrUtil.startWith(rangeByte, StrUtil.DASHED)) {
                    // Range: bytes=-500 表示最后 500 字节的内容
                    fromPos = Math.max(fileSize - split[0], 0);
                    toPos = fileSize;
                } else if (StrUtil.endWith(rangeByte, StrUtil.DASHED)) {
                    // Range: bytes=500- 表示从第 500 字节开始到文件结束部分的内容
                    fromPos = split[0];
                    toPos = fileSize;
                } else {
                    throw new IllegalArgumentException("不支持的 range 格式 " + rangeByte);
                }
            } else {
                throw new IllegalArgumentException("不支持的 range 格式 " + rangeByte);
            }
            downloadSize = toPos > fromPos ? (toPos - fromPos) : (fileSize - fromPos);
        }
        return new long[]{fromPos, downloadSize};
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
        Assert.state(FileUtil.isFile(fileStorageFile), "文件已经不存在啦");
        long fileSize = FileUtil.size(fileStorageFile);
        // 需要考虑文件名中存在非法字符
        String name = ReUtil.replaceAll(storageModel.getName(), "[\\s\\\\/:\\*\\?\\\"<>\\|]", "");
        if (StrUtil.isEmpty(name)) {
            name = fileStorageFile.getName();
        } else if (!StrUtil.endWith(name, StrUtil.DOT + storageModel.getExtName())) {
            name += StrUtil.DOT + storageModel.getExtName();
        }
        String contentType = ObjectUtil.defaultIfNull(FileUtil.getMimeType(name), "application/octet-stream");
        String charset = ObjectUtil.defaultIfNull(response.getCharacterEncoding(), CharsetUtil.UTF_8);
        response.setHeader("Content-Disposition", StrUtil.format("attachment;filename=\"{}\"",
                URLUtil.encode(name, CharsetUtil.charset(charset))));
        response.setContentType(contentType);
        //    解析断点续传相关信息
        long[] resolveRange = this.resolveRange(request, fileSize, storageModel.getId(), storageModel.getName(), response);
        long fromPos = resolveRange[0];
        long downloadSize = resolveRange[1];
        //
        response.setHeader(HttpHeaders.LAST_MODIFIED, DateTime.of(fileStorageFile.lastModified()).toString(DatePattern.HTTP_DATETIME_FORMAT));
        response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
        //  Content-Range: bytes (unit first byte pos) - [last byte pos]/[entity legth]
        response.setHeader(HttpHeaders.CONTENT_RANGE, StrUtil.format("bytes {}-{}/{}", fromPos, downloadSize, fileSize));
        response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(downloadSize));
        // Copy the stream to the response's output stream.
        ServletOutputStream out = null;
        try (RandomAccessFile in = new RandomAccessFile(fileStorageFile, "r"); FileChannel channel = in.getChannel()) {
            out = response.getOutputStream();
            // 设置下载起始位置
            if (fromPos > 0) {
                channel.position(fromPos);
            }
            // 缓冲区大小
            int bufLen = (int) Math.min(downloadSize, IoUtil.DEFAULT_BUFFER_SIZE);
            ByteBuffer buffer = ByteBuffer.allocate(bufLen);
            int num;
            long count = 0;
            // 当前写到客户端的大小
            while ((num = channel.read(buffer)) != NioUtil.EOF) {
                buffer.flip();
                out.write(buffer.array(), 0, num);
                buffer.clear();
                count += num;
                //处理最后一段，计算不满缓冲区的大小
                long last = (downloadSize - count);
                if (last == 0) {
                    break;
                }
                if (last < bufLen) {
                    bufLen = (int) last;
                    buffer = ByteBuffer.allocate(bufLen);
                }
            }
            response.flushBuffer();
        } catch (ClientAbortException clientAbortException) {
            log.warn("客户端终止连接：{}", clientAbortException.getMessage());
        } catch (Exception e) {
            log.error("数据下载失败", e);
            if (out != null) {
                out.write(StrUtil.bytes("error:" + e.getMessage()));
            }
        } finally {
            IoUtil.close(out);
        }
    }
}
