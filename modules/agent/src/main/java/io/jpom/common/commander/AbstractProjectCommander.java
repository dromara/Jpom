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
package io.jpom.common.commander;

import cn.hutool.cache.impl.LRUCache;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.unit.DataSizeUtil;
import cn.hutool.core.lang.JarClassLoader;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.system.SystemUtil;
import io.jpom.common.IllegalArgument2Exception;
import io.jpom.common.commander.impl.LinuxProjectCommander;
import io.jpom.common.commander.impl.MacOsProjectCommander;
import io.jpom.common.commander.impl.WindowsProjectCommander;
import io.jpom.model.RunMode;
import io.jpom.model.data.DslYmlDto;
import io.jpom.model.data.NodeProjectInfoModel;
import io.jpom.model.system.NetstatModel;
import io.jpom.plugin.IPlugin;
import io.jpom.plugin.PluginFactory;
import io.jpom.script.DslScriptBuilder;
import io.jpom.socket.AgentFileTailWatcher;
import io.jpom.system.AgentConfig;
import io.jpom.system.JpomRuntimeException;
import io.jpom.util.CommandUtil;
import io.jpom.util.JvmUtil;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.io.File;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * 项目命令执行基类
 *
 * @author Administrator
 */
@Slf4j
public abstract class AbstractProjectCommander {

    public static final String RUNNING_TAG = "running";
    public static final String STOP_TAG = "stopped";

    private static final long BACK_LOG_MIN_SIZE = DataSizeUtil.parse("100KB");

    private static AbstractProjectCommander abstractProjectCommander = null;
    /**
     * 进程Id 获取端口号
     */
    public static final LRUCache<Integer, String> PID_PORT = new LRUCache<>(100, TimeUnit.MINUTES.toMillis(10));

    private final Charset fileCharset;

    public AbstractProjectCommander(Charset fileCharset) {
        this.fileCharset = fileCharset;
    }

    /**
     * 实例化Commander
     *
     * @return 命令执行对象
     */
    public static AbstractProjectCommander getInstance() {
        if (abstractProjectCommander != null) {
            return abstractProjectCommander;
        }
        AgentConfig agentConfig = SpringUtil.getBean(AgentConfig.class);
        AgentConfig.ProjectConfig.LogConfig logConfig = agentConfig.getProject().getLog();
        if (SystemUtil.getOsInfo().isLinux()) {
            // Linux系统
            abstractProjectCommander = new LinuxProjectCommander(logConfig.getFileCharset());
        } else if (SystemUtil.getOsInfo().isWindows()) {
            // Windows系统
            abstractProjectCommander = new WindowsProjectCommander(logConfig.getFileCharset());
        } else if (SystemUtil.getOsInfo().isMac()) {
            abstractProjectCommander = new MacOsProjectCommander(logConfig.getFileCharset());
        } else {
            throw new JpomRuntimeException("不支持的：" + SystemUtil.getOsInfo().getName());
        }
        return abstractProjectCommander;
    }

    //---------------------------------------------------- 基本操作----start

    /**
     * 生成可以执行的命令
     *
     * @param nodeProjectInfoModel 项目
     * @param javaCopyItem         副本信息
     * @return null 是条件不足
     */
    public abstract String buildJavaCommand(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel.JavaCopyItem javaCopyItem);

    protected String getRunJavaPath(NodeProjectInfoModel nodeProjectInfoModel, boolean w) {
//        if (StrUtil.isEmpty(nodeProjectInfoModel.getJdkId())) {
//            return w ? "javaw" : "java";
//        }
//
//        if (item == null) {
        return w ? "javaw" : "java";
//        }
//        String jdkJavaPath = FileUtils.getJdkJavaPath(item.getPath(), w);
//        if (jdkJavaPath.contains(StrUtil.SPACE)) {
//            jdkJavaPath = String.format("\"%s\"", jdkJavaPath);
//        }
//        return jdkJavaPath;
    }

    /**
     * 启动
     *
     * @param nodeProjectInfoModel 项目
     * @return 结果
     * @throws Exception 异常
     */
    public CommandOpResult start(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel.JavaCopyItem javaCopyItem) throws Exception {
        return this.start(nodeProjectInfoModel, javaCopyItem, false);
    }

    /**
     * 启动
     *
     * @param nodeProjectInfoModel 项目
     * @param sync                 dsl 是否同步执行
     * @return 结果
     * @throws Exception 异常
     */
    public CommandOpResult start(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel.JavaCopyItem javaCopyItem, boolean sync) throws Exception {
        String msg = checkStart(nodeProjectInfoModel, javaCopyItem);
        if (msg != null) {
            return CommandOpResult.of(false, msg);
        }
        RunMode runMode = nodeProjectInfoModel.getRunMode();
        if (runMode == RunMode.Dsl) {
            //
            this.runDsl(nodeProjectInfoModel, "start", (baseProcess, action) -> {
                String log = nodeProjectInfoModel.getAbsoluteLog(javaCopyItem);
                try {
                    DslScriptBuilder.run(baseProcess, nodeProjectInfoModel, action, log, sync);
                } catch (Exception e) {
                    throw Lombok.sneakyThrow(e);
                }
                return null;
            });

        } else {
            String command = this.buildJavaCommand(nodeProjectInfoModel, javaCopyItem);
            if (command == null) {
                return CommandOpResult.of(false, "没有需要执行的命令");
            }
            // 执行命令
            ThreadUtil.execute(() -> {
                try {
                    File file = FileUtil.file(nodeProjectInfoModel.allLib());
                    if (SystemUtil.getOsInfo().isWindows()) {
                        CommandUtil.execSystemCommand(command, file);
                    } else {
                        CommandUtil.asyncExeLocalCommand(file, command);
                    }
                } catch (Exception e) {
                    log.error("执行命令失败", e);
                }
            });
        }
        //
        this.loopCheckRun(nodeProjectInfoModel, javaCopyItem, true);
        CommandOpResult status = this.status(nodeProjectInfoModel, javaCopyItem);
        this.asyncWebHooks(nodeProjectInfoModel, javaCopyItem, "start", "result", status.msgStr());
        return status;
    }

    private <T> T runDsl(NodeProjectInfoModel nodeProjectInfoModel, String opt, BiFunction<DslYmlDto.BaseProcess, String, T> function) {
        DslYmlDto.BaseProcess process = nodeProjectInfoModel.getDslProcess(opt);
        return function.apply(process, opt);
    }

    /**
     * 查询出指定端口信息
     *
     * @param pid       进程id
     * @param listening 是否只获取检查状态的
     * @return 数组
     */
    public abstract List<NetstatModel> listNetstat(int pid, boolean listening);

    /**
     * 停止
     *
     * @param nodeProjectInfoModel 项目
     * @param javaCopyItem         副本信息
     * @return 结果
     * @throws Exception 异常
     */
    public CommandOpResult stop(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel.JavaCopyItem javaCopyItem) throws Exception {
        return this.stop(nodeProjectInfoModel, javaCopyItem, false);
    }

    /**
     * 停止
     *
     * @param nodeProjectInfoModel 项目
     * @param javaCopyItem         副本信息
     * @param sync                 dsl 是否同步执行
     * @return 结果
     * @throws Exception 异常
     */
    public CommandOpResult stop(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel.JavaCopyItem javaCopyItem, boolean sync) throws Exception {
        RunMode runMode = nodeProjectInfoModel.getRunMode();
        if (runMode == RunMode.File) {
            return CommandOpResult.of(true, "file 类型项目没有 stop");
        }
        Tuple tuple = this.stopBefore(nodeProjectInfoModel, javaCopyItem);
        CommandOpResult status = tuple.get(1);
        String webHook = tuple.get(0);
        if (status.isSuccess()) {
            // 运行中
            if (runMode == RunMode.Dsl) {
                //
                this.runDsl(nodeProjectInfoModel, "stop", (process, action) -> {
                    String log = nodeProjectInfoModel.getAbsoluteLog(javaCopyItem);
                    try {
                        DslScriptBuilder.run(process, nodeProjectInfoModel, action, log, sync);
                    } catch (Exception e) {
                        throw Lombok.sneakyThrow(e);
                    }
                    return null;
                });
                boolean checkRun = this.loopCheckRun(nodeProjectInfoModel, javaCopyItem, false);
                return CommandOpResult.of(checkRun, checkRun ? "stop done" : "stop done,but unsuccessful")
                    .appendMsg(status.getMsgs())
                    .appendMsg(webHook);
            } else {
                //
                return this.stopJava(nodeProjectInfoModel, javaCopyItem, status.getPid()).appendMsg(status.getMsgs()).appendMsg(webHook);
            }
        }
        return CommandOpResult.of(true).appendMsg(status.getMsgs()).appendMsg(webHook);
    }

    /**
     * 停止
     *
     * @param nodeProjectInfoModel 项目
     * @param javaCopyItem         副本信息
     * @param pid                  进程ID
     * @return 结果
     * @throws Exception 异常
     */
    public abstract CommandOpResult stopJava(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel.JavaCopyItem javaCopyItem, int pid) throws Exception;

    /**
     * 停止之前
     *
     * @param nodeProjectInfoModel 项目
     * @return 结果
     * @throws Exception 异常
     */
    private Tuple stopBefore(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel.JavaCopyItem javaCopyItem) throws Exception {
        String beforeStop = this.webHooks(nodeProjectInfoModel, javaCopyItem, "beforeStop");
        // 再次查看进程信息
        CommandOpResult result = this.status(nodeProjectInfoModel, javaCopyItem);
        if (result.isSuccess()) {
            // 端口号缓存
            PID_PORT.remove(result.getPid());
        }
        this.asyncWebHooks(nodeProjectInfoModel, javaCopyItem, "stop", "result", result);
        return new Tuple(StrUtil.emptyToDefault(beforeStop, StrUtil.EMPTY), result);
    }

    /**
     * 执行 webhooks 通知
     *
     * @param nodeProjectInfoModel 项目信息
     * @param javaCopyItem         副本信息
     * @param type                 类型
     * @param other                其他参数
     */
    public void asyncWebHooks(NodeProjectInfoModel nodeProjectInfoModel,
                              NodeProjectInfoModel.JavaCopyItem javaCopyItem,
                              String type, Object... other) {
        String token = nodeProjectInfoModel.getToken();
        Opt.ofBlankAble(token)
            .ifPresent(s ->
                ThreadUtil.execute(() -> {
                    try {
                        this.webHooks(nodeProjectInfoModel, javaCopyItem, type, other);
                    } catch (Exception e) {
                        log.error("project webhook", e);
                    }
                })
            );
    }

    /**
     * 执行 webhooks 通知
     *
     * @param nodeProjectInfoModel 项目信息
     * @param javaCopyItem         副本信息
     * @param type                 类型
     * @param other                其他参数
     * @return 结果
     */
    private String webHooks(NodeProjectInfoModel nodeProjectInfoModel,
                            NodeProjectInfoModel.JavaCopyItem javaCopyItem,
                            String type, Object... other) throws Exception {
        String token = nodeProjectInfoModel.getToken();
        IPlugin plugin = PluginFactory.getPlugin("webhook");
        Map<String, Object> map = new HashMap<>(10);
        map.put("projectId", nodeProjectInfoModel.getId());
        map.put("projectName", nodeProjectInfoModel.getName());
        map.put("type", type);
        if (javaCopyItem != null) {
            map.put("copyId", javaCopyItem.getId());
        }
        for (int i = 0; i < other.length; i += 2) {
            map.put(other[i].toString(), other[i + 1]);
        }
        Object execute = plugin.execute(token, map);
        return Convert.toStr(execute, StrUtil.EMPTY);
    }

    /**
     * 重启
     *
     * @param nodeProjectInfoModel 项目
     * @return 结果
     * @throws Exception 异常
     */
    public CommandOpResult restart(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel.JavaCopyItem javaCopyItem) throws Exception {
        RunMode runMode = nodeProjectInfoModel.getRunMode();
        if (runMode == RunMode.File) {
            return CommandOpResult.of(true, "file 类型项目没有 restart");
        }
        this.asyncWebHooks(nodeProjectInfoModel, javaCopyItem, "beforeRestart");
        if (runMode == RunMode.Dsl) {
            DslYmlDto.BaseProcess dslProcess = nodeProjectInfoModel.tryDslProcess("restart");
            if (dslProcess != null) {
                //
                this.runDsl(nodeProjectInfoModel, "restart", (process, action) -> {
                    String log = nodeProjectInfoModel.getAbsoluteLog(javaCopyItem);
                    try {
                        DslScriptBuilder.run(process, nodeProjectInfoModel, action, log, false);
                    } catch (Exception e) {
                        throw Lombok.sneakyThrow(e);
                    }
                    return null;
                });
                // 等待 状态成功
                boolean run = this.loopCheckRun(nodeProjectInfoModel, javaCopyItem, true);
                return CommandOpResult.of(run, run ? "restart done" : "restart done,but unsuccessful");

                //return new Tuple(run ? "restart done,but unsuccessful" : "restart done", resultMsg);
            }
        }
        boolean run = this.isRun(nodeProjectInfoModel, javaCopyItem);
        CommandOpResult stopMsg = null;
        if (run) {
            stopMsg = this.stop(nodeProjectInfoModel, javaCopyItem, true);
        }
        CommandOpResult startMsg = this.start(nodeProjectInfoModel, javaCopyItem);
        if (stopMsg != null) {
            startMsg.appendMsg(stopMsg.getMsgs());
        }
        return startMsg;
    }

    /**
     * 启动项目前基本检查
     *
     * @param nodeProjectInfoModel 项目
     * @return null 检查一切正常
     * @throws Exception 异常
     */
    private String checkStart(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel.JavaCopyItem javaCopyItem) throws Exception {
        CommandOpResult status = this.status(nodeProjectInfoModel, javaCopyItem);
        if (status.isSuccess()) {
            return "当前程序正在运行中，不能重复启动,PID:" + status.getPid();
        }
        String lib = nodeProjectInfoModel.allLib();
        File fileLib = new File(lib);
        File[] files = fileLib.listFiles();
        if (files == null || files.length <= 0) {
            return "项目目录没有任何文件,请先到项目文件管理中上传文件";
        }
        //
        RunMode runMode = nodeProjectInfoModel.getRunMode();
        if (runMode == RunMode.Dsl) {
            //
            String dslContent = nodeProjectInfoModel.getDslContent();
        } else if (runMode == RunMode.ClassPath || runMode == RunMode.JavaExtDirsCp) {
            // 判断主类
            try (JarClassLoader jarClassLoader = JarClassLoader.load(fileLib)) {
                jarClassLoader.loadClass(nodeProjectInfoModel.getMainClass());
            } catch (ClassNotFoundException notFound) {
                return "没有找到对应的MainClass:" + nodeProjectInfoModel.getMainClass();
            }
        } else if (runMode == RunMode.Jar || runMode == RunMode.JarWar) {
            List<File> fileList = NodeProjectInfoModel.listJars(nodeProjectInfoModel);
            if (fileList.size() <= 0) {
                return String.format("没有%s包,请先到文件管理中上传程序的%s", runMode.name(), runMode.name());
            }
            File jarFile = fileList.get(0);
            String checkJar = checkJar(jarFile);
            if (checkJar != null) {
                return checkJar;
            }
        } else {
            return "当前项目类型不支持启动";
        }
        // 备份日志
        backLog(nodeProjectInfoModel, javaCopyItem);
        return null;
    }

    private static String checkJar(File jarFile) {
        try (JarFile jarFile1 = new JarFile(jarFile)) {
            Manifest manifest = jarFile1.getManifest();
            Attributes attributes = manifest.getMainAttributes();
            String mainClass = attributes.getValue(Attributes.Name.MAIN_CLASS);
            if (mainClass == null) {
                return jarFile.getAbsolutePath() + "中没有找到对应的MainClass属性";
            }
            try (JarClassLoader jarClassLoader = JarClassLoader.load(jarFile)) {
                jarClassLoader.loadClass(mainClass);
            } catch (ClassNotFoundException notFound) {
                return jarFile.getAbsolutePath() + "中没有找到对应的MainClass:" + mainClass;
            }
        } catch (Exception e) {
            log.error("解析jar", e);
            return jarFile.getAbsolutePath() + " 解析错误:" + e.getMessage();
        }
        return null;
    }

    /**
     * 解析是否开启 日志备份功能
     *
     * @param nodeProjectInfoModel 项目实体
     * @return true 开启日志备份
     */
    private boolean resolveOpenLogBack(NodeProjectInfoModel nodeProjectInfoModel) {
        RunMode runMode = nodeProjectInfoModel.getRunMode();
        AgentConfig agentConfig = SpringUtil.getBean(AgentConfig.class);
        boolean autoBackToFile = agentConfig.getProject().getLog().isAutoBackupToFile();
        if (runMode == RunMode.Dsl) {
            DslYmlDto dslYmlDto = nodeProjectInfoModel.dslConfig();
            return Optional.ofNullable(dslYmlDto)
                .map(DslYmlDto::getConfig)
                .map(DslYmlDto.Config::getAutoBackToFile)
                .orElse(autoBackToFile);
        }
        return autoBackToFile;
    }

    /**
     * 清空日志信息
     *
     * @param nodeProjectInfoModel 项目
     * @return 结果
     */
    public String backLog(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel.JavaCopyItem javaCopyItem) {
        File file = javaCopyItem == null ? new File(nodeProjectInfoModel.getLog()) : nodeProjectInfoModel.getLog(javaCopyItem);
        if (!file.exists() || file.isDirectory()) {
            return "not exists";
        }
        // 文件内容太少不处理

        if (file.length() <= BACK_LOG_MIN_SIZE) {
            return "ok";
        }
        boolean openLogBack = this.resolveOpenLogBack(nodeProjectInfoModel);
        if (openLogBack) {
            // 开启日志备份才移动文件
            File backPath = javaCopyItem == null ? nodeProjectInfoModel.getLogBack() : nodeProjectInfoModel.getLogBack(javaCopyItem);
            backPath = new File(backPath, DateTime.now().toString(DatePattern.PURE_DATETIME_FORMAT) + ".log");
            FileUtil.copy(file, backPath, true);
        }
        // 清空日志
        String r = AbstractSystemCommander.getInstance().emptyLogFile(file);
        if (StrUtil.isNotEmpty(r)) {
            log.info(r);
        }
        // 重新监听
        AgentFileTailWatcher.reWatcher(file);
        return "ok";
    }

    /**
     * 查询项目状态
     *
     * @param nodeProjectInfoModel 项目
     * @param javaCopyItem         副本
     * @return 状态
     */
    public CommandOpResult status(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel.JavaCopyItem javaCopyItem) {
        RunMode runMode = nodeProjectInfoModel.getRunMode();
        if (runMode == RunMode.File) {
            return CommandOpResult.of(false, "file 类型项目没有运行状态");
        }
        if (runMode == RunMode.Dsl) {
            List<String> status = this.runDsl(nodeProjectInfoModel, "status", (baseProcess, action) -> {
                // 提前判断脚本 id,避免填写错误在删除项目检测状态时候异常
                try {
                    return DslScriptBuilder.syncRun(baseProcess, nodeProjectInfoModel, action);
                } catch (IllegalArgument2Exception argument2Exception) {
                    log.warn("执行 DSL 脚本异常：{}", argument2Exception.getMessage());
                    return CollUtil.newArrayList(argument2Exception.getMessage());
                }
            });

            return Optional.ofNullable(status)
                .map(strings -> {
                    String log = nodeProjectInfoModel.getAbsoluteLog(javaCopyItem);
                    FileUtil.appendLines(strings, FileUtil.file(log), fileCharset);
                    return strings;
                })
                .map(CollUtil::getLast)
                // 此流程特意处理 系统日志标准格式 StrUtil.format("{} [{}] - {}", DateUtil.now(), this.action, line);
                .map(s -> StrUtil.splitTrim(s, StrPool.DASHED))
                .map(CollUtil::getLast)
                .map(CommandOpResult::of)
                .orElseGet(() -> CommandOpResult.of(false, STOP_TAG));
        } else {
            String tag = javaCopyItem == null ? nodeProjectInfoModel.getId() : javaCopyItem.getTagId();
            String statusResult = this.status(tag);
            CommandOpResult of = CommandOpResult.of(statusResult);
            if (!of.isSuccess()) {
                // 只有 java 项目才判断 jps
                Assert.state(JvmUtil.jpsNormal, JvmUtil.JPS_ERROR_MSG);
            }
            return of;
        }
    }

    /**
     * 查看状态
     *
     * @param tag 运行标识
     * @return 查询结果
     */
    protected String status(String tag) {
        String jpsStatus = this.getJpsStatus(tag);
        if (StrUtil.equals(AbstractProjectCommander.STOP_TAG, jpsStatus) && SystemUtil.getOsInfo().isLinux()) {
            return getLinuxPsStatus(tag);
        }
        return jpsStatus;
    }

    /**
     * 尝试jps 中查看进程id
     *
     * @param tag 进程标识
     * @return 运行标识
     */
    private String getJpsStatus(String tag) {
        Integer pid = JvmUtil.getPidByTag(tag);
        if (pid == null || pid <= 0) {
            return AbstractProjectCommander.STOP_TAG;
        }
        return StrUtil.format("{}:{}", AbstractProjectCommander.RUNNING_TAG, pid);
    }


    /**
     * 尝试ps -ef | grep  中查看进程id
     *
     * @param tag 进程标识
     * @return 运行标识
     */
    private String getLinuxPsStatus(String tag) {
        String execSystemCommand = CommandUtil.execSystemCommand("ps -ef | grep " + tag);
        log.debug("getLinuxPsStatus {} {}", tag, execSystemCommand);
        List<String> list = StrSplitter.splitTrim(execSystemCommand, StrUtil.LF, true);
        for (String item : list) {
            if (JvmUtil.checkCommandLineIsJpom(item, tag)) {
                String[] split = StrUtil.splitToArray(item, StrUtil.SPACE);
                return StrUtil.format("{}:{}", AbstractProjectCommander.RUNNING_TAG, split[1]);
            }
        }
        return AbstractProjectCommander.STOP_TAG;
    }

    //---------------------------------------------------- 基本操作----end

    /**
     * 获取进程占用的主要端口
     *
     * @param pid 进程id
     * @return 端口
     */
    public String getMainPort(int pid) {
        if (pid <= 0) {
            return StrUtil.DASHED;
        }
        String cachePort = PID_PORT.get(pid);
        if (cachePort != null) {
            return cachePort;
        }
        List<NetstatModel> list = listNetstat(pid, true);
        if (list == null) {
            return StrUtil.DASHED;
        }
        List<Integer> ports = new ArrayList<>();
        for (NetstatModel model : list) {
            String local = model.getLocal();
            String portStr = getPortFormLocalIp(local);
            if (portStr == null) {
                continue;
            }
            // 取最小的端口号
            int minPort = Convert.toInt(portStr, Integer.MAX_VALUE);
            if (minPort == Integer.MAX_VALUE) {
                continue;
            }
            ports.add(minPort);
        }
        if (CollUtil.isEmpty(ports)) {
            return StrUtil.DASHED;
        }
        String allPort = CollUtil.join(ports, StrUtil.COMMA);
        // 缓存
        PID_PORT.put(pid, allPort);
        return allPort;
    }

    /**
     * 判断ip 信息是否为本地ip
     *
     * @param local ip信息
     * @return true 是本地ip
     */
    private String getPortFormLocalIp(String local) {
        if (StrUtil.isEmpty(local)) {
            return null;
        }
        List<String> ipPort = StrSplitter.splitTrim(local, StrUtil.COLON, true);
        if (ipPort.isEmpty()) {
            return null;
        }
        String anObject = ipPort.get(0);
        if (StrUtil.equalsAny(anObject, "0.0.0.0", "*") || ipPort.size() == 1) {
            // 0.0.0.0:8084  || :::18000 || *:2123
            return ipPort.get(ipPort.size() - 1);
        }
        return null;
    }


    /**
     * 是否正在运行
     *
     * @param nodeProjectInfoModel 项目
     * @return true 正在运行
     */
    public boolean isRun(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel.JavaCopyItem javaCopyItem) {
        CommandOpResult result = this.status(nodeProjectInfoModel, javaCopyItem);
        return result.isSuccess();
    }

    /***
     * 阻塞检查程序状态
     * @param nodeProjectInfoModel 项目
     * @param javaCopyItem  副本
     * @param status 要检查的状态
     *
     * @return 和参数status相反
     */
    protected boolean loopCheckRun(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel.JavaCopyItem javaCopyItem, boolean status) {
        int statusWaitTime = AgentConfig.ProjectConfig.getInstance().getStatusWaitTime();
        return this.loopCheckRun(nodeProjectInfoModel, javaCopyItem, statusWaitTime, status);
    }

    /***
     * 阻塞检查程序状态
     * @param nodeProjectInfoModel 项目
     * @param javaCopyItem  副本
     * @param status 要检查的状态
     * @param waitTime  检查等待时间
     *
     * @return 如果和期望一致则返回 true，反之 false
     */
    protected boolean loopCheckRun(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel.JavaCopyItem javaCopyItem, int waitTime, boolean status) {
        waitTime = Math.max(waitTime, 1);
        int statusDetectionInterval = AgentConfig.ProjectConfig.getInstance().getStatusDetectionInterval();
        statusDetectionInterval = Math.max(statusDetectionInterval, 1);
        int loopCount = (int) (TimeUnit.SECONDS.toMillis(waitTime) / 500);
        int count = 0;
        do {
            if (this.isRun(nodeProjectInfoModel, javaCopyItem) == status) {
                // 是期望的结果
                return true;
            }
            ThreadUtil.sleep(statusDetectionInterval);
        } while (count++ < loopCount);
        return false;
    }
}
