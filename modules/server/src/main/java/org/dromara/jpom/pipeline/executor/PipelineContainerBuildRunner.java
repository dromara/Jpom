/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.executor;

import cn.hutool.core.convert.Convert;
import org.dromara.jpom.pipeline.engine.PipelineContext;
import java.util.function.Consumer;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.plugins.IPlugin;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.build.BuildUtil;
import org.dromara.jpom.build.DockerYmlDsl;
import org.dromara.jpom.func.assets.server.MachineDockerServer;
import org.dromara.jpom.model.docker.DockerInfoModel;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.service.docker.DockerInfoService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流水线「容器构建」执行器
 * <p>
 * 复用在线构建的容器构建通道（同一套 Docker 插件与 {@link DockerYmlDsl}），差别只在：
 * 源目录是流水线工作区、产物目录是流水线阶段产物目录，因此流水线不需要另一套 Docker 逻辑。
 * 阶段配置：{@code executor=container}、{@code image}（基础镜像 tag）、{@code run}（容器内命令）。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
@Service
public class PipelineContainerBuildRunner {

    private final DockerInfoService dockerInfoService;
    private final MachineDockerServer machineDockerServer;

    public PipelineContainerBuildRunner(DockerInfoService dockerInfoService, MachineDockerServer machineDockerServer) {
        this.dockerInfoService = dockerInfoService;
        this.machineDockerServer = machineDockerServer;
    }

    /**
     * 在容器里执行命令
     *
     * @param workspaceId 工作空间
     * @param image       基础镜像 tag（对应 docker 资产 tags）
     * @param workDir     宿主机上的工作目录（会被挂进容器）
     * @param run         容器内执行的命令
     * @param artifactDir 产物输出目录（宿主机）
     * @param env         环境变量
     * @param logSink     日志
     * @return 退出码；-100 表示插件未返回
     */
    public int run(String workspaceId, String image, File workDir, String run, File artifactDir,
                   Map<String, String> env, Consumer<String> logSink,
                   PipelineContext context) throws Exception {
        // 用与在线构建相同的 DSL 表达，避免两套参数
        StringBuilder dslBuilder = new StringBuilder("runsOn: ubuntu-latest\nfromTag: ").append(image)
            .append("\nsteps:\n  - run: |\n");
        for (String line : StrUtil.split(run, '\n')) {
            dslBuilder.append("      ").append(line).append('\n');
        }
        String dsl = dslBuilder.toString();
        DockerYmlDsl dockerYmlDsl = DockerYmlDsl.build(dsl);
        List<DockerInfoModel> dockerInfoModels = dockerInfoService.queryByTag(workspaceId, dockerYmlDsl.getFromTag());
        Map<String, Object> map = machineDockerServer.dockerParameter(dockerInfoModels);
        if (map == null) {
            throw new IllegalStateException("no available docker server for tag: " + image);
        }
        String workingDir = "/home/jpom/";
        map.put("runsOn", dockerYmlDsl.getRunsOn());
        map.put("workingDir", workingDir);
        map.put("hostConfig", dockerYmlDsl.getHostConfig());
        map.put("tempDir", JpomApplication.getInstance().getTempPath());
        String containerName = "jpom-pipeline-build-" + System.nanoTime();
        map.put("dockerName", containerName);
        // 插件内部会用 logRecorder.info(...) 输出容器日志；传 null 会直接 NPE（实测）
        map.put("logRecorder", BuildUtil.emptyLogRecorder());
        map.put("logConsumer", logSink);
        List<String> copy = ObjectUtil.defaultIfNull(dockerYmlDsl.getCopy(), new ArrayList<>());
        copy.add(FileUtil.getAbsolutePath(workDir) + StrUtil.COLON + workingDir + StrUtil.COLON + "true");
        map.put("copy", copy);
        map.put("binds", ObjectUtil.defaultIfNull(dockerYmlDsl.getBinds(), new ArrayList<>()));
        Map<String, String> dockerEnv = ObjectUtil.defaultIfNull(dockerYmlDsl.getEnv(), new HashMap<>(10));
        Map<String, String> allEnv = new HashMap<>(env);
        allEnv.putAll(dockerEnv);
        map.put("env", allEnv);
        map.put("steps", dockerYmlDsl.getSteps());
        map.put("resultFile", FileUtil.normalize(workingDir + StrUtil.SLASH));
        map.put("resultFileOut", FileUtil.getAbsolutePath(artifactDir));
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        if (context != null) {
            // 容器构建同样没有本地进程：取消 = 按容器名删除容器
            Map<String, Object> removeMap = new HashMap<>(map);
            removeMap.put("containerId", containerName);
            context.setStageCancel(reason -> {
                try {
                    plugin.execute("removeContainer", removeMap);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            });
        }
        try {
            Object execute = plugin.execute("build", map);
            return Convert.toInt(execute, -100);
        } finally {
            if (context != null) {
                context.setStageCancel(null);
            }
        }
    }
}