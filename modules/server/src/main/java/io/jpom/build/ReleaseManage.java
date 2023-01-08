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
import cn.hutool.extra.ssh.Sftp;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jcraft.jsch.Session;
import io.jpom.JpomApplication;
import io.jpom.common.JsonMessage;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.AfterOpt;
import io.jpom.model.BaseEnum;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.SshModel;
import io.jpom.model.docker.DockerInfoModel;
import io.jpom.model.enums.BuildReleaseMethod;
import io.jpom.model.enums.BuildStatus;
import io.jpom.model.user.UserModel;
import io.jpom.outgiving.OutGivingRun;
import io.jpom.plugin.IPlugin;
import io.jpom.plugin.PluginFactory;
import io.jpom.service.docker.DockerInfoService;
import io.jpom.service.docker.DockerSwarmInfoService;
import io.jpom.service.node.NodeService;
import io.jpom.service.node.ssh.SshService;
import io.jpom.service.system.WorkspaceEnvVarService;
import io.jpom.system.ExtConfigBean;
import io.jpom.system.JpomRuntimeException;
import io.jpom.util.CommandUtil;
import io.jpom.util.FileUtils;
import io.jpom.util.LogRecorder;
import io.jpom.util.StringUtil;
import lombok.Builder;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
public class ReleaseManage implements Runnable {

    private final UserModel userModel;
    private final Integer buildNumberId;
    private final BuildExtraModule buildExtraModule;
    private final String logId;
    private final BuildExecuteService buildExecuteService;
    private Map<String, String> buildEnv;

    private LogRecorder logRecorder;
    private File resultFile;

    private void init() {
        if (this.logRecorder == null) {
            File logFile = BuildUtil.getLogFile(buildExtraModule.getId(), buildNumberId);
            this.logRecorder = LogRecorder.builder().file(logFile).build();
        }
        this.resultFile = BuildUtil.getHistoryPackageFile(buildExtraModule.getId(), this.buildNumberId, buildExtraModule.getResultDirFile());
        //
        if (buildEnv == null) {
            this.buildEnv = new HashMap<>();
            buildEnv.put("BUILD_ID", this.buildExtraModule.getId());
            buildEnv.put("BUILD_NAME", this.buildExtraModule.getName());
            //buildEnv.put("BUILD_SOURCE_FILE", FileUtil.getAbsolutePath(this.gitFile));
            buildEnv.put("BUILD_NUMBER_ID", this.buildNumberId + StrUtil.EMPTY);
        }
//        envFileMap.put("BUILD_ID", this.buildExtraModule.getId());
//        envFileMap.put("BUILD_NAME", this.buildExtraModule.getName());
        buildEnv.put("BUILD_RESULT_FILE", FileUtil.getAbsolutePath(this.resultFile));
//        envFileMap.put("BUILD_NUMBER_ID", this.buildNumberId + StrUtil.EMPTY);
    }

//	/**
//	 * new ReleaseManage constructor
//	 *
//	 * @param buildModel 构建信息
//	 * @param userModel  用户信息
//	 * @param baseBuild  基础构建
//	 * @param buildId    构建序号ID
//	 */
//	ReleaseManage(BuildExtraModule buildModel, UserModel userModel, int buildId) {
//
//
//		this.buildExtraModule = buildModel;
//		this.buildId = buildId;
//		this.userModel = userModel;
//
//	}

//	/**
//	 * 重新发布
//	 *
//	 * @param buildHistoryLog 构建历史
//	 * @param userModel       用户
//	 */
//	public ReleaseManage(BuildHistoryLog buildHistoryLog, UserModel userModel) {
//		super(BuildUtil.getLogFile(buildHistoryLog.getBuildDataId(), buildHistoryLog.getBuildNumberId()),
//				buildHistoryLog.getBuildDataId());
//		this.buildExtraModule = new BuildExtraModule();
//		this.buildExtraModule.updateValue(buildHistoryLog);
//
//		this.buildId = buildHistoryLog.getBuildNumberId();
//		this.userModel = userModel;
//		this.resultFile = BuildUtil.getHistoryPackageFile(this.buildModelId, this.buildId, buildHistoryLog.getResultDirFile());
//	}


    public void updateStatus(BuildStatus status) {
        buildExecuteService.updateStatus(this.buildExtraModule.getId(), this.logId, this.buildNumberId, status);
    }

    /**
     * 不修改为发布中状态
     */
    public boolean start() throws IOException {
        init();
        this.updateStatus(BuildStatus.PubIng);
        logRecorder.info("start release：" + FileUtil.readableFileSize(FileUtil.size(this.resultFile)));
        if (!this.resultFile.exists()) {
            logRecorder.info("不存在构建产物");
            return false;
        }
        int releaseMethod = this.buildExtraModule.getReleaseMethod();
        logRecorder.info("release method:" + BaseEnum.getDescByCode(BuildReleaseMethod.class, releaseMethod));

        if (releaseMethod == BuildReleaseMethod.Outgiving.getCode()) {
            //
            this.doOutGiving();
        } else if (releaseMethod == BuildReleaseMethod.Project.getCode()) {
            this.doProject();
        } else if (releaseMethod == BuildReleaseMethod.Ssh.getCode()) {
            this.doSsh();
        } else if (releaseMethod == BuildReleaseMethod.LocalCommand.getCode()) {
            this.localCommand();
        } else if (releaseMethod == BuildReleaseMethod.DockerImage.getCode()) {
            this.doDockerImage();
        } else if (releaseMethod == BuildReleaseMethod.No.getCode()) {
            return true;
        } else {
            logRecorder.info(" 没有实现的发布分发:" + releaseMethod);
            return false;
        }
        //logRecorder.info("release complete : " + DateUtil.formatBetween(SystemClock.now() - time, BetweenFormatter.Level.MILLISECOND));
        return true;
    }


    /**
     * 格式化命令模版
     *
     * @param commands 命令
     */
    private Map<String, String> formatCommand(String[] commands) {
        File sourceFile = BuildUtil.getSourceById(this.buildExtraModule.getId());
        //        File envFile = FileUtil.file(sourceFile, ".env");
        Map<String, String> envFileMap = FileUtils.readEnvFile(sourceFile, this.buildExtraModule.getAttachEnv());
        //
        envFileMap.putAll(buildEnv);
        //
        for (int i = 0; i < commands.length; i++) {
            commands[i] = StringUtil.formatStrByMap(commands[i], envFileMap);
        }
        //
        WorkspaceEnvVarService workspaceEnvVarService = SpringUtil.getBean(WorkspaceEnvVarService.class);
        workspaceEnvVarService.formatCommand(this.buildExtraModule.getWorkspaceId(), commands);
        return envFileMap;
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
                logRecorder.info("Warning version number incrementing error, no match for . or :");
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
                logRecorder.info("dockerTag version number incrementing {} -> {}", s, newVersion);
            } else {
                logRecorder.info("Warning version number incrementing error,No numeric version number {} ", s);
            }
            return newVersion;
        }).collect(Collectors.joining(StrUtil.COMMA));
    }

    private void doDockerImage() {
        // 生成临时目录
        File tempPath = FileUtil.file(JpomApplication.getInstance().getTempPath(), "build_temp", "docker_image", this.buildExtraModule.getId() + StrUtil.DASHED + this.buildNumberId);
        try {
            File sourceFile = BuildUtil.getSourceById(this.buildExtraModule.getId());
            FileUtil.copyContent(sourceFile, tempPath, true);
            File historyPackageFile = BuildUtil.getHistoryPackageFile(buildExtraModule.getId(), this.buildNumberId, StrUtil.SLASH);
            FileUtil.copyContent(historyPackageFile, tempPath, true);
            // env file
            Map<String, String> envMap = FileUtils.readEnvFile(sourceFile, this.buildExtraModule.getAttachEnv());
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
                logRecorder.info("仓库目录下没有找到 Dockerfile 文件:", dockerFile);
                return;
            }
            File baseDir = FileUtil.file(tempPath, list.size() == 1 ? StrUtil.SLASH : CollUtil.get(list, 0));
            //
            String fromTag = this.buildExtraModule.getFromTag();
            // 根据 tag 查询
            List<DockerInfoModel> dockerInfoModels = buildExecuteService
                .dockerInfoService
                .queryByTag(this.buildExtraModule.getWorkspaceId(), 1, fromTag);
            DockerInfoModel dockerInfoModel = CollUtil.getFirst(dockerInfoModels);
            if (dockerInfoModel == null) {
                logRecorder.info("没有可用的 docker server");
                return;
            }
            //String dockerBuildArgs = this.buildExtraModule.getDockerBuildArgs();
            for (DockerInfoModel infoModel : dockerInfoModels) {
                this.doDockerImage(infoModel, dockerfile, baseDir, dockerTag, this.buildExtraModule);
            }
            // 推送 - 只选择一个 docker 服务来推送到远程仓库
            Boolean pushToRepository = this.buildExtraModule.getPushToRepository();
            if (pushToRepository != null && pushToRepository) {
                List<String> repositoryList = StrUtil.splitTrim(dockerTag, StrUtil.COMMA);
                for (String repositoryItem : repositoryList) {
                    logRecorder.info("start push to repository in({}),{} {}", dockerInfoModel.getName(), StrUtil.emptyToDefault(dockerInfoModel.getRegistryUrl(), StrUtil.EMPTY), repositoryItem);
                    Map<String, Object> map = dockerInfoModel.toParameter();
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
    }

    private void updateSwarmService(String dockerTag, String swarmId, String serviceName) {
        if (StrUtil.isEmpty(swarmId)) {
            return;
        }
        List<String> splitTrim = StrUtil.splitTrim(dockerTag, StrUtil.COMMA);
        String first = CollUtil.getFirst(splitTrim);
        logRecorder.info("start update swarm service: {} use image {}", serviceName, first);
        Map<String, Object> pluginMap = buildExecuteService.dockerInfoService.getBySwarmPluginMap(swarmId);
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

    private void doDockerImage(DockerInfoModel dockerInfoModel, File dockerfile, File baseDir, String dockerTag, BuildExtraModule extraModule) {
        logRecorder.info("{} start build image {}", dockerInfoModel.getName(), dockerTag);
        Map<String, Object> map = dockerInfoModel.toParameter();
        map.put("Dockerfile", dockerfile);
        map.put("baseDirectory", baseDir);
        //
        map.put("tags", dockerTag);
        map.put("buildArgs", extraModule.getDockerBuildArgs());
        map.put("pull", extraModule.getDockerBuildPull());
        map.put("noCache", extraModule.getDockerNoCache());
        map.put("labels", extraModule.getDockerImagesLabels());
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
    private void localCommand() {
        // 执行命令
        String releaseCommand = this.buildExtraModule.getReleaseCommand();
        if (StrUtil.isEmpty(releaseCommand)) {
            logRecorder.info("没有需要执行的命令");
            return;
        }
        logRecorder.info(DateUtil.now() + " start exec");


        File sourceFile = BuildUtil.getSourceById(this.buildExtraModule.getId());
        Map<String, String> envFileMap = FileUtils.readEnvFile(sourceFile, this.buildExtraModule.getAttachEnv());
        //
        envFileMap.putAll(buildEnv);

        InputStream templateInputStream = ExtConfigBean.getConfigResourceInputStream("/exec/template." + CommandUtil.SUFFIX);
        String s1 = IoUtil.readUtf8(templateInputStream);
        JpomApplication.getInstance()
            .execScript(s1 + releaseCommand, file -> {
                try {
                    return CommandUtil.execWaitFor(file, sourceFile, envFileMap, StrUtil.EMPTY, (s, process) -> logRecorder.info(s));
                } catch (IOException | InterruptedException e) {
                    throw Lombok.sneakyThrow(e);
                }
            });
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
                logRecorder.info("没有找到对应的ssh项：" + releaseMethodDataIdItem);
                continue;
            }
            this.doSsh(item, sshService);
        }
    }

    private void doSsh(SshModel item, SshService sshService) throws IOException {
        Session session = SshService.getSessionByModel(item);
        try {
            String releasePath = this.buildExtraModule.getReleasePath();
            if (StrUtil.isEmpty(releasePath)) {
                logRecorder.info("发布目录为空");
            } else {
                logRecorder.info("{} {} start ftp upload", DateUtil.now(), item.getName());
                try (Sftp sftp = new Sftp(session, item.charset(), item.timeout())) {
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
                    logRecorder.info("{} ftp upload done", item.getName());
                }
            }
        } finally {
            JschUtil.close(session);
        }
        logRecorder.info("");
        // 执行命令
        String[] commands = StrUtil.splitToArray(this.buildExtraModule.getReleaseCommand(), StrUtil.LF);
        if (commands == null || commands.length <= 0) {
            logRecorder.info("没有需要执行的ssh命令");
            return;
        }
        // 替换变量
        this.formatCommand(commands);
        //
        logRecorder.info("{} {} start exec", DateUtil.now(), item.getName());
        String s = sshService.exec(item, commands);
        logRecorder.info(s);
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
            logRecorder.info(StrUtil.format("对比文件结果,产物文件 {} 个、需要上传 {} 个、需要删除 {} 个", CollUtil.size(collect), CollUtil.size(diff), delSize));
        } else {
            logRecorder.info(StrUtil.format("对比文件结果,产物文件 {} 个、需要上传 {} 个", CollUtil.size(collect), CollUtil.size(diff)));
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
            JsonMessage<String> jsonMessage = OutGivingRun.fileUpload(file, startPath,
                projectId, false, last ? afterOpt : AfterOpt.No, nodeModel, false,
                this.buildExtraModule.getProjectUploadCloseFirst(), (total, progressSize) -> {
                    //  total, progressSize
                    logRecorder.info("[SYSTEM-INFO] 上传文件进度:{} {}/{} {}", file.getName(),
                        FileUtil.readableFileSize(progressSize), FileUtil.readableFileSize(total),
                        NumberUtil.formatPercent(((float) progressSize / total), 0));
                });
            Assert.state(jsonMessage.success(), "同步项目文件失败：" + jsonMessage);
            if (last) {
                // 最后一个
                logRecorder.info("发布项目包成功：" + jsonMessage);
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
        File zipFile = BuildUtil.isDirPackage(this.resultFile);
        boolean unZip = true;
        if (zipFile == null) {
            zipFile = this.resultFile;
            unZip = false;
        }
        String name = zipFile.getName();
        JsonMessage<String> jsonMessage = OutGivingRun.fileUpload(zipFile,
            this.buildExtraModule.getProjectSecondaryDirectory(),
            projectId,
            unZip,
            afterOpt,
            nodeModel, clearOld, this.buildExtraModule.getProjectUploadCloseFirst(), (total, progressSize) -> {
                logRecorder.info("[SYSTEM-INFO] 上传文件进度:{} {}/{} {}", name,
                    FileUtil.readableFileSize(progressSize), FileUtil.readableFileSize(total),
                    NumberUtil.formatPercent(((float) progressSize / total), 0));
            });
        if (jsonMessage.success()) {
            logRecorder.info("发布项目包成功：" + jsonMessage);
        } else {
            throw new JpomRuntimeException("发布项目包失败：" + jsonMessage);
        }
    }

    /**
     * 分发包
     */
    private void doOutGiving() {
        String releaseMethodDataId = this.buildExtraModule.getReleaseMethodDataId();
        File zipFile = BuildUtil.isDirPackage(this.resultFile);
        boolean unZip = true;
        if (zipFile == null) {
            zipFile = this.resultFile;
            unZip = false;
        }
        OutGivingRun.startRun(releaseMethodDataId, zipFile, userModel, unZip, 0);
        logRecorder.info("开始执行分发包啦,请到分发中查看当前状态");
    }

    @Override
    public void run() {
        try {
            boolean start = this.start();
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
