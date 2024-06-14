/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
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
import org.dromara.jpom.common.i18n.I18nMessageUtil;
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
import java.util.Objects;

/**
 * controller
 *
 * @author bwcx_jzy
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
        Assert.hasText(fileSumMd5, I18nMessageUtil.get("i18n.file_signature_info_not_found.83bf"));
        Assert.hasText(sliceId, I18nMessageUtil.get("i18n.no_shard_id_info.30f8"));

        Assert.notNull(totalSlice, I18nMessageUtil.get("i18n.incomplete_upload_info_total_slice.7e85"));
        Assert.notNull(nowSlice, I18nMessageUtil.get("i18n.incomplete_upload_info_now_slice.34aa"));
        Assert.state(totalSlice > 0 && nowSlice > -1 && totalSlice >= nowSlice, I18nMessageUtil.get("i18n.current_upload_chunk_info_incorrect.900e"));
        // 保存路径
        File slicePath = FileUtil.file(tempPath, "slice", sliceId);
        File sliceItemPath = FileUtil.file(slicePath, "items");
        Assert.notNull(file, I18nMessageUtil.get("i18n.no_uploaded_file.07ef"));
        String originalFilename = file.getOriginalFilename();
        // 截断序号 xxxxx.avi.1
        String realName = StrUtil.subBefore(originalFilename, StrUtil.DOT, true);
        if (ArrayUtil.isNotEmpty(extNames)) {
            String extName = FileUtil.extName(realName);
            Assert.state(StrUtil.containsAnyIgnoreCase(extName, extNames), I18nMessageUtil.get("i18n.file_type_not_supported2.d497") + extName);
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
        Assert.hasText(fileSumMd5, I18nMessageUtil.get("i18n.file_signature_info_not_found.83bf"));
        Assert.hasText(sliceId, I18nMessageUtil.get("i18n.no_shard_id_info.30f8"));

        Assert.notNull(totalSlice, I18nMessageUtil.get("i18n.incomplete_upload_info_total_slice.7e85"));

        // 保存路径
        File slicePath = FileUtil.file(tempPath, "slice", sliceId);
        File sliceItemPath = FileUtil.file(slicePath, "items");

        // 准备合并
        File[] files = sliceItemPath.listFiles();
        int length = ArrayUtil.length(files);
        Assert.state(files != null && length == totalSlice, StrUtil.format(I18nMessageUtil.get("i18n.file_upload_failure_due_to_missing_chunks.1865"), length, totalSlice));
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
            log.warn(I18nMessageUtil.get("i18n.file_merge_exception_details.e9d0"), FileUtil.getAbsolutePath(successFile), newMd5, fileSumMd5);
            return I18nMessageUtil.get("i18n.file_merge_error.f32f");
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
     * @author bwcx_jzy
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
     * @author bwcx_jzy
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

    protected String getWorkspaceId() {
        return ServletUtil.getHeader(getRequest(), Const.WORKSPACE_ID_REQ_HEADER, CharsetUtil.CHARSET_UTF_8);
    }
}
