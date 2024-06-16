/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
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
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.*;
import cn.hutool.extra.spring.SpringUtil;
import cn.keepbx.jpom.model.BaseIdModel;
import cn.keepbx.jpom.plugins.IPlugin;
import lombok.Builder;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.i18n.I18nThreadUtil;
import org.dromara.jpom.configuration.BuildExtConfig;
import org.dromara.jpom.exception.LogRecorderCloseException;
import org.dromara.jpom.func.assets.server.MachineDockerServer;
import org.dromara.jpom.func.files.service.FileStorageService;
import org.dromara.jpom.model.data.BuildInfoModel;
import org.dromara.jpom.model.data.CommandExecLogModel;
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
import org.dromara.jpom.util.*;
import org.dromara.jpom.webhook.DefaultWebhookPluginImpl;
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
     * 缓存构建中
     */
    public static final Map<String, BuildExecuteManage> BUILD_MANAGE_MAP = new SafeConcurrentHashMap<>();

    private final TaskData taskData;
    private final BuildExtraModule buildExtraModule;
    private final String logId;
    //
    private Process process;
    private LogRecorder logRecorder;
    private File gitFile;
    private Thread currentThread;
    private ReleaseManage releaseManage;
    private String language;

    /**
     * 提交任务时间
     */
    private Long submitTaskTime;

    private static BuildExecuteService buildExecuteService;
    private static ScriptServer scriptServer;
    private static ScriptExecuteLogServer scriptExecuteLogServer;
    private static BuildInfoService buildService;
    private static DbBuildHistoryLogService dbBuildHistoryLogService;
    private static DockerInfoService dockerInfoService;
    private static MachineDockerServer machineDockerServer;
    private static BuildExtConfig buildExtConfig;
    private static FileStorageService fileStorageService;
    private static BuildExecutorPoolService buildExecutorPoolService;

    private void loadService() {
        buildExecuteService = ObjectUtil.defaultIfNull(buildExecuteService, () -> SpringUtil.getBean(BuildExecuteService.class));
        scriptServer = ObjectUtil.defaultIfNull(scriptServer, () -> SpringUtil.getBean(ScriptServer.class));
        scriptExecuteLogServer = ObjectUtil.defaultIfNull(scriptExecuteLogServer, () -> SpringUtil.getBean(ScriptExecuteLogServer.class));
        buildService = ObjectUtil.defaultIfNull(buildService, () -> SpringUtil.getBean(BuildInfoService.class));
        dbBuildHistoryLogService = ObjectUtil.defaultIfNull(dbBuildHistoryLogService, () -> SpringUtil.getBean(DbBuildHistoryLogService.class));
        dockerInfoService = ObjectUtil.defaultIfNull(dockerInfoService, () -> SpringUtil.getBean(DockerInfoService.class));
        machineDockerServer = ObjectUtil.defaultIfNull(machineDockerServer, () -> SpringUtil.getBean(MachineDockerServer.class));
        buildExtConfig = ObjectUtil.defaultIfNull(buildExtConfig, () -> SpringUtil.getBean(BuildExtConfig.class));
        fileStorageService = ObjectUtil.defaultIfNull(fileStorageService, () -> SpringUtil.getBean(FileStorageService.class));
        buildExecutorPoolService = ObjectUtil.defaultIfNull(buildExecutorPoolService, () -> SpringUtil.getBean(BuildExecutorPoolService.class));
    }

    /**
     * 正在构建的数量
     *
     * @return 构建数量
     */
    public static Set<String> buildKeys() {
        return BUILD_MANAGE_MAP.keySet();
    }


    /**
     * 提交任务
     */
    public void submitTask() {
        this.loadService();
        submitTaskTime = SystemClock.now();
        language = I18nMessageUtil.getLanguageByRequest();
        // 创建线程池
        ThreadPoolExecutor threadPoolExecutor = buildExecutorPoolService.getThreadPoolExecutor();
        //
        BuildInfoModel buildInfoModel = taskData.buildInfoModel;
        File logFile = BuildUtil.getLogFile(buildInfoModel.getId(), buildInfoModel.getBuildId());
        this.logRecorder = LogRecorder.builder().file(logFile).build();
        //
        int queueSize = threadPoolExecutor.getQueue().size();
        int size = BUILD_MANAGE_MAP.size();
        logRecorder.system(I18nMessageUtil.get("i18n.build_task_count_and_queue_count.f0b6"), size, queueSize,
            size > buildExtConfig.getPoolSize() ? I18nMessageUtil.get("i18n.build_task_queue_waiting.5f06") : StrUtil.EMPTY);
        //BuildInfoManage manage = new BuildInfoManage(taskData);
        BUILD_MANAGE_MAP.put(buildInfoModel.getId(), this);
        threadPoolExecutor.execute(this);
    }

    /**
     * 取消任务(拒绝执行)
     */
    public void rejectedExecution() {
        ThreadPoolExecutor threadPoolExecutor = buildExecutorPoolService.getThreadPoolExecutor();
        int queueSize = threadPoolExecutor.getQueue().size();
        int limitPoolSize = threadPoolExecutor.getPoolSize();
        int corePoolSize = threadPoolExecutor.getCorePoolSize();
        String format = StrUtil.format(I18nMessageUtil.get("i18n.build_status_message.42a7"), BUILD_MANAGE_MAP.size(), queueSize, limitPoolSize, corePoolSize);
        logRecorder.system(format);
        this.cancelTask(format);
    }

    /**
     * 取消任务
     */
    private void cancelTask(String desc) {
        CommandUtil.kill(process);
        ApacheExecUtil.kill(this.logId);
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
                log.warn(I18nMessageUtil.get("i18n.build_resource_cleanup_failed.c4cf"), e);
            }
        }
        String buildId = taskData.buildInfoModel.getId();
        buildExecuteService.updateStatus(buildId, logId, taskData.buildInfoModel.getBuildId(), BuildStatus.Cancel, desc);
        Optional.ofNullable(currentThread).ifPresent(Thread::interrupt);
        BUILD_MANAGE_MAP.remove(buildId);
        IoUtil.close(logRecorder);
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
                String format = StrUtil.format(I18nMessageUtil.get("i18n.non_existent_build_product.1df4"), resultDirFileAction.getPath());
                logRecorder.systemError(format);
                return format;
            }
            logRecorder.system(I18nMessageUtil.get("i18n.backup_product.53c0"), resultDirFileAction.getPath(), buildInfoModel.getBuildId());
            return null;
        }
        if (resultDirFileAction.getType() == ResultDirFileAction.Type.ANT_PATH) {
            // 通配模式
            List<String> paths = AntPathUtil.antPathMatcher(this.gitFile, resultDirFileAction.getPath());
            int matcherSize = CollUtil.size(paths);
            if (matcherSize <= 0) {
                String format = StrUtil.format(I18nMessageUtil.get("i18n.no_matching_files.b7a6"), resultDirFileAction.getPath());
                logRecorder.systemError(format);
                return format;
            }
            logRecorder.system(I18nMessageUtil.get("i18n.fuzzy_match_files.139d"), resultDirFileAction.getPath(), matcherSize);
            String antSubMatch = resultDirFileAction.antSubMatch();
            ResultDirFileAction.AntFileUploadMode antFileUploadMode = resultDirFileAction.getAntFileUploadMode();
            Assert.notNull(antFileUploadMode, I18nMessageUtil.get("i18n.file_upload_mode_not_configured.b3b2"));
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
                                    // 结束本次循环
                                    break;
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
                        throw new IllegalStateException(I18nMessageUtil.get("i18n.unsupported_mode.a3d3") + antFileUploadMode);
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
                String format = StrUtil.format(I18nMessageUtil.get("i18n.no_matching_files.b7a6"), antSubMatch);
                logRecorder.systemError(format);
                return format;
            }
            logRecorder.system(I18nMessageUtil.get("i18n.secondary_directory_match.0aec"), antSubMatch, subMatchCount, antFileUploadMode);
            // 更新产物路径为普通路径
            dbBuildHistoryLogService.updateResultDirFile(this.logId, StrUtil.SLASH);
            buildInfoModel.setResultDirFile(StrUtil.SLASH);
            this.buildExtraModule.setResultDirFile(StrUtil.SLASH);
        } else if (resultDirFileAction.getType() == ResultDirFileAction.Type.ORIGINAL) {
            File file = FileUtil.file(this.gitFile, resultDirFile);
            if (!file.exists()) {
                String format = StrUtil.format(I18nMessageUtil.get("i18n.non_existent_build_product.1df4"), resultDirFile);
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
            logRecorder.system(I18nMessageUtil.get("i18n.cumulative_filter_files.448d"), excludeReleaseAnt, excludeReleaseAntCount[0]);
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
        logRecorder.system(I18nMessageUtil.get("i18n.start_building_with_number_and_path.c41c"), buildInfoModel.getBuildId(), FileUtil.getAbsolutePath(this.gitFile));
        if (delay != null && delay > 0) {
            // 延迟执行
            logRecorder.system(I18nMessageUtil.get("i18n.wait_for_seconds.ff7b"), delay);
            ThreadUtil.sleep(delay, TimeUnit.SECONDS);
        }
        // 删除缓存
        Boolean cacheBuild = this.buildExtraModule.getCacheBuild();
        if (cacheBuild != null && !cacheBuild) {
            logRecorder.system(I18nMessageUtil.get("i18n.delete_build_cache.c7f3"));
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
                Assert.notNull(tuple, I18nMessageUtil.get("i18n.get_repository_branch_failure.37cc"));
                map.put("reduceProgressRatio", buildExtConfig.getLogReduceProgressRatio());
                map.put("logWriter", logRecorder.getPrintWriter());
                map.put("savePath", gitFile);
                map.put("strictlyEnforce", buildExtraModule.strictlyEnforce());
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
                // 判断是否执行失败
                String errorMsg = ArrayUtil.get(result, 2);
                if (errorMsg != null) {
                    logRecorder.systemError(I18nMessageUtil.get("i18n.pull_code_failed.70d6"), errorMsg);
                    return errorMsg;
                }
                // 判断hash 码和上次构建是否一致
                if (checkRepositoryDiff != null && checkRepositoryDiff) {
                    if (StrUtil.equals(repositoryLastCommitId, result[0])) {
                        // 如果一致，则不构建
                        String format = StrUtil.format(I18nMessageUtil.get("i18n.no_changes_in_repository_code_with_details.fd9f"), result[0], msg);
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
                        String format = StrUtil.format(I18nMessageUtil.get("i18n.no_changes_in_repository_code.b1aa"), result[0]);
                        logRecorder.systemError(format);
                        throw new DiyInterruptException(format);
                    }
                }
                taskData.repositoryLastCommitId = result[0];
            } else {
                String format = StrUtil.format(I18nMessageUtil.get("i18n.unsupported_type_with_placeholder.71a2"), repoType.getDesc());
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
            logRecorder.system(I18nMessageUtil.get("i18n.read_additional_variables.5eb0"), attachEnv, CollUtil.size(queryMap));
            //
            Optional.ofNullable(queryMap).ifPresent(map -> {
                for (Map.Entry<CharSequence, CharSequence> entry : map.entrySet()) {
                    CharSequence value = entry.getValue();
                    if (value != null) {
                        taskData.environmentMapBuilder.put(String.valueOf(entry.getKey()), String.valueOf(value));
                    }
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
        Assert.notNull(map, fromTag + I18nMessageUtil.get("i18n.no_available_docker_server.9fc6"));
        taskData.dockerParameter = new HashMap<>(map);
        logRecorder.system("use docker {}", map.get("name"));
        logRecorder.info("");
        String workingDir = "/home/jpom/";

        map.put("runsOn", dockerYmlDsl.getRunsOn());
        map.put("workingDir", workingDir);
        map.put("hostConfig", dockerYmlDsl.getHostConfig());
        map.put("tempDir", JpomApplication.getInstance().getTempPath());
        String buildInfoModelId = buildInfoModel.getId();
        taskData.buildContainerId = "jpom-build-" + buildInfoModelId;
        map.put("dockerName", taskData.buildContainerId);
        map.put("logRecorder", logRecorder);
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
                return resultCode == 0 ? null : StrUtil.format(I18nMessageUtil.get("i18n.command_non_zero_exit_code.a6e1"), resultCode);
            }
        } catch (Exception e) {
            logRecorder.error(I18nMessageUtil.get("i18n.build_call_container_exception.6e04"), e);
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
            String info = I18nMessageUtil.get("i18n.no_command_to_execute.340b");
            logRecorder.systemError(info);
            return info;
        }
        if (StrUtil.startWith(script, ServerConst.REF_SCRIPT)) {
            String scriptId = StrUtil.removePrefix(script, ServerConst.REF_SCRIPT);
            ScriptModel keyAndGlobal = scriptServer.getByKey(scriptId);
            Assert.notNull(keyAndGlobal, I18nMessageUtil.get("i18n.select_correct_script.ff2d"));
            script = keyAndGlobal.getContext();
            logRecorder.system(I18nMessageUtil.get("i18n.introducing_script_content.a55b"), keyAndGlobal.getName(), scriptId);
        }
        Map<String, String> environment = taskData.environmentMapBuilder.environment();

        InputStream templateInputStream = ExtConfigBean.getConfigResourceInputStream("/exec/template." + CommandUtil.SUFFIX);
        String s1 = IoUtil.readUtf8(templateInputStream);
        try {
            int waitFor = JpomApplication.getInstance()
                .execScript(s1 + script, file -> {
                    try {
                        String execMode = this.buildExtraModule.getCommandExecMode();
                        // ApacheExecUtil.exec
                        if (StrUtil.equals(execMode, "apache_exec")) {
                            return ApacheExecUtil.exec(this.logId, file, this.gitFile, environment, StrUtil.EMPTY, logRecorder);
                        } else {
                            return CommandUtil.execWaitFor(file, this.gitFile, environment, StrUtil.EMPTY, (s, process) -> {
                                BuildExecuteManage.this.process = process;
                                logRecorder.info(s);
                            });
                        }
                    } catch (IOException | InterruptedException e) {
                        throw Lombok.sneakyThrow(e);
                    }
                });
            BuildExecuteManage.this.process = null;
            logRecorder.system(I18nMessageUtil.get("i18n.script_exit_code.716e"), waitFor);
            // 判断是否为严格执行
            if (buildExtraModule.strictlyEnforce()) {
                return waitFor == 0 ? null : StrUtil.format(I18nMessageUtil.get("i18n.command_non_zero_exit_code.a6e1"), waitFor);
            }
        } catch (Exception e) {
            logRecorder.error(I18nMessageUtil.get("i18n.execution_exception.b0d5"), e);
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
        this.releaseManage = ReleaseManage.builder()
            .buildNumberId(buildInfoModel.getBuildId())
            .buildExtraModule(buildExtraModule)
            .userModel(userModel)
            .logId(logId)
            .buildEnv(taskData.environmentMapBuilder)
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
        buildExecuteService.updateStatus(buildInfoModel1.getId(), this.logId, this.taskData.buildInfoModel.getBuildId(), buildStatus, I18nMessageUtil.get("i18n.task_ended_successfully.e176"));
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
                return I18nMessageUtil.get("i18n.prepare_to_build.1830");
            }

            @Override
            public String execute() {
                return BuildExecuteManage.this.startReady();
            }
        });
        suppliers.put("pull", new IProcessItem() {
            @Override
            public String name() {
                return I18nMessageUtil.get("i18n.pull_repository_code.3f51");
            }

            @Override
            public String execute() {
                return BuildExecuteManage.this.pullAndCacheBuildEnv();
            }
        });
        suppliers.put("executeCommand", new IProcessItem() {
            @Override
            public String name() {
                return I18nMessageUtil.get("i18n.build_command_execution.a55c");
            }

            @Override
            public String execute() {
                return BuildExecuteManage.this.executeCommand();
            }
        });
        suppliers.put("packageFile", new IProcessItem() {
            @Override
            public String name() {
                return I18nMessageUtil.get("i18n.package_product.bfbb");
            }

            @Override
            public String execute() {
                return BuildExecuteManage.this.packageFile();
            }
        });
        suppliers.put("release", new IProcessItem() {
            @Override
            public String name() {
                return I18nMessageUtil.get("i18n.publish_product.5925");
            }

            @Override
            public String execute() {
                return BuildExecuteManage.this.release();
            }
        });
        suppliers.put("finish", new IProcessItem() {
            @Override
            public String name() {
                return I18nMessageUtil.get("i18n.build_finished.7f38");
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
        long size = logRecorder.size();
        BuildHistoryLog buildInfoModel = new BuildHistoryLog();
        buildInfoModel.setId(logId);
        buildInfoModel.setResultFileSize(taskData.resultFileSize);
        buildInfoModel.setBuildLogFileSize(size);
        dbBuildHistoryLogService.updateById(buildInfoModel);
    }

    public void runTask() {
        currentThread = Thread.currentThread();
        logRecorder.system(I18nMessageUtil.get("i18n.start_executing_build_task.a5ac"), DateUtil.formatBetween(SystemClock.now() - submitTaskTime));

        // 判断任务是否被取消
        BuildHistoryLog buildHistoryLog = dbBuildHistoryLogService.getByKey(this.logId);
        if (buildHistoryLog == null) {
            logRecorder.systemError(I18nMessageUtil.get("i18n.build_record_lost.f6a2"));
            return;
        }
        if (buildHistoryLog.getStatus() == null || buildHistoryLog.getStatus() == BuildStatus.Cancel.getCode()) {
            logRecorder.systemError(I18nMessageUtil.get("i18n.build_status_abnormal.8ca1"));
            return;
        }
        BuildInfoModel buildInfoModel = this.taskData.buildInfoModel;
        buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, buildInfoModel.getBuildId(), BuildStatus.Ing, I18nMessageUtil.get("i18n.start_building_with_thread_execution.83cd"));
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
                logRecorder.system(I18nMessageUtil.get("i18n.start_executing_process.9cb8"), processItem.name());
                String interruptMsg = this.asyncWebHooks(processName);
                if (interruptMsg != null) {
                    // 事件脚本中断构建流程
                    logRecorder.system(I18nMessageUtil.get("i18n.execution_interrupted_reason.e3d7"), processItem.name());
                    this.asyncWebHooks("stop", "process", processName, "statusMsg", interruptMsg);
                    buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, buildInfoModel.getBuildId(), BuildStatus.Interrupt, interruptMsg);
                    stop = true;
                    break;
                }
                String errorMsg = processItem.execute();
                if (errorMsg != null) {
                    // 有条件结束构建流程
                    logRecorder.systemError(I18nMessageUtil.get("i18n.execution_exception_with_flow.6d4b"), processItem.name(), errorMsg);
                    this.asyncWebHooks("stop", "process", processName, "statusMsg", errorMsg);
                    buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, buildInfoModel.getBuildId(), BuildStatus.Error, errorMsg);
                    stop = true;
                    break;
                }
                logRecorder.system(I18nMessageUtil.get("i18n.execution_ended_with_duration.a59b"), processItem.name(), DateUtil.formatBetween(SystemClock.now() - processItemStartTime));
            }
            if (!stop) {
                // 没有执行 stop
                this.asyncWebHooks("success");
            }
        } catch (LogRecorderCloseException logRecorderCloseException) {
            log.warn(I18nMessageUtil.get("i18n.build_log_recorder_closed.1cc7"), processName);
        } catch (DiyInterruptException diyInterruptException) {
            // 主动中断
            this.asyncWebHooks("stop", "process", processName, "statusMsg", diyInterruptException.getMessage());
            buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, buildInfoModel.getBuildId(), BuildStatus.Interrupt, diyInterruptException.getMessage());
        } catch (java.util.concurrent.CancellationException interruptException) {
            // 异常中断
            String string = I18nMessageUtil.get("i18n.system_interruption.e37c");
            this.asyncWebHooks("stop", "process", processName, "statusMsg", string);
            buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, buildInfoModel.getBuildId(), BuildStatus.Interrupt, string);
        } catch (Exception e) {
            buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, buildInfoModel.getBuildId(), BuildStatus.Error, e.getMessage());
            logRecorder.error(I18nMessageUtil.get("i18n.build_failed.a79a") + processName, e);
            this.asyncWebHooks("error", "process", processName, "statusMsg", e.getMessage());
        } finally {
            this.clearResources();
            logRecorder.system(I18nMessageUtil.get("i18n.build_finished_duration.7f7c"), DateUtil.formatBetween(SystemClock.now() - startTime));
            this.asyncWebHooks("done");
            IoUtil.close(logRecorder);
            BaseServerController.removeAll();
        }
    }

    public void run() {
        BuildInfoModel buildInfoModel = this.taskData.buildInfoModel;
        try {
            I18nMessageUtil.setLanguage(this.language);
            this.runTask();
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.build_unknown_error.dad6"), e);
        } finally {
            BUILD_MANAGE_MAP.remove(buildInfoModel.getId());
            I18nMessageUtil.clearLanguage();
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
                I18nThreadUtil.execute(() -> {
                    try {
                        IPlugin plugin = PluginFactory.getPlugin("webhook");
                        map.put("JPOM_WEBHOOK_EVENT", DefaultWebhookPluginImpl.WebhookEvent.BUILD);
                        plugin.execute(s, map);
                    } catch (Exception e) {
                        log.error(I18nMessageUtil.get("i18n.webhooks_invocation_error.9792"), e);
                    }
                })
            );
        // 执行对应的事件脚本
        try {
            return this.noticeScript(type, map);
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.notice_script_invocation_error.9002"), e);
            logRecorder.error(I18nMessageUtil.get("i18n.execute_event_script_error.7c69"), e);
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
        List<String> list = StrUtil.splitTrim(noticeScriptId, StrUtil.COMMA);
        for (String noticeScriptIdItem : list) {
            String error = this.noticeScript(noticeScriptIdItem, type, map);
            if (error != null) {
                return error;
            }
        }
        return null;
    }

    /**
     * 执行事件脚本
     *
     * @param noticeScriptId 脚本id
     * @param type           事件类型
     * @param map            相关参数
     * @return 是否还继续整个构建流程
     */
    private String noticeScript(String noticeScriptId, String type, Map<String, Object> map) {
        ScriptModel scriptModel = scriptServer.getByKey(noticeScriptId);
        if (scriptModel == null) {
            logRecorder.systemWarning(I18nMessageUtil.get("i18n.event_script_does_not_exist.e726"), type, noticeScriptId);
            return null;
        }
        // 判断是否包含需要执行的事件
        if (!StrUtil.containsAnyIgnoreCase(scriptModel.getDescription(), type, "all")) {
            log.warn(I18nMessageUtil.get("i18n.ignore_execution_event_script.8872"), type, scriptModel.getName(), noticeScriptId);
            return null;
        }
        logRecorder.system(I18nMessageUtil.get("i18n.start_executing_event_script.377e"), type);
        // 环境变量
        Map<String, String> environment = taskData.environmentMapBuilder.environment(map);
        ScriptExecuteLogModel logModel = scriptExecuteLogServer.create(scriptModel, 3, this.taskData.buildInfoModel.getWorkspaceId());
        File logFile = scriptModel.logFile(logModel.getId());
        File scriptFile = null;
        LogRecorder scriptLog = LogRecorder.builder().file(logFile).build();
        final String[] lastMsg = new String[1];
        try {
            // 创建执行器
            scriptFile = scriptExecuteLogServer.toExecLogFile(scriptModel);
            scriptExecuteLogServer.updateStatus(logModel.getId(), CommandExecLogModel.Status.ING);
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
                    scriptExecuteLogServer.updateStatus(logModel.getId(), CommandExecLogModel.Status.ERROR);
                    throw Lombok.sneakyThrow(e);
                }
            });
            logRecorder.system(I18nMessageUtil.get("i18n.execute_script_exit_code.64a8"), type, waitFor);
            scriptExecuteLogServer.updateStatus(logModel.getId(), CommandExecLogModel.Status.DONE, waitFor);
            // 判断是否为严格执行
            if (buildExtraModule.strictlyEnforce() && waitFor != 0) {
                //logRecorder.systemError("严格执行模式，事件脚本返回状态码异常");
                return I18nMessageUtil.get("i18n.strict_execution_mode_event_script_error.c82a") + waitFor;
            }
            if (StrUtil.startWithIgnoreCase(lastMsg[0], "interrupt " + type)) {
                return I18nMessageUtil.get("i18n.event_script_interrupted.8c79") + lastMsg[0];
            }
            return null;
        } finally {
            try {
                FileUtil.del(scriptFile);
            } catch (Exception ignored) {
            }
            IoUtil.close(scriptLog);
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
            buildExecuteManage1.cancelTask(I18nMessageUtil.get("i18n.manual_cancel_task.e592"));
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
        Assert.notEmpty(list, I18nMessageUtil.get("i18n.no_branches_or_tags_in_repository.76b6"));
        if (AntPathUtil.ANT_PATH_MATCHER.isPattern(pattern)) {
            List<String> collect = list.stream().filter(s -> AntPathUtil.ANT_PATH_MATCHER.match(pattern, s)).collect(Collectors.toList());
            return CollUtil.getFirst(collect);
        }
        return pattern;
    }
}
