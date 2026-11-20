/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.transport;

import lombok.Builder;
import lombok.Data;

import java.io.InputStream;

/**
 * @author bwcx_jzy
 * @since 2022/12/24
 */
@Builder
@Data
public class DownloadCallback {

    private String contentDisposition;

    private String contentType;

    /**
     * 响应状态码（200 全量 / 206 分片，其他视为异常响应）
     */
    @Builder.Default
    private int status = 200;

    /**
     * 响应声明的内容长度，-1 表示未知（chunked 或隧道未透传）
     * <p>
     * 用途：把「对端少发了字节（连接被中断）」和「产物本身大小不对」区分开。HTTP 客户端在
     * 固定长度响应被提前关闭时不会抛异常，只会静默返回 EOF，没有这个长度就无法判断是传输截断。
     */
    @Builder.Default
    private long contentLength = -1L;

    private InputStream inputStream;
}
