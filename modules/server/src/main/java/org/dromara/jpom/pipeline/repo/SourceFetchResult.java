/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.repo;

/**
 * 仓库源拉取结果
 * <p>
 * 只放「拉到了哪个提交、有没有用上缓存」，阶段执行器与运行记录都从这里取；
 * 日志文案不在这里拼，避免把「结果」和「展示」耦合在一起。
 * </p>
 *
 * @author jpom
 * @since 2.11.15.3
 */
public class SourceFetchResult {

    /**
     * 本次拉取到的提交号
     */
    private final String commitId;
    /**
     * 是否复用了已有的本地缓存（false 表示本次是新建缓存或直接克隆）
     */
    private final boolean cacheReused;
    /**
     * 缓存标识（未使用缓存时为 null）
     */
    private final String cacheKey;

    public SourceFetchResult(String commitId, boolean cacheReused, String cacheKey) {
        this.commitId = commitId;
        this.cacheReused = cacheReused;
        this.cacheKey = cacheKey;
    }

    public SourceFetchResult(String commitId) {
        this(commitId, false, null);
    }

    /**
     * 解析插件返回值（插件约定 {@code String[]{commitId, commitMsg, errorMsg}}）
     *
     * @param result 插件返回值
     * @return 结果
     */
    public static SourceFetchResult fromPluginResult(Object result) {
        if (result instanceof String[]) {
            String[] array = (String[]) result;
            return new SourceFetchResult(array.length > 0 ? array[0] : null);
        }
        return new SourceFetchResult(null);
    }

    public String getCommitId() {
        return commitId;
    }

    public boolean isCacheReused() {
        return cacheReused;
    }

    public String getCacheKey() {
        return cacheKey;
    }
}
