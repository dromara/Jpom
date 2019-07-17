package cn.keepbx.build;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.JpomApplication;
import cn.keepbx.jpom.model.data.BuildHistoryLog;
import cn.keepbx.jpom.model.data.BuildModel;
import cn.keepbx.jpom.model.data.MonitorNotifyLog;
import cn.keepbx.jpom.service.build.BuildService;
import cn.keepbx.jpom.system.ConfigBean;
import cn.keepbx.jpom.system.JpomRuntimeException;
import cn.keepbx.jpom.system.db.DbConfig;
import cn.keepbx.util.GitUtil;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
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

    private BuildManage(final BuildModel buildModel) {
        this.buildModel = buildModel;
        this.gitFile = FileUtil.file(ConfigBean.getInstance().getDataPath(), "build", buildModel.getId(), "source");
        this.logFile = getLogFile(buildModel, buildModel.getBuildId());
    }

    public static File getLogFile(BuildModel buildModel, int buildId) {
        return FileUtil.file(ConfigBean.getInstance().getDataPath(),
                "build",
                buildModel.getId(),
                "history",
                BuildModel.getBuildIdStr(buildId),
                "info.log");
    }

    public static BuildManage create(BuildModel buildModel) {
        if (BUILD_MANAGE_MAP.containsKey(buildModel.getId())) {
            throw new JpomRuntimeException("当前构建还在进行中");
        }
        BuildManage buildManage = new BuildManage(buildModel);
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
        buildHistoryLog.setStartTime(System.currentTimeMillis());

        Db db = Db.use();
        db.setWrapper((Character) null);
        try {
            Entity entity = new Entity(BuildHistoryLog.TABLE_NAME);
            entity.parseBean(buildHistoryLog);
            db.insert(entity);
            //
            DbConfig.autoClear(BuildHistoryLog.TABLE_NAME, "startTime");
        } catch (SQLException e) {
            DefaultSystemLog.ERROR().error("db error", e);
        }
    }

    /**
     * 打包构建产物
     */
    private void packageFile() {
        File file = FileUtil.file(this.gitFile, buildModel.getResultDirFile());
        if (!file.exists()) {
            this.log(buildModel.getResultDirFile() + "不存在，处理构建产物失败");
            return;
        }
        File toFile = FileUtil.file(ConfigBean.getInstance().getDataPath(), "build", buildModel.getId(), "history", buildModel.getBuildIdStr(), buildModel.getResultDirFile());
        FileUtil.copyContent(file, toFile, true);
        this.log(StrUtil.format("mv {} {}", buildModel.getResultDirFile(), buildModel.getBuildIdStr()));
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
            packageFile();
            //
            updateStatus(BuildModel.Status.Success);
        } finally {
            BUILD_MANAGE_MAP.remove(buildModel.getId());
        }
    }

    private void log(String title, Throwable throwable) {
        DefaultSystemLog.ERROR().error(title, throwable);
        FileUtil.appendLines(CollectionUtil.toList(title), this.logFile, CharsetUtil.CHARSET_UTF_8);
        String s = ExceptionUtil.stacktraceToString(throwable);
        FileUtil.appendLines(CollectionUtil.toList(s), this.logFile, CharsetUtil.CHARSET_UTF_8);
        updateStatus(BuildModel.Status.Error);
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
        Process process = processBuilder.start();
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
        process = processBuilder.start();
        return status[0];
    }
}
