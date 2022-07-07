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
import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.extra.ssh.Sftp;
import cn.hutool.http.HttpStatus;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jcraft.jsch.Session;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.AfterOpt;
import io.jpom.model.BaseEnum;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.SshModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.docker.DockerInfoModel;
import io.jpom.model.enums.BuildReleaseMethod;
import io.jpom.model.enums.BuildStatus;
import io.jpom.outgiving.OutGivingRun;
import io.jpom.plugin.IPlugin;
import io.jpom.plugin.PluginFactory;
import io.jpom.service.docker.DockerInfoService;
import io.jpom.service.docker.DockerSwarmInfoService;
import io.jpom.service.node.NodeService;
import io.jpom.service.node.ssh.SshService;
import io.jpom.service.system.WorkspaceEnvVarService;
import io.jpom.system.ConfigBean;
import io.jpom.system.JpomRuntimeException;
import io.jpom.util.CommandUtil;
import io.jpom.util.FileUtils;
import io.jpom.util.LogRecorder;
import io.jpom.util.StringUtil;
import lombok.Builder;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
public class ReleaseManage implements Runnable {

    private final UserModel userModel;
    private final int buildId;
    private final BuildExtraModule buildExtraModule;
    private final String logId;
    private final BuildExecuteService buildExecuteService;

    private LogRecorder logRecorder;
    private File resultFile;

    private void init() {
        if (this.logRecorder == null) {
            File logFile = BuildUtil.getLogFile(buildExtraModule.getId(), buildId);
            this.logRecorder = LogRecorder.builder().file(logFile).build();
        }
        this.resultFile = BuildUtil.getHistoryPackageFile(buildExtraModule.getId(), this.buildId, buildExtraModule.getResultDirFile());
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
        buildExecuteService.updateStatus(this.buildExtraModule.getId(), this.logId, status);
    }

    /**
     * 不修改为发布中状态
     */
    public void start() {
        init();
        updateStatus(BuildStatus.PubIng);
        logRecorder.info("start release：" + FileUtil.readableFileSize(FileUtil.size(this.resultFile)));
        if (!this.resultFile.exists()) {
            logRecorder.info("不存在构建产物");
            updateStatus(BuildStatus.PubError);
            return;
        }
        long time = SystemClock.now();
        int releaseMethod = this.buildExtraModule.getReleaseMethod();
        logRecorder.info("release method:" + BaseEnum.getDescByCode(BuildReleaseMethod.class, releaseMethod));
        try {
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
            } else {
                logRecorder.info(" 没有实现的发布分发:" + releaseMethod);
            }
        } catch (Exception e) {
            this.pubLog("发布异常", e);
            return;
        }
        logRecorder.info("release complete : " + DateUtil.formatBetween(SystemClock.now() - time, BetweenFormatter.Level.MILLISECOND));
        updateStatus(BuildStatus.PubSuccess);
    }


    /**
     * 格式化命令模版
     *
     * @param commands 命令
     */
    private Map<String, String> formatCommand(String[] commands) {
        File sourceFile = BuildUtil.getSourceById(this.buildExtraModule.getId());
        File envFile = FileUtil.file(sourceFile, ".env");
        Map<String, String> envFileMap = FileUtils.readEnvFile(envFile);
        //
        envFileMap.put("BUILD_ID", this.buildExtraModule.getId());
        envFileMap.put("BUILD_NAME", this.buildExtraModule.getName());
        envFileMap.put("BUILD_RESULT_FILE", FileUtil.getAbsolutePath(this.resultFile));
        envFileMap.put("BUILD_NUMBER_ID", this.buildId + StrUtil.EMPTY);
        //
        for (int i = 0; i < commands.length; i++) {
            commands[i] = StringUtil.formatStrByMap(commands[i], envFileMap);
        }
        //
        WorkspaceEnvVarService workspaceEnvVarService = SpringUtil.getBean(WorkspaceEnvVarService.class);
        workspaceEnvVarService.formatCommand(this.buildExtraModule.getWorkspaceId(), commands);
        return envFileMap;
    }

    private String parseDockerTag(File envFile, String tag) {
        if (!FileUtil.isFile(envFile)) {
            return tag;
        }
        final String[] newTag = {tag};
        FileUtil.readLines(envFile, StandardCharsets.UTF_8, (LineHandler) line -> {
            line = StrUtil.trim(line);
            if (StrUtil.startWith(line, "#")) {
                return;
            }
            List<String> list = StrUtil.splitTrim(line, "=");
            if (CollUtil.size(list) != 2) {
                return;
            }
            newTag[0] = StrUtil.replace(newTag[0], "${" + list.get(0) + "}", list.get(1));
        });
        return newTag[0];
    }

    private void doDockerImage() {
        // 生成临时目录
        File tempPath = FileUtil.file(ConfigBean.getInstance().getTempPath(), "build_temp", "docker_image", this.buildExtraModule.getId() + StrUtil.DASHED + this.buildId);
        try {
            File sourceFile = BuildUtil.getSourceById(this.buildExtraModule.getId());
            FileUtil.copyContent(sourceFile, tempPath, true);
            File historyPackageFile = BuildUtil.getHistoryPackageFile(buildExtraModule.getId(), this.buildId, StrUtil.SLASH);
            FileUtil.copyContent(historyPackageFile, tempPath, true);
            // env file
            File envFile = FileUtil.file(tempPath, ".env");
            String dockerTag = this.buildExtraModule.getDockerTag();
            dockerTag = this.parseDockerTag(envFile, dockerTag);
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
            for (DockerInfoModel infoModel : dockerInfoModels) {
                this.doDockerImage(infoModel, dockerfile, baseDir, dockerTag);
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
            logRecorder.error("调用容器异常", e);
        }
    }

    private void doDockerImage(DockerInfoModel dockerInfoModel, File dockerfile, File baseDir, String dockerTag) {
        logRecorder.info("{} start build image {}", dockerInfoModel.getName(), dockerTag);
        Map<String, Object> map = dockerInfoModel.toParameter();
        map.put("Dockerfile", dockerfile);
        map.put("baseDirectory", baseDir);
        //
        map.put("tags", dockerTag);
        Consumer<String> logConsumer = s -> logRecorder.append(s);
        map.put("logConsumer", logConsumer);
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        try {
            plugin.execute("buildImage", map);
        } catch (Exception e) {
            logRecorder.error("调用容器异常", e);
        }
    }

    /**
     * 本地命令执行
     */
    private void localCommand() {
        // 执行命令
        String[] commands = StrUtil.splitToArray(this.buildExtraModule.getReleaseCommand(), StrUtil.LF);
        if (ArrayUtil.isEmpty(commands)) {
            logRecorder.info("没有需要执行的命令");
            return;
        }
        String command = StrUtil.EMPTY;
        logRecorder.info(DateUtil.now() + " start exec");
        InputStream templateInputStream = null;
        try {
            templateInputStream = ResourceUtil.getStream("classpath:/bin/execTemplate." + CommandUtil.SUFFIX);
            if (templateInputStream == null) {
                logRecorder.info("系统中没有命令模版");
                return;
            }
            String sshExecTemplate = IoUtil.readUtf8(templateInputStream);
            StringBuilder stringBuilder = new StringBuilder(sshExecTemplate);
            // 替换变量
            this.formatCommand(commands);
            //
            stringBuilder.append(ArrayUtil.join(commands, StrUtil.LF));
            File tempPath = ConfigBean.getInstance().getTempPath();
            File commandFile = FileUtil.file(tempPath, "build", this.buildExtraModule.getId() + StrUtil.DOT + CommandUtil.SUFFIX);
            FileUtil.writeUtf8String(stringBuilder.toString(), commandFile);
            //
            //			command = SystemUtil.getOsInfo().isWindows() ? StrUtil.EMPTY : CommandUtil.SUFFIX;
            command = CommandUtil.generateCommand(commandFile, "");
            //CommandUtil.EXECUTE_PREFIX + StrUtil.SPACE + FileUtil.getAbsolutePath(commandFile);
            String result = CommandUtil.execSystemCommand(command);
            logRecorder.info(result);
        } catch (Exception e) {
            this.pubLog("执行本地命令异常：" + command, e);
        } finally {
            IoUtil.close(templateInputStream);
        }
    }

    /**
     * ssh 发布
     */
    private void doSsh() {
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

    private void doSsh(SshModel item, SshService sshService) {
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
                            sftp.delDir(normalizePath);
                        } catch (Exception e) {
                            if (!StrUtil.startWithIgnoreCase(e.getMessage(), "No such file")) {
                                this.pubLog("清除构建产物失败", e);
                            }
                        }
                    }
                    sftp.syncUpload(this.resultFile, normalizePath);
                    logRecorder.info("{} ftp upload done", item.getName());
                } catch (Exception e) {
                    this.pubLog("执行ssh发布异常", e);
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
        try {
            String s = sshService.exec(item, commands);
            logRecorder.info(s);
        } catch (Exception e) {
            this.pubLog(item.getName() + " 执行异常", e);
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
        JsonMessage<JSONObject> requestBody = NodeForward.requestBody(nodeModel, NodeUrl.MANAGE_FILE_DIFF_FILE, this.userModel, jsonObject);
        if (requestBody.getCode() != HttpStatus.HTTP_OK) {
            throw new JpomRuntimeException("对比项目文件失败：" + requestBody);
        }
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
            requestBody = NodeForward.requestBody(nodeModel, NodeUrl.MANAGE_FILE_BATCH_DELETE, this.userModel, jsonObject);
            if (requestBody.getCode() != HttpStatus.HTTP_OK) {
                throw new JpomRuntimeException("删除项目文件失败：" + requestBody);
            }
        }
        for (int i = 0; i < diffSize; i++) {
            boolean last = (i == diffSize - 1);
            JSONObject diffData = (JSONObject) diff.get(i);
            String name = diffData.getString("name");
            File file = FileUtil.file(resultFileParent, name);
            //
            String startPath = StringUtil.delStartPath(file, resultFileParent, false);
            //
            JsonMessage<String> jsonMessage = OutGivingRun.fileUpload(file, startPath,
                projectId, false, last ? afterOpt : AfterOpt.No, nodeModel, this.userModel, false);
            if (jsonMessage.getCode() != HttpStatus.HTTP_OK) {
                throw new JpomRuntimeException("同步项目文件失败：" + jsonMessage);
            }
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
//		AfterOpt afterOpt, boolean clearOld, boolean diffSync
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
        JsonMessage<String> jsonMessage = OutGivingRun.fileUpload(zipFile, null,
            projectId,
            unZip,
            afterOpt,
            nodeModel, this.userModel, clearOld);
        if (jsonMessage.getCode() == HttpStatus.HTTP_OK) {
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
        OutGivingRun.startRun(releaseMethodDataId, zipFile, userModel, unZip);
        logRecorder.info("开始执行分发包啦,请到分发中查看当前状态");
    }


    /**
     * 发布异常日志
     *
     * @param title     描述
     * @param throwable 异常
     */
    private void pubLog(String title, Throwable throwable) {
        logRecorder.error(title, throwable);
        this.updateStatus(BuildStatus.PubError);
    }

    @Override
    public void run() {
        this.start();
    }
}
