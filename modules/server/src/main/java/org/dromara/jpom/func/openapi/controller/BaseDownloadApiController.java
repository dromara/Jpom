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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.NioUtil;
import cn.hutool.core.util.*;
import cn.hutool.extra.servlet.ServletUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.dromara.jpom.common.BaseJpomController;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 23/12/28 028
 */
@Slf4j
public abstract class BaseDownloadApiController extends BaseJpomController {

    protected long[] resolveRange(HttpServletRequest request, long fileSize, String id, String name, HttpServletResponse response) {
        String range = ServletUtil.getHeader(request, HttpHeaders.RANGE, CharsetUtil.CHARSET_UTF_8);
        log.debug(I18nMessageUtil.get("i18n.download_file_description.10cb"), id, name, range);
        long fromPos = 0, toPos, downloadSize;
        if (StrUtil.isEmpty(range)) {
            downloadSize = fileSize;
        } else {
            // 设置状态码 206
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            List<String> list = StrUtil.splitTrim(range, "=");
            String rangeByte = CollUtil.getLast(list);
            //  Range: bytes=0-499 表示第 0-499 字节范围的内容
            //  Range: bytes=500-999 表示第 500-999 字节范围的内容
            //  Range: bytes=-500 表示最后 500 字节的内容
            //  Range: bytes=500- 表示从第 500 字节开始到文件结束部分的内容
            //  Range: bytes=0-0,-1 表示第一个和最后一个字节
            //  Range: bytes=500-600,601-999 同时指定几个范围
            Assert.state(!StrUtil.contains(rangeByte, StrUtil.COMMA), I18nMessageUtil.get("i18n.multi_download_not_supported.94b9"));
            // TODO 解析更多格式的 RANGE 请求头
            long[] split = StrUtil.splitToLong(rangeByte, StrUtil.DASHED);
            Assert.state(split != null, I18nMessageUtil.get("i18n.incorrect_range_information.a41c"));
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
                    throw new IllegalArgumentException(I18nMessageUtil.get("i18n.range_format_not_supported.d69e") + rangeByte);
                }
            } else {
                throw new IllegalArgumentException(I18nMessageUtil.get("i18n.range_format_not_supported.d69e") + rangeByte);
            }
            downloadSize = toPos > fromPos ? (toPos - fromPos) : (fileSize - fromPos);
        }
        return new long[]{fromPos, downloadSize};
    }

    protected String convertName(String name1, String extName, String defaultName) {
        // 需要考虑文件名中存在非法字符
        String name = ReUtil.replaceAll(name1, "[\\s\\\\/:\\*\\?\\\"<>\\|]", "");
        if (StrUtil.isEmpty(name)) {
            name = defaultName;
        } else if (!StrUtil.endWith(name, StrUtil.DOT + extName)) {
            name += StrUtil.DOT + extName;
        }
        return name;
    }

    public void download(File file, long fileSize, String name, long[] resolveRange, HttpServletResponse response) throws IOException {
        Assert.state(FileUtil.isFile(file), I18nMessageUtil.get("i18n.file_does_not_exist_anymore.2fab"));
        String contentType = ObjectUtil.defaultIfNull(FileUtil.getMimeType(name), "application/octet-stream");
        String charset = ObjectUtil.defaultIfNull(response.getCharacterEncoding(), CharsetUtil.UTF_8);
        response.setHeader("Content-Disposition", StrUtil.format("attachment;filename=\"{}\"",
            URLUtil.encode(name, CharsetUtil.charset(charset))));
        response.setContentType(contentType);
        //    解析断点续传相关信息
        long fromPos = resolveRange[0];
        long downloadSize = resolveRange[1];
        //
        response.setHeader(HttpHeaders.LAST_MODIFIED, DateTime.of(file.lastModified()).toString(DatePattern.HTTP_DATETIME_FORMAT));
        response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
        //  Content-Range: bytes (unit first byte pos) - [last byte pos]/[entity legth]
        response.setHeader(HttpHeaders.CONTENT_RANGE, StrUtil.format("bytes {}-{}/{}", fromPos, downloadSize, fileSize));
        response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(downloadSize));
        // Copy the stream to the response's output stream.
        ServletOutputStream out = null;
        try (RandomAccessFile in = new RandomAccessFile(file, "r"); FileChannel channel = in.getChannel()) {
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
            log.warn(I18nMessageUtil.get("i18n.client_terminated_connection.6886"), clientAbortException.getMessage());
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.data_download_failed.9499"), e);
            if (out != null) {
                out.write(StrUtil.bytes("error:" + e.getMessage()));
            }
        } finally {
            IoUtil.close(out);
        }
    }
}
