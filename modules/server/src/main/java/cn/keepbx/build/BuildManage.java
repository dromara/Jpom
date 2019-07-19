package cn.keepbx.build;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.*;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.http.HttpStatus;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.JpomApplication;
import cn.keepbx.jpom.model.BaseEnum;
import cn.keepbx.jpom.model.data.*;
import cn.keepbx.jpom.model.log.BuildHistoryLog;
import cn.keepbx.jpom.model.log.MonitorNotifyLog;
import cn.keepbx.jpom.service.build.BuildService;
import cn.keepbx.jpom.service.node.NodeService;
import cn.keepbx.jpom.system.ConfigBean;
import cn.keepbx.jpom.system.JpomRuntimeException;
import cn.keepbx.outgiving.OutGivingRun;
import cn.keepbx.util.GitUtil;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 构建管理
 *
 * @author bwcx_jzy
 * @date 2019/7/16
 */
public class BuildManage implements Runnable {

    private static final Map<String, BuildManage> BUILD_MANAGE_MAP = new ConcurrentHashMap<>();

    private static final List<String> COMMAND = new ArrayList<>();

    static {
        if (SystemUtil.getOsInfo().isLinux()) {
            //执行linux系统命令
            COMMAND.add("/bin/sh");
            COMMAND.add("-c");
        } else if (SystemUtil.getOsInfo().isMac()) {
            COMMAND.add("/bin/sh");
            COMMAND.add("-c");
        } else {
            COMMAND.add("cmd");
            COMMAND.add("/c");
        }
    }

    private BuildModel buildModel;
    private File gitFile;
    private File logFile;
    private Process process;
    private String logId;
    private String optUserName;
    private UserModel userModel;

    private BuildManage(final BuildModel buildModel, final UserModel userModel) {
        this.buildModel = buildModel;
        this.gitFile = FileUtil.file(getBuildDataFile(buildModel.getId()), "source");
        this.logFile = getLogFile(buildModel, buildModel.getBuildId());
        this.optUserName = UserModel.getOptUserName(userModel);
        this.userModel = userModel;
    }

    public static File getBuildDataFile(String id) {
        return FileUtil.file(ConfigBean.getInstance().getDataPath(), "build", id);
    }

    /**
     * 获取构建产物存放路径
     *
     * @param buildModel 构建实体
     * @param buildId    id
     * @param resultFile 结果目录
     * @return file
     */
    public static File getHistoryPackageFile(BuildModel buildModel, int buildId, String resultFile) {
        return FileUtil.file(getBuildDataFile(buildModel.getId()),
                "history",
                BuildModel.getBuildIdStr(buildId),
                resultFile);
    }

    /**
     * 如果为文件夹自动打包为zip ,反之返回null
     *
     * @param file file
     * @return 压缩包文件
     */
    public static File isDirPackage(File file) {
        if (file.isFile()) {
            return null;
        }
        File zipFile = FileUtil.file(file.getParentFile(), FileUtil.mainName(file) + ".zip");
        if (!zipFile.exists()) {
            // 不存在则打包
            ZipUtil.zip(file);
        }
        return zipFile;
    }

    /**
     * 获取日志记录文件
     *
     * @param buildModel 对象
     * @param buildId    构建编号
     * @return file
     */
    public static File getLogFile(BuildModel buildModel, int buildId) {
        return FileUtil.file(ConfigBean.getInstance().getDataPath(),
                "build",
                buildModel.getId(),
                "history",
                BuildModel.getBuildIdStr(buildId),
                "info.log");
    }

    /**
     * 取消构建
     *
     * @param id id
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

    private boolean updateStatus(BuildModel.Status status) {
        try {
            BuildService buildService = SpringUtil.getBean(BuildService.class);
            BuildModel item = buildService.getItem(buildModel.getId());
            item.setStatus(status.getCode());
            buildService.updateItem(item);
            if (status == BuildModel.Status.Ing) {
                this.insertLog();
            } else {
                this.updateLog(status, status != BuildModel.Status.PubIng);
            }
            return true;
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("构建状态变更失败", e);
            return false;
        }
    }

    private void updateLog(BuildModel.Status status, boolean end) {
        if (this.logId == null) {
            return;
        }
        Db db = Db.use();
        db.setWrapper((Character) null);
        try {
            Entity entity = new Entity(BuildHistoryLog.TABLE_NAME);
            entity.set("status", status.getCode());
            if (end) {
                // 结束
                entity.set("endTime", System.currentTimeMillis());
            }
            //
            Entity where = new Entity(MonitorNotifyLog.TABLE_NAME);
            where.set("id", this.logId);
            db.update(entity, where);
        } catch (SQLException e) {
            DefaultSystemLog.ERROR().error("db error", e);
        }
    }

    /**
     * 插入记录
     */
    private void insertLog() {
        this.logId = IdUtil.fastSimpleUUID();
        BuildHistoryLog buildHistoryLog = new BuildHistoryLog();
        buildHistoryLog.setBuildDataId(buildModel.getId());
        buildHistoryLog.setResultDirFile(buildModel.getResultDirFile());
        buildHistoryLog.setId(this.logId);
        buildHistoryLog.setStatus(BuildModel.Status.Ing.getCode());
        buildHistoryLog.setStartTime(System.currentTimeMillis());
        buildHistoryLog.setBuildNumberId(buildModel.getBuildId());
        buildHistoryLog.setBuildUser(optUserName);

        Db db = Db.use();
        db.setWrapper((Character) null);
        try {
            Entity entity = new Entity(BuildHistoryLog.TABLE_NAME);
            entity.parseBean(buildHistoryLog);
            db.insert(entity);
        } catch (SQLException e) {
            DefaultSystemLog.ERROR().error("db error", e);
        }
    }

    /**
     * 打包构建产物
     */
    private boolean packageFile() {
        File file = FileUtil.file(this.gitFile, buildModel.getResultDirFile());
        if (!file.exists()) {
            this.log(buildModel.getResultDirFile() + "不存在，处理构建产物失败");
            return false;
        }
        File toFile = getHistoryPackageFile(buildModel, buildModel.getBuildId(), buildModel.getResultDirFile());
        FileUtil.copyContent(file, toFile, true);
        this.log(StrUtil.format("mv {} {}", buildModel.getResultDirFile(), buildModel.getBuildIdStr()));
        return true;
    }

    @Override
    public void run() {
        try {
            if (!updateStatus(BuildModel.Status.Ing)) {
                return;
            }
            try {
                GitUtil.checkoutPull(buildModel.getGitUrl(), gitFile, buildModel.getBranchName(), new UsernamePasswordCredentialsProvider(buildModel.getUserName(), buildModel.getPassword()));
                // 记录最后一次提交记录
                String msg = GitUtil.getLastCommitMsg(gitFile, buildModel.getBranchName());
                this.log(msg);
            } catch (Exception e) {
                this.log("拉取代码失败", e);
                return;
            }
            String[] commands = StrUtil.split(buildModel.getScript(), StrUtil.LF);
            if (commands == null || commands.length <= 0) {
                this.log("没有需要执行的命令");
                return;
            }
            for (String item : commands) {
                try {
                    boolean s = runCommand(item);
                    if (!s) {
                        return;
                    }
                } catch (IOException e) {
                    this.log(item + " 执行异常", e);
                    return;
                }
            }
            boolean status = packageFile();
            if (status && buildModel.getReleaseMethod() != BuildModel.ReleaseMethod.No.getCode()) {
                // 发布文件
                this.pub();
            } else {
                //
                updateStatus(BuildModel.Status.Success);
            }
        } finally {
            BUILD_MANAGE_MAP.remove(buildModel.getId());
        }
    }

    private void pub() {
        updateStatus(BuildModel.Status.PubIng);
        this.log("start pub");
        if (buildModel.getReleaseMethod() == BuildModel.ReleaseMethod.Outgiving.getCode()) {
            //
            try {
                this.doOutGiving();
            } catch (Exception e) {
                this.pubLog("发布分发包异常", e);
                return;
            }
        } else if (buildModel.getReleaseMethod() == BuildModel.ReleaseMethod.Project.getCode()) {
            BuildModel.AfterOpt afterOpt = BaseEnum.getEnum(BuildModel.AfterOpt.class, buildModel.getAfterOpt());
            if (afterOpt == null) {
                afterOpt = BuildModel.AfterOpt.No;
            }
            try {
                this.doProject(afterOpt);
            } catch (Exception e) {
                this.pubLog("发布包异常", e);
                return;
            }
        }
        this.log("pub end");
        updateStatus(BuildModel.Status.PubSuccess);
    }

    /**
     * 发布项目
     *
     * @param afterOpt 后续操作
     */
    private void doProject(BuildModel.AfterOpt afterOpt) {
        String dataId = buildModel.getReleaseMethodDataId();
        String[] strings = StrUtil.split(dataId, ":");
        if (strings == null || strings.length != 2) {
            throw new JpomRuntimeException(dataId + " error");
        }
        NodeService nodeService = SpringUtil.getBean(NodeService.class);
        NodeModel nodeModel = nodeService.getItem(strings[0]);
        Objects.requireNonNull(nodeModel, "节点不存在");
        File logFile = BuildManage.getHistoryPackageFile(buildModel, buildModel.getBuildId(), buildModel.getResultDirFile());
        File zipFile = BuildManage.isDirPackage(logFile);
        boolean unZip = true;
        if (zipFile == null) {
            zipFile = logFile;
            unZip = false;
        }
        JsonMessage jsonMessage = OutGivingRun.fileUpload(zipFile,
                strings[1],
                unZip,
                afterOpt != BuildModel.AfterOpt.No,
                nodeModel, this.userModel);
        if (jsonMessage.getCode() == HttpStatus.HTTP_OK) {
            this.log("发布项目包成功：" + jsonMessage.toString());
        } else {
            throw new JpomRuntimeException("发布项目包失败：" + jsonMessage.toString());
        }
    }

    /**
     * 分发包
     *
     * @throws IOException IO
     */
    private void doOutGiving() throws IOException {
        String dataId = buildModel.getReleaseMethodDataId();
        File logFile = BuildManage.getHistoryPackageFile(buildModel, buildModel.getBuildId(), buildModel.getResultDirFile());
        File zipFile = BuildManage.isDirPackage(logFile);
        boolean unZip = true;
        if (zipFile == null) {
            zipFile = logFile;
            unZip = false;
        }
        OutGivingRun.startRun(dataId, zipFile, userModel, unZip);
        this.log("开始执行分发包啦,请到分发中查看当前状态");
    }

    private void log(String title, Throwable throwable) {
        log(title, throwable, BuildModel.Status.Error);
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

    private void log(String title, Throwable throwable, BuildModel.Status status) {
        DefaultSystemLog.ERROR().error(title, throwable);
        FileUtil.appendLines(CollectionUtil.toList(title), this.logFile, CharsetUtil.CHARSET_UTF_8);
        String s = ExceptionUtil.stacktraceToString(throwable);
        FileUtil.appendLines(CollectionUtil.toList(s), this.logFile, CharsetUtil.CHARSET_UTF_8);
        updateStatus(status);
    }

    private void log(String info) {
        FileUtil.appendLines(CollectionUtil.toList(info), this.logFile, CharsetUtil.CHARSET_UTF_8);
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
        List<String> commands = ObjectUtil.clone(COMMAND);
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
