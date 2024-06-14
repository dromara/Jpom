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
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.extra.ssh.JschUtil;
import cn.keepbx.jpom.model.JsonMessage;
import cn.keepbx.jpom.plugins.IPlugin;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import lombok.Builder;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.configuration.BuildExtConfig;
import org.dromara.jpom.func.assets.model.MachineSshModel;
import org.dromara.jpom.func.assets.server.MachineDockerServer;
import org.dromara.jpom.func.files.service.FileStorageService;
import org.dromara.jpom.model.AfterOpt;
import org.dromara.jpom.model.BaseEnum;
import org.dromara.jpom.model.EnvironmentMapBuilder;
import org.dromara.jpom.model.data.BuildInfoModel;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.data.SshModel;
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
import org.dromara.jpom.service.node.ssh.SshService;
import org.dromara.jpom.system.ExtConfigBean;
import org.dromara.jpom.system.JpomRuntimeException;
import org.dromara.jpom.util.CommandUtil;
import org.dromara.jpom.util.LogRecorder;
import org.dromara.jpom.util.MySftp;
import org.dromara.jpom.util.StringUtil;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
    private Process process;

    private static BuildExecuteService buildExecuteService;
    private static DockerInfoService dockerInfoService;
    private static MachineDockerServer machineDockerServer;
    private static BuildExtConfig buildExtConfig;
    private static FileStorageService fileStorageService;

    private void loadService() {
        buildExecuteService = ObjectUtil.defaultIfNull(buildExecuteService, () -> SpringUtil.getBean(BuildExecuteService.class));
        dockerInfoService = ObjectUtil.defaultIfNull(dockerInfoService, () -> SpringUtil.getBean(DockerInfoService.class));
        machineDockerServer = ObjectUtil.defaultIfNull(machineDockerServer, () -> SpringUtil.getBean(MachineDockerServer.class));
        buildExtConfig = ObjectUtil.defaultIfNull(buildExtConfig, () -> SpringUtil.getBean(BuildExtConfig.class));
        fileStorageService = ObjectUtil.defaultIfNull(fileStorageService, () -> SpringUtil.getBean(FileStorageService.class));
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
        // 先同步到文件管理中心
        Boolean syncFileStorage = this.buildExtraModule.getSyncFileStorage();
        if (syncFileStorage != null && syncFileStorage) {
            // 处理保留天数
            Integer fileStorageKeepDay =
                Optional.ofNullable(this.buildExtraModule.getFileStorageKeepDay())
                    .map(integer -> Convert.toInt(buildExtraModule.getFileStorageKeepDay()))
                    .filter(integer -> integer > 0)
                    .orElse(null);
            String keepMsg = fileStorageKeepDay == null ? StrUtil.EMPTY : StrUtil.format(I18nMessageUtil.get("i18n.retention_days.3c7d"), fileStorageKeepDay);
            logRecorder.system(I18nMessageUtil.get("i18n.start_syncing_to_file_management_center.0a03"), keepMsg);
            boolean tarGz = this.buildEnv.getBool(BuildUtil.USE_TAR_GZ, false);
            File dirPackage = BuildUtil.loadDirPackage(this.buildExtraModule.getId(), this.getRealBuildNumberId(), this.resultFile, tarGz, (unZip, file) -> file);
            String string = I18nMessageUtil.get("i18n.build_source.2ef9");
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
        }
        //
        int releaseMethod = this.buildExtraModule.getReleaseMethod();
        logRecorder.system(I18nMessageUtil.get("i18n.publish_method_format.4622"), BaseEnum.getDescByCode(BuildReleaseMethod.class, releaseMethod));

        if (releaseMethod == BuildReleaseMethod.Outgiving.getCode()) {
            //
            this.doOutGiving();
        } else if (releaseMethod == BuildReleaseMethod.Project.getCode()) {
            this.doProject();
        } else if (releaseMethod == BuildReleaseMethod.Ssh.getCode()) {
            this.doSsh();
        } else if (releaseMethod == BuildReleaseMethod.LocalCommand.getCode()) {
            return this.localCommand();
        } else if (releaseMethod == BuildReleaseMethod.DockerImage.getCode()) {
            return this.doDockerImage();
        } else if (releaseMethod == BuildReleaseMethod.No.getCode()) {
            return null;
        } else {
            String format = StrUtil.format(I18nMessageUtil.get("i18n.no_implemented_publish_distribution.fcf8"), releaseMethod);
            logRecorder.systemError(format);
            return format;
        }
        return null;
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
                    logRecorder.system("dockerTag version number incrementing {} -> {}", s, newVersion);
                } else {
                    logRecorder.systemWarning("version number incrementing error,No numeric version number {} ", s);
                }
                return newVersion;
            })
            .collect(Collectors.joining(StrUtil.COMMA));
    }

    private String doDockerImage() {
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
            //String dockerBuildArgs = this.buildExtraModule.getDockerBuildArgs();
            for (DockerInfoModel infoModel : dockerInfoModels) {
                boolean done = this.doDockerImage(infoModel, envMap, dockerfile, baseDir, dockerTag, this.buildExtraModule);
                if (!done) {
                    logRecorder.systemWarning(I18nMessageUtil.get("i18n.container_build_exception.a98f"), infoModel.getName(), dockerTag);
                    if (buildExtraModule.strictlyEnforce()) {
                        return I18nMessageUtil.get("i18n.strict_mode_image_build_failure.ecea");
                    }
                }
            }
            // 推送 - 只选择一个 docker 服务来推送到远程仓库
            Boolean pushToRepository = this.buildExtraModule.getPushToRepository();
            if (pushToRepository != null && pushToRepository) {
                List<String> repositoryList = StrUtil.splitTrim(dockerTag, StrUtil.COMMA);
                for (String repositoryItem : repositoryList) {
                    logRecorder.system("start push to repository in({}),{} {}{}", map.get("name"), StrUtil.emptyToDefault((String) map.get("registryUrl"), StrUtil.EMPTY), repositoryItem, System.lineSeparator());
                    //
                    map.put("repository", repositoryItem);
                    Consumer<String> logConsumer = logRecorder::info;
                    map.put("logConsumer", logConsumer);
                    IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
                    try {
                        plugin.execute("pushImage", map);
                    } catch (Exception e) {
                        logRecorder.error(I18nMessageUtil.get("i18n.push_image_container_exception.2090"), e);
                    }
                }
            }
            // 发布 docker 服务
            this.updateSwarmService(dockerTag, this.buildExtraModule.getDockerSwarmId(), this.buildExtraModule.getDockerSwarmServiceName());
        } finally {
            CommandUtil.systemFastDel(tempPath);
        }
        return null;
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
        logRecorder.system("{} start build image {}{}", dockerInfoModel.getName(), dockerTag, System.lineSeparator());
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
    private String localCommand() {
        // 执行命令
        String releaseCommand = this.buildExtraModule.getReleaseCommand();
        if (StrUtil.isEmpty(releaseCommand)) {
            logRecorder.systemError(I18nMessageUtil.get("i18n.no_command_to_execute.340b"));
            return null;
        }
        logRecorder.system("{} start exec{}", DateUtil.now(), System.lineSeparator());

        File sourceFile = BuildUtil.getSourceById(this.buildExtraModule.getId());
        Map<String, String> envFileMap = buildEnv.environment();

        InputStream templateInputStream = ExtConfigBean.getConfigResourceInputStream("/exec/template." + CommandUtil.SUFFIX);
        String s1 = IoUtil.readUtf8(templateInputStream);
        int waitFor = JpomApplication.getInstance()
            .execScript(s1 + releaseCommand, file -> {
                try {
                    return CommandUtil.execWaitFor(file, sourceFile, envFileMap, StrUtil.EMPTY, (s, process) -> {
                        ReleaseManage.this.process = process;
                        logRecorder.info(s);
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
     * ssh 发布
     */
    private void doSsh() throws IOException {
        String releaseMethodDataId = this.buildExtraModule.getReleaseMethodDataId();
        SshService sshService = SpringUtil.getBean(SshService.class);
        List<String> strings = StrUtil.splitTrim(releaseMethodDataId, StrUtil.COMMA);
        for (String releaseMethodDataIdItem : strings) {
            SshModel item = sshService.getByKey(releaseMethodDataIdItem, false);
            if (item == null) {
                logRecorder.systemError(I18nMessageUtil.get("i18n.no_ssh_entry_found.d0e1"), releaseMethodDataIdItem);
                continue;
            }
            this.doSsh(item, sshService);
        }
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
                JschUtils.execCallbackLine(session, charset, timeout, this.buildExtraModule.getReleaseBeforeCommand(), StrUtil.EMPTY, envFileMap, logRecorder::info);
            }

            if (StrUtil.isEmpty(releasePath)) {
                logRecorder.systemWarning(I18nMessageUtil.get("i18n.publish_directory_is_empty.79c6"));
            } else {
                logRecorder.system("{} {} start ftp upload{}", DateUtil.now(), item.getName(), System.lineSeparator());
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
            JschUtils.execCallbackLine(session, charset, timeout, this.buildExtraModule.getReleaseCommand(), StrUtil.EMPTY, envFileMap, logRecorder::info);
        } finally {
            JschUtil.close(channelSftp);
            JschUtil.close(session);
        }
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
            JsonMessage<String> jsonMessage = OutGivingRun.fileUpload(file, startPath,
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
    private void doProject() {
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
        Objects.requireNonNull(nodeModel, I18nMessageUtil.get("i18n.node_does_not_exist.4ce4"));
        String projectId = strings[1];
        if (diffSync) {
            this.diffSyncProject(nodeModel, projectId, afterOpt, clearOld);
            return;
        }
        boolean tarGz = this.buildEnv.getBool(BuildUtil.USE_TAR_GZ, false);
        JsonMessage<String> jsonMessage = BuildUtil.loadDirPackage(this.buildExtraModule.getId(), this.getRealBuildNumberId(), this.resultFile, tarGz, (unZip, zipFile) -> {
            String name = zipFile.getName();
            Set<Integer> progressRangeList = ConcurrentHashMap.newKeySet((int) Math.floor((float) 100 / buildExtConfig.getLogReduceProgressRatio()));
            return OutGivingRun.fileUpload(zipFile,
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
        } else {
            throw new JpomRuntimeException(I18nMessageUtil.get("i18n.publish_project_package_failed.9514") + jsonMessage);
        }
    }

    /**
     * 分发包
     */
    private void doOutGiving() throws ExecutionException, InterruptedException {
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
