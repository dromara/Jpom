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
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.ftp.Ftp;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.extra.ssh.JschUtil;
import cn.keepbx.jpom.model.JsonMessage;
import cn.keepbx.jpom.plugins.IPlugin;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.core.exec.ProcessKillResult;
import org.dromara.jpom.core.release.ReleaseTarget;
import org.dromara.jpom.core.release.ReleaseTargetRegistry;
import org.dromara.jpom.core.exec.ProcessTree;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.build.version.BuildVersionService;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.configuration.BuildExtConfig;
import org.dromara.jpom.func.assets.model.MachineFtpModel;
import org.dromara.jpom.func.assets.model.MachineSshModel;
import org.dromara.jpom.func.assets.server.MachineDockerServer;
import org.dromara.jpom.func.assets.server.MachineFtpServer;
import org.dromara.jpom.func.assets.server.ScriptLibraryServer;
import org.dromara.jpom.func.files.service.FileStorageService;
import org.dromara.jpom.model.AfterOpt;
import org.dromara.jpom.model.BaseEnum;
import org.dromara.jpom.model.EnvironmentMapBuilder;
import org.dromara.jpom.model.data.BuildInfoModel;
import org.dromara.jpom.model.data.FtpModel;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.data.SshModel;
import org.dromara.jpom.model.data.SshProjectModel;
import org.dromara.jpom.model.sshproject.SshProjectExecuteResult;
import org.dromara.jpom.service.sshproject.SshProjectMachineResolver;
import org.dromara.jpom.service.sshproject.SshProjectService;
import org.dromara.jpom.service.sshproject.SshProjectVersionService;
import org.dromara.jpom.func.assets.model.MachineSshModel;
import org.dromara.jpom.func.assets.server.MachineSshServer;
import org.dromara.jpom.socket.ConsoleCommandOp;
import org.dromara.jpom.model.docker.DockerInfoModel;
import org.dromara.jpom.model.enums.BuildReleaseMethod;
import org.dromara.jpom.model.enums.BuildStatus;
import org.dromara.jpom.model.outgiving.OutGivingModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.outgiving.OutGivingRun;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.plugins.JschUtils;
import org.dromara.jpom.service.docker.DockerInfoService;
import org.dromara.jpom.service.docker.DockerSwarmInfoService;
import org.dromara.jpom.service.node.NodeService;
import org.dromara.jpom.service.node.ftp.FtpService;
import org.dromara.jpom.service.node.ssh.SshService;
import org.dromara.jpom.system.ExtConfigBean;
import org.dromara.jpom.system.JpomRuntimeException;
import org.dromara.jpom.util.CommandUtil;
import org.dromara.jpom.util.LogRecorder;
import org.dromara.jpom.util.MySftp;
import org.dromara.jpom.util.StringUtil;
import org.springframework.util.Assert;

/**
 * 发布管理
 *
 * @author bwcx_jzy
 * @since 2019/7/19
 */
@Builder
@Slf4j
public class ReleaseManage {

    private final UserModel userModel;
    private final Integer buildNumberId;
    /**
     * 回滚来源的构建 id
     */
    private Integer fromBuildNumberId;
    private final BuildExtraModule buildExtraModule;
    private final String logId;
    private EnvironmentMapBuilder buildEnv;

    private final LogRecorder logRecorder;
    private File resultFile;
    private volatile Process process;
    /**
     * 本次发布的构建信息（发布目标实现需要工作空间、名称、别名码等；由 {@link #start(Consumer, BuildInfoModel)} 注入）
     */
    private BuildInfoModel buildInfoModel;
    /**
     * 进程登记回调：让构建编排把发布命令的进程登记进执行体（取消「发布中」任务时才能杀掉）
     */
    private Consumer<Process> processListener;

    private static BuildExecuteService buildExecuteService;
    private static DockerInfoService dockerInfoService;
    private static MachineDockerServer machineDockerServer;
    private static BuildExtConfig buildExtConfig;
    private static FileStorageService fileStorageService;
    private static ScriptLibraryServer scriptLibraryServer;
    private static MachineFtpServer machineFtpServer;


    /**
     * 终止本次发布当前正在执行的命令进程（取消「发布中」任务用）
     *
     * @return true 确认进程已退出；false 表示杀不掉或本来就没有进程
     */
    public boolean killProcess() {
        Process current = this.process;
        if (current == null) {
            return false;
        }
        ProcessKillResult result = ProcessTree.kill(current);
        if (!result.isSuccess()) {
            log.warn(I18nMessageUtil.get("i18n.process_tree_residual.d0c1"), result.getRemainingPids());
        }
        return result.isSuccess();
    }

    private void loadService() {
        buildExecuteService = ObjectUtil.defaultIfNull(buildExecuteService, () -> SpringUtil.getBean(BuildExecuteService.class));
        dockerInfoService = ObjectUtil.defaultIfNull(dockerInfoService, () -> SpringUtil.getBean(DockerInfoService.class));
        machineDockerServer = ObjectUtil.defaultIfNull(machineDockerServer, () -> SpringUtil.getBean(MachineDockerServer.class));
        buildExtConfig = ObjectUtil.defaultIfNull(buildExtConfig, () -> SpringUtil.getBean(BuildExtConfig.class));
        fileStorageService = ObjectUtil.defaultIfNull(fileStorageService, () -> SpringUtil.getBean(FileStorageService.class));
        scriptLibraryServer = ObjectUtil.defaultIfNull(scriptLibraryServer, () -> SpringUtil.getBean(ScriptLibraryServer.class));
        machineFtpServer = ObjectUtil.defaultIfNull(machineFtpServer, () -> SpringUtil.getBean(MachineFtpServer.class));
    }

    private Integer getRealBuildNumberId() {
        return ObjectUtil.defaultIfNull(this.fromBuildNumberId, this.buildNumberId);
    }

    private void init() {
        this.loadService();
//        if (this.logRecorder == null) {
//            // 回滚的时候需要重新创建对象
//            File logFile = BuildUtil.getLogFile(buildExtraModule.getId(), this.buildNumberId);
//            this.logRecorder = LogRecorder.builder().file(logFile).build();
//        }
        Assert.notNull(buildEnv, I18nMessageUtil.get("i18n.no_environment_variables_found.46ad"));
    }


    private void updateStatus(BuildStatus status, String msg) {
        buildExecuteService.updateStatus(this.buildExtraModule.getId(), this.logId, this.buildNumberId, status, msg);
    }

    /**
     * 不修改为发布中状态
     */
    public String start(Consumer<Long> consumer, BuildInfoModel buildInfoModel) throws Exception {
        this.init();
        this.buildInfoModel = buildInfoModel;
        this.resultFile = buildExtraModule.resultDirFile(this.getRealBuildNumberId());
        this.buildEnv.put("BUILD_RESULT_FILE", FileUtil.getAbsolutePath(this.resultFile));
        this.buildEnv.put("BUILD_RESULT_DIR_FILE", buildExtraModule.getResultDirFile());
        //
        this.updateStatus(BuildStatus.PubIng, I18nMessageUtil.get("i18n.start_publishing.c0b9"));
        if (FileUtil.isEmpty(this.resultFile)) {
            String info = I18nMessageUtil.get("i18n.empty_file_or_folder_for_publish.cae8");
            logRecorder.systemError(info);
            return info;
        }
        long resultFileSize = FileUtil.size(this.resultFile);
        logRecorder.system(I18nMessageUtil.get("i18n.start_executing_publishing_with_file_size.5039"), FileUtil.readableFileSize(resultFileSize));
        Optional.ofNullable(consumer).ifPresent(consumer1 -> consumer1.accept(resultFileSize));
        // 先同步到文件管理中心（与流水线「文件中心」发布方式共用同一段实现）
        Boolean syncFileStorage = this.buildExtraModule.getSyncFileStorage();
        if (syncFileStorage != null && syncFileStorage) {
            this.syncToFileStorage(buildInfoModel);
        }
        //
        int releaseMethod = this.buildExtraModule.getReleaseMethod();
        logRecorder.system(I18nMessageUtil.get("i18n.publish_method_format.4622"), BaseEnum.getDescByCode(BuildReleaseMethod.class, releaseMethod));

        // 装配点：发布方式 → 实现类由注册表决定，新增一种方式不再改这里
        ReleaseTarget target = ReleaseTargetRegistry.get(releaseMethod);
        if (target == null) {
            String format = StrUtil.format(I18nMessageUtil.get("i18n.no_implemented_publish_distribution.fcf8"), releaseMethod);
            logRecorder.systemError(format);
            return format;
        }
        return target.release(this);
    }

    /**
     * 当前发布的构建信息（发布目标实现用）
     *
     * @return 构建信息；未经 {@link #start(Consumer, BuildInfoModel)} 注入时为 null
     */
    BuildInfoModel getBuildInfoModel() {
        return buildInfoModel;
    }

    /**
     * 运行日志（发布目标实现要写日志时用）
     *
     * @return 日志记录器
     */
    LogRecorder getLogRecorder() {
        return logRecorder;
    }

    /**
     * 同步产物到文件中心
     * <p>
     * 两个调用方共用同一段实现：①在线构建的「同步到文件中心」开关（{@link #start}）；②流水线的
     * 「文件中心」发布方式（{@code FileStorageReleaseTarget}）。抽取时保持日志与判定逐字不变，
     * 老构建的行为不因新增发布方式而改变。
     * </p>
     *
     * @param buildInfoModel 构建信息（工作空间、名称、别名码）
     * @return 同步结果（不抛异常，失败原因在 message 里）
     */
    FileStorageSyncResult syncToFileStorage(BuildInfoModel buildInfoModel) {
        // 处理保留天数
        Integer fileStorageKeepDay =
            Optional.ofNullable(this.buildExtraModule.getFileStorageKeepDay())
                .map(integer -> Convert.toInt(buildExtraModule.getFileStorageKeepDay()))
                .filter(integer -> integer > 0)
                .orElse(null);
        String keepMsg = fileStorageKeepDay == null ? StrUtil.EMPTY : StrUtil.format(I18nMessageUtil.get("i18n.retention_days.3c7d"), fileStorageKeepDay);
        logRecorder.system(I18nMessageUtil.get("i18n.start_syncing_to_file_management_center.0a03"), keepMsg);
        boolean tarGz = this.buildEnv == null ? false : this.buildEnv.getBool(BuildUtil.USE_TAR_GZ, false);
        File dirPackage = BuildUtil.loadDirPackage(this.buildExtraModule.getId(), this.getRealBuildNumberId(), this.resultFile, tarGz, (unZip, file) -> file);
        if (dirPackage == null || !dirPackage.exists()) {
            String message = I18nMessageUtil.get("i18n.pipeline_file_storage_package_missing");
            logRecorder.systemWarning(message);
            return FileStorageSyncResult.failed(message);
        }
        String string = I18nMessageUtil.get("i18n.build_source.2ef9");
        // 同名同 MD5 的产物在文件中心已存在时 addFile 会返回 null，这里单独判定，避免把「已存在」误报成失败
        String fileMd5 = SecureUtil.md5(dirPackage);
        boolean existed = fileStorageService.getByKey(fileMd5) != null;
        String successMd5 = fileStorageService.addFile(dirPackage, 1,
            buildInfoModel.getWorkspaceId(),
            string + buildInfoModel.getName(),
            // 默认的别名码为构建id
            StrUtil.emptyToDefault(buildInfoModel.getAliasCode(), buildInfoModel.getId()),
            fileStorageKeepDay);
        if (successMd5 != null) {
            logRecorder.system(I18nMessageUtil.get("i18n.build_product_sync_success.f7d1"), successMd5);
        } else {
            logRecorder.systemWarning(I18nMessageUtil.get("i18n.build_product_file_sync_failed.0e64"));
        }
        return new FileStorageSyncResult(successMd5, existed, dirPackage.getName());
    }

    /**
     * 文件中心同步结果
     * <p>
     * {@code md5} 非空表示本次新写入；{@code existed} 表示同 MD5 产物早已在文件中心
     * （在线构建历史上把它算作「同步失败」的告警，这里保留原判定，同时让流水线可以按「已存在＝成功」处理）。
     * </p>
     */
    static final class FileStorageSyncResult {
        private final String md5;
        private final boolean existed;
        private final String fileName;

        FileStorageSyncResult(String md5, boolean existed, String fileName) {
            this.md5 = md5;
            this.existed = existed;
            this.fileName = fileName;
        }

        static FileStorageSyncResult failed(String message) {
            return new FileStorageSyncResult(null, false, message);
        }

        String getMd5() {
            return md5;
        }

        boolean isExisted() {
            return existed;
        }

        String getFileName() {
            return fileName;
        }

        /**
         * @return 产物是否已在文件中心（新写入或本来就存在）
         */
        boolean available() {
            return md5 != null || existed;
        }
    }

    /**
     * 版本号递增
     *
     * @param dockerTagIncrement 是否开启版本号递增
     * @param dockerTag          当前版本号
     * @return 递增后到版本号
     */
    private String dockerTagIncrement(Boolean dockerTagIncrement, String dockerTag) {
        if (dockerTagIncrement == null || !dockerTagIncrement) {
            return dockerTag;
        }
        List<String> list = StrUtil.splitTrim(dockerTag, StrUtil.COMMA);
        return list.stream()
            .map(s -> {
                List<String> tag = StrUtil.splitTrim(s, StrUtil.COLON);
                String version = CollUtil.getLast(tag);
                List<String> versionList = StrUtil.splitTrim(version, StrUtil.DOT);
                int tagSize = CollUtil.size(tag);
                if (tagSize <= 1 || CollUtil.size(versionList) <= 1) {
                    logRecorder.systemWarning("version number incrementing error, no match for . or :");
                    return s;
                }
                boolean match = false;
                for (int i = versionList.size() - 1; i >= 0; i--) {
                    String versionParting = versionList.get(i);
                    int versionPartingInt = Convert.toInt(versionParting, Integer.MIN_VALUE);
                    if (versionPartingInt != Integer.MIN_VALUE) {
                        versionList.set(i, this.buildNumberId + StrUtil.EMPTY);
                        match = true;
                        break;
                    }
                }
                tag.set(tagSize - 1, CollUtil.join(versionList, StrUtil.DOT));
                String newVersion = CollUtil.join(tag, StrUtil.COLON);
                if (match) {
                    logRecorder.system(I18nMessageUtil.get("i18n.docker_image_tag_version_increment.d436"), s, newVersion);
                } else {
                    logRecorder.systemWarning(I18nMessageUtil.get("i18n.version_increment_error.0157"), s);
                }
                return newVersion;
            })
            .collect(Collectors.joining(StrUtil.COMMA));
    }

    String doDockerImage() {
        // 生成临时目录
        File tempPath = FileUtil.file(JpomApplication.getInstance().getTempPath(), "build_temp", "docker_image", this.buildExtraModule.getId() + StrUtil.DASHED + this.buildNumberId);
        try {
            File sourceFile = BuildUtil.getSourceById(this.buildExtraModule.getId());
            FileUtil.copyContent(sourceFile, tempPath, true);
            // 将产物文件 copy 到本地仓库目录
            File historyPackageFile = BuildUtil.getHistoryPackageFile(buildExtraModule.getId(), this.getRealBuildNumberId(), StrUtil.SLASH);
            FileUtil.copyContent(historyPackageFile, tempPath, true);
            // env file
            Map<String, String> envMap = buildEnv.environment();
            //File envFile = FileUtil.file(tempPath, ".env");
            String dockerTag = StringUtil.formatStrByMap(this.buildExtraModule.getDockerTag(), envMap);
            //
            dockerTag = this.dockerTagIncrement(this.buildExtraModule.getDockerTagIncrement(), dockerTag);
            // docker file
            String moduleDockerfile = this.buildExtraModule.getDockerfile();
            List<String> list = StrUtil.splitTrim(moduleDockerfile, StrUtil.COLON);
            String dockerFile = CollUtil.getLast(list);
            File dockerfile = FileUtil.file(tempPath, dockerFile);
            if (!FileUtil.isFile(dockerfile)) {
                String format = StrUtil.format(I18nMessageUtil.get("i18n.dockerfile_not_found_in_repository.4168"), dockerFile);
                logRecorder.systemError(format);
                return format;
            }
            File baseDir = FileUtil.file(tempPath, list.size() == 1 ? StrUtil.SLASH : CollUtil.get(list, 0));
            //
            String fromTag = this.buildExtraModule.getFromTag();
            // 根据 tag 查询
            List<DockerInfoModel> dockerInfoModels = dockerInfoService
                .queryByTag(this.buildExtraModule.getWorkspaceId(), fromTag);
            Map<String, Object> map = machineDockerServer.dockerParameter(dockerInfoModels);
            if (map == null) {
                String format = StrUtil.format(I18nMessageUtil.get("i18n.no_available_docker_server.6aaa"), fromTag);
                logRecorder.systemError(format);
                return format;
            }
            // 逐台构建：任一失败在严格模式下直接失败，否则仅告警（沿用历史语义）
            String buildError = this.buildImages(dockerInfoModels, envMap, dockerfile, baseDir, dockerTag);
            if (buildError != null) {
                return buildError;
            }
            // 推送 - 只选择一个 docker 服务来推送到远程仓库
            Boolean pushToRepository = this.buildExtraModule.getPushToRepository();
            Boolean pushToRepositoryAfterDelete = this.buildExtraModule.getPushToRepositoryAfterDelete();
            if (pushToRepository != null && pushToRepository) {
                this.pushImages(map, dockerTag);
            }
            // 发布 docker 服务
            this.updateSwarmService(dockerTag, this.buildExtraModule.getDockerSwarmId(), this.buildExtraModule.getDockerSwarmServiceName());
            // 推送后删除本地镜像
            if (pushToRepository != null && pushToRepository && pushToRepositoryAfterDelete != null && pushToRepositoryAfterDelete) {
                this.removeLocalImages(map, dockerTag);
            }
        } finally {
            CommandUtil.systemFastDel(tempPath);
        }
        return null;
    }

    /**
     * 逐台 Docker 服务构建镜像
     *
     * @return 错误信息；null 表示全部按策略完成
     */
    private String buildImages(List<DockerInfoModel> dockerInfoModels, Map<String, String> envMap,
                               File dockerfile, File baseDir, String dockerTag) {
        for (DockerInfoModel infoModel : dockerInfoModels) {
            boolean done = this.doDockerImage(infoModel, envMap, dockerfile, baseDir, dockerTag, this.buildExtraModule);
            if (!done) {
                logRecorder.systemWarning(I18nMessageUtil.get("i18n.container_build_exception.a98f"), infoModel.getName(), dockerTag);
                if (buildExtraModule.strictlyEnforce()) {
                    return I18nMessageUtil.get("i18n.strict_mode_image_build_failure.ecea");
                }
            }
        }
        return null;
    }

    /**
     * 推送镜像到远程仓库（逐个 tag）
     */
    private void pushImages(Map<String, Object> map, String dockerTag) {
        Map<String, Object> map2 = new HashMap<>(map);
        for (String repositoryItem : StrUtil.splitTrim(dockerTag, StrUtil.COMMA)) {
            String registryUrl = StrUtil.emptyToDefault((String) map2.get("registryUrl"), StrUtil.EMPTY);
            Object name = map2.get("name");
            logRecorder.system(I18nMessageUtil.get("i18n.start_push_image_to_remote_repo.10a7"), name, registryUrl, repositoryItem, System.lineSeparator());
            map2.put("repository", repositoryItem);
            Consumer<String> logConsumer = logRecorder::info;
            map2.put("logConsumer", logConsumer);
            IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
            try {
                plugin.execute("pushImage", map2);
            } catch (Exception e) {
                logRecorder.error(I18nMessageUtil.get("i18n.push_image_container_exception.2090"), e);
            }
        }
    }

    /**
     * 推送完成后删除本地镜像（逐个 tag）
     */
    private void removeLocalImages(Map<String, Object> map, String dockerTag) {
        Map<String, Object> map2 = new HashMap<>(map);
        for (String repositoryItem : StrUtil.splitTrim(dockerTag, StrUtil.COMMA)) {
            Object name = map2.get("name");
            logRecorder.system(I18nMessageUtil.get("i18n.auto_delete_local_image_after_push.c13d"), name, repositoryItem, System.lineSeparator());
            map2.put("imageId", repositoryItem);
            IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
            try {
                plugin.execute("removeImage", map2);
            } catch (Exception e) {
                logRecorder.error(I18nMessageUtil.get("i18n.delete_local_image_failed.91fa"), e);
            }
        }
    }

    private void updateSwarmService(String dockerTag, String swarmId, String serviceName) {
        if (StrUtil.isEmpty(swarmId)) {
            return;
        }
        List<String> splitTrim = StrUtil.splitTrim(dockerTag, StrUtil.COMMA);
        String first = CollUtil.getFirst(splitTrim);
        logRecorder.system("start update swarm service: {} use image {}", serviceName, first);
        Map<String, Object> pluginMap = machineDockerServer.dockerParameter(swarmId);
        pluginMap.put("serviceId", serviceName);
        pluginMap.put("image", first);
        try {
            IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
            plugin.execute("updateServiceImage", pluginMap);
        } catch (Exception e) {
            logRecorder.error(I18nMessageUtil.get("i18n.update_container_service_exception.2249"), e);
            throw Lombok.sneakyThrow(e);
        }
    }

    private boolean doDockerImage(DockerInfoModel dockerInfoModel, Map<String, String> envMap, File dockerfile, File baseDir, String dockerTag, BuildExtraModule extraModule) {
        logRecorder.system(I18nMessageUtil.get("i18n.start_building_image.eacd"), dockerInfoModel.getName(), dockerTag, System.lineSeparator());
        Map<String, Object> map = machineDockerServer.dockerParameter(dockerInfoModel);
        //.toParameter();
        map.put("Dockerfile", dockerfile);
        map.put("baseDirectory", baseDir);
        //
        map.put("tags", dockerTag);
        map.put("buildArgs", extraModule.getDockerBuildArgs());
        map.put("pull", extraModule.getDockerBuildPull());
        map.put("noCache", extraModule.getDockerNoCache());
        map.put("labels", extraModule.getDockerImagesLabels());
        map.put("networkMode", extraModule.getDockerImagesNetworkMode());
        map.put("env", envMap);
        Consumer<String> logConsumer = logRecorder::append;
        map.put("logConsumer", logConsumer);
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        try {
            return (boolean) plugin.execute("buildImage", map);
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.build_image_call_container_exception.7e13"), e);
            logRecorder.error(I18nMessageUtil.get("i18n.build_image_call_container_exception.7e13"), e);
            return false;
        }
    }

    /**
     * 本地命令执行
     */
    String localCommand() {
        // 执行命令
        String releaseCommand = this.buildExtraModule.getReleaseCommand();
        if (StrUtil.isEmpty(releaseCommand)) {
            logRecorder.systemError(I18nMessageUtil.get("i18n.no_command_to_execute.340b"));
            return null;
        }
        logRecorder.system(I18nMessageUtil.get("i18n.start_executing.87e7"), DateUtil.now(), System.lineSeparator());
        // 替换脚本库 // 替换全局变量
        releaseCommand = scriptLibraryServer.referenceReplace(releaseCommand);
        File sourceFile = BuildUtil.getSourceById(this.buildExtraModule.getId());
        Map<String, String> envFileMap = buildEnv.environment();

        InputStream templateInputStream = ExtConfigBean.getConfigResourceInputStream("/exec/template." + CommandUtil.SUFFIX);
        String s1 = IoUtil.readUtf8(templateInputStream);
        int waitFor = JpomApplication.getInstance()
            .execScript(s1 + releaseCommand, file -> {
                try {
                    // 进程启动即登记（发布命令同样可能是无输出命令）
                    return CommandUtil.execWaitFor(file, sourceFile, envFileMap, StrUtil.EMPTY,
                        (s, process) -> logRecorder.info(s),
                        process -> {
                            ReleaseManage.this.process = process;
                            Optional.ofNullable(ReleaseManage.this.processListener).ifPresent(listener -> listener.accept(process));
                        });
                } catch (IOException | InterruptedException e) {
                    throw Lombok.sneakyThrow(e);
                }
            });
        ReleaseManage.this.process = null;
        logRecorder.system(I18nMessageUtil.get("i18n.publish_script_exit_code.0f69"), waitFor);
        // 判断是否为严格执行
        if (buildExtraModule.strictlyEnforce()) {
            return waitFor == 0 ? null : StrUtil.format(I18nMessageUtil.get("i18n.publish_command_non_zero_exit_code.ea80"), waitFor);
        }
        return null;
    }

    /**
     * 记录发布命令退出码，并按「严格执行」决定是否把该目标判失败
     * <p>
     * 收口点：SSH 发布前命令、SSH 发布后命令、SSH 项目重启三处共用，避免各写一套判断。
     * 口径见 {@link ReleaseCommandExitCode}：未知（-1）只提示；非 0 且开启严格执行才抛错。
     *
     * @param phaseLabel 阶段文案
     * @param exitCode   退出码
     * @param logRecorder 日志
     */
    private void recordReleaseCommandExit(String phaseLabel, int exitCode, LogRecorder logRecorder) {
        boolean strictlyEnforce = this.buildExtraModule.strictlyEnforce();
        if (ReleaseCommandExitCode.unknown(exitCode)) {
            if (ReleaseCommandExitCode.shouldFailTarget(exitCode, strictlyEnforce)) {
                // 严格模式下「读不到退出码」＝不能证明成功，按失败处理（2026-11-25 群反馈复测口径）
                String message = ReleaseCommandExitCode.unknownFailureMessage(phaseLabel);
                logRecorder.systemError(message);
                throw new JpomRuntimeException(message);
            }
            logRecorder.systemWarning(I18nMessageUtil.get("i18n.release_command_exit_unknown.a7f6"), phaseLabel);
            return;
        }
        logRecorder.system(I18nMessageUtil.get("i18n.execute_script_exit_code.64a8"), phaseLabel, exitCode);
        if (!ReleaseCommandExitCode.shouldFailTarget(exitCode, strictlyEnforce)) {
            return;
        }
        String message = ReleaseCommandExitCode.failureMessage(phaseLabel, exitCode);
        logRecorder.systemError(message);
        throw new JpomRuntimeException(message);
    }

    /**
     * 逐个发布目标，统一「任一目标失败即终止」策略
     * <p>
     * SSH 与 SSH 项目共用这一处：失败判定（目标不存在、连接/上传异常）由调用方抛出，
     * 循环与失败聚合由 {@link MultiTargetRelease} 负责（行为见 MultiTargetReleaseTest）。
     *
     * @param targetIds 逗号分隔的目标 id
     * @param action    单个目标的发布动作
     */
    private void releaseTargets(String targetIds, MultiTargetRelease.TargetAction action) {
        MultiTargetRelease.Outcome outcome = MultiTargetRelease.execute(targetIds, this.buildExtraModule.failFastOnRelease(), action);
        for (MultiTargetRelease.Failure failure : outcome.getFailures()) {
            logRecorder.systemError(I18nMessageUtil.get("i18n.release_target_failed.a7c3"), failure.getTargetId(), failure.getMessage());
        }
        if (outcome.getSkipped() > 0) {
            logRecorder.systemWarning(I18nMessageUtil.get("i18n.release_abort_remaining.b2f1"), outcome.getSkipped());
        }
        if (outcome.isAborted()) {
            // 失败即终止：首个失败已经决定本次发布结果，剩余目标不再发布
            MultiTargetRelease.Failure first = outcome.getFailures().get(0);
            throw new JpomRuntimeException(StrUtil.format(I18nMessageUtil.get("i18n.release_target_failed.a7c3"), first.getTargetId(), first.getMessage()));
        }
        if (!outcome.hasFailure()) {
            return;
        }
        // 未开启失败即终止：剩余目标已发布，但只要有目标失败本次发布仍然判定失败
        List<String> failures = outcome.getFailures().stream().map(MultiTargetRelease.Failure::toString).collect(Collectors.toList());
        String message = StrUtil.format(I18nMessageUtil.get("i18n.release_partial_failed.c6d4"), failures.size(), String.join("; ", failures));
        logRecorder.systemError(message);
        throw new JpomRuntimeException(message);
    }

    /**
     * ssh 发布（支持多目标；失败策略见 {@link #releaseTargets}）
     */
    void doSsh() {
        SshService sshService = SpringUtil.getBean(SshService.class);
        this.releaseTargets(this.buildExtraModule.getReleaseMethodDataId(), targetId -> {
            SshModel item = sshService.getByKey(targetId, false);
            if (item == null) {
                throw new JpomRuntimeException(StrUtil.format(I18nMessageUtil.get("i18n.no_ssh_entry_found.d0e1"), targetId));
            }
            this.doSsh(item, sshService);
        });
    }

    private void doSsh(SshModel item, SshService sshService) throws IOException {
        Map<String, String> envFileMap = buildEnv.environment();
        MachineSshModel machineSshModel = sshService.getMachineSshModel(item);
        Session session = null;
        ChannelSftp channelSftp = null;
        try {
            session = sshService.getSessionByModel(machineSshModel);
            Charset charset = machineSshModel.charset();
            int timeout = machineSshModel.timeout();
            String releasePath = this.buildExtraModule.getReleasePath();
            envFileMap.put("SSH_RELEASE_PATH", releasePath);
            // 执行发布前命令
            if (StrUtil.isNotEmpty(this.buildExtraModule.getReleaseBeforeCommand())) {
                //
                logRecorder.system(I18nMessageUtil.get("i18n.start_executing_pre_release_command.6c7e"), item.getName());
                int beforeExitCode = JschUtils.execCallbackLine(session, charset, timeout, this.buildExtraModule.getReleaseBeforeCommand(), StrUtil.EMPTY, envFileMap, logRecorder::info);
                this.recordReleaseCommandExit(I18nMessageUtil.get("i18n.release_phase_before_command.a7f1"), beforeExitCode, logRecorder);
            }

            if (StrUtil.isEmpty(releasePath)) {
                logRecorder.systemWarning(I18nMessageUtil.get("i18n.publish_directory_is_empty.79c6"));
            } else {
                logRecorder.system(I18nMessageUtil.get("i18n.start_upload_ftp_file.20be"), DateUtil.now(), item.getName(), System.lineSeparator());
                MySftp.ProgressMonitor sftpProgressMonitor = sshService.createProgressMonitor(logRecorder);
                MySftp sftp = new MySftp(session, charset, timeout, sftpProgressMonitor);
                channelSftp = sftp.getClient();
                String prefix = "";
                if (!StrUtil.startWith(releasePath, StrUtil.SLASH)) {
                    prefix = sftp.pwd();
                }
                String normalizePath = FileUtil.normalize(prefix + StrUtil.SLASH + releasePath);
                if (this.buildExtraModule.isClearOld()) {
                    try {
                        if (sftp.exist(normalizePath)) {
                            sftp.delDir(normalizePath);
                        }
                    } catch (Exception e) {
                        if (!StrUtil.startWithIgnoreCase(e.getMessage(), "No such file")) {
                            logRecorder.error(I18nMessageUtil.get("i18n.clear_build_product_failed.edd4"), e);
                        }
                    }
                }
                sftp.syncUpload(this.resultFile, normalizePath);
                logRecorder.system("{} ftp upload done", item.getName());
            }
            // 执行发布后命令
            if (StrUtil.isEmpty(this.buildExtraModule.getReleaseCommand())) {
                logRecorder.systemWarning(I18nMessageUtil.get("i18n.no_ssh_commands_to_execute_after_publish.89ba"));
                return;
            }
            //
            logRecorder.system(I18nMessageUtil.get("i18n.start_executing_post_release_command.fd06"), item.getName());
            int afterExitCode = JschUtils.execCallbackLine(session, charset, timeout, this.buildExtraModule.getReleaseCommand(), StrUtil.EMPTY, envFileMap, logRecorder::info);
            this.recordReleaseCommandExit(I18nMessageUtil.get("i18n.release_phase_after_command.a7f2"), afterExitCode, logRecorder);
        } finally {
            JschUtil.close(channelSftp);
            JschUtil.close(session);
        }
    }


    /**
     * 发布到 SSH 项目：产物直接落到项目目录，然后按项目生命周期脚本重启
     * <p>
     * 与「发布到项目」一致支持版本快照：发布前登记基线、发布成功后登记本次版本，
     * 构建历史里的「快速回滚」据此把远端目录切回历史版本（不重新传输产物）。
     */
    void doSshProject() {
        SshProjectService sshProjectService = SpringUtil.getBean(SshProjectService.class);
        this.releaseTargets(this.buildExtraModule.getReleaseMethodDataId(), targetId -> {
            SshProjectModel project = sshProjectService.getByKey(targetId);
            if (project == null) {
                throw new JpomRuntimeException(I18nMessageUtil.get("i18n.ssh_project_not_exist.5d18") + targetId);
            }
            this.doSshProject(project);
        });
    }

    /**
     * 发布到单个 SSH 项目：产物落到项目目录，然后按项目生命周期脚本重启
     *
     * @param project 项目
     * @throws IOException IO
     */
    private void doSshProject(SshProjectModel project) throws IOException {
        SshProjectMachineResolver machineResolver = SpringUtil.getBean(SshProjectMachineResolver.class);
        MachineSshModel machineSshModel = machineResolver.resolve(project);
        // 版本快照：只按构建配置的开关决定，失败只告警不阻断发布
        SshProjectVersionService sshProjectVersionService = SpringUtil.getBean(SshProjectVersionService.class);
        boolean versionEnabled = SpringUtil.getBean(BuildVersionService.class).enabled(this.buildExtraModule);
        boolean versionBegun = versionEnabled && this.sshProjectVersionBegin(sshProjectVersionService, project);
        boolean publishSuccess = false;
        try {
            Session session = null;
            ChannelSftp channelSftp = null;
            try {
                session = SpringUtil.getBean(MachineSshServer.class).getSessionByModel(machineSshModel);
                Charset charset = machineSshModel.charset();
                int timeout = machineSshModel.timeout();
                MySftp sftp = new MySftp(session, charset, timeout, SpringUtil.getBean(SshService.class).createProgressMonitor(logRecorder));
                channelSftp = sftp.getClient();
                String normalizePath = FileUtil.normalize(StrUtil.nullToEmpty(project.getPath()));
                if (this.buildExtraModule.isClearOld() && sftp.exist(normalizePath)) {
                    try {
                        sftp.delDir(normalizePath);
                    } catch (Exception e) {
                        logRecorder.error(I18nMessageUtil.get("i18n.clear_build_product_failed.edd4"), e);
                    }
                }
                sftp.syncUpload(this.resultFile, normalizePath);
                logRecorder.system(I18nMessageUtil.get("i18n.ssh_project_upload_done.a7fa"), project.getName());
            } finally {
                JschUtil.close(channelSftp);
                JschUtil.close(session);
            }
            publishSuccess = true;
            // 发布完成后按项目自身脚本重启（没有 restart 钩子时执行器会退化为 stop + start）
            SshProjectExecuteResult result = null;
            try {
                result = SpringUtil.getBean(SshProjectService.class)
                    .execute(project, ConsoleCommandOp.restart.name(), line -> logRecorder.info(line));
            } catch (Exception e) {
                // 项目没有配置任何生命周期脚本时不影响产物发布结果，只提示
                logRecorder.systemWarning(I18nMessageUtil.get("i18n.ssh_project_no_lifecycle_script.a7f9"), project.getName(), e.getMessage());
            }
            if (result == null) {
                return;
            }
            if (result.isSuccess()) {
                logRecorder.system(I18nMessageUtil.get("i18n.ssh_project_restart_done.a7f8"), project.getName());
                return;
            }
            // 重启失败默认只记录错误（保持 2.11.15 行为，避免老配置被改判）；
            // 开启「严格执行」时按同一条退出码口径判该目标失败（见 ReleaseCommandExitCode）
            String restartPhase = I18nMessageUtil.get("i18n.release_phase_ssh_restart.a7f3");
            logRecorder.systemError(I18nMessageUtil.get("i18n.ssh_project_restart_failed.a7f7"), project.getName(),
                StrUtil.emptyToDefault(result.getMessage(), String.valueOf(result.getExitCode())));
            this.recordReleaseCommandExit(restartPhase, result.getExitCode(), logRecorder);
        } finally {
            if (versionBegun && publishSuccess) {
                this.sshProjectVersionCommit(sshProjectVersionService, project);
            }
        }
    }

    /**
     * SSH 项目：发布前准备版本快照（失败只告警，不阻断发布）
     *
     * @param versionService 版本服务
     * @param project        项目
     * @return 是否准备成功
     */
    private boolean sshProjectVersionBegin(SshProjectVersionService versionService, SshProjectModel project) {
        try {
            JSONObject begin = versionService.begin(project, this.getRealBuildNumberId(), this.buildExtraModule.getVersionKeepCount());
            if (begin.getJSONObject("baseline") != null) {
                logRecorder.system(I18nMessageUtil.get("i18n.version_baseline_created.2b6c"), project.getName(),
                    begin.getJSONObject("baseline").getString("versionId"));
            }
            logRecorder.system(I18nMessageUtil.get("i18n.version_snapshot_prepare_success.a1f7"));
            return true;
        } catch (Exception e) {
            log.warn(I18nMessageUtil.get("i18n.version_snapshot_prepare_exception.c3e5"), e);
            logRecorder.systemWarning(I18nMessageUtil.get("i18n.version_snapshot_prepare_exception.c3e5"));
        }
        return false;
    }

    /**
     * SSH 项目：发布成功后登记版本（失败只告警，不阻断发布）
     *
     * @param versionService 版本服务
     * @param project        项目
     */
    private void sshProjectVersionCommit(SshProjectVersionService versionService, SshProjectModel project) {
        try {
            JSONObject commit = versionService.commit(project, String.valueOf(this.getRealBuildNumberId()),
                SshProjectVersionService.SOURCE_BUILD, this.getRealBuildNumberId(), this.buildExtraModule.getId(),
                this.buildExtraModule.getVersionKeepCount());
            logRecorder.system(I18nMessageUtil.get("i18n.ssh_project_version_commit_success.2c85"),
                commit.getString("versionId"), commit.getIntValue("fileCount"), FileUtil.readableFileSize(commit.getLongValue("fileSize")));
        } catch (Exception e) {
            log.warn(I18nMessageUtil.get("i18n.version_snapshot_commit_exception.f5b3"), e);
            logRecorder.systemWarning(I18nMessageUtil.get("i18n.version_snapshot_commit_exception.f5b3"));
        }
    }

    /**
     * ftp 发布（支持多目标；失败策略见 {@link #releaseTargets}）
     * <p>
     * 2026-11-23 复测：原来目标已删除只记一条错误日志、发布目录为空只告警，构建仍算成功，
     * 与 SSH / SSH 项目「目标缺失或上传异常算失败」的口径不一致，这里统一走 releaseTargets。
     */
    void doFtp() {
        FtpService ftpService = SpringUtil.getBean(FtpService.class);
        this.releaseTargets(this.buildExtraModule.getReleaseMethodDataId(), targetId -> {
            FtpModel item = ftpService.getByKey(targetId, false);
            if (item == null) {
                throw new JpomRuntimeException(StrUtil.format(I18nMessageUtil.get("i18n.ftp_item_not_found.60a7"), targetId));
            }
            this.doFtp(item, ftpService);
        });
    }

    /**
     * 发布到单个 FTP 目标
     *
     * @param item       ftp 资产
     * @param ftpService ftp service
     */
    private void doFtp(FtpModel item, FtpService ftpService) {
        String releasePath = this.buildExtraModule.getReleaseFtpPath();
        if (StrUtil.isEmpty(releasePath)) {
            // 发布目录为空等于什么都没发出去，不能当作发布成功
            throw new JpomRuntimeException(I18nMessageUtil.get("i18n.publish_directory_is_empty.79c6"));
        }
        logRecorder.system(I18nMessageUtil.get("i18n.start_upload_ftp_file.20be"), DateUtil.now(), item.getName(), System.lineSeparator());
        MachineFtpModel machineFtpModel = ftpService.getMachineFtpModel(item);
        try (Ftp ftp = machineFtpServer.getFtpClient(machineFtpModel)) {
            String prefix = "";
            if (!StrUtil.startWith(releasePath, StrUtil.SLASH)) {
                prefix = ftp.pwd();
            }
            String normalizePath = FileUtil.normalize(prefix + StrUtil.SLASH + releasePath);
            if (this.buildExtraModule.isClearOld()) {
                try {
                    if (ftp.exist(normalizePath)) {
                        ftp.delDir(normalizePath);
                    }
                } catch (Exception e) {
                    if (!StrUtil.startWithIgnoreCase(e.getMessage(), "No such file")) {
                        logRecorder.error(I18nMessageUtil.get("i18n.clear_build_product_failed.edd4"), e);
                    }
                }
            }
            ftpService.uploadWithProgress(ftp, this.resultFile, normalizePath, logRecorder, 5);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        logRecorder.system("{} {} ftp upload done", DateUtil.now(), item.getName());
    }

    /**
     * 差异上传发布
     *
     * @param nodeModel 节点
     * @param projectId 项目ID
     * @param afterOpt  发布后的操作
     */
    private void diffSyncProject(NodeModel nodeModel, String projectId, AfterOpt afterOpt, boolean clearOld) {
        File resultFile = this.resultFile;
        String resultFileParent = resultFile.isFile() ?
            FileUtil.getAbsolutePath(resultFile.getParent()) : FileUtil.getAbsolutePath(this.resultFile);
        //
        List<File> files = FileUtil.loopFiles(resultFile);
        List<JSONObject> collect = files.stream().map(file -> {
            //
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", StringUtil.delStartPath(file, resultFileParent, true));
            jsonObject.put("sha1", SecureUtil.sha1(file));
            return jsonObject;
        }).collect(Collectors.toList());
        //
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", projectId);
        jsonObject.put("data", collect);
        String directory = this.buildExtraModule.getProjectSecondaryDirectory();
        directory = Opt.ofBlankAble(directory).orElse(StrUtil.SLASH);
        jsonObject.put("dir", directory);
        JsonMessage<JSONObject> requestBody = NodeForward.requestBody(nodeModel, NodeUrl.MANAGE_FILE_DIFF_FILE, jsonObject);
        Assert.state(requestBody.success(), I18nMessageUtil.get("i18n.compare_project_failure.e6ab") + requestBody);

        JSONObject data = requestBody.getData();
        JSONArray diff = data.getJSONArray("diff");
        JSONArray del = data.getJSONArray("del");
        int delSize = CollUtil.size(del);
        int diffSize = CollUtil.size(diff);
        if (clearOld) {
            logRecorder.system(I18nMessageUtil.get("i18n.compare_files_result_with_delete.033d"), CollUtil.size(collect), CollUtil.size(diff), delSize);
        } else {
            logRecorder.system(I18nMessageUtil.get("i18n.compare_files_result.bec4"), CollUtil.size(collect), CollUtil.size(diff));
        }
        // 清空发布才先执行删除
        if (delSize > 0 && clearOld) {
            jsonObject.put("data", del);
            requestBody = NodeForward.requestBody(nodeModel, NodeUrl.MANAGE_FILE_BATCH_DELETE, jsonObject);
            Assert.state(requestBody.success(), I18nMessageUtil.get("i18n.delete_project_file_failure_with_full_stop.85b8") + requestBody);
        }
        for (int i = 0; i < diffSize; i++) {
            boolean last = (i == diffSize - 1);
            JSONObject diffData = (JSONObject) diff.get(i);
            String name = diffData.getString("name");
            File file = FileUtil.file(resultFileParent, name);
            //
            String startPath = StringUtil.delStartPath(file, resultFileParent, false);
            startPath = FileUtil.normalize(startPath + StrUtil.SLASH + directory);
            //
            Set<Integer> progressRangeList = ConcurrentHashMap.newKeySet((int) Math.floor((float) 100 / buildExtConfig.getLogReduceProgressRatio()));
            int finalI = i;
            JsonMessage<String> jsonMessage = OutGivingRun.fileUpload(file, file.getName(), startPath,
                projectId, false, last ? afterOpt : AfterOpt.No, nodeModel, false,
                this.buildExtraModule.getProjectUploadCloseFirst(), (total, progressSize) -> {
                    double progressPercentage = Math.floor(((float) progressSize / total) * 100);
                    int progressRange = (int) Math.floor(progressPercentage / buildExtConfig.getLogReduceProgressRatio());
                    if (progressRangeList.add(progressRange)) {
                        //  total, progressSize
                        String info = I18nMessageUtil.get("i18n.upload_progress_message_format.b91c");
                        logRecorder.system(info, file.getName(),
                            (finalI + 1), diffSize,
                            FileUtil.readableFileSize(progressSize), FileUtil.readableFileSize(total),
                            NumberUtil.formatPercent(((float) progressSize / total), 0)
                        );
                    }
                });
            Assert.state(jsonMessage.success(), I18nMessageUtil.get("i18n.synchronize_project_files_failed.6aa4") + jsonMessage);
            if (last) {
                // 最后一个
                logRecorder.system(I18nMessageUtil.get("i18n.publish_project_package_success.b0ce"), jsonMessage);
            }
        }
    }

    /**
     * 发布项目
     */
    void doProject() {
        //AfterOpt afterOpt, boolean clearOld, boolean diffSync
        AfterOpt afterOpt = BaseEnum.getEnum(AfterOpt.class, this.buildExtraModule.getAfterOpt(), AfterOpt.No);
        boolean clearOld = this.buildExtraModule.isClearOld();
        boolean diffSync = this.buildExtraModule.isDiffSync();
        String releaseMethodDataId = this.buildExtraModule.getReleaseMethodDataId();
        String[] strings = StrUtil.splitToArray(releaseMethodDataId, CharPool.COLON);
        if (ArrayUtil.length(strings) != 2) {
            throw new IllegalArgumentException(releaseMethodDataId + " error");
        }
        NodeService nodeService = SpringUtil.getBean(NodeService.class);
        NodeModel nodeModel = nodeService.getByKey(strings[0]);
        Assert.notNull(nodeModel, I18nMessageUtil.get("i18n.node_does_not_exist.4ce4"));
        String projectId = strings[1];
        BuildVersionService buildVersionService = SpringUtil.getBean(BuildVersionService.class);
        boolean versionEnabled = buildVersionService.enabled(this.buildExtraModule);
        boolean versionBegun = false;
        boolean publishSuccess = false;
        if (versionEnabled) {
            versionBegun = this.versionBegin(buildVersionService, nodeModel, projectId);
        }
        try {
            if (diffSync) {
                this.diffSyncProject(nodeModel, projectId, afterOpt, clearOld);
                publishSuccess = true;
                return;
            }
            boolean tarGz = this.buildEnv.getBool(BuildUtil.USE_TAR_GZ, false);
            JsonMessage<String> jsonMessage = BuildUtil.loadDirPackage(this.buildExtraModule.getId(), this.getRealBuildNumberId(), this.resultFile, tarGz, (unZip, zipFile) -> {
                String name = zipFile.getName();
                Set<Integer> progressRangeList = ConcurrentHashMap.newKeySet((int) Math.floor((float) 100 / buildExtConfig.getLogReduceProgressRatio()));
                return OutGivingRun.fileUpload(zipFile, zipFile.getName(),
                    this.buildExtraModule.getProjectSecondaryDirectory(),
                    projectId,
                    unZip,
                    afterOpt,
                    nodeModel, clearOld, this.buildExtraModule.getProjectUploadCloseFirst(), (total, progressSize) -> {
                        double progressPercentage = Math.floor(((float) progressSize / total) * 100);
                        int progressRange = (int) Math.floor(progressPercentage / buildExtConfig.getLogReduceProgressRatio());
                        if (progressRangeList.add(progressRange)) {
                            logRecorder.system(I18nMessageUtil.get("i18n.upload_progress_with_colon.dd5b"), name,
                                FileUtil.readableFileSize(progressSize), FileUtil.readableFileSize(total),
                                NumberUtil.formatPercent(((float) progressSize / total), 0));
                        }
                    });
            });
            if (jsonMessage.success()) {
                logRecorder.system(I18nMessageUtil.get("i18n.publish_project_package_success.b0ce"), jsonMessage);
                publishSuccess = true;
            } else {
                throw new JpomRuntimeException(I18nMessageUtil.get("i18n.publish_project_package_failed.9514") + jsonMessage);
            }
        } finally {
            if (versionBegun && publishSuccess) {
                this.versionCommit(buildVersionService, nodeModel, projectId);
            }
        }
    }

    /**
     * 发布前准备版本快照（失败只告警，不阻断发布）
     *
     * @param buildVersionService 版本服务
     * @param nodeModel           节点
     * @param projectId           项目ID
     * @return 是否准备成功
     */
    private boolean versionBegin(BuildVersionService buildVersionService, NodeModel nodeModel, String projectId) {
        try {
            JsonMessage<JSONObject> beginMessage = buildVersionService.begin(nodeModel, projectId,
                this.getRealBuildNumberId(), this.buildExtraModule.getVersionKeepCount());
            if (!beginMessage.success()) {
                logRecorder.systemWarning(I18nMessageUtil.get("i18n.version_snapshot_prepare_failed.b8d2"), beginMessage);
                return false;
            }
            if (!BuildVersionService.effective(beginMessage)) {
                // 插件端未启用版本快照：返回的是 success + enabled=false，不能当成功
                logRecorder.systemWarning(I18nMessageUtil.get("i18n.version_snapshot_not_effective.4c7a"));
                return false;
            }
            logRecorder.system(I18nMessageUtil.get("i18n.version_snapshot_prepare_success.a1f7"));
            return true;
        } catch (Exception e) {
            log.warn(I18nMessageUtil.get("i18n.version_snapshot_prepare_exception.c3e5"), e);
            logRecorder.systemWarning(I18nMessageUtil.get("i18n.version_snapshot_prepare_exception.c3e5"));
        }
        return false;
    }

    /**
     * 发布成功后登记版本（失败只告警，不阻断发布）
     *
     * @param buildVersionService 版本服务
     * @param nodeModel           节点
     * @param projectId           项目ID
     */
    private void versionCommit(BuildVersionService buildVersionService, NodeModel nodeModel, String projectId) {
        try {
            JsonMessage<JSONObject> commitMessage = buildVersionService.commit(nodeModel, projectId,
                this.getRealBuildNumberId(), this.buildExtraModule.getId(),
                this.buildExtraModule.getVersionKeepCount(),
                this.resultFile == null ? null : FileUtil.getAbsolutePath(this.resultFile));
            if (!commitMessage.success()) {
                logRecorder.systemWarning(I18nMessageUtil.get("i18n.version_snapshot_commit_failed.e4c1"), commitMessage);
                return;
            }
            if (!BuildVersionService.effective(commitMessage)) {
                // 插件端未启用版本快照：success 但没有登记任何版本，快速回滚会「没有记录」
                logRecorder.systemWarning(I18nMessageUtil.get("i18n.version_snapshot_not_effective.4c7a"));
                return;
            }
            logRecorder.system(I18nMessageUtil.get("i18n.version_snapshot_commit_success.d2a8"));
        } catch (Exception e) {
            log.warn(I18nMessageUtil.get("i18n.version_snapshot_commit_exception.f5b3"), e);
            logRecorder.systemWarning(I18nMessageUtil.get("i18n.version_snapshot_commit_exception.f5b3"));
        }
    }

    /**
     * 分发包
     */
    void doOutGiving() throws ExecutionException, InterruptedException {
        String releaseMethodDataId = this.buildExtraModule.getReleaseMethodDataId();
        String projectSecondaryDirectory = this.buildExtraModule.getProjectSecondaryDirectory();
        //
        String selectProject = buildEnv.get("dispatchSelectProject");
        boolean tarGz = buildEnv.getBool(BuildUtil.USE_TAR_GZ, false);
        Future<OutGivingModel.Status> statusFuture = BuildUtil.loadDirPackage(this.buildExtraModule.getId(), this.getRealBuildNumberId(), this.resultFile, tarGz, (unZip, zipFile) -> {
            OutGivingRun.OutGivingRunBuilder outGivingRunBuilder = OutGivingRun.builder()
                .id(releaseMethodDataId)
                .file(zipFile)
                .logRecorder(logRecorder)
                .userModel(userModel)
                .mode("build-trigger")
                .modeData(buildExtraModule.getId())
                // 版本快照：按节点登记构建版本，失败只告警不阻断分发
                .buildNumberId(getRealBuildNumberId())
                .buildDataId(buildExtraModule.getId())
                .versionSnapshot(buildExtraModule.getVersionSnapshot())
                .versionKeepCount(buildExtraModule.getVersionKeepCount())
                .unzip(unZip)
                // 由构建配置决定是否删除
                .doneDeleteFile(false)
                .projectSecondaryDirectory(projectSecondaryDirectory)
                .stripComponents(0);
            return outGivingRunBuilder.build().startRun(selectProject);
        });
        //OutGivingRun.startRun(releaseMethodDataId, zipFile, userModel, unZip, 0);
        logRecorder.system(I18nMessageUtil.get("i18n.start_executing_distribution_package.a2cc"));
        OutGivingModel.Status status = statusFuture.get();
        logRecorder.system(I18nMessageUtil.get("i18n.distribute_result.a230"), status.getDesc());
        if (status == OutGivingModel.Status.FAIL) {
            // 与 SSH / SSH 项目 / FTP 同口径：分发失败判定本次构建失败，避免「构建成功但产物没发出去」
            String message = StrUtil.format(I18nMessageUtil.get("i18n.distribute_result.a230"), status.getDesc());
            logRecorder.systemError(message);
            throw new JpomRuntimeException(message);
        }
    }

    /**
     * 回滚
     *
     * @param item 构建对象
     */
    public void rollback(BuildInfoModel item) {
        try {
            BaseServerController.resetInfo(userModel);
            this.init();
            //
            buildEnv.eachStr(logRecorder::system);
            logRecorder.system(I18nMessageUtil.get("i18n.start_rolling_back.f020"), DateTime.now());
            //
            String errorMsg = this.start(null, item);
            String emptied = StrUtil.emptyToDefault(errorMsg, "ok");
            logRecorder.system(I18nMessageUtil.get("i18n.rollback_ended.fb1d"), emptied);
            if (errorMsg == null) {
                this.updateStatus(BuildStatus.PubSuccess, I18nMessageUtil.get("i18n.publish_success.2fff"));
            } else {
                this.updateStatus(BuildStatus.PubError, errorMsg);
            }
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.publish_exception.cf0b"), e);
            logRecorder.error(I18nMessageUtil.get("i18n.publish_exception.cf0b"), e);
            this.updateStatus(BuildStatus.PubError, e.getMessage());
        } finally {
            IoUtil.close(this.logRecorder);
        }
    }
}