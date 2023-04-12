/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.dromara.jpom.plugin;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.comparator.VersionComparator;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.util.CommandUtil;
import org.eclipse.jgit.lib.Constants;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.*;

/**
 * @author Hong
 * @since 2023/4/10
 */
@Slf4j
public class SystemGitProcess extends AbstractGitProcess {

    protected SystemGitProcess(IWorkspaceEnvPlugin workspaceEnvPlugin, Map<String, Object> parameter) {
        super(workspaceEnvPlugin, parameter);
        PrintWriter printWriter = (PrintWriter) parameter.get("logWriter");
        if (printWriter != null) {
            printWriter.println();
            printWriter.println("use system git");
            printWriter.flush();
        }
    }
//    ssh-agent bash -c 'ssh-add <path-to-private-key>; git clone git@<host>:<username>/<repo-name>.git'
    // ssh-agent bash -c 'ssh-add /path/to/private_key && ssh -o StrictHostKeyChecking=yes git@github.com && git clone git@github.com:user/repo.git'

    /**
     * 获取仓库地址 需要拼接账号密码
     *
     * @return url
     * @throws MalformedURLException url 错误
     */
    private String getCovertUrl() throws MalformedURLException {
        String url = (String) parameter.get("url");
        String username = (String) parameter.getOrDefault("username", "");
        String password = (String) parameter.getOrDefault("password", "");
        int protocol = (int) parameter.getOrDefault("protocol", 0);
        if (protocol == 0) {
            if (StrUtil.contains(url, "@")) {
                // 已经配置
                return url;
            }
            username = URLUtil.encode(username);
            password = URLUtil.encode(password);
            String userInfo = username + ":" + password;
            URL url1 = new URL(null, url, new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL u) throws IOException {
                    return null;
                }

                @Override
                protected void setURL(URL u, String protocol, String host, int port, String authority, String userInfo2, String path, String query, String ref) {
                    super.setURL(u, protocol, host, port, StrUtil.format("{}@{}", userInfo, authority), userInfo, path, query, ref);
                }
            });
            return url1.toString();
        }
        // ssh 原样返回
        return url;
    }

    private String warpSsh(String command) {
        int protocol = (int) parameter.getOrDefault("protocol", 0);
        if (protocol == 0) {
            return command;
        } else if (protocol == 1) {
            // TODO 需要实现本地 git ssh 指定证书拉取
            File rsaFile = (File) parameter.get("rsaFile");
            if (FileUtil.isFile(rsaFile)) {
                throw new IllegalStateException("暂时不支持本地 git 指定证书拉取代码");
            }
            // 默认的方式去执行
            return command;
        } else {
            throw new IllegalArgumentException("不支持的 protocol" + protocol);
        }
    }

    @Override
    public Tuple branchAndTagList() throws Exception {
        String command = StrUtil.format("git ls-remote {}", this.getCovertUrl());
        command = this.warpSsh(command);
        String result = CommandUtil.execSystemCommand(command);
        List<String> branchRemote = new ArrayList<>();
        List<String> tagRemote = new ArrayList<>();
        List<String> list = StrUtil.splitTrim(result, StrUtil.LF);
        for (String branch : list) {
            List<String> list1 = StrUtil.splitTrim(branch, StrUtil.TAB);
            String last = CollUtil.getLast(list1);
            if (StrUtil.startWith(last, Constants.R_HEADS)) {
                branchRemote.add(StrUtil.removePrefix(last, Constants.R_HEADS));
            } else if (StrUtil.startWith(last, Constants.R_TAGS)) {
                tagRemote.add(StrUtil.removePrefix(last, Constants.R_TAGS));
            }
        }
        branchRemote.sort((o1, o2) -> VersionComparator.INSTANCE.compare(o2, o1));
        tagRemote.sort((o1, o2) -> VersionComparator.INSTANCE.compare(o2, o1));
        return new Tuple(branchRemote, tagRemote);
    }

    @Override
    public String[] pull() throws Exception {
        String branchName = (String) parameter.get("branchName");
        Assert.hasText(branchName, "没有 branch name");
        return pull(parameter, branchName);
    }

    @Override
    public String[] pullByTag() throws Exception {
        String tagName = (String) parameter.get("tagName");
        Assert.hasText(tagName, "没有 tag name");
        return pull(parameter, tagName);
    }

    private String[] pull(Map<String, Object> map, String branchOrTag) throws IOException {
        PrintWriter printWriter = (PrintWriter) parameter.get("logWriter");
        boolean needClone = this.needClone();
        if (needClone) {
            // clone
            this.reClone(printWriter);
        }
        File saveFile = getSaveFile();
        {
            // 更新
            CommandUtil.exec(saveFile, null, line -> {
                printWriter.println(line);
                printWriter.flush();
            }, "git", "pull");
        }
        // 获取提交日志
        String[] command = {"git", "log", "-1", branchOrTag};
        String[] commitId = new String[1];

        CommandUtil.exec(saveFile, null, line -> {
            printWriter.println(line);
            printWriter.flush();
            if (StrUtil.isEmpty(commitId[0]) && StrUtil.startWithIgnoreCase(line, "commit")) {
                List<String> list = StrUtil.splitTrim(line, StrUtil.SPACE);
                commitId[0] = CollUtil.get(list, 1);
            }
        }, command);
        return new String[]{commitId[0], StrUtil.EMPTY};
    }

    private void reClone(PrintWriter printWriter) throws IOException {
        printWriter.println("Automatically re-clones repositories");
        // 先删除本地目录
        File savePath = getSaveFile();
        if (!FileUtil.clean(savePath)) {
            FileUtil.del(savePath.toPath());
        }
        //
        String branchName = getBranchName();
        String depthStr = Optional.ofNullable((Integer) parameter.get("depth"))
            .map(integer -> {
                if (integer > 0) {
                    return integer;
                }
                return null;
            })
            .map(integer -> "--depth=" + integer)
            .orElse(StrUtil.EMPTY);
        Map<String, String> env = new HashMap<>(4);
        Optional.ofNullable((Integer) parameter.get("timeout"))
            .map(integer -> {
                if (integer > 0) {
                    return integer;
                }
                return null;
            }).ifPresent(integer -> env.put("GIT_HTTP_TIMEOUT", String.valueOf(integer)));
        //
        String[] command = new String[]{"git", "clone", depthStr, "-b", branchName, this.getCovertUrl(), savePath.getAbsolutePath()};
        FileUtil.mkdir(savePath);
        CommandUtil.exec(savePath, env, line -> {
            printWriter.println(line);
            printWriter.flush();
        }, command);
    }

    /**
     * 是否存在GIT仓库
     */
    private boolean needClone() throws MalformedURLException {
        File savePath = getSaveFile();
        File file = FileUtil.file(savePath, Constants.DOT_GIT);
        if (!FileUtil.exist(file)) {
            return true;
        }
        // 判断远程
        String url = (String) parameter.get("url");
        String checkRemote = CommandUtil.execSystemCommand("git remote -v", savePath);
        if (!StrUtil.containsAny(checkRemote, url, this.getCovertUrl())) {
            return true;
        }
        String branchName = getBranchName();
        if (StrUtil.isNotEmpty(branchName)) {
            String checkBranch = CommandUtil.execSystemCommand("git rev-parse --abbrev-ref HEAD", savePath);
            checkBranch = StrUtil.trim(checkBranch);
            return !StrUtil.equals(checkBranch, branchName);
        }
        // tag 模式
        return true;
    }
}
