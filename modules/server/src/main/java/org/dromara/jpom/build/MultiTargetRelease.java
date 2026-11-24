/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.build;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 多目标发布（SSH / SSH 项目）：按目标顺序逐个发布，并统一「任一目标失败即终止」策略
 * <p>
 * 2026-11-23 构建发布支持多目标（SSH 项目多选）后，失败策略是需要被验证的行为：
 * 开启时首个失败即停止、剩下的目标不再发布；关闭时继续发布剩余目标，但只要有目标失败
 * 本次发布仍然判定失败。把这段策略从 {@link ReleaseManage} 的私有方法里提出来，
 * 是为了让「继续 / 终止 / 失败聚合」有行为级单测（原实现只能靠 Spring 容器 + 真实目标机才能触发）。
 *
 * @author bwcx_jzy
 * @since 2026/11/23
 */
public final class MultiTargetRelease {

    private MultiTargetRelease() {
    }

    /**
     * 单个目标的发布动作
     */
    @FunctionalInterface
    public interface TargetAction {
        /**
         * 发布一个目标
         *
         * @param targetId 目标 id
         * @throws Exception 发布失败（目标不存在、连接/上传异常等）
         */
        void release(String targetId) throws Exception;
    }

    /**
     * 一个目标的失败信息
     */
    public static final class Failure {
        private final String targetId;
        private final String message;

        Failure(String targetId, String message) {
            this.targetId = targetId;
            this.message = message;
        }

        public String getTargetId() {
            return targetId;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return targetId + ": " + message;
        }
    }

    /**
     * 一轮多目标发布的结果
     */
    public static final class Outcome {
        private final List<Failure> failures;
        private final int skipped;
        private final boolean aborted;

        Outcome(List<Failure> failures, int skipped, boolean aborted) {
            this.failures = Collections.unmodifiableList(failures);
            this.skipped = skipped;
            this.aborted = aborted;
        }

        /**
         * @return 失败的目标（按发布顺序）
         */
        public List<Failure> getFailures() {
            return failures;
        }

        /**
         * @return 因失败即终止而没有发布的目标数量
         */
        public int getSkipped() {
            return skipped;
        }

        /**
         * @return 是否因「任一目标失败即终止」提前结束
         */
        public boolean isAborted() {
            return aborted;
        }

        /**
         * @return 是否存在失败目标
         */
        public boolean hasFailure() {
            return CollUtil.isNotEmpty(failures);
        }
    }

    /**
     * 逐个发布目标
     *
     * @param targetIds 逗号分隔的目标 id（空值与多余空白忽略）
     * @param failFast  任一目标失败是否立即终止（true：首个失败即停；false：继续发布剩余目标）
     * @param action    单个目标的发布动作
     * @return 发布结果（失败目标、跳过数量、是否提前终止）
     */
    public static Outcome execute(String targetIds, boolean failFast, TargetAction action) {
        List<String> targets = StrUtil.splitTrim(StrUtil.nullToEmpty(targetIds), StrUtil.COMMA);
        List<Failure> failures = new ArrayList<>();
        int skipped = 0;
        boolean aborted = false;
        for (int i = 0; i < targets.size(); i++) {
            String targetId = targets.get(i);
            try {
                action.release(targetId);
            } catch (Exception e) {
                String message = StrUtil.emptyToDefault(e.getMessage(), e.getClass().getSimpleName());
                failures.add(new Failure(targetId, message));
                if (failFast) {
                    skipped = targets.size() - i - 1;
                    aborted = true;
                    break;
                }
            }
        }
        return new Outcome(failures, skipped, aborted);
    }
}
