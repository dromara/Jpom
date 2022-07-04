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
package io.jpom.plugin;

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
import io.jpom.system.JpomRuntimeException;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.*;
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
 * https://developer.aliyun.com/ask/275691
 * <p>
 * https://github.com/centic9/jgit-cookbook
 *
 * @author bwcx_jzy
 * @author Hotstrip
 * add git with ssh key to visit repository
 * @since 2019/7/15
 **/
public class GitUtil {

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
     * 删除重新clone
     *
     * @param parameter 参数
     * @param file      文件
     * @return git
     * @throws GitAPIException api
     * @throws IOException     删除文件失败
     */
    private static Git reClone(Map<String, Object> parameter, String branchName, File file, PrintWriter printWriter) throws GitAPIException, IOException {
        if (!FileUtil.clean(file)) {
            FileUtil.del(file.toPath());
            //throw new IOException("del error:" + file.getPath());
        }
        CloneCommand cloneCommand = Git.cloneRepository();
        if (printWriter != null) {
            cloneCommand.setProgressMonitor(new TextProgressMonitor(printWriter));
        }
        if (branchName != null) {
            cloneCommand.setBranch(Constants.R_HEADS + branchName);
            cloneCommand.setBranchesToClone(Collections.singletonList(Constants.R_HEADS + branchName));
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
        Integer protocol = (Integer) parameter.get("protocol");
        String username = (String) parameter.get("username");
        String password = StrUtil.emptyToDefault((String) parameter.get("password"), StrUtil.EMPTY);
        if (protocol == 0) {
            // http
            UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(username, password);
            transportCommand.setCredentialsProvider(credentialsProvider);
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

    private static Git initGit(Map<String, Object> parameter, String branchName, File file, PrintWriter printWriter) throws IOException, GitAPIException {
        Git git;
        if (FileUtil.file(file, Constants.DOT_GIT).exists()) {
            String url = (String) parameter.get("url");
            if (checkRemoteUrl(url, file)) {
                git = Git.open(file);
                //
                if (branchName != null) {
                    PullCommand pull = git.pull();
                    if (printWriter != null) {
                        pull.setProgressMonitor(new TextProgressMonitor(printWriter));
                    }
                    pull.setRemoteBranchName(branchName);
                    // 更新凭证
                    setCredentials(pull, parameter);
                    pull.call();
                }
            } else {
                git = reClone(parameter, branchName, file, printWriter);
            }
        } else {
            git = reClone(parameter, branchName, file, printWriter);
        }
        return git;
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

//	/**
//	 * load repository branch list by git
//	 *
//	 * @return list
//	 * @throws Exception 异常
//	 */
//	public static List<String> getBranchList(RepositoryModel repositoryModel) throws Exception {
//		Tuple tuple = getBranchAndTagList(repositoryModel);
//
//		List<String> branch = tuple == null ? null : tuple.get(0);
//		if (CollUtil.isEmpty(branch)) {
//			throw new JpomRuntimeException("该仓库还没有任何分支");
//		}
//		return tuple.get(0);
//	}

//	public static Tuple getBranchAndTagListCheck(RepositoryModel repositoryModel) throws Exception {
//		Tuple branchAndTagList = getBranchAndTagList(repositoryModel);
//
//		return branchAndTagList;
//	}

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
            try (Git git = initGit(parameter, branchName, file, printWriter)) {
                // 拉取代码
                PullResult pull = pull(git, parameter, branchName, null, printWriter);
                // 最后一次提交记录
                return getLastCommitMsg(file, branchName);
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
     * @param tagOpt      tag 操作
     * @param printWriter 日志流
     * @return pull result
     * @throws Exception 异常
     */
    private static PullResult pull(Git git, Map<String, Object> parameter, String branchName, TagOpt tagOpt, PrintWriter printWriter) throws Exception {
        // 判断本地是否存在对应分支
        List<Ref> list = git.branchList().call();
        boolean createBranch = true;
        for (Ref ref : list) {
            String name = ref.getName();
            if (name.startsWith(Constants.R_HEADS + branchName)) {
                createBranch = false;
                break;
            }
        }
        // 切换分支
        TextProgressMonitor progressMonitor = new TextProgressMonitor(printWriter);
        if (tagOpt == null && !StrUtil.equals(git.getRepository().getBranch(), branchName)) {
            println(printWriter, "start switch branch from {} to {}", git.getRepository().getBranch(), branchName);
            git.checkout().
                setCreateBranch(createBranch).
                setName(branchName).
                setForceRefUpdate(true).
                setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM).
                setProgressMonitor(progressMonitor).
                call();
        }
        PullCommand pull = git.pull();
        if (tagOpt != null) {
            pull.setTagOpt(tagOpt);
        }
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
     * @param branchName  分支名
     * @param printWriter 日志输出留
     * @param parameter   参数
     * @param file        仓库路径
     * @param tagName     标签名
     * @throws IOException     IO
     * @throws GitAPIException api
     */
    public static String checkoutPullTag(Map<String, Object> parameter, File file, String branchName, String tagName, PrintWriter printWriter) throws Exception {
        String url = (String) parameter.get("url");
        String path = FileUtil.getAbsolutePath(file);
        synchronized (StrUtil.concat(false, url, path).intern()) {
            try (Git git = initGit(parameter, null, file, printWriter)) {
                // 拉取最新代码
                PullResult pull = pull(git, parameter, branchName, TagOpt.FETCH_TAGS, printWriter);
                // 切换到对应的 tag
                git.checkout()
                    .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                    .setForceRefUpdate(true)
                    .setName(tagName)
                    .setForced(true)
                    .call();
                // 获取最后提交信息
                Collection<ReflogEntry> reflogEntries = git.reflog().setRef(Constants.HEAD).call();
                ReflogEntry first = CollUtil.getFirst(reflogEntries);
                if (first != null) {
                    return getLastCommitMsg(file, tagName, first.getNewId());
                }

            } catch (Exception t) {
                checkTransportException(t, file, printWriter);
            }
        }
        return StrUtil.EMPTY;
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
                throw new JpomRuntimeException("git账号密码不正确", ex);
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
    private static ObjectId getAnyObjectId(Git git, String branchName) throws GitAPIException {
        List<Ref> list = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
        for (Ref ref : list) {
            String name = ref.getName();
            if (name.startsWith(Constants.R_HEADS + branchName)) {
                return ref.getObjectId();
            }
        }
        return null;
    }

    /**
     * 获取对应分支的最后一次提交记录
     *
     * @param file       仓库文件夹
     * @param branchName 分支
     * @return String[] 第一个元素为最后一次 hash 值， 第二个元素为描述
     * @throws IOException     IO
     * @throws GitAPIException api
     */
    public static String[] getLastCommitMsg(File file, String branchName) throws IOException, GitAPIException {
        try (Git git = Git.open(file)) {
            ObjectId anyObjectId = getAnyObjectId(git, branchName);
            Objects.requireNonNull(anyObjectId, "没有" + branchName + "分支");
            //System.out.println(anyObjectId.getName());
            String lastCommitMsg = getLastCommitMsg(file, branchName, anyObjectId);
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

//	/**
//	 * git clone with ssh way
//	 *
//	 * @param url         远程仓库地址
//	 * @param file        本地存档的文件地址
//	 * @param branchName  分支名称
//	 * @param rsaFile     私钥文件
//	 * @param rsaPass     私钥密码
//	 * @param printWriter
//	 */
//	public static String gitCloneWithSSH(String url, File file, String branchName,
//										 File rsaFile, String rsaPass, PrintWriter printWriter) throws GitAPIException {
//		// if file exists，delete first
//		if (FileUtil.exist(file)) {
//			FileUtil.del(file);
//		}
//
//		// git clone
//		Git.cloneRepository()
//				.setProgressMonitor(new TextProgressMonitor(printWriter))
//				.setURI(url)
//				.setDirectory(file)
//				.setBranch(branchName)
//				.setTransportConfigCallback(transport -> {
//					SshTransport sshTransport = (SshTransport) transport;
//					sshTransport.setSshSessionFactory(new JschConfigSessionFactory() {
//
//						@Override
//						protected JSch createDefaultJSch(FS fs) throws JSchException {
//							JSch jSch = super.createDefaultJSch(fs);
//							// 添加私钥文件
//							jSch.addIdentity(rsaFile.getPath(), rsaPass);
//							return jSch;
//						}
//					});
//				})
//				.call();
//		return StrUtil.EMPTY;
//	}
}
