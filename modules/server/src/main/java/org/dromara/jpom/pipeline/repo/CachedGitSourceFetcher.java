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
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import org.dromara.jpom.model.data.RepositoryModel;
import org.dromara.jpom.pipeline.model.PipelineSource;
import org.dromara.jpom.plugin.GitProcessFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 本地 bare 缓存 + 系统 git 拉取（默认实现）
 * <p>
 * 为什么需要它：流水线每次运行都是独立工作区（按运行号隔离），老实现每次都会从远端全新克隆一遍，
 * 大仓库每次十几分钟，且配了「克隆深度」也不会生效。这里改成两级：
 * </p>
 * <ol>
 *     <li>缓存：{@code data/pipeline/cache/<仓库>-<key>.git} 一个 bare 仓，只在首次克隆，
 *     之后每次只做增量 fetch（{@code --depth} 跟随克隆深度，保持浅仓）；</li>
 *     <li>物化：每次运行从缓存做本地 fetch（本地路径复制对象，不占双份空间也不依赖缓存长期存在），
 *     再 checkout 到本次运行目录，工作区之间互不影响。</li>
 * </ol>
 * <p>
 * 缓存损坏（目录被手工删改、fetch 冲突）时重置一次重试；仍失败则抛给
 * {@link PipelineSourceSync} 回退到插件克隆，保证构建不被打断。
 * </p>
 *
 * @author jpom
 * @since 2.11.15.3
 */
public class CachedGitSourceFetcher implements PipelineSourceFetcher {

    /**
     * 缓存内用于判断「这是一个可复用的 bare 仓」的标志文件
     */
    private static final String BARE_HEAD = "HEAD";

    @Override
    public String label() {
        return "cache";
    }

    @Override
    public boolean supports(RepositoryModel repository) {
        return RepositorySupport.isGit(repository) && GitProcessFactory.existsSystemGit();
    }

    @Override
    public SourceFetchResult fetch(PipelineSource source, RepositoryModel repository, File mountDir, File cacheRoot,
                                   PipelineSourceSync.LogSink log) throws Exception {
        Integer depth = source.effectiveCloneDepth();
        boolean tag = source.isTagRef();
        Map<String, String> env = environment(repository);
        FileUtil.mkdir(cacheRoot);
        String refName = source.refName();
        if (StrUtil.isEmpty(refName)) {
            refName = this.defaultBranch(cacheRoot, repository, env, log);
        }
        File cacheDir = FileUtil.file(cacheRoot, cacheName(repository, tag, refName, depth));
        boolean reused = isCache(cacheDir);
        log.info(PipelineSourceMessages.text(reused ? "i18n.pipeline_source_cache_reuse.6b12" : "i18n.pipeline_source_cache_build.4a7d")
            + cacheDir.getName() + (depth == null ? StrUtil.EMPTY : " (depth=" + depth + ")"));
        try {
            this.syncCache(cacheDir, repository, refName, tag, depth, env, log);
        } catch (Exception e) {
            log.info(PipelineSourceMessages.text("i18n.pipeline_source_cache_reset.2f8c") + e.getMessage());
            FileUtil.del(cacheDir);
            reused = false;
            this.syncCache(cacheDir, repository, refName, tag, depth, env, log);
        }
        String sha = new GitCommandRunner(cacheDir, env, log).output("rev-parse", revOf(refName, tag));
        log.info(PipelineSourceMessages.text("i18n.pipeline_source_cache_commit.8e52") + sha);
        this.materialize(mountDir, cacheDir, refName, tag, sha, depth, env, log);
        return new SourceFetchResult(sha, reused, cacheDir.getName());
    }

    /**
     * 同步缓存仓：首次建 bare 仓，之后增量 fetch
     *
     * @param cacheDir   缓存目录
     * @param repository 仓库配置
     * @param refName    分支/标签名
     * @param tag        是否标签
     * @param depth      克隆深度（null 表示全量）
     * @param env        环境变量
     * @param log        日志
     * @throws Exception 拉取失败
     */
    private void syncCache(File cacheDir, RepositoryModel repository, String refName, boolean tag, Integer depth,
                           Map<String, String> env, PipelineSourceSync.LogSink log) throws Exception {
        FileUtil.mkdir(cacheDir);
        GitCommandRunner git = new GitCommandRunner(cacheDir, env, log);
        if (!isCache(cacheDir)) {
            int code = git.run("init", "--bare", "-q", ".");
            if (code != 0) {
                throw new IOException("git init bare exit " + code);
            }
            if (!tag) {
                git.run("symbolic-ref", "HEAD", "refs/heads/" + refName);
            }
        }
        List<String> args = new ArrayList<>();
        args.add("fetch");
        args.add("--prune");
        args.add("--no-tags");
        if (depth != null) {
            args.add("--depth=" + depth);
        }
        args.add(GitUrls.covertUrl(repository));
        args.add((tag ? "+refs/tags/" : "+refs/heads/") + refName + ":" + (tag ? "refs/tags/" : "refs/heads/") + refName);
        int code = git.run(args.toArray(new String[0]));
        if (code != 0) {
            throw new IOException("git fetch exit " + code + " (" + GitUrls.sanitize(repository.getGitUrl()) + ")");
        }
    }

    /**
     * 把缓存仓里指定提交物化到本次运行的工作目录
     *
     * @param mountDir 工作目录
     * @param cacheDir 缓存目录（本地路径 fetch，对象以硬链接复制，工作区不依赖缓存存活）
     * @param refName  分支/标签名
     * @param tag      是否标签
     * @param sha      提交号
     * @param depth    克隆深度
     * @param env      环境变量
     * @param log      日志
     * @throws Exception 物化失败
     */
    private void materialize(File mountDir, File cacheDir, String refName, boolean tag, String sha, Integer depth,
                             Map<String, String> env, PipelineSourceSync.LogSink log) throws Exception {
        FileUtil.mkdir(mountDir);
        GitCommandRunner git = new GitCommandRunner(mountDir, env, log);
        if (!FileUtil.exist(FileUtil.file(mountDir, ".git"))) {
            int initCode = git.run("init", "-q", ".");
            if (initCode != 0) {
                throw new IOException("git init exit " + initCode);
            }
        }
        String cachePath = cacheDir.getAbsolutePath();
        int code = depth == null
            ? git.run("fetch", "--no-tags", cachePath, sha)
            : git.run("fetch", "--no-tags", "--depth=" + depth, cachePath, sha);
        if (code != 0) {
            throw new IOException("git fetch from cache exit " + code);
        }
        if (tag) {
            int tagCode = depth == null
                ? git.run("fetch", "--no-tags", cachePath, "refs/tags/" + refName + ":refs/tags/" + refName)
                : git.run("fetch", "--no-tags", "--depth=" + depth, cachePath, "refs/tags/" + refName + ":refs/tags/" + refName);
            int checkoutCode = tagCode == 0
                ? git.run("checkout", "-q", "-f", "refs/tags/" + refName)
                : git.run("checkout", "-q", "-f", sha);
            if (checkoutCode != 0) {
                throw new IOException("git checkout exit " + checkoutCode);
            }
        } else {
            int checkoutCode = git.run("checkout", "-q", "-f", "-B", refName, "FETCH_HEAD");
            if (checkoutCode != 0) {
                throw new IOException("git checkout exit " + checkoutCode);
            }
        }
        int submoduleCode = git.run("submodule", "update", "--init", "--recursive");
        if (submoduleCode != 0) {
            throw new IOException(PipelineSourceMessages.text("i18n.pipeline_source_submodule_failed.5c37") + submoduleCode);
        }
    }

    /**
     * 未指定引用时，取远端默认分支（{@code git ls-remote --symref <url> HEAD}）
     *
     * @param cacheRoot  缓存根目录
     * @param repository 仓库配置
     * @param env        环境变量
     * @param log        日志
     * @return 分支名
     * @throws Exception 解析失败
     */
    private String defaultBranch(File cacheRoot, RepositoryModel repository, Map<String, String> env,
                                 PipelineSourceSync.LogSink log) throws Exception {
        GitCommandRunner git = new GitCommandRunner(cacheRoot, env, log);
        String output = git.output("ls-remote", "--symref", GitUrls.covertUrl(repository), "HEAD");
        String branch = null;
        for (String line : StrUtil.splitTrim(output, StrUtil.LF)) {
            if (!StrUtil.startWith(line, "ref: refs/heads/")) {
                continue;
            }
            // 输出形如 "ref: refs/heads/main<TAB>HEAD"，按空白切分拿第二段
            String[] parts = line.trim().split("\\s+");
            if (parts.length >= 2) {
                branch = StrUtil.removePrefix(parts[1], "refs/heads/");
            }
            break;
        }
        if (StrUtil.isEmpty(branch)) {
            throw new IOException("cannot resolve remote default branch: " + GitUrls.sanitize(repository.getGitUrl()));
        }
        log.info(PipelineSourceMessages.text("i18n.pipeline_source_default_branch.d1f4") + branch);
        return branch;
    }

    /**
     * 是否为可复用的 bare 缓存
     */
    private static boolean isCache(File cacheDir) {
        return FileUtil.isDirectory(cacheDir) && FileUtil.exist(FileUtil.file(cacheDir, BARE_HEAD));
    }

    private static String revOf(String refName, boolean tag) {
        return (tag ? "refs/tags/" : "refs/heads/") + refName;
    }

    /**
     * 缓存目录名：仓库 id（可读前缀）+ 引用/深度摘要（防止不同引用共用一份缓存）
     *
     * @param repository 仓库
     * @param tag        是否标签
     * @param refName    引用名
     * @param depth      克隆深度
     * @return 目录名
     */
    static String cacheName(RepositoryModel repository, boolean tag, String refName, Integer depth) {
        String raw = StrUtil.format("{}|{}|{}|{}", repository.getId(), tag ? "tag" : "branch", refName,
            depth == null ? "full" : depth);
        String hash = SecureUtil.sha1(raw).substring(0, 12);
        String prefix = StrUtil.nullToEmpty(repository.getId()).replaceAll("[^A-Za-z0-9_-]", "_");
        if (prefix.length() > 16) {
            prefix = prefix.substring(0, 16);
        }
        return (StrUtil.isEmpty(prefix) ? "repo" : prefix) + "-" + hash + ".git";
    }

    /**
     * 命令环境变量：禁止交互式认证（否则会挂住构建）、跟随仓库超时、ssh 私钥
     *
     * @param repository 仓库配置
     * @return 环境变量
     */
    static Map<String, String> environment(RepositoryModel repository) {
        Map<String, String> env = new HashMap<>(4);
        env.put("GIT_TERMINAL_PROMPT", "0");
        Integer timeout = repository.getTimeout();
        if (timeout != null && timeout > 0) {
            env.put("GIT_HTTP_TIMEOUT", String.valueOf(timeout));
        }
        Integer protocol = repository.getProtocol();
        if (protocol != null && protocol == 1) {
            Object rsaFile = repository.toMap().get("rsaFile");
            if (rsaFile instanceof File && FileUtil.isFile((File) rsaFile)) {
                env.put("GIT_SSH_COMMAND", StrUtil.format(
                    "ssh -i \"{}\" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o BatchMode=yes",
                    ((File) rsaFile).getAbsolutePath()));
            }
        }
        return env;
    }
}
