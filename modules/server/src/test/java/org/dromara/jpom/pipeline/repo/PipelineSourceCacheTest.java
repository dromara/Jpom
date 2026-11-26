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

import cn.hutool.core.io.FileUtil;
import org.dromara.jpom.model.data.RepositoryModel;
import org.dromara.jpom.pipeline.model.PipelineSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * 源码缓存的行为测试（真实 git，本地 bare 仓当远端）
 * <p>
 * 覆盖群反馈（2026-11-26）的第二个问题：每次运行都全量拉取。这里用真实 git 验证四件事：
 * 首次建缓存、第二次只做增量且命中缓存、`--depth` 真的让工作区是浅仓、
 * 工作区与缓存解耦（删缓存不影响已拉好的工作区）。
 * </p>
 *
 * @author jpom
 * @since 2.11.15.3
 */
public class PipelineSourceCacheTest {

    private static File root;
    private static File remoteGit;
    private static File sourceRepo;

    @BeforeAll
    public static void prepare() throws Exception {
        Assumptions.assumeTrue(hasGit(), "需要系统 git 才能验证源码缓存");
        root = Files.createTempDirectory("jpom-source-cache").toFile();
        remoteGit = new File(root, "remote.git");
        sourceRepo = new File(root, "src");
        exec(root, "git", "init", "--bare", "remote.git");
        exec(root, "git", "init", "src");
        exec(sourceRepo, "git", "symbolic-ref", "HEAD", "refs/heads/main");
        exec(sourceRepo, "git", "config", "user.email", "test@example.com");
        exec(sourceRepo, "git", "config", "user.name", "test");
        commit("c1");
        commit("c2");
        exec(sourceRepo, "git", "tag", "-a", "v1", "-m", "v1");
        exec(sourceRepo, "git", "remote", "add", "origin", remoteGit.getAbsolutePath());
        exec(sourceRepo, "git", "push", "-q", "origin", "main", "--tags");
        exec(remoteGit, "git", "symbolic-ref", "HEAD", "refs/heads/main");
    }

    private static void commit(String message) throws Exception {
        FileUtil.writeUtf8String(message, new File(sourceRepo, "f.txt"));
        exec(sourceRepo, "git", "add", "f.txt");
        exec(sourceRepo, "git", "commit", "-q", "-m", message);
    }

    private static RepositoryModel repository() {
        RepositoryModel repository = new RepositoryModel();
        repository.setId("repo-cache-test");
        repository.setName("cache-test");
        repository.setGitUrl(remoteGit.getAbsolutePath());
        return repository;
    }

    private static PipelineSource source(String ref, Integer depth) {
        PipelineSource source = new PipelineSource();
        source.setId("src1");
        source.setRepoId("repo-cache-test");
        source.setRef(ref);
        source.setCloneDepth(depth);
        return source;
    }

    private static PipelineSourceSync.LogSink logSink(List<String> lines) {
        return new PipelineSourceSync.LogSink() {
            @Override
            public void info(String message) {
                lines.add(message);
            }

            @Override
            public PrintWriter writer() {
                return new PrintWriter(new StringWriter());
            }
        };
    }

    private static File newDir(String name) {
        File dir = new File(root, name + "-" + System.nanoTime());
        FileUtil.mkdir(dir);
        return dir;
    }

    @Test
    public void secondRunReusesCacheAndStaysShallow() throws Exception {
        File cacheRoot = newDir("cache");
        List<String> logs = new ArrayList<>();
        CachedGitSourceFetcher fetcher = new CachedGitSourceFetcher();
        File mount1 = newDir("mount1");
        SourceFetchResult first = fetcher.fetch(source("branch:main", 1), repository(), mount1, cacheRoot, logSink(logs));
        Assertions.assertNotNull(first.getCommitId());
        Assertions.assertFalse(first.isCacheReused(), "首次运行需要建立缓存");
        Assertions.assertEquals(1, commitCount(mount1), "克隆深度=1 时工作区只能是浅仓");
        Assertions.assertTrue(FileUtil.exist(new File(mount1, ".git/shallow")), "浅仓标志文件必须存在");

        commit("c3");
        exec(sourceRepo, "git", "push", "-q", "origin", "main");

        File mount2 = newDir("mount2");
        SourceFetchResult second = fetcher.fetch(source("branch:main", 1), repository(), mount2, cacheRoot, logSink(logs));
        Assertions.assertTrue(second.isCacheReused(), "同一仓库/引用/深度第二次运行必须命中缓存");
        Assertions.assertNotEquals(first.getCommitId(), second.getCommitId(), "远端有新提交时工作区要跟上");
        Assertions.assertEquals(1, commitCount(mount2));
        Assertions.assertEquals(1, cacheDirs(cacheRoot).size(), "同一引用只允许一份缓存");

        // 工作区与缓存解耦：删缓存不影响已经拉好的工作区（运行记录里还要能回看）
        FileUtil.del(cacheRoot);
        Assertions.assertEquals(1, commitCount(mount1));
    }

    @Test
    public void tagRefChecksOutExactCommit() throws Exception {
        File cacheRoot = newDir("cache-tag");
        CachedGitSourceFetcher fetcher = new CachedGitSourceFetcher();
        File mount = newDir("mount-tag");
        fetcher.fetch(source("tag:v1", 1), repository(), mount, cacheRoot, logSink(new ArrayList<>()));
        String expected = exec(remoteGit, "git", "rev-parse", "refs/tags/v1^{commit}").trim();
        Assertions.assertEquals(expected, exec(mount, "git", "rev-parse", "HEAD").trim());
    }

    @Test
    public void emptyRefUsesRemoteDefaultBranch() throws Exception {
        File cacheRoot = newDir("cache-default");
        CachedGitSourceFetcher fetcher = new CachedGitSourceFetcher();
        File mount = newDir("mount-default");
        SourceFetchResult result = fetcher.fetch(source("", null), repository(), mount, cacheRoot, logSink(new ArrayList<>()));
        String expected = exec(remoteGit, "git", "rev-parse", "refs/heads/main").trim();
        Assertions.assertEquals(expected, result.getCommitId(), "留空引用必须跟随远端默认分支");
        Assertions.assertEquals(commitCount(remoteGit), commitCount(mount), "未设置深度时保持完整克隆");
    }

    private static List<File> cacheDirs(File cacheRoot) {
        File[] files = cacheRoot.listFiles(File::isDirectory);
        List<File> list = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                list.add(file);
            }
        }
        return list;
    }

    private static int commitCount(File repo) throws Exception {
        return Integer.parseInt(exec(repo, "git", "rev-list", "--count", "HEAD").trim());
    }

    private static boolean hasGit() {
        try {
            return exec(null, "git", "--version").contains("git version");
        } catch (Exception e) {
            return false;
        }
    }

    private static String exec(File dir, String... command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        if (dir != null) {
            builder.directory(dir);
        }
        builder.redirectErrorStream(true);
        Process process = builder.start();
        byte[] bytes = readAll(process.getInputStream());
        int code = process.waitFor();
        String output = new String(bytes, StandardCharsets.UTF_8);
        if (code != 0) {
            throw new IOException("command failed(" + code + "): " + String.join(" ", command) + "\n" + output);
        }
        return output;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = in.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
        return out.toByteArray();
    }
}
