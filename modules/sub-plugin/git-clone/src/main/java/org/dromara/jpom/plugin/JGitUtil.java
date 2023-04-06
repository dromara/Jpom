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

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.comparator.VersionComparator;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.StrUtil;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.Lombok;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.util.FS;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * git工具
 * <p>
 * <a href="https://developer.aliyun.com/ask/275691">https://developer.aliyun.com/ask/275691</a>
 * <p>
 * <a href="https://github.com/centic9/jgit-cookbook">https://github.com/centic9/jgit-cookbook</a>
 *
 * @author bwcx_jzy
 * @author Hotstrip
 * add git with ssh key to visit repository
 * @since 2019/7/15
 **/
public class JGitUtil {

    /**
     * 检查本地的remote是否存在对应的url
     *
     * @param url  要检查的url
     * @param file 本地仓库文件
     * @return true 存在对应url
     * @throws IOException     IO
     * @throws GitAPIException E
     */
    private static boolean checkRemoteUrl(String url, File file) throws IOException, GitAPIException {
        try (Git git = Git.open(file)) {
            RemoteListCommand remoteListCommand = git.remoteList();
            boolean urlTrue = false;
            List<RemoteConfig> list = remoteListCommand.call();
            end:
            for (RemoteConfig remoteConfig : list) {
                for (URIish urIish : remoteConfig.getURIs()) {
                    if (urIish.toString().equals(url)) {
                        urlTrue = true;
                        break end;
                    }
                }
            }
            return urlTrue;
        }
    }

    /**
     * 检查本地的仓库是否存在对应的分支
     *
     * @param branchName 要检查的 branchName
     * @param file       本地仓库文件
     * @return true 存在对应url
     * @throws IOException     IO
     * @throws GitAPIException E
     */
    private static boolean checkBranchName(String branchName, File file) throws IOException, GitAPIException {
        try (Git pullGit = Git.open(file)) {
            // 判断本地是否存在对应分支
            List<Ref> list = pullGit.branchList().call();
            for (Ref ref : list) {
                String name = ref.getName();
                if (StrUtil.equals(name, Constants.R_HEADS + branchName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 删除重新clone
     *
     * @param parameter   参数
     * @param branchName  分支
     * @param tagName     标签
     * @param printWriter 日志流
     * @param file        文件
     * @return git
     * @throws GitAPIException api
     * @throws IOException     删除文件失败
     */
    private static Git reClone(Map<String, Object> parameter, String branchName, String tagName, File file, PrintWriter printWriter) throws GitAPIException, IOException {
        println(printWriter, StrUtil.EMPTY);
        println(printWriter, "Automatically re-clones repositories");
        if (!FileUtil.clean(file)) {
            FileUtil.del(file.toPath());
        }
        CloneCommand cloneCommand = Git.cloneRepository();
        if (printWriter != null) {
            Integer progressRatio = (Integer) parameter.get("reduceProgressRatio");
            cloneCommand.setProgressMonitor(new SmallTextProgressMonitor(printWriter, progressRatio));
        }
        if (branchName != null) {
            cloneCommand.setBranch(Constants.R_HEADS + branchName);
            cloneCommand.setBranchesToClone(Collections.singletonList(Constants.R_HEADS + branchName));
        }
        if (tagName != null) {
            cloneCommand.setBranch(Constants.R_TAGS + tagName);
        }
        String url = (String) parameter.get("url");
        CloneCommand command = cloneCommand.setURI(url)
            .setDirectory(file);
        // 设置凭证
        setCredentials(command, parameter);
        return command.call();
    }

    /**
     * 设置仓库凭证
     *
     * @param transportCommand git 相关操作
     * @param parameter        参数
     */
    private static void setCredentials(TransportCommand<?, ?> transportCommand, Map<String, Object> parameter) {
        // 设置超时时间
        Integer timeout = (Integer) parameter.get("timeout");
        // 设置账号密码
        Integer protocol = (Integer) parameter.get("protocol");
        String username = (String) parameter.get("username");
        String password = StrUtil.emptyToDefault((String) parameter.get("password"), StrUtil.EMPTY);
        if (protocol == 0) {
            // http
            CredentialsProvider credentialsProvider = new SslVerifyUsernamePasswordCredentialsProvider(username, password);
            transportCommand.setCredentialsProvider(credentialsProvider);
            //
            Optional.ofNullable(timeout)
                .map(integer -> integer <= 0 ? null : integer)
                .ifPresent(transportCommand::setTimeout);
        } else if (protocol == 1) {
            // ssh
            //File rsaFile = BuildUtil.getRepositoryRsaFile(repositoryModel);
            File rsaFile = (File) parameter.get("rsaFile");
            transportCommand.setTransportConfigCallback(transport -> {
                SshTransport sshTransport = (SshTransport) transport;
                sshTransport.setSshSessionFactory(new JschConfigSessionFactory() {
                    @Override
                    protected void configure(OpenSshConfig.Host hc, Session session) {
                        session.setConfig("StrictHostKeyChecking", "no");
                        // ssh 需要单独设置超时
                        Optional.ofNullable(timeout)
                            .map(integer -> integer <= 0 ? null : integer)
                            .ifPresent(integer -> {
                                try {
                                    session.setTimeout(integer * 1000);
                                } catch (JSchException e) {
                                    throw Lombok.sneakyThrow(e);
                                }
                            });
                    }

                    @Override
                    protected JSch createDefaultJSch(FS fs) throws JSchException {
                        JSch jSch = super.createDefaultJSch(fs);
                        if (rsaFile == null) {
                            return jSch;
                        }
                        // 添加私钥文件
                        //String rsaPass = repositoryModel.getPassword();
                        if (StrUtil.isEmpty(password)) {
                            jSch.addIdentity(rsaFile.getPath());
                        } else {
                            jSch.addIdentity(rsaFile.getPath(), password);
                        }
                        return jSch;
                    }
                });
            });
        } else {
            throw new IllegalStateException("不支持到协议类型");
        }
    }

    private static Git initGit(Map<String, Object> parameter, String branchName, String tagName, File file, PrintWriter printWriter) {
        String url = (String) parameter.get("url");
        return Optional.of(file).flatMap(file12 -> {
                // 文件信息
                if (FileUtil.file(file12, Constants.DOT_GIT).exists()) {
                    return Optional.of(true);
                }
                return Optional.empty();
            }).flatMap(status -> {
                try {
                    // 远程地址
                    if (checkRemoteUrl(url, file)) {
                        return Optional.of(true);
                    }
                } catch (IOException | GitAPIException e) {
                    throw Lombok.sneakyThrow(e);
                }
                return Optional.empty();
            }).flatMap(aBoolean -> {
                if (StrUtil.isEmpty(tagName)) {
                    // 分支模式，继续验证
                    return Optional.of(true);
                }
                // 标签模式直接中断
                return Optional.empty();
            })
            .flatMap(status -> {
                // 本地分支
                try {
                    // 远程地址
                    if (checkBranchName(branchName, file)) {
                        return Optional.of(true);
                    }
                } catch (IOException | GitAPIException e) {
                    throw Lombok.sneakyThrow(e);
                }
                return Optional.empty();
            }).map(aBoolean -> {
                try {
                    return aBoolean ? Git.open(file) : reClone(parameter, branchName, tagName, file, printWriter);
                } catch (IOException | GitAPIException e) {
                    throw Lombok.sneakyThrow(e);
                }
            }).orElseGet(() -> {
                try {
                    return reClone(parameter, branchName, tagName, file, printWriter);
                } catch (GitAPIException | IOException e) {
                    throw Lombok.sneakyThrow(e);
                }
            });
    }

    /**
     * 获取仓库远程的所有分支
     *
     * @param parameter 参数
     * @return Tuple
     * @throws GitAPIException api
     */
    public static Tuple getBranchAndTagList(Map<String, Object> parameter) throws Exception {
        String url = (String) parameter.get("url");

        try {
            LsRemoteCommand lsRemoteCommand = Git.lsRemoteRepository()
                .setRemote(url);
            // 更新凭证
            setCredentials(lsRemoteCommand, parameter);
            //
            Collection<Ref> call = lsRemoteCommand
                .setHeads(true)
                .setTags(true)
                .call();
            if (CollUtil.isEmpty(call)) {
                return null;
            }
            Map<String, List<Ref>> refMap = CollStreamUtil.groupByKey(call, ref -> {
                String name = ref.getName();
                if (name.startsWith(Constants.R_TAGS)) {
                    return Constants.R_TAGS;
                } else if (name.startsWith(Constants.R_HEADS)) {
                    return Constants.R_HEADS;
                }
                return null;
            });

            // branch list
            List<Ref> branchListRef = refMap.get(Constants.R_HEADS);
            if (branchListRef == null) {
                return null;
            }
            List<String> branchList = branchListRef.stream().map(ref -> {
                String name = ref.getName();
                if (name.startsWith(Constants.R_HEADS)) {
                    return name.substring((Constants.R_HEADS).length());
                }
                return null;
            }).filter(Objects::nonNull).sorted((o1, o2) -> VersionComparator.INSTANCE.compare(o2, o1)).collect(Collectors.toList());

            // list tag
            List<Ref> tagListRef = refMap.get(Constants.R_TAGS);
            List<String> tagList = tagListRef == null ? new ArrayList<>() : tagListRef.stream().map(ref -> {
                String name = ref.getName();
                if (name.startsWith(Constants.R_TAGS)) {
                    return name.substring((Constants.R_TAGS).length());
                }
                return null;
            }).filter(Objects::nonNull).sorted((o1, o2) -> VersionComparator.INSTANCE.compare(o2, o1)).collect(Collectors.toList());
            return new Tuple(branchList, tagList);
        } catch (Exception t) {
            checkTransportException(t, null, null);
            return null;
        }
    }

    /**
     * 拉取对应分支最新代码
     *
     * @param parameter  参数
     * @param file       仓库路径
     * @param branchName 分支名
     * @return 返回最新一次提交信息
     * @throws IOException     IO
     * @throws GitAPIException api
     */
    public static String[] checkoutPull(Map<String, Object> parameter, File file, String branchName, PrintWriter printWriter) throws Exception {
        String url = (String) parameter.get("url");
        String path = FileUtil.getAbsolutePath(file);
        synchronized (StrUtil.concat(false, url, path).intern()) {
            try (Git git = initGit(parameter, branchName, null, file, printWriter)) {
                // 拉取代码
                PullResult pull = pull(git, parameter, branchName, printWriter);
                // 最后一次提交记录
                return getLastCommitMsg(file, false, branchName);
            } catch (Exception t) {
                checkTransportException(t, file, printWriter);
            }
        }
        return new String[]{StrUtil.EMPTY, StrUtil.EMPTY};
    }

    /**
     * 拉取远程最新代码
     *
     * @param git         仓库对象
     * @param branchName  分支
     * @param parameter   参数
     * @param printWriter 日志流
     * @return pull result
     * @throws Exception 异常
     */
    private static PullResult pull(Git git, Map<String, Object> parameter, String branchName, PrintWriter printWriter) throws Exception {
        Integer progressRatio = (Integer) parameter.get("reduceProgressRatio");
        SmallTextProgressMonitor progressMonitor = new SmallTextProgressMonitor(printWriter, progressRatio);
        // 放弃本地修改
        git.checkout().setName(branchName).setForced(true).setProgressMonitor(progressMonitor).call();

        PullCommand pull = git.pull();
        //
        setCredentials(pull, parameter);
        //
        PullResult call = pull
            .setRemoteBranchName(branchName)
            .setProgressMonitor(progressMonitor)
            .call();
        // 输出拉取结果
        if (call != null) {
            String fetchedFrom = call.getFetchedFrom();
            FetchResult fetchResult = call.getFetchResult();
            MergeResult mergeResult = call.getMergeResult();
            RebaseResult rebaseResult = call.getRebaseResult();
            if (mergeResult != null) {
                println(printWriter, "mergeResult {}", mergeResult);
            }
            if (rebaseResult != null) {
                println(printWriter, "rebaseResult {}", rebaseResult);
            }
            if (fetchedFrom != null) {
                println(printWriter, "fetchedFrom {}", fetchedFrom);
            }
            //			if (fetchResult != null) {
            //				println(printWriter, "fetchResult {}", fetchResult);
            //			}
        }
        return call;
    }

    /**
     * 拉取对应分支最新代码
     *
     * @param printWriter 日志输出流
     * @param parameter   参数
     * @param file        仓库路径
     * @param tagName     标签名
     * @throws IOException     IO
     * @throws GitAPIException api
     */
    public static String[] checkoutPullTag(Map<String, Object> parameter, File file, String tagName, PrintWriter printWriter) throws Exception {
        String url = (String) parameter.get("url");
        String path = FileUtil.getAbsolutePath(file);
        synchronized (StrUtil.concat(false, url, path).intern()) {
            try (Git git = initGit(parameter, null, tagName, file, printWriter)) {
                // 获取最后提交信息
                return getLastCommitMsg(file, true, tagName);
            } catch (Exception t) {
                checkTransportException(t, file, printWriter);
            }
        }
        return new String[]{StrUtil.EMPTY, StrUtil.EMPTY};
    }

    /**
     * 检查异常信息
     *
     * @param ex          异常信息
     * @param gitFile     仓库地址
     * @param printWriter 日志流
     * @throws TransportException 非账号密码异常
     */
    private static void checkTransportException(Exception ex, File gitFile, PrintWriter printWriter) throws Exception {
        println(printWriter, "");
        if (ex instanceof TransportException) {
            String msg = ex.getMessage();
            if (msg.contains(JGitText.get().notAuthorized) || msg.contains(JGitText.get().authenticationNotSupported)) {
                throw new IllegalArgumentException("git账号密码不正确," + msg, ex);
            }
            throw ex;
        } else if (ex instanceof NoHeadException) {
            println(printWriter, "拉取代码异常,已经主动清理本地仓库缓存内容,请手动重试。" + ex.getMessage());
            if (gitFile == null) {
                throw ex;
            } else {
                FileUtil.del(gitFile);
            }
            throw ex;
        } else if (ex instanceof CheckoutConflictException) {
            println(printWriter, "拉取代码发生冲突,可以尝试清除构建或者解决仓库里面的冲突后重新操作。：" + ex.getMessage());
            throw ex;
        } else {
            println(printWriter, "拉取代码发生未知异常建议清除构建重新操作：" + ex.getMessage());
            throw ex;
        }
    }

    /**
     * 输出日志信息
     *
     * @param printWriter 日志流
     * @param template    日志内容模版
     * @param params      参数
     */
    private static void println(PrintWriter printWriter, CharSequence template, Object... params) {
        if (printWriter == null) {
            return;
        }
        printWriter.println(StrUtil.format(template, params));
        // 需要 flush 让输出立即生效
        IoUtil.flush(printWriter);
    }

    /**
     * 解析仓库指定分支最新提交
     *
     * @param git        仓库
     * @param branchName 分支
     * @return objectID
     * @throws GitAPIException 异常
     */
    private static ObjectId getBranchAnyObjectId(Git git, String branchName) throws GitAPIException {
        List<Ref> list = git.branchList().call();
        for (Ref ref : list) {
            String name = ref.getName();
            if (name.startsWith(Constants.R_HEADS + branchName)) {
                return ref.getObjectId();
            }
        }
        return null;
    }

    /**
     * 解析仓库指定分支最新提交
     *
     * @param git     仓库
     * @param tagName 标签
     * @return objectID
     * @throws GitAPIException 异常
     */
    private static ObjectId getTagAnyObjectId(Git git, String tagName) throws GitAPIException {
        List<Ref> list = git.tagList().call();
        for (Ref ref : list) {
            String name = ref.getName();
            if (name.startsWith(Constants.R_TAGS + tagName)) {
                return ref.getObjectId();
            }
        }
        return null;
    }

    /**
     * 获取对应分支的最后一次提交记录
     *
     * @param file    仓库文件夹
     * @param refName 名称
     * @return String[] 第一个元素为最后一次 hash 值， 第二个元素为描述
     * @throws IOException     IO
     * @throws GitAPIException api
     */
    public static String[] getLastCommitMsg(File file, boolean tag, String refName) throws IOException, GitAPIException {
        try (Git git = Git.open(file)) {
            ObjectId anyObjectId = tag ? getTagAnyObjectId(git, refName) : getBranchAnyObjectId(git, refName);
            Objects.requireNonNull(anyObjectId, "没有" + refName + "分支/标签");
            //System.out.println(anyObjectId.getName());
            String lastCommitMsg = getLastCommitMsg(file, refName, anyObjectId);
            return new String[]{anyObjectId.getName(), lastCommitMsg};
        }
    }

    /**
     * 解析提交信息
     *
     * @param file     仓库文件夹
     * @param desc     描述
     * @param objectId 提交信息
     * @return 描述
     * @throws IOException IO
     */
    private static String getLastCommitMsg(File file, String desc, ObjectId objectId) throws IOException {
        try (Git git = Git.open(file)) {
            RevWalk walk = new RevWalk(git.getRepository());
            RevCommit revCommit = walk.parseCommit(objectId);
            String time = new DateTime(revCommit.getCommitTime() * 1000L).toString();
            PersonIdent personIdent = revCommit.getAuthorIdent();
            return StrUtil.format("{} {} {}[{}] {} {}",
                desc,
                revCommit.getShortMessage(),
                personIdent.getName(),
                personIdent.getEmailAddress(),
                time,
                revCommit.getParentCount());
        }
    }
}
