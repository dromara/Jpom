/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common.version;

import cn.hutool.core.thread.ThreadUtil;
import cn.keepbx.jpom.RemoteVersion;
import lombok.extern.slf4j.Slf4j;

/**
 * 远端版本信息解析器：在线刷新（失败自动重试）与本机缓存兜底
 *
 * <p>升级/下载、页面展示、显式「检查更新」三种语义分别由
 * {@link #resolveForAction()}、{@link #resolveForDisplay()}、{@link #resolveForRefresh()} 表达，
 * 避免每个入口各写一套「拉取失败怎么办」。</p>
 *
 * <p>本类刻意不依赖 Spring 与 i18n：只做重试与兜底决策，日志用英文 slf4j；用户可见文案
 * 统一由 {@link RemoteVersions} 出 i18n。</p>
 *
 * @author jpom
 * @since 2.11.14.7
 */
@Slf4j
public class RemoteVersionResolver {

    /**
     * 动作类请求（升级、下载升级包）的在线刷新尝试次数
     */
    public static final int DEFAULT_FETCH_ATTEMPTS = 3;
    /**
     * 重试间隔（毫秒）
     */
    public static final long DEFAULT_RETRY_INTERVAL_MILLIS = 500L;
    /**
     * 展示类请求（页面加载）在缓存缺失时的在线刷新尝试次数：只试一次，避免拖慢页面
     */
    public static final int DISPLAY_FETCH_ATTEMPTS = 1;

    /**
     * 默认装配点：官方下载中心（经三方库处理转发地址与授权）+ 本机缓存文件
     */
    private static final RemoteVersionResolver DEFAULT = new RemoteVersionResolver(new RemoteVersionSource() {
        @Override
        public RemoteVersion fetch() {
            return RemoteVersion.loadRemoteInfo();
        }

        @Override
        public RemoteVersion cached() {
            return RemoteVersion.cacheInfo();
        }
    });

    private final RemoteVersionSource source;
    private final int fetchAttempts;
    private final long retryIntervalMillis;

    public RemoteVersionResolver(RemoteVersionSource source) {
        this(source, DEFAULT_FETCH_ATTEMPTS, DEFAULT_RETRY_INTERVAL_MILLIS);
    }

    public RemoteVersionResolver(RemoteVersionSource source, int fetchAttempts, long retryIntervalMillis) {
        if (source == null) {
            throw new IllegalArgumentException("remote version source must not be null");
        }
        this.source = source;
        this.fetchAttempts = Math.max(fetchAttempts, 1);
        this.retryIntervalMillis = Math.max(retryIntervalMillis, 0L);
    }

    /**
     * 默认解析器（装配点）
     *
     * @return 使用官方下载中心与本机缓存的解析器
     */
    public static RemoteVersionResolver defaultResolver() {
        return DEFAULT;
    }

    /**
     * 升级/下载动作：在线刷新（首次失败自动重试）→ 本机缓存兜底 → 抛 {@link UnavailableException}
     *
     * @return 远端版本信息
     */
    public RemoteVersion resolveForAction() {
        RemoteVersion remoteVersion = this.refresh();
        if (remoteVersion != null) {
            return remoteVersion;
        }
        RemoteVersion cached = this.cached();
        if (cached != null) {
            log.warn("remote version info unavailable, fallback to local cache: {}", cached.getTagName());
            return cached;
        }
        throw new UnavailableException();
    }

    /**
     * 页面展示：优先本机缓存（与「检查更新」写回的缓存同源），缓存缺失时才在线刷新一次
     *
     * @return 远端版本信息；拿不到返回 null
     */
    public RemoteVersion resolveForDisplay() {
        RemoteVersion cached = this.cached();
        if (cached != null) {
            return cached;
        }
        return this.refresh(DISPLAY_FETCH_ATTEMPTS);
    }

    /**
     * 显式「检查更新」：在线刷新（失败自动重试）→ 本机缓存兜底 → null（保持接口原行为，不抛异常）
     *
     * @return 远端版本信息；拿不到返回 null
     */
    public RemoteVersion resolveForRefresh() {
        RemoteVersion remoteVersion = this.refresh();
        if (remoteVersion != null) {
            return remoteVersion;
        }
        RemoteVersion cached = this.cached();
        if (cached != null) {
            log.warn("remote version info unavailable, fallback to local cache: {}", cached.getTagName());
            return cached;
        }
        return null;
    }

    private RemoteVersion refresh() {
        return this.refresh(this.fetchAttempts);
    }

    private RemoteVersion refresh(int attempts) {
        for (int i = 0; i < attempts; i++) {
            if (i > 0 && this.retryIntervalMillis > 0) {
                ThreadUtil.sleep(this.retryIntervalMillis);
            }
            RemoteVersion remoteVersion = this.source.fetch();
            if (remoteVersion != null) {
                return remoteVersion;
            }
            log.debug("fetch remote version info failed, attempt {}/{}", i + 1, attempts);
        }
        return null;
    }

    private RemoteVersion cached() {
        try {
            return this.source.cached();
        } catch (RuntimeException e) {
            log.warn("read cached remote version info failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 在线刷新与本机缓存都没有远端版本信息
     */
    public static class UnavailableException extends IllegalStateException {

        public UnavailableException() {
            super("remote version info unavailable");
        }
    }
}
