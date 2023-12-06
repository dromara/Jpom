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
package org.dromara.jpom.build;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileCopier;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.map.SafeConcurrentHashMap;
import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.*;
import cn.keepbx.jpom.model.BaseIdModel;
import cn.keepbx.jpom.plugins.IPlugin;
import lombok.Builder;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.func.assets.server.MachineDockerServer;
import org.dromara.jpom.func.files.service.FileStorageService;
import org.dromara.jpom.model.data.BuildInfoModel;
import org.dromara.jpom.model.data.RepositoryModel;
import org.dromara.jpom.model.docker.DockerInfoModel;
import org.dromara.jpom.model.enums.BuildReleaseMethod;
import org.dromara.jpom.model.enums.BuildStatus;
import org.dromara.jpom.model.log.BuildHistoryLog;
import org.dromara.jpom.model.script.ScriptExecuteLogModel;
import org.dromara.jpom.model.script.ScriptModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.service.dblog.BuildInfoService;
import org.dromara.jpom.service.dblog.DbBuildHistoryLogService;
import org.dromara.jpom.service.docker.DockerInfoService;
import org.dromara.jpom.service.script.ScriptExecuteLogServer;
import org.dromara.jpom.service.script.ScriptServer;
import org.dromara.jpom.system.ExtConfigBean;
import org.dromara.jpom.system.extconf.BuildExtConfig;
import org.dromara.jpom.util.AntPathUtil;
import org.dromara.jpom.util.CommandUtil;
import org.dromara.jpom.util.FileUtils;
import org.dromara.jpom.util.LogRecorder;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2023/3/30
 */
@Builder
@Slf4j
public class BuildExecuteManage implements Runnable {

    /**
     * 构建线程池
     */
    private static ThreadPoolExecutor threadPoolExecutor;


    /**
     * 缓存构建中
     */
    public static final Map<String, BuildExecuteManage> BUILD_MANAGE_MAP = new SafeConcurrentHashMap<>();

    private final TaskData taskData;
    private final BuildExtraModule buildExtraModule;
    private final String logId;
    private final BuildExecuteService buildExecuteService;
    private final ScriptServer scriptServer;
    private final ScriptExecuteLogServer scriptExecuteLogServer;
    private final BuildInfoService buildService;
    private final DbBuildHistoryLogService dbBuildHistoryLogService;
    private final DockerInfoService dockerInfoService;
    private final MachineDockerServer machineDockerServer;
    private final BuildExtConfig buildExtConfig;
    private final FileStorageService fileStorageService;
    //
    private Process process;
    private LogRecorder logRecorder;
    private File gitFile;
    private Thread currentThread;

    /**
     * 提交任务时间
     */
    private Long submitTaskTime;


    /**
     * 正在构建的数量
     *
     * @return 构建数量
     */
    public static Set<String> buildKeys() {
        return BUILD_MANAGE_MAP.keySet();
    }

    /**
     * 创建构建线程池
     */
    private synchronized void initPool() {
        if (threadPoolExecutor != null) {
            return;
        }
        ExecutorBuilder executorBuilder = ExecutorBuilder.create();
        int poolSize = buildExtConfig.getPoolSize();
        if (poolSize > 0) {
            executorBuilder.setCorePoolSize(poolSize).setMaxPoolSize(poolSize);
        }
        executorBuilder.useArrayBlockingQueue(Math.max(buildExtConfig.getPoolWaitQueue(), 1));
        executorBuilder.setHandler(new ThreadPoolExecutor.DiscardPolicy() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                if (r instanceof BuildExecuteManage) {
                    // 取消任务
                    BuildExecuteManage buildExecuteManage = (BuildExecuteManage) r;
                    buildExecuteManage.rejectedExecution();
                } else {
                    log.warn("构建线程池拒绝了未知任务：{}", r.getClass());
                }
            }
        });
        threadPoolExecutor = executorBuilder.build();
        JpomApplication.register("build", threadPoolExecutor);
    }

    /**
     * 提交任务
     */
    public void submitTask() {
        submitTaskTime = SystemClock.now();
        // 创建线程池
        initPool();
        //
        BuildInfoModel buildInfoModel = taskData.buildInfoModel;
        File logFile = BuildUtil.getLogFile(buildInfoModel.getId(), buildInfoModel.getBuildId());
        this.logRecorder = LogRecorder.builder().file(logFile).build();
        //
        int queueSize = threadPoolExecutor.getQueue().size();
        int size = BUILD_MANAGE_MAP.size();
        logRecorder.system("当前构建中任务数：{},队列中任务数：{} {}", size, queueSize,
            size > buildExtConfig.getPoolSize() ? "构建任务开始进入队列等待...." : StrUtil.EMPTY);
        //BuildInfoManage manage = new BuildInfoManage(taskData);
        BUILD_MANAGE_MAP.put(buildInfoModel.getId(), this);
        threadPoolExecutor.execute(this);
    }

    /**
     * 取消任务(拒绝执行)
     */
    private void rejectedExecution() {
        int queueSize = threadPoolExecutor.getQueue().size();
        int limitPoolSize = threadPoolExecutor.getPoolSize();
        int corePoolSize = threadPoolExecutor.getCorePoolSize();
        String format = StrUtil.format("当前构建中任务数：{},队列中任务数：{} 构建任务等待超时或者超出最大等待数量,当前运行中的任务数：{}/{},取消执行当前构建", BUILD_MANAGE_MAP.size(), queueSize, limitPoolSize, corePoolSize);
        logRecorder.system(format);
        this.cancelTask(format);
    }

    /**
     * 取消任务
     */
    private void cancelTask(String desc) {
        Optional.ofNullable(process).ifPresent(Process::destroy);
        Integer buildMode = taskData.buildInfoModel.getBuildMode();
        if (buildMode != null && buildMode == 1) {
            // 容器构建 删除容器
            try {
                Optional.ofNullable(taskData.dockerParameter).ifPresent(parameter -> {
                    IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
                    parameter.put("containerId", taskData.buildContainerId);
                    try {
                        plugin.execute("removeContainer", parameter);
                    } catch (Exception e) {
                        throw Lombok.sneakyThrow(e);
                    }
                });

            } catch (Exception e) {
                log.warn("清理构建资源失败", e);
            }
        }
        String buildId = taskData.buildInfoModel.getId();
        buildExecuteService.updateStatus(buildId, logId, taskData.buildInfoModel.getBuildId(), BuildStatus.Cancel, desc);
        Optional.ofNullable(currentThread).ifPresent(Thread::interrupt);
        BUILD_MANAGE_MAP.remove(buildId);
    }

    /**
     * 打包构建产物
     */
    private String packageFile() {
        BuildInfoModel buildInfoModel = taskData.buildInfoModel;
        Integer buildMode = taskData.buildInfoModel.getBuildMode();
        String resultDirFile = buildInfoModel.getResultDirFile();
        String excludeReleaseAnt = this.buildExtraModule.getExcludeReleaseAnt();
        boolean releaseHideFile = ObjectUtil.defaultIfNull(this.buildExtraModule.getReleaseHideFile(), false);
        List<String> excludeReleaseAnts = StrUtil.splitTrim(excludeReleaseAnt, StrUtil.COMMA);
        ResultDirFileAction resultDirFileAction = ResultDirFileAction.parse(resultDirFile);
        final int[] excludeReleaseAntCount = {0};
        Predicate<String> predicate = file -> {
            if (CollUtil.isEmpty(excludeReleaseAnts)) {
                return true;
            }
            for (String releaseAnt : excludeReleaseAnts) {
                if (AntPathUtil.ANT_PATH_MATCHER.match(releaseAnt, file)) {
                    // 过滤
                    excludeReleaseAntCount[0]++;
                    return false;
                }
            }
            return true;
        };
        if (buildMode != null && buildMode == 1) {
            // 容器构建直接下载到 结果目录
            File toFile = BuildUtil.getHistoryPackageFile(buildInfoModel.getId(), buildInfoModel.getBuildId(), resultDirFileAction.getPath());
            if (!FileUtil.exist(toFile)) {
                String format = StrUtil.format("{} 不存在，处理构建产物失败", resultDirFileAction.getPath());
                logRecorder.systemError(format);
                return format;
            }
            logRecorder.system("备份产物 {} {}", resultDirFileAction.getPath(), buildInfoModel.getBuildId());
            return null;
        }
        if (resultDirFileAction.getType() == ResultDirFileAction.Type.ANT_PATH) {
            // 通配模式
            List<String> paths = AntPathUtil.antPathMatcher(this.gitFile, resultDirFileAction.getPath());
            int matcherSize = CollUtil.size(paths);
            if (matcherSize <= 0) {
                String format = StrUtil.format("{} 没有匹配到任何文件", resultDirFileAction.getPath());
                logRecorder.systemError(format);
                return format;
            }
            logRecorder.system("{} 模糊匹配到 {} 个文件", resultDirFileAction.getPath(), matcherSize);
            String antSubMatch = resultDirFileAction.antSubMatch();
            ResultDirFileAction.AntFileUploadMode antFileUploadMode = resultDirFileAction.getAntFileUploadMode();
            Assert.notNull(antFileUploadMode, "没有配置文件上传模式");
            File historyPackageFile = BuildUtil.getHistoryPackageFile(buildInfoModel.getId(), buildInfoModel.getBuildId(), StrUtil.SLASH);
            int subMatchCount = paths.stream()
                .filter(s -> {
                    // 需要能满足二级匹配
                    return StrUtil.isEmpty(antSubMatch) || AntPathUtil.ANT_PATH_MATCHER.matchStart(antSubMatch + "**", s);
                })
                .filter(predicate)
                .mapToInt(path -> {
                    File toFile;
                    if (antFileUploadMode == ResultDirFileAction.AntFileUploadMode.KEEP_DIR) {
                        // 剔除文件夹层级
                        List<String> list = StrUtil.splitTrim(path, StrUtil.SLASH);
                        int notMathIndex;
                        int pathItemSize = list.size();
                        if (StrUtil.isEmpty(antSubMatch) || StrUtil.equals(antSubMatch, StrUtil.SLASH)) {
                            notMathIndex = 0;
                        } else {
                            notMathIndex = ArrayUtil.INDEX_NOT_FOUND;
                            for (int i = pathItemSize - 1; i >= 0; i--) {
                                String suffix = i == pathItemSize - 1 ? StrUtil.EMPTY : StrUtil.SLASH;
                                String itemS = StrUtil.SLASH + CollUtil.join(CollUtil.sub(list, 0, i + 1), StrUtil.SLASH) + suffix;
                                if (AntPathUtil.ANT_PATH_MATCHER.match(antSubMatch, itemS)) {
                                    notMathIndex = i + 1;
                                    break; // 结束本次循环
                                }
                            }
                            if (notMathIndex == ArrayUtil.INDEX_NOT_FOUND) {
                                return 0;
                            }
                        }
                        // 保留文件夹层级
                        String itemEnd = CollUtil.join(CollUtil.sub(list, notMathIndex, pathItemSize), StrUtil.SLASH);
                        toFile = FileUtil.file(historyPackageFile, itemEnd);
                    } else if (antFileUploadMode == ResultDirFileAction.AntFileUploadMode.SAME_DIR) {
                        toFile = historyPackageFile;
                    } else {
                        throw new IllegalStateException("暂不支持的模式：" + antFileUploadMode);
                    }
                    // 创建文件夹，避免出现文件全部为相关文件名（result）
                    BuildUtil.mkdirHistoryPackageFile(buildInfoModel.getId(), buildInfoModel.getBuildId());
                    File srcFile = FileUtil.file(this.gitFile, path);
                    //
                    FileCopier.create(srcFile, toFile)
                        .setCopyContentIfDir(true)
                        .setOverride(true)
                        .setCopyAttributes(true)
                        .setCopyFilter(file -> releaseHideFile || !file.isHidden())
                        .copy();
                    return 1;
                }).sum();
            if (subMatchCount <= 0) {
                String format = StrUtil.format("{} 没有匹配到任何文件", antSubMatch);
                logRecorder.systemError(format);
                return format;
            }
            logRecorder.system("{} 二级目录模糊匹配到 {} 个文件, 当前文件保留方式 {}", antSubMatch, subMatchCount, antFileUploadMode);
            // 更新产物路径为普通路径
            dbBuildHistoryLogService.updateResultDirFile(this.logId, StrUtil.SLASH);
            buildInfoModel.setResultDirFile(StrUtil.SLASH);
            this.buildExtraModule.setResultDirFile(StrUtil.SLASH);
        } else if (resultDirFileAction.getType() == ResultDirFileAction.Type.ORIGINAL) {
            File file = FileUtil.file(this.gitFile, resultDirFile);
            if (!file.exists()) {
                String format = StrUtil.format("{} 不存在，处理构建产物失败", resultDirFile);
                logRecorder.systemError(format);
                return format;
            }
            BuildUtil.mkdirHistoryPackageFile(buildInfoModel.getId(), buildInfoModel.getBuildId());
            File toFile = BuildUtil.getHistoryPackageFile(buildInfoModel.getId(), buildInfoModel.getBuildId(), resultDirFile);
            //
            String rootDir = FileUtil.getAbsolutePath(gitFile);
            FileCopier.create(file, toFile)
                .setCopyContentIfDir(true)
                .setOverride(true)
                .setCopyAttributes(true)
                .setCopyFilter(file12 -> {
                    if (!releaseHideFile && file12.isHidden()) {
                        return false;
                    }
                    String subPath = FileUtil.subPath(rootDir, file12);
                    subPath = FileUtil.normalize(StrUtil.SLASH + subPath);
                    return predicate.test(subPath);
                })
                .copy();
        }
        if (CollUtil.isNotEmpty(excludeReleaseAnts)) {
            logRecorder.system("{} 累积过滤：{} 个文件 ", excludeReleaseAnt, excludeReleaseAntCount[0]);
        }
        return null;
    }

    /**
     * 准备构建
     *
     * @return false 执行异常需要结束
     */
    private String startReady() {
        BuildInfoModel buildInfoModel = taskData.buildInfoModel;
        this.gitFile = BuildUtil.getSourceById(buildInfoModel.getId());

        Integer delay = taskData.delay;
        logRecorder.system("开始构建 #{} 构建执行路径 : {}", buildInfoModel.getBuildId(), FileUtil.getAbsolutePath(this.gitFile));
        if (delay != null && delay > 0) {
            // 延迟执行
            logRecorder.system("执行等待 {} 秒", delay);
            ThreadUtil.sleep(delay, TimeUnit.SECONDS);
        }
        // 删除缓存
        Boolean cacheBuild = this.buildExtraModule.getCacheBuild();
        if (cacheBuild != null && !cacheBuild) {
            logRecorder.system("删除构建缓存");
            CommandUtil.systemFastDel(this.gitFile);
        }
        //
        taskData.environmentMapBuilder.put("BUILD_ID", this.buildExtraModule.getId());
        taskData.environmentMapBuilder.put("BUILD_NAME", this.buildExtraModule.getName());
        taskData.environmentMapBuilder.put("BUILD_SOURCE_FILE", FileUtil.getAbsolutePath(this.gitFile));
        taskData.environmentMapBuilder.put("BUILD_NUMBER_ID", String.valueOf(this.taskData.buildInfoModel.getBuildId()));
        taskData.environmentMapBuilder.put("BUILD_ORIGINAL_RESULT_DIR_FILE", buildInfoModel.getResultDirFile());
        // 配置的分支名称，可能存在模糊匹配的情况
        taskData.environmentMapBuilder.put("BUILD_CONFIG_BRANCH_NAME", this.taskData.buildInfoModel.getBranchName());
        return null;
    }

    /**
     * 拉取代码后并缓存环境变量
     *
     * @return pull 的结果
     */
    private String pullAndCacheBuildEnv() {
        String pull = this.pull();
        if (pull == null) {
            BuildHistoryLog buildInfoModel = new BuildHistoryLog();
            buildInfoModel.setId(logId);
            buildInfoModel.setBuildEnvCache(taskData.environmentMapBuilder.toDataJsonStr());
            dbBuildHistoryLogService.updateById(buildInfoModel);
        }
        return pull;
    }

    /**
     * 拉取代码
     *
     * @return false 执行异常需要结束
     */
    private String pull() {
        RepositoryModel repositoryModel = taskData.repositoryModel;
        BuildInfoModel buildInfoModel = taskData.buildInfoModel;
        try {
            String msg;
            Integer repoTypeCode = repositoryModel.getRepoType();
            RepositoryModel.RepoType repoType = EnumUtil.likeValueOf(RepositoryModel.RepoType.class, repoTypeCode);
            Boolean checkRepositoryDiff = Optional.ofNullable(taskData.checkRepositoryDiff).orElse(buildExtraModule.getCheckRepositoryDiff());
            String repositoryLastCommitId = buildInfoModel.getRepositoryLastCommitId();
            if (repoType == RepositoryModel.RepoType.Git) {
                // git with password
                IPlugin plugin = PluginFactory.getPlugin("git-clone");
                Map<String, Object> map = repositoryModel.toMap();
                // 指定 clone 深度
                Integer cloneDepth = buildExtraModule.getCloneDepth();
                map.put("depth", cloneDepth);
                if (cloneDepth != null) {
                    // 使用系统
                    map.put("gitProcessType", "SystemGit");
                }
                Tuple tuple = (Tuple) plugin.execute("branchAndTagList", map);
                //GitUtil.getBranchAndTagList(repositoryModel);
                Assert.notNull(tuple, "获取仓库分支失败");
                map.put("reduceProgressRatio", buildExtConfig.getLogReduceProgressRatio());
                map.put("logWriter", logRecorder.getPrintWriter());
                map.put("savePath", gitFile);
                // 模糊匹配 标签
                String branchTagName = buildInfoModel.getBranchTagName();
                String[] result;
                if (StrUtil.isNotEmpty(branchTagName)) {
                    String newBranchTagName = fuzzyMatch(tuple.get(1), branchTagName);
                    if (StrUtil.isEmpty(newBranchTagName)) {
                        String format = StrUtil.format("{} Did not match the corresponding tag", branchTagName);
                        logRecorder.systemError(format);
                        return format;
                    }
                    // author bwcx_jzy 2022.11.28 map.put("branchName", newBranchName);
                    map.put("tagName", newBranchTagName);
                    //author bwcx_jzy 2022.11.28 buildEnv.put("BUILD_BRANCH_NAME", newBranchName);
                    taskData.environmentMapBuilder.put("BUILD_TAG_NAME", newBranchTagName);
                    // 标签拉取模式
                    logRecorder.system("repository tag [{}] clone pull from {}", branchTagName, newBranchTagName);
                    result = (String[]) plugin.execute("pullByTag", map);
                } else {
                    String branchName = buildInfoModel.getBranchName();
                    // 模糊匹配分支
                    String newBranchName = fuzzyMatch(tuple.get(0), branchName);
                    if (StrUtil.isEmpty(newBranchName)) {
                        String format = StrUtil.format("{} Did not match the corresponding branch", branchName);
                        logRecorder.systemError(format);
                        //buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, this.taskData.buildInfoModel.getBuildId(), BuildStatus.Error);
                        return format;
                    }
                    // 分支模式
                    map.put("branchName", newBranchName);
                    // 真实使用的分支名
                    taskData.environmentMapBuilder.put("BUILD_BRANCH_NAME", newBranchName);
                    logRecorder.system("repository [{}] clone pull from {}", branchName, newBranchName);
                    result = (String[]) plugin.execute("pull", map);
                }
                msg = result[1];
                // 判断hash 码和上次构建是否一致
                if (checkRepositoryDiff != null && checkRepositoryDiff) {
                    if (StrUtil.equals(repositoryLastCommitId, result[0])) {
                        // 如果一致，则不构建
                        String format = StrUtil.format("仓库代码没有任何变动终止本次构建：{} {}", result[0], msg);
                        logRecorder.systemError(format);
                        throw new DiyInterruptException(format);
                    }
                }
                taskData.repositoryLastCommitId = result[0];
            } else if (repoType == RepositoryModel.RepoType.Svn) {
                // svn
                Map<String, Object> map = repositoryModel.toMap();

                IPlugin plugin = PluginFactory.getPlugin("svn-clone");
                String[] result = (String[]) plugin.execute(gitFile, map);
                //msg = SvnKitUtil.checkOut(repositoryModel, gitFile);
                msg = ArrayUtil.get(result, 1);
                // 判断版本号和上次构建是否一致
                if (checkRepositoryDiff != null && checkRepositoryDiff) {
                    if (StrUtil.equals(repositoryLastCommitId, result[0])) {
                        // 如果一致，则不构建
                        String format = StrUtil.format("仓库代码没有任何变动终止本次构建：{}", result[0]);
                        logRecorder.systemError(format);
                        throw new DiyInterruptException(format);
                    }
                }
                taskData.repositoryLastCommitId = result[0];
            } else {
                String format = StrUtil.format("不支持的类型：{}", repoType.getDesc());
                logRecorder.systemError(format);
                return format;
            }
            taskData.environmentMapBuilder.put("BUILD_COMMIT_ID", taskData.repositoryLastCommitId);
            logRecorder.system(msg);
        } catch (Exception e) {
            throw Lombok.sneakyThrow(e);
        }
        // env file
        String attachEnv = this.buildExtraModule.getAttachEnv();
        Opt.ofBlankAble(attachEnv).ifPresent(s -> {
            UrlQuery of = UrlQuery.of(attachEnv, CharsetUtil.CHARSET_UTF_8);
            Map<CharSequence, CharSequence> queryMap = of.getQueryMap();
            logRecorder.system("读取附件变量：{} {}", attachEnv, CollUtil.size(queryMap));
            //
            Optional.ofNullable(queryMap).ifPresent(map -> {
                for (Map.Entry<CharSequence, CharSequence> entry : map.entrySet()) {
                    taskData.environmentMapBuilder.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
            });
            Map<String, String> envFileMap = FileUtils.readEnvFile(this.gitFile, s);
            taskData.environmentMapBuilder.putStr(envFileMap);
        });
        // 输出环境变量
        taskData.environmentMapBuilder.eachStr(logRecorder::system);
        return null;
    }

    private String dockerCommand() {
        BuildInfoModel buildInfoModel = taskData.buildInfoModel;
        String script = buildInfoModel.getScript();
        DockerYmlDsl dockerYmlDsl = DockerYmlDsl.build(script);
        String fromTag = dockerYmlDsl.getFromTag();
        // 根据 tag 查询
        List<DockerInfoModel> dockerInfoModels = dockerInfoService
            .queryByTag(buildInfoModel.getWorkspaceId(), fromTag);
        Map<String, Object> map = machineDockerServer.dockerParameter(dockerInfoModels);
        Assert.notNull(map, fromTag + " 没有可用的 docker server");
        taskData.dockerParameter = new HashMap<>(map);
        logRecorder.system("use docker {}", map.get("name"));
        String workingDir = "/home/jpom/";

        map.put("runsOn", dockerYmlDsl.getRunsOn());
        map.put("workingDir", workingDir);
        map.put("tempDir", JpomApplication.getInstance().getTempPath());
        String buildInfoModelId = buildInfoModel.getId();
        taskData.buildContainerId = "jpom-build-" + buildInfoModelId;
        map.put("dockerName", taskData.buildContainerId);
        map.put("logFile", logRecorder.getFile());
        //
        List<String> copy = ObjectUtil.defaultIfNull(dockerYmlDsl.getCopy(), new ArrayList<>());
        // 将仓库文件上传到容器
        copy.add(FileUtil.getAbsolutePath(this.gitFile) + StrUtil.COLON + workingDir + StrUtil.COLON + "true");
        map.put("copy", copy);
        map.put("binds", ObjectUtil.defaultIfNull(dockerYmlDsl.getBinds(), new ArrayList<>()));

        Map<String, String> dockerEnv = ObjectUtil.defaultIfNull(dockerYmlDsl.getEnv(), new HashMap<>(10));
        Map<String, String> env = taskData.environmentMapBuilder.environment();
        env.putAll(dockerEnv);
        env.put("JPOM_BUILD_ID", buildInfoModelId);
        env.put("JPOM_WORKING_DIR", workingDir);
        map.put("env", env);
        map.put("steps", dockerYmlDsl.getSteps());
        // 构建产物
        String resultDirFile = buildInfoModel.getResultDirFile();
        String resultFile = FileUtil.normalize(workingDir + StrUtil.SLASH + resultDirFile);
        map.put("resultFile", resultFile);
        // 产物输出目录
        File toFile = BuildUtil.getHistoryPackageFile(buildInfoModelId, buildInfoModel.getBuildId(), resultDirFile);
        map.put("resultFileOut", FileUtil.getAbsolutePath(toFile));
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        try {
            Object execute = plugin.execute("build", map);
            int resultCode = Convert.toInt(execute, -100);
            // 严格模式
            if (buildExtraModule.strictlyEnforce()) {
                return resultCode == 0 ? null : StrUtil.format("执行命令退出码非0，{}", resultCode);
            }
        } catch (Exception e) {
            logRecorder.error("构建调用容器异常", e);
            return e.getMessage();
        }
        return null;
    }

    /**
     * 执行构建命令
     *
     * @return false 执行异常需要结束
     */
    private String executeCommand() {
        BuildInfoModel buildInfoModel = taskData.buildInfoModel;
        Integer buildMode = buildInfoModel.getBuildMode();
        if (buildMode != null && buildMode == 1) {
            // 容器构建
            return this.dockerCommand();
        }
        String script = buildInfoModel.getScript();
        if (StrUtil.isEmpty(script)) {
            String info = "没有需要执行的命令";
            logRecorder.systemError(info);
            return info;
        }
        if (StrUtil.startWith(script, ServerConst.REF_SCRIPT)) {
            String scriptId = StrUtil.removePrefix(script, ServerConst.REF_SCRIPT);
            ScriptModel keyAndGlobal = scriptServer.getByKey(scriptId);
            Assert.notNull(keyAndGlobal, "请选择正确的脚本");
            script = keyAndGlobal.getContext();
            logRecorder.system("引入脚本内容：{}[{}]", keyAndGlobal.getName(), scriptId);
        }
        Map<String, String> environment = taskData.environmentMapBuilder.environment();

        InputStream templateInputStream = ExtConfigBean.getConfigResourceInputStream("/exec/template." + CommandUtil.SUFFIX);
        String s1 = IoUtil.readUtf8(templateInputStream);
        try {
            int waitFor = JpomApplication.getInstance()
                .execScript(s1 + script, file -> {
                    try {
                        return CommandUtil.execWaitFor(file, this.gitFile, environment, StrUtil.EMPTY, (s, process) -> logRecorder.info(s));
                    } catch (IOException | InterruptedException e) {
                        throw Lombok.sneakyThrow(e);
                    }
                });
            logRecorder.system("执行脚本的退出码是：{}", waitFor);
            // 判断是否为严格执行
            if (buildExtraModule.strictlyEnforce()) {
                return waitFor == 0 ? null : StrUtil.format("执行命令退出码非0，{}", waitFor);
            }
        } catch (Exception e) {
            logRecorder.error("执行异常", e);
            return e.getMessage();
        }
        return null;
    }

    /**
     * 打包发布
     *
     * @return false 执行需要结束
     */
    private String release() {
        BuildInfoModel buildInfoModel = taskData.buildInfoModel;
        UserModel userModel = taskData.userModel;
        // 发布文件
        ReleaseManage releaseManage = ReleaseManage.builder()
            .buildNumberId(buildInfoModel.getBuildId())
            .buildExtraModule(buildExtraModule)
            .userModel(userModel)
            .logId(logId)
            .dockerInfoService(dockerInfoService)
            .machineDockerServer(machineDockerServer)
            .buildEnv(taskData.environmentMapBuilder)
            .fileStorageService(fileStorageService)
            .buildExecuteService(buildExecuteService)
            .buildExtConfig(buildExtConfig)
            .logRecorder(logRecorder)
            .build();
        try {
            return releaseManage.start(resultFileSize -> taskData.resultFileSize = resultFileSize, buildInfoModel);
        } catch (Exception e) {
            throw Lombok.sneakyThrow(e);
        }
    }

    /**
     * 结束流程
     *
     * @return 流程执行是否成功
     */
    private String finish() {
        BuildInfoModel buildInfoModel1 = taskData.buildInfoModel;
        if (StrUtil.isNotEmpty(taskData.repositoryLastCommitId)) {
            BuildInfoModel buildInfoModel = new BuildInfoModel();
            buildInfoModel.setId(buildInfoModel1.getId());
            buildInfoModel.setRepositoryLastCommitId(taskData.repositoryLastCommitId);
            buildService.updateById(buildInfoModel);
        }
        //
        BuildStatus buildStatus = buildInfoModel1.getReleaseMethod() != BuildReleaseMethod.No.getCode() ? BuildStatus.PubSuccess : BuildStatus.Success;
        buildExecuteService.updateStatus(buildInfoModel1.getId(), this.logId, this.taskData.buildInfoModel.getBuildId(), buildStatus, "任务正常结束");
        // 判断是否保留产物
        Boolean saveBuildFile = this.buildExtraModule.getSaveBuildFile();
        if (saveBuildFile != null && !saveBuildFile) {
            // 删除 产物文件夹
            File historyPackageFile = BuildUtil.getHistoryPackageFile(buildExtraModule.getId(), buildInfoModel1.getBuildId(), StrUtil.SLASH);
            CommandUtil.systemFastDel(historyPackageFile);
            // 被删除后
            this.taskData.resultFileSize = 0L;
        }
        return null;
    }

    private Map<String, IProcessItem> createProcess() {
        // 初始化构建流程 准备构建->拉取仓库代码->执行构建命令->打包产物->发布产物->构建结束
        Map<String, IProcessItem> suppliers = new LinkedHashMap<>(10);
        suppliers.put("startReady", new IProcessItem() {
            @Override
            public String name() {
                return "准备构建";
            }

            @Override
            public String execute() {
                return BuildExecuteManage.this.startReady();
            }
        });
        suppliers.put("pull", new IProcessItem() {
            @Override
            public String name() {
                return "拉取仓库代码";
            }

            @Override
            public String execute() {
                return BuildExecuteManage.this.pullAndCacheBuildEnv();
            }
        });
        suppliers.put("executeCommand", new IProcessItem() {
            @Override
            public String name() {
                return "执行构建命令";
            }

            @Override
            public String execute() {
                return BuildExecuteManage.this.executeCommand();
            }
        });
        suppliers.put("packageFile", new IProcessItem() {
            @Override
            public String name() {
                return "打包产物";
            }

            @Override
            public String execute() {
                return BuildExecuteManage.this.packageFile();
            }
        });
        suppliers.put("release", new IProcessItem() {
            @Override
            public String name() {
                return "发布产物";
            }

            @Override
            public String execute() {
                return BuildExecuteManage.this.release();
            }
        });
        suppliers.put("finish", new IProcessItem() {
            @Override
            public String name() {
                return "构建结束";
            }

            @Override
            public String execute() {
                return BuildExecuteManage.this.finish();
            }
        });
        return suppliers;
    }

    /**
     * 清理构建资源
     */
    private void clearResources() {
        //
        BuildInfoModel buildInfoModel1 = taskData.buildInfoModel;
        File historyPackageZipFile = BuildUtil.getHistoryPackageZipFile(buildExtraModule.getId(), buildInfoModel1.getBuildId());
        CommandUtil.systemFastDel(historyPackageZipFile);
        // 计算文件占用大小
        File file = logRecorder.getFile();
        long size = FileUtil.size(file);

        BuildHistoryLog buildInfoModel = new BuildHistoryLog();
        buildInfoModel.setId(logId);
        buildInfoModel.setResultFileSize(taskData.resultFileSize);
        buildInfoModel.setBuildLogFileSize(size);
        dbBuildHistoryLogService.updateById(buildInfoModel);
    }

    public void runTask() {
        currentThread = Thread.currentThread();
        logRecorder.system("开始执行构建任务,任务等待时间：{}", DateUtil.formatBetween(SystemClock.now() - submitTaskTime));

        // 判断任务是否被取消
        BuildHistoryLog buildHistoryLog = dbBuildHistoryLogService.getByKey(this.logId);
        if (buildHistoryLog == null) {
            logRecorder.systemError("构建记录丢失,无法继续构建");
            return;
        }
        if (buildHistoryLog.getStatus() == null || buildHistoryLog.getStatus() == BuildStatus.Cancel.getCode()) {
            logRecorder.systemError("构建状态异常或者被取消");
            return;
        }
        BuildInfoModel buildInfoModel = this.taskData.buildInfoModel;
        buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, buildInfoModel.getBuildId(), BuildStatus.Ing, "开始构建,构建线程执行");
        //
        Map<String, IProcessItem> processItemMap = this.createProcess();
        // 依次执行流程，发生异常结束整个流程
        String processName = StrUtil.EMPTY;
        long startTime = SystemClock.now();
        if (taskData.triggerBuildType == 2) {
            // 系统触发构建
            BaseServerController.resetInfo(UserModel.EMPTY);
        } else {
            BaseServerController.resetInfo(taskData.userModel);
        }

        try {
            boolean stop = false;
            for (Map.Entry<String, IProcessItem> stringSupplierEntry : processItemMap.entrySet()) {
                processName = stringSupplierEntry.getKey();
                IProcessItem processItem = stringSupplierEntry.getValue();
                //
                long processItemStartTime = SystemClock.now();
                logRecorder.system("开始执行 {}流程", processItem.name());
                String interruptMsg = this.asyncWebHooks(processName);
                if (interruptMsg != null) {
                    // 事件脚本中断构建流程
                    logRecorder.system("执行中断 {} 流程,原因事件脚本中断", processItem.name());
                    this.asyncWebHooks("stop", "process", processName, "statusMsg", interruptMsg);
                    buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, buildInfoModel.getBuildId(), BuildStatus.Interrupt, interruptMsg);
                    stop = true;
                    break;
                }
                String errorMsg = processItem.execute();
                if (errorMsg != null) {
                    // 有条件结束构建流程
                    logRecorder.systemError("执行异常[{}]流程：{}", processItem.name(), errorMsg);
                    this.asyncWebHooks("stop", "process", processName, "statusMsg", errorMsg);
                    buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, buildInfoModel.getBuildId(), BuildStatus.Error, errorMsg);
                    stop = true;
                    break;
                }
                logRecorder.system("执行结束 {}流程,耗时：{}", processItem.name(), DateUtil.formatBetween(SystemClock.now() - processItemStartTime));
            }
            if (!stop) { // 没有执行 stop
                this.asyncWebHooks("success");
            }
        } catch (DiyInterruptException diyInterruptException) {
            // 主动中断
            this.asyncWebHooks("stop", "process", processName);
            buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, buildInfoModel.getBuildId(), BuildStatus.Interrupt, diyInterruptException.getMessage());
        } catch (java.util.concurrent.CancellationException interruptException) {
            // 异常中断
            this.asyncWebHooks("stop", "process", processName);
            buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, buildInfoModel.getBuildId(), BuildStatus.Interrupt, "系统中断异常");
        } catch (Exception e) {
            buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, buildInfoModel.getBuildId(), BuildStatus.Error, e.getMessage());
            logRecorder.error("构建失败:" + processName, e);
            this.asyncWebHooks("error", "process", processName, "statusMsg", e.getMessage());
        } finally {
            this.clearResources();
            logRecorder.system("构建结束-累计耗时:{}", DateUtil.formatBetween(SystemClock.now() - startTime));
            this.asyncWebHooks("done");
            BaseServerController.removeAll();
        }
    }

    public void run() {
        BuildInfoModel buildInfoModel = this.taskData.buildInfoModel;
        try {
            this.runTask();
        } catch (Exception e) {
            log.error("构建发生未知错误", e);
        } finally {
            BUILD_MANAGE_MAP.remove(buildInfoModel.getId());
        }
    }

    /**
     * 执行 webhooks 通知
     *
     * @param type  类型
     * @param other 其他参数
     * @return 是否还继续整个构建流程
     */
    private String asyncWebHooks(String type, Object... other) {
        BuildInfoModel buildInfoModel = taskData.buildInfoModel;
        Map<String, Object> map = new HashMap<>(10);
        //
        for (int i = 0; i < other.length; i += 2) {
            map.put(other[i].toString(), other[i + 1]);
        }
        map.put("buildId", buildInfoModel.getId());
        map.put("buildNumberId", this.taskData.buildInfoModel.getBuildId());
        map.put("buildName", buildInfoModel.getName());
        map.put("buildSourceFile", FileUtil.getAbsolutePath(this.gitFile));
        map.put("type", type);
        map.put("triggerBuildType", taskData.triggerBuildType);
        map.put("triggerTime", SystemClock.now());
        String triggerUser = Optional.ofNullable(taskData.userModel).map(BaseIdModel::getId).orElse(UserModel.SYSTEM_ADMIN);
        map.put("triggerUser", triggerUser);
        String resultDirFile = buildExtraModule.getResultDirFile();
        map.put("buildResultDirFile", resultDirFile);
        map.put("buildResultFile", BuildUtil.getHistoryPackageFile(buildInfoModel.getId(), this.taskData.buildInfoModel.getBuildId(), resultDirFile));

        Opt.ofBlankAble(buildInfoModel.getWebhook())
            .ifPresent(s ->
                ThreadUtil.execute(() -> {
                    try {
                        IPlugin plugin = PluginFactory.getPlugin("webhook");
                        plugin.execute(s, map);
                    } catch (Exception e) {
                        log.error("WebHooks 调用错误", e);
                    }
                })
            );
        // 执行对应的事件脚本
        try {
            return this.noticeScript(type, map);
        } catch (Exception e) {
            log.error("noticeScript 调用错误", e);
            logRecorder.error("执行事件脚本错误", e);
            // 执行事件脚本发送异常不终止构建流程
            return null;
        }
    }

    /**
     * 执行事件脚本
     *
     * @param type 事件类型
     * @param map  相关参数
     * @return 是否还继续整个构建流程
     */
    private String noticeScript(String type, Map<String, Object> map) {
        String noticeScriptId = this.buildExtraModule.getNoticeScriptId();
        if (StrUtil.isEmpty(noticeScriptId)) {
            return null;
        }
        ScriptModel scriptModel = scriptServer.getByKey(noticeScriptId);
        if (scriptModel == null) {
            logRecorder.systemWarning("事件脚本不存在:{} {}", type, noticeScriptId);
            return null;
        }
        // 判断是否包含需要执行的事件
        if (!StrUtil.containsAnyIgnoreCase(scriptModel.getDescription(), type, "all")) {
            log.warn("忽略执行事件脚本 {} {} {}", type, scriptModel.getName(), noticeScriptId);
            return null;
        }
        logRecorder.system("开始执行事件脚本： {}", type);
        // 环境变量
        Map<String, String> environment = taskData.environmentMapBuilder.environment(map);
        ScriptExecuteLogModel logModel = scriptExecuteLogServer.create(scriptModel, 1, this.taskData.buildInfoModel.getWorkspaceId());
        File logFile = scriptModel.logFile(logModel.getId());
        File scriptFile = null;
        LogRecorder scriptLog = LogRecorder.builder().file(logFile).build();
        final String[] lastMsg = new String[1];
        try {
            // 创建执行器
            scriptFile = scriptModel.scriptFile();
            int waitFor = JpomApplication.getInstance().execScript(scriptModel.getContext(), file -> {
                try {
                    // 输出环境变量
                    taskData.environmentMapBuilder.eachStr(s -> {
                        logRecorder.system(s);
                        scriptLog.info(s);
                    }, map);
                    //
                    return CommandUtil.execWaitFor(file, null, environment, null, (s, process) -> {
                        logRecorder.info(s);
                        scriptLog.info(s);
                        lastMsg[0] = s;
                    });
                } catch (IOException | InterruptedException e) {
                    throw Lombok.sneakyThrow(e);
                }
            });
            logRecorder.system("执行 {} 类型脚本的退出码是：{}", type, waitFor);
            // 判断是否为严格执行
            if (buildExtraModule.strictlyEnforce() && waitFor != 0) {
                //logRecorder.systemError("严格执行模式，事件脚本返回状态码异常");
                return "严格执行模式，事件脚本返回状态码异常," + waitFor;
            }
            if (StrUtil.startWithIgnoreCase(lastMsg[0], "interrupt " + type)) {
                return "事件脚本中断：" + lastMsg[0];
            }
            return null;
        } finally {
            try {
                FileUtil.del(scriptFile);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 取消构建
     *
     * @param id id
     * @return bool
     */
    public static boolean cancelTaskById(String id) {
        return Optional.ofNullable(BuildExecuteManage.BUILD_MANAGE_MAP.get(id)).map(buildExecuteManage1 -> {
            buildExecuteManage1.cancelTask("手动取消任务");
            return true;
        }).orElse(false);
    }

    /**
     * 模糊匹配
     *
     * @param list    待匹配待列表
     * @param pattern 迷糊的表达式
     * @return 匹配到到值
     */
    private static String fuzzyMatch(List<String> list, String pattern) {
        Assert.notEmpty(list, "仓库没有任何分支或者标签");
        if (AntPathUtil.ANT_PATH_MATCHER.isPattern(pattern)) {
            List<String> collect = list.stream().filter(s -> AntPathUtil.ANT_PATH_MATCHER.match(pattern, s)).collect(Collectors.toList());
            return CollUtil.getFirst(collect);
        }
        return pattern;
    }
}
