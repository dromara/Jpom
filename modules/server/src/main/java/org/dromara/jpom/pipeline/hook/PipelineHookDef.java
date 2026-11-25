/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.hook;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 事件钩子配置（存 {@code PIPELINE_INFO.hooksJson}）
 * <p>
 * 一个钩子＝「一组事件 + 一个投递方式」。投递方式由 {@link #type} 决定，具体实现在
 * {@link PipelineHookHandler} 的实现类里；新增投递方式不需要改这个 DTO。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
@Data
public class PipelineHookDef {

    /**
     * 投递方式：script（服务端脚本）/ webhook（HTTP 回调）
     */
    private String type;
    /**
     * 是否启用
     */
    private Boolean enabled = Boolean.TRUE;
    /**
     * 触发事件码（见 {@link PipelineEvent}，支持 {@link PipelineEvent#ALL}）
     */
    private List<String> events = new ArrayList<>();
    /**
     * 服务端脚本 ID（type=script）
     */
    private String scriptId;
    /**
     * 回调地址（type=webhook）
     */
    private String url;
    /**
     * 请求方式 GET / POST（type=webhook，默认 POST）
     */
    private String method = "POST";
    /**
     * 请求体格式 form / json（type=webhook，仅 POST 生效）
     */
    private String bodyType = "json";
    /**
     * 附加请求头，每行一个 {@code KEY: VALUE}（type=webhook）
     */
    private String headers;

    /**
     * @return 是否启用（null 视为启用）
     */
    public boolean enabled() {
        return enabled == null || enabled;
    }

    /**
     * 是否匹配某个事件
     *
     * @param event 事件
     * @return true 表示该事件的钩子
     */
    public boolean match(PipelineEvent event) {
        if (!this.enabled() || event == null || this.events == null) {
            return false;
        }
        for (String code : this.events) {
            if (StrUtil.equals(code, event.getCode()) || StrUtil.equalsIgnoreCase(code, PipelineEvent.ALL)) {
                return true;
            }
        }
        return false;
    }
}
