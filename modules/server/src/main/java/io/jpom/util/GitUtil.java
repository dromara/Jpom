package io.jpom.util;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import io.jpom.system.ConfigBean;
import io.jpom.system.JpomRuntimeException;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * git工具
 * <p>
 * https://developer.aliyun.com/ask/275691
 *
 * @author bwcx_jzy
 * @date 2019/7/15
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
     * @param url                 url
     * @param file                文件
     * @param credentialsProvider 凭证
     * @return git
     * @throws GitAPIException api
     * @throws IOException     删除文件失败
     */
    private static Git reClone(String url, String branchName, File file, CredentialsProvider credentialsProvider, PrintWriter printWriter) throws GitAPIException, IOException {
        if (!FileUtil.clean(file)) {
            FileUtil.del(file.toPath());
            //throw new IOException("del error:" + file.getPath());
        }
        CloneCommand cloneCommand = Git.cloneRepository();
        if (printWriter != null) {
            cloneCommand
                    .setProgressMonitor(new TextProgressMonitor(printWriter));
        }
        if (branchName != null) {
            cloneCommand.setBranch(Constants.R_HEADS + branchName);
            cloneCommand.setBranchesToClone(Collections.singletonList(Constants.R_HEADS + branchName));
        }
        return cloneCommand.setURI(url)
                .setDirectory(file)
                .setCredentialsProvider(credentialsProvider)
                .call();
    }

    private static Git initGit(String url, String branchName, File file, CredentialsProvider credentialsProvider, PrintWriter printWriter) throws IOException, GitAPIException {
        Git git;
        if (FileUtil.file(file, Constants.DOT_GIT).exists()) {
            if (checkRemoteUrl(url, file)) {
                git = Git.open(file);
                //
                PullCommand pull = git.pull();
                if (printWriter != null) {
                    pull.setProgressMonitor(new TextProgressMonitor(printWriter));
                }
                pull.setCredentialsProvider(credentialsProvider).call();
            } else {
                git = reClone(url, branchName, file, credentialsProvider, printWriter);
            }
        } else {
            git = reClone(url, branchName, file, credentialsProvider, printWriter);
        }
        return git;
    }

    /**
     * 获取仓库远程的所有分支
     *
     * @param url                 远程url
     * @param file                仓库clone到本地的文件夹
     * @param credentialsProvider 凭证
     * @return list
     * @throws GitAPIException api
     * @throws IOException     IO
     */
    private static List<String> branchList(String url, File file, CredentialsProvider credentialsProvider, PrintWriter printWriter) throws GitAPIException, IOException {
        try (Git git = initGit(url, null, file, credentialsProvider, printWriter)) {
            //
            List<Ref> list = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call();
            List<String> all = new ArrayList<>(list.size());
            list.forEach(ref -> {
                String name = ref.getName();
                if (name.startsWith(Constants.R_REMOTES + Constants.DEFAULT_REMOTE_NAME)) {
                    all.add(name.substring((Constants.R_REMOTES + Constants.DEFAULT_REMOTE_NAME).length() + 1));
                }
            });
            return all;
        } catch (TransportException t) {
            checkTransportException(t);
            throw t;
        }
    }

    public static List<String> getBranchList(String url, String userName, String userPwd, PrintWriter printWriter) throws GitAPIException, IOException {
        //  生成临时路径
        String tempId = SecureUtil.md5(url);
        File file = ConfigBean.getInstance().getTempPath();
        File gitFile = FileUtil.file(file, "gitTemp", tempId);
        try {
            List<String> list = branchList(url, gitFile, new UsernamePasswordCredentialsProvider(userName, userPwd), printWriter);
            if (list.isEmpty()) {
                throw new JpomRuntimeException("该仓库还没有任何分支");
            }
            return list;
        } catch (org.eclipse.jgit.errors.RepositoryNotFoundException ignored) {
            FileUtil.del(gitFile);
        }
        return new ArrayList<>();
    }

    /**
     * 拉取对应分支最新代码
     *
     * @param url                 远程url
     * @param file                仓库路径
     * @param branchName          分支名
     * @param credentialsProvider 凭证
     * @throws IOException     IO
     * @throws GitAPIException api
     */
    public static void checkoutPull(String url, File file, String branchName, CredentialsProvider credentialsProvider, PrintWriter printWriter) throws IOException, GitAPIException {
        try (Git git = initGit(url, branchName, file, credentialsProvider, printWriter)) {
            // 判断本地是否存在对应分支
            List<Ref> list = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
            boolean createBranch = true;
            for (Ref ref : list) {
                String name = ref.getName();
                if (name.startsWith(Constants.R_HEADS + branchName)) {
                    createBranch = false;
                    break;
                }
            }
            // 切换分支
            if (!StrUtil.equals(git.getRepository().getBranch(), branchName)) {
                printWriter.println(StrUtil.format("start switch branch from {} to {}", git.getRepository().getBranch(), branchName));
                git.checkout().
                        setCreateBranch(createBranch).
                        setName(branchName).
                        setForceRefUpdate(true).
                        setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM).
                        setProgressMonitor(new TextProgressMonitor(printWriter)).
                        call();
            }
            git.pull().setCredentialsProvider(credentialsProvider).call();
        } catch (TransportException t) {
            checkTransportException(t);
            throw t;
        }
    }

    private static void checkTransportException(TransportException ex) {
        String msg = ex.getMessage();
        if (msg.contains(JGitText.get().notAuthorized) || msg.contains(JGitText.get().authenticationNotSupported)) {
            throw new JpomRuntimeException("git账号密码不正常", ex);
        }
    }

    private static AnyObjectId getAnyObjectId(Git git, String branchName) throws GitAPIException {
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
     * @return 描述
     * @throws IOException     IO
     * @throws GitAPIException api
     */
    public static String getLastCommitMsg(File file, String branchName) throws IOException, GitAPIException {
        try (Git git = Git.open(file)) {
            AnyObjectId anyObjectId = getAnyObjectId(git, branchName);
            Objects.requireNonNull(anyObjectId, "没有" + branchName + "分支");
            RevWalk walk = new RevWalk(git.getRepository());
            RevCommit revCommit = walk.parseCommit(anyObjectId);
            String time = new DateTime(revCommit.getCommitTime() * 1000L).toString();
            PersonIdent personIdent = revCommit.getAuthorIdent();
            return StrUtil.format("{} {} {}[{}] {}", branchName, revCommit.getShortMessage(), personIdent.getName(), personIdent.getEmailAddress(), time);
        }
    }
}
