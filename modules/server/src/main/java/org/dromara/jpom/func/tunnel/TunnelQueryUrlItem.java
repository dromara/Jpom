/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.tunnel;

import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.transport.DataContentType;
import org.dromara.jpom.transport.IUrlItem;

import java.util.Map;

/**
 * 把 query 追加到请求路径上的 {@link IUrlItem} 包装。
 * <p>
 * 反向隧道的下载类请求在插件端是 {@code @GetMapping}，GET 不解析请求体，参数必须放在 query 上。
 * 这里只包装路径，其余（超时、工作空间、contentType、请求头）全部委派给原对象，正向/隧道其余链路零改动。
 * </p>
 *
 * @author jpom
 * @since 2026/11/20
 */
class TunnelQueryUrlItem implements IUrlItem {

    private final IUrlItem delegate;
    private final String query;

    TunnelQueryUrlItem(IUrlItem delegate, String query) {
        this.delegate = delegate;
        this.query = StrUtil.nullToEmpty(query);
    }

    @Override
    public String path() {
        String base = delegate.path();
        if (StrUtil.isEmpty(query)) {
            return base;
        }
        return base + (StrUtil.contains(base, "?") ? "&" : "?") + query;
    }

    @Override
    public Integer timeout() {
        return delegate.timeout();
    }

    @Override
    public String workspaceId() {
        return delegate.workspaceId();
    }

    @Override
    public DataContentType contentType() {
        return delegate.contentType();
    }

    @Override
    public Map<String, String> header() {
        return delegate.header();
    }
}
