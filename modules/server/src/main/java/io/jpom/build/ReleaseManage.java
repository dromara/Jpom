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
package io.jpom.build;

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
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.extra.ssh.JschUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import io.jpom.JpomApplication;
import io.jpom.common.BaseServerController;
import io.jpom.common.JsonMessage;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.func.assets.model.MachineSshModel;
import io.jpom.func.files.service.FileStorageService;
import io.jpom.model.AfterOpt;
import io.jpom.model.BaseEnum;
import io.jpom.model.EnvironmentMapBuilder;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.SshModel;
import io.jpom.model.docker.DockerInfoModel;
import io.jpom.model.enums.BuildReleaseMethod;
import io.jpom.model.enums.BuildStatus;
import io.jpom.model.outgiving.OutGivingModel;
import io.jpom.model.user.UserModel;
import io.jpom.outgiving.OutGivingRun;
import io.jpom.plugin.IPlugin;
import io.jpom.plugin.PluginFactory;
import io.jpom.service.docker.DockerInfoService;
import io.jpom.service.docker.DockerSwarmInfoService;
import io.jpom.service.node.NodeService;
import io.jpom.service.node.ssh.SshService;
import io.jpom.system.ExtConfigBean;
import io.jpom.system.JpomRuntimeException;
import io.jpom.system.extconf.BuildExtConfig;
import io.jpom.util.*;
import lombok.Builder;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
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
    private final BuildExtraModule buildExtraModule;
    private final String logId;
    private final BuildExecuteService buildExecuteService;
    private EnvironmentMapBuilder buildEnv;
    private FileStorageService fileStorageService;
    private LogRecorder logRecorder;
    private File resultFile;
    private BuildExtConfig buildExtConfig;

    private void init() {
        if (this.logRecorder == null) {
            File logFile = BuildUtil.getLogFile(buildExtraModule.getId(), buildNumberId);
            this.logRecorder = LogRecorder.builder().file(logFile).build();
        }
        fileStorageService = SpringUtil.getBean(FileStorageService.class);
    }


    public void updateStatus(BuildStatus status) {
        buildExecuteService.updateStatus(this.buildExtraModule.getId(), this.logId, this.buildNumberId, status);
    }

    /**
     * 不修改为发布中状态
     */
    public boolean start(Consumer<Long> consumer, BuildInfoModel buildInfoModel) throws Exception {
        this.init();
        this.resultFile = buildExtraModule.resultDirFile(this.buildNumberId);
        this.buildEnv.put("BUILD_RESULT_FILE", FileUtil.getAbsolutePath(this.resultFile));
        this.buildExtConfig = SpringUtil.getBean(BuildExtConfig.class);
        //
        this.updateStatus(BuildStatus.PubIng);
        if (FileUtil.isEmpty(this.resultFile)) {
            logRecorder.systemError("发布的文件或者文件夹为空,不能继续发布");
            return false;
        }
        long resultFileSize = FileUtil.size(this.resultFile);
        logRecorder.system("开始执行发布,需要发布的文件大小：{}", FileUtil.readableFileSize(resultFileSize));
        Optional.ofNullable(consumer).ifPresent(consumer1 -> consumer1.accept(resultFileSize));
        // 先同步到文件管理中心
        Boolean syncFileStorage = this.buildExtraModule.getSyncFileStorage();
        if (syncFileStorage != null && syncFileStorage) {
            logRecorder.system("开始同步到文件管理中心");
            File dirPackage = BuildUtil.loadDirPackage(this.buildExtraModule.getId(), this.buildNumberId, this.resultFile, (unZip, file) -> file);
            boolean success = fileStorageService.addFile(dirPackage, 1,
                    buildInfoModel.getWorkspaceId(),
                    buildInfoModel.getName(),
                    // 默认的别名码为构建id
                    StrUtil.emptyToDefault(buildInfoModel.getAliasCode(), buildInfoModel.getId()));
            if (success) {
                logRecorder.system("构建产物文件成功同步到文件管理中心");
            } else {
                logRecorder.systemWarning("构建产物文件同步到文件管理中心失败,当前文件已经存文件管理中心存在啦");
            }
        }
        //
        int releaseMethod = this.buildExtraModule.getReleaseMethod();
        logRecorder.system("发布的方式：{}", BaseEnum.getDescByCode(BuildReleaseMethod.class, releaseMethod));

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
            return true;
        } else {
            logRecorder.systemError("没有实现的发布分发:{}", releaseMethod);
            return false;
        }
        return true;
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
        return list.stream().map(s -> {
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
        }).collect(Collectors.joining(StrUtil.COMMA));
    }

    private boolean doDockerImage() {
        // 生成临时目录
        File tempPath = FileUtil.file(JpomApplication.getInstance().getTempPath(), "build_temp", "docker_image", this.buildExtraModule.getId() + StrUtil.DASHED + this.buildNumberId);
        try {
            File sourceFile = BuildUtil.getSourceById(this.buildExtraModule.getId());
            FileUtil.copyContent(sourceFile, tempPath, true);
            // 将产物文件 copy 到本地仓库目录
            File historyPackageFile = BuildUtil.getHistoryPackageFile(buildExtraModule.getId(), this.buildNumberId, StrUtil.SLASH);
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
                logRecorder.systemError("仓库目录下没有找到 Dockerfile 文件: {}", dockerFile);
                return false;
            }
            File baseDir = FileUtil.file(tempPath, list.size() == 1 ? StrUtil.SLASH : CollUtil.get(list, 0));
            //
            String fromTag = this.buildExtraModule.getFromTag();
            // 根据 tag 查询
            List<DockerInfoModel> dockerInfoModels = buildExecuteService
                    .dockerInfoService
                    .queryByTag(this.buildExtraModule.getWorkspaceId(), fromTag);
            Map<String, Object> map = buildExecuteService.machineDockerServer.dockerParameter(dockerInfoModels);
            if (map == null) {
                logRecorder.systemError("{} 没有可用的 docker server", fromTag);
                return false;
            }
            //String dockerBuildArgs = this.buildExtraModule.getDockerBuildArgs();
            for (DockerInfoModel infoModel : dockerInfoModels) {
                this.doDockerImage(infoModel, envMap, dockerfile, baseDir, dockerTag, this.buildExtraModule);
            }
            // 推送 - 只选择一个 docker 服务来推送到远程仓库
            Boolean pushToRepository = this.buildExtraModule.getPushToRepository();
            if (pushToRepository != null && pushToRepository) {
                List<String> repositoryList = StrUtil.splitTrim(dockerTag, StrUtil.COMMA);
                for (String repositoryItem : repositoryList) {
                    logRecorder.system("start push to repository in({}),{} {}", map.get("name"), StrUtil.emptyToDefault((String) map.get("registryUrl"), StrUtil.EMPTY), repositoryItem);
                    //
                    map.put("repository", repositoryItem);
                    Consumer<String> logConsumer = s -> logRecorder.info(s);
                    map.put("logConsumer", logConsumer);
                    IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
                    try {
                        plugin.execute("pushImage", map);
                    } catch (Exception e) {
                        logRecorder.error("推送镜像调用容器异常", e);
                    }
                }
            }
            // 发布 docker 服务
            this.updateSwarmService(dockerTag, this.buildExtraModule.getDockerSwarmId(), this.buildExtraModule.getDockerSwarmServiceName());
        } finally {
            CommandUtil.systemFastDel(tempPath);
        }
        return true;
    }

    private void updateSwarmService(String dockerTag, String swarmId, String serviceName) {
        if (StrUtil.isEmpty(swarmId)) {
            return;
        }
        List<String> splitTrim = StrUtil.splitTrim(dockerTag, StrUtil.COMMA);
        String first = CollUtil.getFirst(splitTrim);
        logRecorder.system("start update swarm service: {} use image {}", serviceName, first);
        Map<String, Object> pluginMap = buildExecuteService.machineDockerServer.dockerParameter(swarmId);
        pluginMap.put("serviceId", serviceName);
        pluginMap.put("image", first);
        try {
            IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
            plugin.execute("updateServiceImage", pluginMap);
        } catch (Exception e) {
            logRecorder.error("更新容器服务调用容器异常", e);
            throw Lombok.sneakyThrow(e);
        }
    }

    private void doDockerImage(DockerInfoModel dockerInfoModel, Map<String, String> envMap, File dockerfile, File baseDir, String dockerTag, BuildExtraModule extraModule) {
        logRecorder.system("{} start build image {}", dockerInfoModel.getName(), dockerTag);
        Map<String, Object> map = buildExecuteService.machineDockerServer.dockerParameter(dockerInfoModel);
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
        Consumer<String> logConsumer = s -> logRecorder.append(s);
        map.put("logConsumer", logConsumer);
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        try {
            plugin.execute("buildImage", map);
        } catch (Exception e) {
            logRecorder.error("构建镜像调用容器异常", e);
        }
    }

    /**
     * 本地命令执行
     */
    private boolean localCommand() {
        // 执行命令
        String releaseCommand = this.buildExtraModule.getReleaseCommand();
        if (StrUtil.isEmpty(releaseCommand)) {
            logRecorder.systemError("没有需要执行的命令");
            return true;
        }
        logRecorder.system("{} start exec", DateUtil.now());

        File sourceFile = BuildUtil.getSourceById(this.buildExtraModule.getId());
        Map<String, String> envFileMap = buildEnv.environment();

        InputStream templateInputStream = ExtConfigBean.getConfigResourceInputStream("/exec/template." + CommandUtil.SUFFIX);
        String s1 = IoUtil.readUtf8(templateInputStream);
        int waitFor = JpomApplication.getInstance()
                .execScript(s1 + releaseCommand, file -> {
                    try {
                        return CommandUtil.execWaitFor(file, sourceFile, envFileMap, StrUtil.EMPTY, (s, process) -> logRecorder.info(s));
                    } catch (IOException | InterruptedException e) {
                        throw Lombok.sneakyThrow(e);
                    }
                });
        logRecorder.system("执行发布脚本的退出码是：{}", waitFor);
        // 判断是否为严格执行
        if (buildExtraModule.strictlyEnforce()) {
            return waitFor == 0;
        }
        return true;
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
                logRecorder.systemError("没有找到对应的ssh项：{}", releaseMethodDataIdItem);
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
            // 执行发布前命令
            if (StrUtil.isNotEmpty(this.buildExtraModule.getReleaseBeforeCommand())) {
                //
                logRecorder.system("开始执行 {} 发布前命令", item.getName());
                JschUtils.execCallbackLine(session, charset, timeout, this.buildExtraModule.getReleaseBeforeCommand(), StrUtil.EMPTY, envFileMap, logRecorder::info);
            }

            String releasePath = this.buildExtraModule.getReleasePath();
            if (StrUtil.isEmpty(releasePath)) {
                logRecorder.systemWarning("发布目录为空");
            } else {
                logRecorder.system("{} {} start ftp upload", DateUtil.now(), item.getName());
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
                            logRecorder.error("清除构建产物失败", e);
                        }
                    }
                }
                sftp.syncUpload(this.resultFile, normalizePath);
                logRecorder.system("{} ftp upload done", item.getName());
            }
            // 执行发布后命令
            if (StrUtil.isEmpty(this.buildExtraModule.getReleaseCommand())) {
                logRecorder.systemWarning("没有需要执行的ssh命令");
                return;
            }
            //
            logRecorder.system("开始执行 {} 发布后命令", item.getName());
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
        Assert.state(requestBody.success(), "对比项目文件失败：" + requestBody);

        JSONObject data = requestBody.getData();
        JSONArray diff = data.getJSONArray("diff");
        JSONArray del = data.getJSONArray("del");
        int delSize = CollUtil.size(del);
        int diffSize = CollUtil.size(diff);
        if (clearOld) {
            logRecorder.system("对比文件结果,产物文件 {} 个、需要上传 {} 个、需要删除 {} 个", CollUtil.size(collect), CollUtil.size(diff), delSize);
        } else {
            logRecorder.system("对比文件结果,产物文件 {} 个、需要上传 {} 个", CollUtil.size(collect), CollUtil.size(diff));
        }
        // 清空发布才先执行删除
        if (delSize > 0 && clearOld) {
            jsonObject.put("data", del);
            requestBody = NodeForward.requestBody(nodeModel, NodeUrl.MANAGE_FILE_BATCH_DELETE, jsonObject);
            Assert.state(requestBody.success(), "删除项目文件失败：" + requestBody);
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
                            logRecorder.system("上传文件进度:{}[{}/{}] {}/{} {} ", file.getName(),
                                    (finalI + 1), diffSize,
                                    FileUtil.readableFileSize(progressSize), FileUtil.readableFileSize(total),
                                    NumberUtil.formatPercent(((float) progressSize / total), 0)
                            );
                        }
                    });
            Assert.state(jsonMessage.success(), "同步项目文件失败：" + jsonMessage);
            if (last) {
                // 最后一个
                logRecorder.system("发布项目包成功：{}", jsonMessage);
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
        Objects.requireNonNull(nodeModel, "节点不存在");
        String projectId = strings[1];
        if (diffSync) {
            this.diffSyncProject(nodeModel, projectId, afterOpt, clearOld);
            return;
        }
        JsonMessage<String> jsonMessage = BuildUtil.loadDirPackage(this.buildExtraModule.getId(), this.buildNumberId, this.resultFile, (unZip, zipFile) -> {
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
                            logRecorder.system("上传文件进度:{} {}/{} {}", name,
                                    FileUtil.readableFileSize(progressSize), FileUtil.readableFileSize(total),
                                    NumberUtil.formatPercent(((float) progressSize / total), 0));
                        }
                    });
        });
        if (jsonMessage.success()) {
            logRecorder.system("发布项目包成功：{}", jsonMessage);
        } else {
            throw new JpomRuntimeException("发布项目包失败：" + jsonMessage);
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
        Future<OutGivingModel.Status> statusFuture = BuildUtil.loadDirPackage(this.buildExtraModule.getId(), this.buildNumberId, this.resultFile, (unZip, zipFile) -> {
            OutGivingRun.OutGivingRunBuilder outGivingRunBuilder = OutGivingRun.builder()
                    .id(releaseMethodDataId)
                    .file(zipFile)
                    .logRecorder(logRecorder)
                    .userModel(userModel)
                    .unzip(unZip)
                    // 由构建配置决定是否删除
                    .doneDeleteFile(false)
                    .projectSecondaryDirectory(projectSecondaryDirectory)
                    .stripComponents(0);
            return outGivingRunBuilder.build().startRun(selectProject);
        });
        //OutGivingRun.startRun(releaseMethodDataId, zipFile, userModel, unZip, 0);
        logRecorder.system("开始执行分发包啦,请到分发中查看详情状态");
        OutGivingModel.Status status = statusFuture.get();
        logRecorder.system("分发结果：{}", status.getDesc());
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
            logRecorder.system("开始回滚:{}", DateTime.now());
            //
            boolean start = this.start(null, item);
            logRecorder.system("执行回滚结束：{}", start);
            if (start) {
                this.updateStatus(BuildStatus.PubSuccess);
            } else {
                this.updateStatus(BuildStatus.PubError);
            }
        } catch (Exception e) {
            log.error("执行发布异常", e);
            logRecorder.error("执行发布异常", e);
            this.updateStatus(BuildStatus.PubError);
        }
    }
}
