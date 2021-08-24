package io.jpom.util;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.StrUtil;
import io.jpom.model.data.RepositoryModel;
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
			cloneCommand.setProgressMonitor(new TextProgressMonitor(printWriter));
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
				if (branchName != null) {
					PullCommand pull = git.pull();
					if (printWriter != null) {
						pull.setProgressMonitor(new TextProgressMonitor(printWriter));
					}
					pull.setRemoteBranchName(branchName);
					pull.setCredentialsProvider(credentialsProvider).call();
				}
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
	 * @param credentialsProvider 凭证
	 * @return Tuple
	 * @throws GitAPIException api
	 */
	public static Tuple getBranchAndTagList(String url, CredentialsProvider credentialsProvider) throws Exception {
		synchronized (url.intern()) {
			try {
				Collection<Ref> call = Git.lsRemoteRepository()
						.setRemote(url)
						.setCredentialsProvider(credentialsProvider)
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
				}).filter(Objects::nonNull).collect(Collectors.toList());

				// list tag
				List<Ref> tagListRef = refMap.get(Constants.R_TAGS);
				List<String> tagList = tagListRef == null ? new ArrayList<>() : tagListRef.stream().map(ref -> {
					String name = ref.getName();
					if (name.startsWith(Constants.R_TAGS)) {
						return name.substring((Constants.R_TAGS).length());
					}
					return null;
				}).filter(Objects::nonNull).collect(Collectors.toList());
				return new Tuple(branchList, tagList);
			} catch (Exception t) {
				checkTransportException(t, null, null);
				return null;
			}
		}
	}

	public static List<String> getBranchList(String url, String userName, String userPwd) throws Exception {
		Tuple tuple = getBranchAndTagList(url, new UsernamePasswordCredentialsProvider(userName, userPwd));
		List<String> branch = tuple == null ? null : tuple.get(0);
		if (CollUtil.isEmpty(branch)) {
			throw new JpomRuntimeException("该仓库还没有任何分支");
		}
		return tuple.get(0);
	}

	/**
	 * 根据仓库获取分支信息
	 *
	 * @param repositoryModel 仓库信息
	 * @return 分支+tag
	 * @throws Exception 异常
	 */
	public static Tuple getBranchAndTagList(RepositoryModel repositoryModel) throws Exception {
		String url = repositoryModel.getGitUrl();
		String userName = repositoryModel.getUserName();
		String userPwd = repositoryModel.getPassword();
		Tuple tuple = getBranchAndTagList(url, new UsernamePasswordCredentialsProvider(userName, userPwd));
		List<String> branch = tuple == null ? null : tuple.get(0);
		if (CollUtil.isEmpty(branch)) {
			throw new JpomRuntimeException("该仓库还没有任何分支");
		}
		return tuple;
	}


	/**
	 * 拉取对应分支最新代码
	 *
	 * @param url                 远程url
	 * @param file                仓库路径
	 * @param branchName          分支名
	 * @param credentialsProvider 凭证
	 * @return 返回最新一次提交信息
	 * @throws IOException     IO
	 * @throws GitAPIException api
	 */
	public static String checkoutPull(String url, File file, String branchName, CredentialsProvider credentialsProvider, PrintWriter printWriter) throws Exception {
		synchronized (url.intern()) {
			try (Git git = initGit(url, branchName, file, credentialsProvider, printWriter)) {
				// 拉取代码
				PullResult pull = pull(git, branchName, credentialsProvider, null, printWriter);
				// 最后一次提交记录
				return getLastCommitMsg(file, branchName);
			} catch (Exception t) {
				checkTransportException(t, file, printWriter);
			}
		}
		return StrUtil.EMPTY;
	}

	/**
	 * 拉取远程最新代码
	 *
	 * @param git                 仓库对象
	 * @param branchName          分支
	 * @param credentialsProvider 认证信息
	 * @param tagOpt              tag 操作
	 * @param printWriter         日志流
	 * @return pull result
	 * @throws Exception 异常
	 */
	private static PullResult pull(Git git, String branchName, CredentialsProvider credentialsProvider, TagOpt tagOpt, PrintWriter printWriter) throws Exception {
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
		if (!StrUtil.equals(git.getRepository().getBranch(), branchName)) {
			println(printWriter, "start switch branch from {} to {}", git.getRepository().getBranch(), branchName);
			git.checkout().
					setCreateBranch(createBranch).
					setName(branchName).
					setForceRefUpdate(true).
					setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM).
					setProgressMonitor(new TextProgressMonitor(printWriter)).
					call();
		}
		PullCommand pull = git.pull();
		if (tagOpt != null) {
			pull.setTagOpt(tagOpt);
		}
		PullResult call = pull.setCredentialsProvider(credentialsProvider).call();
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
	 * @param url                 远程url
	 * @param file                仓库路径
	 * @param tagName             标签名
	 * @param credentialsProvider 凭证
	 * @throws IOException     IO
	 * @throws GitAPIException api
	 */
	public static String checkoutPullTag(String url, File file, String branchName, String tagName, CredentialsProvider credentialsProvider, PrintWriter printWriter) throws Exception {
		synchronized (url.intern()) {
			try (Git git = initGit(url, null, file, credentialsProvider, printWriter)) {
				// 拉取最新代码
				PullResult pull = pull(git, branchName, credentialsProvider, null, printWriter);
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
		if (ex instanceof TransportException) {
			String msg = ex.getMessage();
			if (msg.contains(JGitText.get().notAuthorized) || msg.contains(JGitText.get().authenticationNotSupported)) {
				throw new JpomRuntimeException("git账号密码不正常", ex);
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
	 * @return 描述
	 * @throws IOException     IO
	 * @throws GitAPIException api
	 */
	public static String getLastCommitMsg(File file, String branchName) throws IOException, GitAPIException {
		try (Git git = Git.open(file)) {
			ObjectId anyObjectId = getAnyObjectId(git, branchName);
			Objects.requireNonNull(anyObjectId, "没有" + branchName + "分支");
			return getLastCommitMsg(file, branchName, anyObjectId);
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
	public static String getLastCommitMsg(File file, String desc, ObjectId objectId) throws IOException {
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
