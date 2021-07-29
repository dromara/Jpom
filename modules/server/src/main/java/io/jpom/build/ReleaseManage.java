package io.jpom.build;

import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.Sftp;
import cn.hutool.http.HttpStatus;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.jcraft.jsch.Session;
import io.jpom.model.AfterOpt;
import io.jpom.model.BaseEnum;
import io.jpom.model.data.BuildModel;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.SshModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.log.BuildHistoryLog;
import io.jpom.outgiving.OutGivingRun;
import io.jpom.service.node.NodeService;
import io.jpom.service.node.ssh.SshService;
import io.jpom.system.JpomRuntimeException;

import java.io.File;
import java.util.Objects;

/**
 * 发布管理
 *
 * @author bwcx_jzy
 * @date 2019/7/19
 */
public class ReleaseManage extends BaseBuild {

	private final UserModel userModel;
	private final int buildId;
	private final BaseBuildModule baseBuildModule;
	private File resultFile;
	private BaseBuild baseBuild;

	ReleaseManage(BuildModel buildModel, UserModel userModel, BaseBuild baseBuild) {
		super(BuildUtil.getLogFile(buildModel.getId(), buildModel.getBuildId()),
				buildModel.getId());
		this.baseBuildModule = buildModel;
		this.buildId = buildModel.getBuildId();
		this.userModel = userModel;
		this.baseBuild = baseBuild;
		this.init();
	}

	/**
	 * 重新发布
	 *
	 * @param buildHistoryLog 构建历史
	 * @param userModel       用户
	 */
	public ReleaseManage(BuildHistoryLog buildHistoryLog, UserModel userModel) {
		super(BuildUtil.getLogFile(buildHistoryLog.getBuildDataId(), buildHistoryLog.getBuildNumberId()),
				buildHistoryLog.getBuildDataId());
		this.baseBuildModule = buildHistoryLog;
		this.buildId = buildHistoryLog.getBuildNumberId();
		this.userModel = userModel;
		this.init();
	}


	@Override
	public boolean updateStatus(BuildModel.Status status) {
		if (baseBuild == null) {
			return super.updateStatus(status);
		} else {
			return baseBuild.updateStatus(status);
		}
	}

	private void init() {
		this.resultFile = BuildUtil.getHistoryPackageFile(this.buildModelId, this.buildId, this.baseBuildModule.getResultDirFile());
	}

	/**
	 * 不修改为发布中状态
	 */
	public void start2() {
		this.log("start release：" + FileUtil.readableFileSize(FileUtil.size(this.resultFile)));
		if (!this.resultFile.exists()) {
			this.log("不存在构建产物");
			updateStatus(BuildModel.Status.PubError);
			return;
		}
		long time = SystemClock.now();
		this.log("release method:" + BaseEnum.getDescByCode(BuildModel.ReleaseMethod.class, this.baseBuildModule.getReleaseMethod()));
		try {
			if (this.baseBuildModule.getReleaseMethod() == BuildModel.ReleaseMethod.Outgiving.getCode()) {
				//
				this.doOutGiving();
			} else if (this.baseBuildModule.getReleaseMethod() == BuildModel.ReleaseMethod.Project.getCode()) {
				AfterOpt afterOpt = BaseEnum.getEnum(AfterOpt.class, this.baseBuildModule.getAfterOpt());
				if (afterOpt == null) {
					afterOpt = AfterOpt.No;
				}
				this.doProject(afterOpt, this.baseBuildModule.isClearOld());
			} else if (this.baseBuildModule.getReleaseMethod() == BuildModel.ReleaseMethod.Ssh.getCode()) {
				this.doSsh();
			} else {
				this.log(" 没有实现的发布分发");
			}
		} catch (Exception e) {
			this.pubLog("发布异常", e);
			return;
		}
		this.log("release complete : " + DateUtil.formatBetween(SystemClock.now() - time, BetweenFormatter.Level.MILLISECOND));
		updateStatus(BuildModel.Status.PubSuccess);
	}

	/**
	 * 修改为发布中状态
	 */
	public void start() {
		updateStatus(BuildModel.Status.PubIng);
		this.start2();
	}

	private void doSsh() {
		String releaseMethodDataId = this.baseBuildModule.getReleaseMethodDataId();
		SshService sshService = SpringUtil.getBean(SshService.class);
		SshModel item = sshService.getItem(releaseMethodDataId);
		if (item == null) {
			this.log("没有找到对应的ssh项：" + releaseMethodDataId);
			return;
		}
		Session session = SshService.getSession(item);
		try (Sftp sftp = new Sftp(session, item.getCharsetT())) {
			if (this.baseBuildModule.isClearOld() && StrUtil.isNotEmpty(this.baseBuildModule.getReleasePath())) {
				try {
					sftp.delDir(this.baseBuildModule.getReleasePath());
				} catch (Exception e) {
					this.pubLog("清除构建产物失败", e);
				}
			}
			String prefix = "";
			if (!StrUtil.startWith(this.baseBuildModule.getReleasePath(), StrUtil.SLASH)) {
				prefix = sftp.pwd();
			}
			String normalizePath = FileUtil.normalize(prefix + "/" + this.baseBuildModule.getReleasePath());
			sftp.syncUpload(this.resultFile, normalizePath);
//            if (this.resultFile.isFile()) {
//                // 文件
//                String normalizePath = FileUtil.normalize(prefix + "/" + this.baseBuildModule.getReleasePath());
//                try {
//                    sftp.mkDirs(normalizePath);
//                } catch (Exception e) {
//                    this.pubLog(" 切换目录失败：" + normalizePath, e);
//                }
//                sftp.cd(normalizePath);
//                sftp.put(this.resultFile.getAbsolutePath(), this.resultFile.getName());
//            } else if (this.resultFile.isDirectory()) {
//                // 文件夹
//                List<File> files = FileUtil.loopFiles(this.resultFile, pathname -> !pathname.isHidden());
//                String absolutePath = FileUtil.getAbsolutePath(this.resultFile);
//                //
//                for (File file : files) {
//                    String itemAbsPath = FileUtil.getAbsolutePath(file);
//                    String remoteItemAbsPath = StrUtil.removePrefix(itemAbsPath, absolutePath);
//                    remoteItemAbsPath = FileUtil.normalize(prefix + "/" + this.baseBuildModule.getReleasePath() + "/" + remoteItemAbsPath);
//                    String parent = StrUtil.subBefore(remoteItemAbsPath, StrUtil.SLASH, true);
//                    try {
//                        sftp.mkDirs(parent);
//                    } catch (Exception e) {
//                        this.pubLog(" 切换目录失败：" + parent, e);
//                    }
//                    sftp.cd(parent);
//                    sftp.put(itemAbsPath, file.getName());
//                }
//            }
		} catch (Exception e) {
			this.pubLog("执行ssh发布异常", e);
		}
		this.log("");
		// 执行命令
		String[] commands = StrUtil.splitToArray(this.baseBuildModule.getReleaseCommand(), StrUtil.LF);
		if (commands == null || commands.length <= 0) {
			this.log("没有需要执行的ssh命令");
			return;
		}
		this.log(DateUtil.now() + " start exec");
		try {
			String s = sshService.exec(item, commands);
			this.log(s);
		} catch (Exception e) {
			this.pubLog("执行异常", e);
		}
	}

	/**
	 * 发布项目
	 *
	 * @param afterOpt 后续操作
	 */
	private void doProject(AfterOpt afterOpt, boolean clearOld) {
		String releaseMethodDataId = this.baseBuildModule.getReleaseMethodDataId();
		String[] strings = StrUtil.splitToArray(releaseMethodDataId, ":");
		if (strings == null || strings.length != 2) {
			throw new JpomRuntimeException(releaseMethodDataId + " error");
		}
		NodeService nodeService = SpringUtil.getBean(NodeService.class);
		NodeModel nodeModel = nodeService.getItem(strings[0]);
		Objects.requireNonNull(nodeModel, "节点不存在");

		File zipFile = BuildUtil.isDirPackage(this.resultFile);
		boolean unZip = true;
		if (zipFile == null) {
			zipFile = this.resultFile;
			unZip = false;
		}
		JsonMessage<String> jsonMessage = OutGivingRun.fileUpload(zipFile,
				strings[1],
				unZip,
				afterOpt,
				nodeModel, this.userModel, clearOld);
		if (jsonMessage.getCode() == HttpStatus.HTTP_OK) {
			this.log("发布项目包成功：" + jsonMessage.toString());
		} else {
			throw new JpomRuntimeException("发布项目包失败：" + jsonMessage.toString());
		}
	}

	/**
	 * 分发包
	 */
	private void doOutGiving() {
		String releaseMethodDataId = this.baseBuildModule.getReleaseMethodDataId();
		File zipFile = BuildUtil.isDirPackage(this.resultFile);
		boolean unZip = true;
		if (zipFile == null) {
			zipFile = this.resultFile;
			unZip = false;
		}
		OutGivingRun.startRun(releaseMethodDataId, zipFile, userModel, unZip);
		this.log("开始执行分发包啦,请到分发中查看当前状态");
	}


	/**
	 * 发布异常日志
	 *
	 * @param title     描述
	 * @param throwable 异常
	 */
	private void pubLog(String title, Throwable throwable) {
		log(title, throwable, BuildModel.Status.PubError);
	}
}
