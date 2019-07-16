package cn.keepbx.build;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.JpomApplication;
import cn.keepbx.jpom.model.data.BuildModel;
import cn.keepbx.jpom.service.build.BuildService;
import cn.keepbx.jpom.system.ConfigBean;
import cn.keepbx.jpom.system.JpomRuntimeException;
import cn.keepbx.util.GitUtil;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.*;
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

    private BuildManage(final BuildModel buildModel) {
        this.buildModel = buildModel;
        this.gitFile = FileUtil.file(ConfigBean.getInstance().getDataPath(), "build", buildModel.getId(), "source");
        this.logFile = FileUtil.file(ConfigBean.getInstance().getDataPath(), "build", buildModel.getId(), "history", "#" + buildModel.getBuildId(), "info.log");
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
            return true;
        } catch (Exception e) {
            this.log("切换状态失败", e);
            return false;
        }
    }

    @Override
    public void run() {
        try {
            if (!updateStatus(BuildModel.Status.Ing)) {
                return;
            }
            try {
                GitUtil.checkoutPull(buildModel.getGitUrl(), gitFile, buildModel.getBranchName(), new UsernamePasswordCredentialsProvider(buildModel.getUserName(), buildModel.getPassword()));
            } catch (Exception e) {
                this.log("拉取代码失败", e);
                return;
            }
            String[] commands = StrUtil.split(buildModel.getScript(), StrUtil.CRLF);
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
        } finally {
            updateStatus(BuildModel.Status.Success);
            BUILD_MANAGE_MAP.remove(buildModel.getId());
        }
    }

    private void log(String title, Throwable throwable) {
        DefaultSystemLog.ERROR().error(title, throwable);
        FileUtil.appendLines(CollectionUtil.toList(title), this.logFile, CharsetUtil.CHARSET_UTF_8);
        String s = ExceptionUtil.stacktraceToString(throwable);
        FileUtil.appendLines(CollectionUtil.toList(s), this.logFile, CharsetUtil.CHARSET_UTF_8);
    }

    private void log(String info) {
        FileUtil.appendLines(CollectionUtil.toList(info), this.logFile, CharsetUtil.CHARSET_UTF_8);
    }

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
        processBuilder.start();
        return status[0];
    }
}
