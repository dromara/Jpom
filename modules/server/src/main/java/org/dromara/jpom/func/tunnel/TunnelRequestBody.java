/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.tunnel;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.Resource;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.encrypt.EncryptFactory;
import org.dromara.jpom.encrypt.Encryptor;
import org.dromara.jpom.transport.DataContentType;
import org.dromara.jpom.transport.INodeInfo;
import org.dromara.jpom.transport.IUrlItem;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 把一次转发调用的参数序列化成 HTTP 请求体，供隧道发送。
 * <p>
 * 与正向 {@code HttpTransportServer} 的行为保持一致：表单值按 {@code transport-encryption}
 * 规则加密、文件型参数走 {@code multipart/form-data} 且不加密。
 *
 * @author jpom
 * @since 2026/11/18
 */
public class TunnelRequestBody {

    private static final String CRLF = "\r\n";

    private final byte[] body;
    private final String contentType;

    private TunnelRequestBody(byte[] body, String contentType) {
        this.body = body;
        this.contentType = contentType;
    }

    public byte[] getBody() {
        return body;
    }

    public String getContentType() {
        return contentType;
    }

    /**
     * 构建请求体
     *
     * @param nodeInfo 节点信息（取传输加密方式）
     * @param urlItem  请求信息（取请求类型）
     * @param data     参数
     * @return 请求体
     */
    @SuppressWarnings("unchecked")
    public static TunnelRequestBody build(INodeInfo nodeInfo, IUrlItem urlItem, Object data) {
        DataContentType dataContentType = urlItem.contentType();
        if (dataContentType == DataContentType.JSON) {
            String json = data == null ? "{}" : JSONObject.toJSONString(data);
            Encryptor encryptor = createEncryptor(nodeInfo);
            return new TunnelRequestBody(encrypt(encryptor, json).getBytes(StandardCharsets.UTF_8), "application/json");
        }
        if (data == null) {
            return new TunnelRequestBody(new byte[0], "application/x-www-form-urlencoded");
        }
        if (!(data instanceof Map)) {
            throw new IllegalArgumentException("反向隧道暂不支持该参数类型：" + data.getClass());
        }
        Map<String, Object> map = (Map<String, Object>) data;
        Encryptor encryptor = createEncryptor(nodeInfo);
        boolean multipart = map.values().stream().anyMatch(TunnelRequestBody::isFileValue);
        if (multipart) {
            return buildMultipart(map, encryptor);
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String[]) {
                String[] values = (String[]) value;
                for (String item : values) {
                    appendFormItem(builder, entry.getKey(), encrypt(encryptor, item));
                }
                continue;
            }
            appendFormItem(builder, entry.getKey(), value == null ? StrUtil.EMPTY : encrypt(encryptor, StrUtil.toStringOrNull(value)));
        }
        return new TunnelRequestBody(builder.toString().getBytes(StandardCharsets.UTF_8), "application/x-www-form-urlencoded");
    }

    private static Encryptor createEncryptor(INodeInfo nodeInfo) {
        try {
            return EncryptFactory.createEncryptor(nodeInfo.transportEncryption());
        } catch (Exception e) {
            throw new org.dromara.jpom.transport.TransportAgentException("创建传输加密器失败：" + e.getMessage());
        }
    }

    private static String encrypt(Encryptor encryptor, String value) {
        try {
            return encryptor.encrypt(value);
        } catch (Exception e) {
            throw new org.dromara.jpom.transport.TransportAgentException("参数加密失败：" + e.getMessage());
        }
    }

    private static void appendFormItem(StringBuilder builder, String name, String value) {
        if (builder.length() > 0) {
            builder.append('&');
        }
        // 必须做 URL 编码：参数值里出现 & = + % 等字符时（文件内容、脚本、命令、路径），
        // 不编码会把值截断成多个参数，插件端只能读到第一个 & 之前的内容（文件被写坏）。
        builder.append(encodeForm(name)).append('=').append(encodeForm(value));
    }

    /**
     * 表单值编码：{@code application/x-www-form-urlencoded} 语义（空格编码为 {@code +}）。
     *
     * @param value 原始值
     * @return 编码后的值
     */
    private static String encodeForm(String value) {
        try {
            return URLEncoder.encode(StrUtil.nullToEmpty(value), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean isFileValue(Object value) {
        return value instanceof File || value instanceof Resource || value instanceof byte[];
    }

    private static TunnelRequestBody buildMultipart(Map<String, Object> map, Encryptor encryptor) {
        String boundary = "----jpomTunnel" + IdUtil.fastSimpleUUID();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String[]) {
                for (String item : (String[]) value) {
                    writeTextPart(outputStream, boundary, name, encrypt(encryptor, item));
                }
            } else if (value instanceof File) {
                File file = (File) value;
                writeFilePart(outputStream, boundary, name, file.getName(), FileUtil.readBytes(file));
            } else if (value instanceof Resource) {
                Resource resource = (Resource) value;
                try (InputStream inputStream = resource.getStream()) {
                    writeFilePart(outputStream, boundary, name, resource.getName(), IoUtil.readBytes(inputStream));
                } catch (IOException e) {
                    throw new IllegalStateException("读取上传资源失败：" + resource.getName(), e);
                }
            } else if (value instanceof byte[]) {
                writeFilePart(outputStream, boundary, name, name, (byte[]) value);
            } else {
                writeTextPart(outputStream, boundary, name, encrypt(encryptor, StrUtil.toStringOrNull(value)));
            }
        }
        write(outputStream, "--" + boundary + "--" + CRLF);
        String contentType = "multipart/form-data; boundary=" + boundary;
        return new TunnelRequestBody(outputStream.toByteArray(), contentType);
    }

    private static void writeTextPart(ByteArrayOutputStream outputStream, String boundary, String name, String value) {
        write(outputStream, "--" + boundary + CRLF
            + "Content-Disposition: form-data; name=\"" + name + "\"" + CRLF
            + "Content-Type: text/plain; charset=UTF-8" + CRLF + CRLF);
        write(outputStream, StrUtil.nullToEmpty(value));
        write(outputStream, CRLF);
    }

    private static void writeFilePart(ByteArrayOutputStream outputStream, String boundary, String name, String fileName, byte[] content) {
        write(outputStream, "--" + boundary + CRLF
            + "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName + "\"" + CRLF
            + "Content-Type: application/octet-stream" + CRLF + CRLF);
        outputStream.write(content, 0, content.length);
        write(outputStream, CRLF);
    }

    private static void write(ByteArrayOutputStream outputStream, String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        outputStream.write(bytes, 0, bytes.length);
    }
}
