package io.jpom.build;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.io.file.FileCopier;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.JpomApplication;
import io.jpom.model.data.BuildModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.log.BuildHistoryLog;
import io.jpom.service.dblog.DbBuildHistoryLogService;
import io.jpom.system.JpomRuntimeException;
import io.jpom.util.CommandUtil;
import io.jpom.util.GitUtil;
import io.jpom.util.SvnKitUtil;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 构建管理
 *
 * @author bwcx_jzy
 * @date 2019/7/16
 */
public class BuildManage extends BaseBuild implements Runnable {
    /**
     * 缓存构建中
     */
    private static final Map<String, BuildManage> BUILD_MANAGE_MAP = new ConcurrentHashMap<>();

    private final BuildModel buildModel;
    private final File gitFile;
    private Process process;
    private String logId;
    private final String optUserName;
    private final UserModel userModel;

    private BuildManage(final BuildModel buildModel, final UserModel userModel) {
        super(BuildUtil.getLogFile(buildModel.getId(), buildModel.getBuildId()),
                buildModel.getId());
        this.buildModel = buildModel;
        this.gitFile = BuildUtil.getSource(buildModel);
        this.optUserName = UserModel.getOptUserName(userModel);
        this.userModel = userModel;
    }

    /**
     * 取消构建
     *
     * @param id id
     * @return bool
     */
    public static boolean cancel(String id) {
        BuildManage buildManage = BUILD_MANAGE_MAP.get(id);
        if (buildManage == null) {
            return false;
        }
        if (buildManage.process != null) {
            try {
                buildManage.process.destroy();
            } catch (Exception ignored) {
            }
        }
        buildManage.updateStatus(BuildModel.Status.Cancel);
        BUILD_MANAGE_MAP.remove(id);
        return true;
    }

    /**
     * 创建构建
     *
     * @param buildModel 构建项
     * @param userModel  操作人
     * @return this
     */
    public static BuildManage create(BuildModel buildModel, UserModel userModel) {
        if (BUILD_MANAGE_MAP.containsKey(buildModel.getId())) {
            throw new JpomRuntimeException("当前构建还在进行中");
        }
        BuildManage buildManage = new BuildManage(buildModel, userModel);
        BUILD_MANAGE_MAP.put(buildModel.getId(), buildManage);
        //
        ThreadUtil.execute(buildManage);
        return buildManage;
    }

    @Override
    protected boolean updateStatus(BuildModel.Status status) {
        try {
            super.updateStatus(status);
            if (status == BuildModel.Status.Ing) {
                this.insertLog();
            } else {
                DbBuildHistoryLogService dbBuildHistoryLogService = SpringUtil.getBean(DbBuildHistoryLogService.class);
                dbBuildHistoryLogService.updateLog(this.logId, status);
            }
            return true;
        } catch (Exception e) {
            DefaultSystemLog.getLog().error("构建状态变更失败", e);
            return false;
        }
    }


    /**
     * 插入记录
     */
    private void insertLog() {
        this.logId = IdUtil.fastSimpleUUID();
        BuildHistoryLog buildHistoryLog = new BuildHistoryLog();

//        buildHistoryLog.setResultDirFile(buildModel.getResultDirFile());
//        buildHistoryLog.setReleaseMethod(this.buildModel.getReleaseMethod());
//        buildHistoryLog.setReleaseMethodDataId(this.buildModel.getReleaseMethodDataId());
//        buildHistoryLog.setAfterOpt(this.buildModel.getAfterOpt());
//        buildHistoryLog.setReleaseCommand(this.buildModel.getReleaseCommand());
        BeanUtil.copyProperties(this.buildModel, buildHistoryLog);

        buildHistoryLog.setId(this.logId);
        buildHistoryLog.setBuildDataId(buildModel.getId());
        buildHistoryLog.setStatus(BuildModel.Status.Ing.getCode());
        buildHistoryLog.setStartTime(System.currentTimeMillis());
        buildHistoryLog.setBuildNumberId(buildModel.getBuildId());
        buildHistoryLog.setBuildUser(optUserName);

        DbBuildHistoryLogService dbBuildHistoryLogService = SpringUtil.getBean(DbBuildHistoryLogService.class);
        dbBuildHistoryLogService.insert(buildHistoryLog);
    }

    /**
     * 打包构建产物
     */
    private boolean packageFile() throws InterruptedException {
        Thread.sleep(2000);
        File file = FileUtil.file(this.gitFile, buildModel.getResultDirFile());
        if (!file.exists()) {
            this.log(buildModel.getResultDirFile() + "不存在，处理构建产物失败");
            return false;
        }
        File toFile = BuildUtil.getHistoryPackageFile(buildModel.getId(), buildModel.getBuildId(), buildModel.getResultDirFile());
        FileCopier.create(file, toFile)
                .setCopyContentIfDir(true)
                .setOverride(true)
                .setCopyAttributes(true)
                .setCopyFilter(file1 -> !file1.isHidden())
                .copy();
        this.log(StrUtil.format("mv {} {}", buildModel.getResultDirFile(), buildModel.getBuildIdStr()));
        return true;
    }

    @Override
    public void run() {
        try {
            if (!updateStatus(BuildModel.Status.Ing)) {
                this.log("初始化构建记录失败,异常结束");
                return;
            }
            try {
                this.log("start build in file : " + FileUtil.getAbsolutePath(this.gitFile));
                //
                String branchName = buildModel.getBranchName();
                this.log("repository clone pull from " + branchName);
                String msg = "error";
                if (buildModel.getRepoType() == BuildModel.RepoType.Git.getCode()) {
                    // git
                    UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(buildModel.getUserName(), buildModel.getPassword());
                    GitUtil.checkoutPull(buildModel.getGitUrl(), gitFile, branchName, credentialsProvider, this.getPrintWriter());
                    // 记录最后一次提交记录
                    msg = GitUtil.getLastCommitMsg(gitFile, branchName);
                } else if (buildModel.getRepoType() == BuildModel.RepoType.Svn.getCode()) {
                    // svn
                    msg = SvnKitUtil.checkOut(buildModel.getGitUrl(), buildModel.getUserName(), buildModel.getPassword(), gitFile);
                }
                this.log(msg);
            } catch (Exception e) {
                this.log("拉取代码失败", e);
                return;
            }
            String[] commands = CharSequenceUtil.splitToArray(buildModel.getScript(), StrUtil.LF);
            if (commands == null || commands.length <= 0) {
                this.log("没有需要执行的命令");
                this.updateStatus(BuildModel.Status.Error);
                return;
            }
            for (String item : commands) {
                try {
                    boolean s = runCommand(item);
                    if (!s) {
                        this.log("命令执行存在error");
                    }
                } catch (IOException e) {
                    this.log(item + " 执行异常", e);
                    return;
                }
            }
            boolean status = packageFile();
            if (status && buildModel.getReleaseMethod() != BuildModel.ReleaseMethod.No.getCode()) {
                // 发布文件
                new ReleaseManage(this.buildModel, this.userModel, this).start();
            } else {
                //
                updateStatus(BuildModel.Status.Success);
            }
        } catch (Exception e) {
            this.log("构建失败", e);
        } finally {
            BUILD_MANAGE_MAP.remove(buildModel.getId());
        }
    }

    private void log(String title, Throwable throwable) {
        log(title, throwable, BuildModel.Status.Error);
    }

    /**
     * 执行命令
     *
     * @param command 命令
     * @return 是否存在错误
     * @throws IOException IO
     */
    private boolean runCommand(String command) throws IOException {
        this.log(command);
        //
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(this.gitFile);
        List<String> commands = CommandUtil.getCommand();
        commands.add(command);
        processBuilder.command(commands);
        final boolean[] status = new boolean[1];
        process = processBuilder.start();
        //
        InputStream inputStream = process.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, JpomApplication.getCharset());
        BufferedReader results = new BufferedReader(inputStreamReader);
        IoUtil.readLines(results, (LineHandler) line -> {
            log(line);
            status[0] = true;
        });
        //
        InputStream errorInputStream = process.getErrorStream();
        InputStreamReader errorInputStreamReader = new InputStreamReader(errorInputStream, JpomApplication.getCharset());
        BufferedReader errorResults = new BufferedReader(errorInputStreamReader);
        IoUtil.readLines(errorResults, (LineHandler) line -> {
            log(line);
            status[0] = false;
        });
        return status[0];
    }
}
