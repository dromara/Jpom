/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.user;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 触发器地址模板集合：响应 key → 地址模板（{id}/{token} 占位）
 * <p>
 * 每类触发器只声明自己的「响应 key 与地址模板」，其余（查不到 405、token 发放与审计、contextPath 拼装、
 * 响应字段补齐）全部由 {@link TriggerUrlAssembler} 统一处理——新增一种触发器不再复制整套流程。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
public final class TriggerUrls {

    private final List<Entry> entries = new ArrayList<>();
    private boolean includeDataId = true;

    private TriggerUrls() {
    }

    public static TriggerUrls builder() {
        return new TriggerUrls();
    }

    /**
     * 增加一个地址
     *
     * @param responseKey 响应 key（与历史保持一致，不得改名，见 §0.15）
     * @param template    {@code ServerOpenApi} 中的地址模板
     * @return this
     */
    public TriggerUrls add(String responseKey, String template) {
        entries.add(new Entry(responseKey, template, false));
        return this;
    }

    /**
     * 增加一个「别名地址」：模板中的 {id} 用数据别名（如文件存储的 aliasCode）替换，别名为空时该条不输出
     *
     * @param responseKey 响应 key
     * @param template    地址模板
     * @return this
     */
    public TriggerUrls addAlias(String responseKey, String template) {
        entries.add(new Entry(responseKey, template, true));
        return this;
    }

    /**
     * 不输出 id 字段（全局 token 这类没有实体 id 的场景）
     *
     * @return this
     */
    public TriggerUrls withoutDataId() {
        this.includeDataId = false;
        return this;
    }

    /**
     * 结束构建（语义化收尾，便于链式书写）
     *
     * @return this
     */
    public TriggerUrls build() {
        return this;
    }

    List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }

    boolean includeDataId() {
        return includeDataId;
    }

    @Getter
    static final class Entry {
        private final String responseKey;
        private final String template;
        private final boolean alias;

        private Entry(String responseKey, String template, boolean alias) {
            this.responseKey = responseKey;
            this.template = template;
            this.alias = alias;
        }
    }
}
