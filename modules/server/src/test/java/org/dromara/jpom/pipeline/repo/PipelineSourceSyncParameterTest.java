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

import org.dromara.jpom.model.data.RepositoryModel;
import org.dromara.jpom.pipeline.model.PipelineSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 仓库源拉取的行为测试：克隆深度必须切系统 git、引用解析、缓存 key、回退链路
 * <p>
 * 这组规则是群反馈（2026-11-26）的直接回归点：漏了 `gitProcessType=SystemGit`，
 * 界面上配的「克隆深度」就会被 JGit 静默丢弃，且日志里什么都没有。
 * </p>
 *
 * @author jpom
 * @since 2.11.15.3
 */
public class PipelineSourceSyncParameterTest {

    private static RepositoryModel repository() {
        RepositoryModel repository = new RepositoryModel();
        repository.setId("repo1");
        repository.setName("demo");
        repository.setGitUrl("https://example.com/demo.git");
        return repository;
    }

    private static PipelineSource source(String ref, Integer depth) {
        PipelineSource source = new PipelineSource();
        source.setId("src1");
        source.setRepoId("repo1");
        source.setRef(ref);
        source.setCloneDepth(depth);
        return source;
    }

    @Test
    public void cloneDepthMustSwitchToSystemGit() {
        Map<String, Object> map = PluginGitSourceFetcher.buildParameter(source("branch:main", 1), repository());
        Assertions.assertEquals(1, map.get("depth"));
        Assertions.assertEquals("SystemGit", map.get("gitProcessType"),
            "克隆深度只有系统 git 实现会消费，不切 SystemGit 就会被 JGit 静默丢弃");
    }

    @Test
    public void fullCloneKeepsPluginDefault() {
        Map<String, Object> none = PluginGitSourceFetcher.buildParameter(source("branch:main", null), repository());
        Assertions.assertFalse(none.containsKey("depth"));
        Assertions.assertFalse(none.containsKey("gitProcessType"));

        Map<String, Object> zero = PluginGitSourceFetcher.buildParameter(source("branch:main", 0), repository());
        Assertions.assertFalse(zero.containsKey("depth"), "0 表示完整克隆，不应再传 depth");
        Assertions.assertFalse(zero.containsKey("gitProcessType"));
    }

    @Test
    public void refParsing() {
        PipelineSource tag = source("tag:v1.0", 1);
        Assertions.assertTrue(tag.isTagRef());
        Assertions.assertEquals("v1.0", tag.refName());

        PipelineSource branch = source("branch:dev", 1);
        Assertions.assertFalse(branch.isTagRef());
        Assertions.assertEquals("dev", branch.refName());

        PipelineSource empty = source(null, 1);
        Assertions.assertFalse(empty.isTagRef());
        Assertions.assertEquals("", empty.refName(), "留空表示仓库默认分支");
    }

    @Test
    public void cloneDepthNormalized() {
        Assertions.assertNull(source("branch:main", null).effectiveCloneDepth());
        Assertions.assertNull(source("branch:main", 0).effectiveCloneDepth());
        Assertions.assertNull(source("branch:main", -1).effectiveCloneDepth());
        Assertions.assertEquals(3, source("branch:main", 3).effectiveCloneDepth());
    }

    @Test
    public void cacheNameSeparatesRefAndDepth() {
        String base = CachedGitSourceFetcher.cacheName(repository(), false, "main", 1);
        Assertions.assertEquals(base, CachedGitSourceFetcher.cacheName(repository(), false, "main", 1), "同参数必须复用同一份缓存");
        Assertions.assertNotEquals(base, CachedGitSourceFetcher.cacheName(repository(), false, "main", null));
        Assertions.assertNotEquals(base, CachedGitSourceFetcher.cacheName(repository(), false, "dev", 1));
        Assertions.assertNotEquals(base, CachedGitSourceFetcher.cacheName(repository(), true, "v1", 1));
        Assertions.assertFalse(base.contains("/") || base.contains("\\"), "缓存目录名不能带路径分隔符");
    }

    @Test
    public void fallbackToNextFetcherWhenCacheFails() throws Exception {
        List<String> logs = new ArrayList<>();
        AtomicInteger pluginCalls = new AtomicInteger();
        PipelineSourceFetcher broken = new PipelineSourceFetcher() {
            @Override
            public String label() {
                return "cache";
            }

            @Override
            public boolean supports(RepositoryModel repository) {
                return true;
            }

            @Override
            public SourceFetchResult fetch(PipelineSource source, RepositoryModel repository, File mountDir,
                                           File cacheRoot, PipelineSourceSync.LogSink log) {
                throw new IllegalStateException("cache boom");
            }
        };
        PipelineSourceFetcher plugin = new PipelineSourceFetcher() {
            @Override
            public String label() {
                return "plugin";
            }

            @Override
            public boolean supports(RepositoryModel repository) {
                return true;
            }

            @Override
            public SourceFetchResult fetch(PipelineSource source, RepositoryModel repository, File mountDir,
                                           File cacheRoot, PipelineSourceSync.LogSink log) {
                pluginCalls.incrementAndGet();
                return new SourceFetchResult("abc123");
            }
        };
        PipelineSourceSync sync = new PipelineSourceSync(null, Arrays.asList(broken, plugin));
        PipelineSourceSync.LogSink sink = new PipelineSourceSync.LogSink() {
            @Override
            public void info(String message) {
                logs.add(message);
            }

            @Override
            public PrintWriter writer() {
                return new PrintWriter(new StringWriter());
            }
        };
        SourceFetchResult result = sync.syncOne(new File("/tmp"), repository(), source("branch:main", 1),
            new File("/tmp"), sink);
        Assertions.assertEquals("abc123", result.getCommitId());
        Assertions.assertEquals(1, pluginCalls.get());
        Assertions.assertEquals(1, logs.size(), "回退必须在运行日志里留一行，用户才知道缓存没生效");
    }

    @Test
    public void unsupportedFetchersAreSkipped() throws Exception {
        List<String> labels = new ArrayList<>();
        PipelineSourceFetcher skipped = new PipelineSourceFetcher() {
            @Override
            public String label() {
                return "skipped";
            }

            @Override
            public boolean supports(RepositoryModel repository) {
                return false;
            }

            @Override
            public SourceFetchResult fetch(PipelineSource source, RepositoryModel repository, File mountDir,
                                           File cacheRoot, PipelineSourceSync.LogSink log) {
                labels.add("skipped");
                return new SourceFetchResult("never");
            }
        };
        PipelineSourceSync sync = new PipelineSourceSync(null, Collections.singletonList(skipped));
        Assertions.assertThrows(IllegalStateException.class, () -> sync.syncOne(new File("/tmp"), repository(),
            source("branch:main", null), new File("/tmp"), new PipelineSourceSync.LogSink() {
                @Override
                public void info(String message) {
                }

                @Override
                public PrintWriter writer() {
                    return new PrintWriter(new StringWriter());
                }
            }));
        Assertions.assertTrue(labels.isEmpty());
    }
}
