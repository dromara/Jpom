/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.transport.tunnel.ws;

import java.io.InputStream;

/**
 * 隧道下载响应（对应 {@code TransportServer.download} 的回调内容）。
 *
 * @author jpom
 * @since 2026/11/18
 */
public class TunnelDownload {

    private final int status;
    private final String contentDisposition;
    private final String contentType;
    private final long contentLength;
    private final InputStream inputStream;

    public TunnelDownload(int status, String contentDisposition, String contentType, InputStream inputStream) {
        this(status, contentDisposition, contentType, -1L, inputStream);
    }

    public TunnelDownload(int status, String contentDisposition, String contentType, long contentLength, InputStream inputStream) {
        this.status = status;
        this.contentDisposition = contentDisposition;
        this.contentType = contentType;
        this.contentLength = contentLength;
        this.inputStream = inputStream;
    }

    public int getStatus() {
        return status;
    }

    public String getContentDisposition() {
        return contentDisposition;
    }

    public String getContentType() {
        return contentType;
    }

    /**
     * 响应声明的内容长度（-1 表示未知）
     *
     * @return 字节数
     */
    public long getContentLength() {
        return contentLength;
    }

    public InputStream getInputStream() {
        return inputStream;
    }
}
