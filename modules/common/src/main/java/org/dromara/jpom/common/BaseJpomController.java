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
package org.dromara.jpom.common;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.*;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.servlet.ServletUtil;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.dromara.jpom.common.multipart.MultipartFileBuilder;
import org.dromara.jpom.util.FileUtils;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * controller
 *
 * @author jiangzeyin
 * @since 2019/4/16
 */
@Slf4j
public abstract class BaseJpomController {

    /**
     * 获取请求的ip 地址
     *
     * @return ip
     */
    protected String getIp() {
        return ServletUtil.getClientIP(getRequest());
    }

    /**
     * 上传保存分片信息
     *
     * @param file       上传的文件信息
     * @param tempPath   临时保存目录
     * @param sliceId    分片id
     * @param totalSlice 累积分片
     * @param nowSlice   当前分片
     * @param fileSumMd5 文件签名信息
     * @throws IOException 异常
     */
    public void uploadSharding(MultipartFile file,
                               String tempPath,
                               String sliceId,
                               Integer totalSlice,
                               Integer nowSlice,
                               String fileSumMd5,
                               String... extNames) throws IOException {
        Assert.hasText(fileSumMd5, "没有文件签名信息");
        Assert.hasText(sliceId, "没有分片 id 信息");

        Assert.notNull(totalSlice, "上传信息不完成：totalSlice");
        Assert.notNull(nowSlice, "上传信息不完成：nowSlice");
        Assert.state(totalSlice > 0 && nowSlice > -1 && totalSlice >= nowSlice, "当前上传的分片信息错误");
        // 保存路径
        File slicePath = FileUtil.file(tempPath, "slice", sliceId);
        File sliceItemPath = FileUtil.file(slicePath, "items");
        Assert.notNull(file, "没有上传文件");
        String originalFilename = file.getOriginalFilename();
        // 截断序号 xxxxx.avi.1
        String realName = StrUtil.subBefore(originalFilename, StrUtil.DOT, true);
        if (ArrayUtil.isNotEmpty(extNames)) {
            String extName = FileUtil.extName(realName);
            Assert.state(StrUtil.containsAnyIgnoreCase(extName, extNames), "不支持的文件类型：" + extName);
        }
        assert originalFilename != null;
        File slice = FileUtil.file(sliceItemPath, originalFilename);
        FileUtil.mkParentDirs(slice);
        // 保存
        file.transferTo(slice);
    }

    /**
     * 合并分片
     *
     * @param tempPath   临时保存目录
     * @param sliceId    上传id
     * @param totalSlice 累积分片
     * @param fileSumMd5 文件签名
     * @return 合并后的文件
     * @throws IOException io
     */
    public File shardingTryMerge(String tempPath,
                                 String sliceId,
                                 Integer totalSlice,
                                 String fileSumMd5) throws IOException {
        Assert.hasText(fileSumMd5, "没有文件签名信息");
        Assert.hasText(sliceId, "没有分片 id 信息");

        Assert.notNull(totalSlice, "上传信息不完成：totalSlice");

        // 保存路径
        File slicePath = FileUtil.file(tempPath, "slice", sliceId);
        File sliceItemPath = FileUtil.file(slicePath, "items");

        // 准备合并
        File[] files = sliceItemPath.listFiles();
        int length = ArrayUtil.length(files);
        Assert.state(files != null && length == totalSlice, "文件上传失败,存在分片丢失的情况, " + length + " != " + totalSlice);
        // 文件真实名称
        String name = files[0].getName();
        name = StrUtil.subBefore(name, StrUtil.DOT, true);
        File successFile = FileUtil.file(slicePath, name);
        try (FileOutputStream fileOutputStream = new FileOutputStream(successFile)) {
            try (FileChannel channel = fileOutputStream.getChannel()) {
                Arrays.stream(files).sorted((o1, o2) -> {
                    // 排序
                    Integer o1Int = Convert.toInt(FileUtil.extName(o1), 0);
                    Integer o2Int = Convert.toInt(FileUtil.extName(o2), 0);
                    return o1Int.compareTo(o2Int);
                }).forEach(file12 -> {
                    try {
                        FileUtils.appendChannel(file12, channel);
                    } catch (Exception e) {
                        throw Lombok.sneakyThrow(e);
                    }
                });
            }
        }
        // 删除分片信息
        FileUtil.del(sliceItemPath);
        // 对比文件信息
        String newMd5 = SecureUtil.md5(successFile);
        Assert.state(StrUtil.equals(newMd5, fileSumMd5), () -> {
            log.warn("文件合并异常 {}:{} -> {}", FileUtil.getAbsolutePath(successFile), newMd5, fileSumMd5);
            return "文件合并后异常,文件不完成可能被损坏";
        });
        return successFile;
    }

    protected String getParameter(String name) {
        return getParameter(name, null);
    }

    protected String[] getParameters(String name) {
        return getRequest().getParameterValues(name);
    }

    /**
     * 获取指定参数名的值
     *
     * @param name 参数名
     * @param def  默认值
     * @return str
     */
    protected String getParameter(String name, String def) {
        String value = getRequest().getParameter(name);
        return value == null ? def : value;
    }

    protected int getParameterInt(String name, int def) {
        return Convert.toInt(getParameter(name), def);
    }


    // ----------------文件上传
    /**
     * cache
     */
    private static final ThreadLocal<MultipartHttpServletRequest> THREAD_LOCAL_MULTIPART_HTTP_SERVLET_REQUEST = new ThreadLocal<>();

    /**
     * 释放资源
     */
    public static void clearResources() {
        THREAD_LOCAL_MULTIPART_HTTP_SERVLET_REQUEST.remove();
    }

    /**
     * 获取文件上传请求对象
     *
     * @return multipart
     */
    protected MultipartHttpServletRequest getMultiRequest() {
        HttpServletRequest request = getRequest();
        if (request instanceof MultipartHttpServletRequest) {
            return (MultipartHttpServletRequest) request;
        }
        if (ServletFileUpload.isMultipartContent(request)) {
            MultipartHttpServletRequest multipartHttpServletRequest = THREAD_LOCAL_MULTIPART_HTTP_SERVLET_REQUEST.get();
            if (multipartHttpServletRequest != null) {
                return multipartHttpServletRequest;
            }
            multipartHttpServletRequest = new StandardMultipartHttpServletRequest(request);
            THREAD_LOCAL_MULTIPART_HTTP_SERVLET_REQUEST.set(multipartHttpServletRequest);
            return multipartHttpServletRequest;
        }
        throw new IllegalArgumentException("not MultipartHttpServletRequest");
    }

    /**
     * 判断是否存在文件
     *
     * @return true 存在文件
     */
    protected boolean hasFile() {
        Map<String, MultipartFile> fileMap = getMultiRequest().getFileMap();
        return fileMap.size() > 0;
    }

    /**
     * 创建多文件上传对象
     *
     * @return MultipartFileBuilder
     */
    protected MultipartFileBuilder createMultipart() {
        return new MultipartFileBuilder(getMultiRequest());
    }
    // ------------------------文件上传结束

    /**
     * 全局获取请求对象
     *
     * @return req
     */
    public static ServletRequestAttributes getRequestAttributes() {
        ServletRequestAttributes servletRequestAttributes = tryGetRequestAttributes();
        Objects.requireNonNull(servletRequestAttributes);
        return servletRequestAttributes;
    }

    /**
     * 尝试获取
     *
     * @return ServletRequestAttributes
     */
    public static ServletRequestAttributes tryGetRequestAttributes() {
        RequestAttributes attributes = null;
        try {
            attributes = RequestContextHolder.currentRequestAttributes();
        } catch (IllegalStateException ignored) {
        }
        if (attributes == null) {
            return null;
        }
        if (attributes instanceof ServletRequestAttributes) {
            return (ServletRequestAttributes) attributes;
        }
        return null;
    }

    /**
     * 获取客户端的ip地址
     *
     * @return 如果没有就返回null
     */
    public static String getClientIP() {
        ServletRequestAttributes servletRequest = tryGetRequestAttributes();
        if (servletRequest == null) {
            return null;
        }
        HttpServletRequest request = servletRequest.getRequest();
        if (request == null) {
            return null;
        }
        return ServletUtil.getClientIP(request);
    }

    public HttpServletRequest getRequest() {
        HttpServletRequest request = getRequestAttributes().getRequest();
        Objects.requireNonNull(request, "request null");
        return request;
    }

    /**
     * 获取session 字符串
     *
     * @param name name
     * @return str
     * @author jiangzeyin
     */
    public String getSessionAttribute(String name) {
        return Objects.toString(getSessionAttributeObj(name), "");
    }

    /**
     * 获取session 中对象
     *
     * @param name name
     * @return obj
     */
    public Object getSessionAttributeObj(String name) {
        return getRequestAttributes().getAttribute(name, RequestAttributes.SCOPE_SESSION);
    }

    /**
     * 移除session 值
     *
     * @param name name
     * @author jiangzeyin
     */
    public void removeSessionAttribute(String name) {
        getRequestAttributes().removeAttribute(name, RequestAttributes.SCOPE_SESSION);
    }

    /**
     * 设置session 字符串
     *
     * @param name   name
     * @param object 值
     */
    public void setSessionAttribute(String name, Object object) {
        getRequestAttributes().setAttribute(name, object, RequestAttributes.SCOPE_SESSION);
    }

    protected void setApplicationHeader(HttpServletResponse response, String fileName) {
        String contentType = ObjectUtil.defaultIfNull(FileUtil.getMimeType(fileName), "application/octet-stream");
        response.setHeader("Content-Disposition", StrUtil.format("attachment;filename=\"{}\"",
                URLUtil.encode(fileName, CharsetUtil.CHARSET_UTF_8)));
        response.setContentType(contentType);
    }

}
