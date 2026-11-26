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

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 一次事件的执行记录（存 {@code PIPELINE_RUN.eventsJson}）
 * <p>
 * 记录「谁在什么事件上做了什么、结果如何」：脚本退出码、webhook 投递结果都落在这里，
 * 运行详情能直接看到，不必翻服务端日志。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
@Data
public class PipelineEventRecord {

    /**
     * 事件码
     */
    private String event;
    /**
     * 触发时间
     */
    private long time = System.currentTimeMillis();
    /**
     * 阶段 ID（运行级事件为空）
     */
    private String stageId;
    /**
     * 阶段名称
     */
    private String stageName;
    /**
     * 触发时的运行状态码
     */
    private Integer status;
    /**
     * 脚本钩子执行结果
     */
    private List<ScriptRecord> scripts = new ArrayList<>();
    /**
     * webhook 投递结果
     */
    private List<WebhookRecord> webhooks = new ArrayList<>();
    /**
     * 未落到具体记录上的失败（未知投递方式、钩子实现异常、脚本退出非 0 等）
     * <p>
     * 单独一列是为了让「这次事件其实失败了」不会被记录漏掉：
     * 之前只看 scripts/webhooks，实现层返回错误但没写记录时会被误判为成功。
     * </p>
     */
    private List<HookError> errors = new ArrayList<>();

    /**
     * 脚本执行记录
     */
    @Data
    public static class ScriptRecord {
        private String scriptId;
        private String name;
        private Integer exitCode;
        private long duration;
        private boolean success;
        private String message;
    }

    /**
     * webhook 投递记录
     */
    @Data
    public static class WebhookRecord {
        private String url;
        private boolean success;
        private int attempts;
        private long duration;
        private String message;
    }

    /**
     * 钩子失败记录
     */
    @Data
    public static class HookError {
        private String type;
        private String target;
        private String message;
    }

    /**
     * @return 是否所有钩子都成功（没有钩子也算成功）
     */
    public boolean allSuccess() {
        if (!errors.isEmpty()) {
            return false;
        }
        for (ScriptRecord record : scripts) {
            if (!record.isSuccess()) {
                return false;
            }
        }
        for (WebhookRecord record : webhooks) {
            if (!record.isSuccess()) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return 钩子总数（含失败记录，未知投递方式也算一次尝试）
     */
    public int hookCount() {
        return scripts.size() + webhooks.size() + errors.size();
    }
}
