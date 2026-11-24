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

import cn.keepbx.jpom.RemoteVersion;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 远端版本信息解析（在线刷新重试 + 缓存兜底）行为级回归。
 *
 * <p>背景：2.11.14.6 在 demo 走在线升级时，下载中心偶发 {@code SSLException: Connection reset}，
 * {@code loadRemoteInfo()} 只拉一次、失败即返回 null，升级/下载接口直接 405，且文案带 {@code :-1}。
 * 现在动作类请求刷新最多 3 次、失败回退本机缓存，都没有才抛可读异常。</p>
 *
 * @author jpom
 * @since 2.11.14.7
 */
public class RemoteVersionResolverTest {

    private static RemoteVersion remote(String tagName) {
        RemoteVersion remoteVersion = new RemoteVersion();
        remoteVersion.setTagName(tagName);
        return remoteVersion;
    }

    /**
     * 假来源：按脚本依次返回 fetch 结果（null 视为拉取失败），并记录调用次数
     */
    private static class FakeSource implements RemoteVersionSource {

        private final List<RemoteVersion> fetchResults = new ArrayList<>();
        private RemoteVersion cached;
        private RuntimeException cachedException;
        private int fetchCount;

        private FakeSource fetchResults(RemoteVersion... results) {
            this.fetchResults.addAll(Arrays.asList(results));
            return this;
        }

        private FakeSource cached(RemoteVersion cached) {
            this.cached = cached;
            return this;
        }

        @Override
        public RemoteVersion fetch() {
            int index = this.fetchCount++;
            return index < this.fetchResults.size() ? this.fetchResults.get(index) : null;
        }

        @Override
        public RemoteVersion cached() {
            if (this.cachedException != null) {
                throw this.cachedException;
            }
            return this.cached;
        }
    }

    @Test
    public void actionRetriesUntilFetched() {
        FakeSource source = new FakeSource().fetchResults(null, remote("v2.11.14.7"));
        RemoteVersionResolver resolver = new RemoteVersionResolver(source, 3, 0L);
        RemoteVersion remoteVersion = resolver.resolveForAction();
        Assert.assertNotNull("第二次拉取成功应继续动作，不应报错", remoteVersion);
        Assert.assertEquals("v2.11.14.7", remoteVersion.getTagName());
        Assert.assertEquals("首次失败应自动重试（共 2 次拉取）", 2, source.fetchCount);
    }

    @Test
    public void actionFallsBackToCacheWhenAllFetchesFail() {
        RemoteVersion cached = remote("v2.11.14.6");
        FakeSource source = new FakeSource().cached(cached);
        RemoteVersionResolver resolver = new RemoteVersionResolver(source, 3, 0L);
        RemoteVersion remoteVersion = resolver.resolveForAction();
        Assert.assertEquals("在线刷新全失败应回退本机缓存", "v2.11.14.6", remoteVersion.getTagName());
        Assert.assertEquals("动作类请求应尝试 3 次", 3, source.fetchCount);
    }

    @Test
    public void actionThrowsWhenNoFetchAndNoCache() {
        FakeSource source = new FakeSource();
        RemoteVersionResolver resolver = new RemoteVersionResolver(source, 3, 0L);
        try {
            resolver.resolveForAction();
            Assert.fail("既无在线信息又无缓存时应抛 UnavailableException");
        } catch (RemoteVersionResolver.UnavailableException e) {
            Assert.assertEquals(3, source.fetchCount);
        }
    }

    @Test
    public void displayPrefersCacheAndSkipsNetwork() {
        FakeSource source = new FakeSource().cached(remote("v2.11.14.6")).fetchResults(remote("v2.11.14.7"));
        RemoteVersionResolver resolver = new RemoteVersionResolver(source, 3, 0L);
        RemoteVersion remoteVersion = resolver.resolveForDisplay();
        Assert.assertEquals("v2.11.14.6", remoteVersion.getTagName());
        Assert.assertEquals("缓存命中时不应发起网络请求", 0, source.fetchCount);
    }

    @Test
    public void displayRefreshesOnceWhenCacheMissing() {
        FakeSource source = new FakeSource().fetchResults(null, remote("v2.11.14.7"));
        RemoteVersionResolver resolver = new RemoteVersionResolver(source, 3, 0L);
        Assert.assertNull("展示类只刷新一次，失败返回 null 不报错", resolver.resolveForDisplay());
        Assert.assertEquals("展示类请求不应做动作类的 3 次重试", 1, source.fetchCount);
    }

    @Test
    public void refreshFallsBackToCacheAndNeverThrows() {
        FakeSource source = new FakeSource().cached(remote("v2.11.14.6"));
        RemoteVersionResolver resolver = new RemoteVersionResolver(source, 3, 0L);
        RemoteVersion remoteVersion = resolver.resolveForRefresh();
        Assert.assertEquals("v2.11.14.6", remoteVersion.getTagName());
        Assert.assertEquals(3, source.fetchCount);
        // 连缓存都没有时保持接口原行为：返回 null（不抛异常、不 405）
        Assert.assertNull(new RemoteVersionResolver(new FakeSource(), 3, 0L).resolveForRefresh());
    }

    @Test
    public void brokenCacheDoesNotBreakDisplay() {
        FakeSource source = new FakeSource().fetchResults(remote("v2.11.14.7"));
        source.cachedException = new IllegalStateException("bad cache file");
        RemoteVersionResolver resolver = new RemoteVersionResolver(source, 3, 0L);
        Assert.assertEquals("缓存文件损坏时按未命中处理并回落在在线刷新", "v2.11.14.7",
            resolver.resolveForDisplay().getTagName());
    }
}
