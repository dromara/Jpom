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
package io.jpom;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.unit.DataSizeUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.InvocationBuilder;
import io.jpom.plugin.IDefaultPlugin;
import io.jpom.plugin.PluginConfig;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * docker 插件
 *
 * @author bwcx_jzy
 * @since 2022/1/26
 */
@PluginConfig(name = "docker-cli")
public class DefaultDockerPluginImpl implements IDefaultPlugin {

    @Override
    public Object execute(Object main, Map<String, Object> parameter) throws Exception {
        String type = main.toString();
        switch (type) {
            case "build":
                try (DockerBuild dockerBuild = new DockerBuild(parameter)) {
                    dockerBuild.build();
                }
                return null;
            case "listContainer":
                return this.listContainerCmd(parameter);
            case "stopContainer":
                this.stopContainerCmd(parameter);
                return null;
            case "removeContainer":
                this.removeContainerCmd(parameter);
                return null;
            case "startContainer":
                this.startContainerCmd(parameter);
                return null;
            case "restartContainer":
                this.restartContainerCmd(parameter);
                return null;
            case "listImages":
                return this.listImagesCmd(parameter);
            case "removeImage":
                this.removeImageCmd(parameter);
                return null;
            case "listVolumes":
                return this.listVolumesCmd(parameter);
            case "removeVolume":
                this.removeVolumeCmd(parameter);
                return null;
            case "logContainer":
                this.logContainerCmd(parameter);
                return null;
            case "exec":
                this.execCreateCmd(parameter);
                return null;
            case "buildImage":
                this.buildImageCmd(parameter);
                return null;
            case "inspectImage":
                return this.inspectImageCmd(parameter);
            case "createContainer":
                this.createContainerCmd(parameter);
                return null;
            case "pullImage":
                this.pullImageCmd(parameter);
                return null;
            case "listNetworks":
                return this.listNetworksCmd(parameter);
            case "stats":
                return this.statsCmd(parameter);
            case "inspectContainer":
                return this.inspectContainerCmd(parameter);
            case "updateContainer":
                return this.updateContainerCmd(parameter);
            default:
                throw new IllegalArgumentException("不支持的类型");
        }
    }

    private Map<String, JSONObject> statsCmd(Map<String, Object> parameter) throws IOException {
        try (DockerClient dockerClient = DockerUtil.build(parameter)) {
            String containerId = (String) parameter.get("containerId");
            List<String> split = StrUtil.split(containerId, StrUtil.COMMA);
            return split.stream().map(s -> {
                Statistics statistics = dockerClient.statsCmd(s).exec(new InvocationBuilder.AsyncResultCallback<Statistics>() {
                    @SneakyThrows
                    @Override
                    public void onNext(Statistics object) {
                        super.onNext(object);
                        super.close();
                    }
                }).awaitResult();
                return new Tuple(s, JSONObject.toJSON(statistics));
            }).collect(Collectors.toMap(tuple -> tuple.get(0), tuple -> tuple.get(1)));
        }
    }

    private JSONObject updateContainerCmd(Map<String, Object> parameter) throws IOException {
        try (DockerClient dockerClient = DockerUtil.build(parameter)) {
            String containerId = (String) parameter.get("containerId");
            UpdateContainerCmd updateContainerCmd = dockerClient.updateContainerCmd(containerId);
            //
            Optional.ofNullable(parameter.get("cpusetCpus"))
                .map(StrUtil::toStringOrNull)
                .ifPresent(updateContainerCmd::withCpusetCpus);

            Optional.ofNullable(parameter.get("cpusetMems"))
                .map(StrUtil::toStringOrNull)
                .ifPresent(updateContainerCmd::withCpusetMems);

            Optional.ofNullable(parameter.get("cpuPeriod"))
                .map(Convert::toInt)
                .ifPresent(updateContainerCmd::withCpuPeriod);

            Optional.ofNullable(parameter.get("cpuQuota"))
                .map(Convert::toInt)
                .ifPresent(updateContainerCmd::withCpuQuota);

            Optional.ofNullable(parameter.get("cpuShares"))
                .map(Convert::toInt)
                .ifPresent(updateContainerCmd::withCpuShares);

            Optional.ofNullable(parameter.get("blkioWeight"))
                .map(Convert::toInt)
                .ifPresent(updateContainerCmd::withBlkioWeight);

            Optional.ofNullable(parameter.get("memoryReservation"))
                .map(StrUtil::toStringOrNull)
                .map(s -> {
                    if (StrUtil.isEmpty(s)) {
                        return null;
                    }
                    return DataSizeUtil.parse(s);
                })
                .ifPresent(updateContainerCmd::withMemoryReservation);

            Optional.ofNullable(parameter.get("memory"))
                .map(StrUtil::toStringOrNull)
                .map(s -> {
                    if (StrUtil.isEmpty(s)) {
                        return null;
                    }
                    return DataSizeUtil.parse(s);
                })
                .ifPresent(updateContainerCmd::withMemory);

            //            updateContainerCmd.withKernelMemory(DataSizeUtil.parse("10M"));

            Optional.ofNullable(parameter.get("memorySwap"))
                .map(StrUtil::toStringOrNull)
                .map(s -> {
                    if (StrUtil.isEmpty(s)) {
                        return null;
                    }
                    return DataSizeUtil.parse(s);
                })
                .ifPresent(updateContainerCmd::withMemorySwap);

            UpdateContainerResponse updateContainerResponse = updateContainerCmd.exec();
            return (JSONObject) JSONObject.toJSON(updateContainerResponse);
        }
    }

    private JSONObject inspectContainerCmd(Map<String, Object> parameter) throws IOException {
        try (DockerClient dockerClient = DockerUtil.build(parameter)) {
            String containerId = (String) parameter.get("containerId");
            InspectContainerResponse containerResponse = dockerClient.inspectContainerCmd(containerId).withSize(true).exec();
            return (JSONObject) JSONObject.toJSON(containerResponse);
        }
    }

    private List<JSONObject> listNetworksCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.build(parameter);
        try {
            ListNetworksCmd listNetworksCmd = dockerClient.listNetworksCmd();

            String name = (String) parameter.get("name");
            if (StrUtil.isNotEmpty(name)) {
                listNetworksCmd.withNameFilter(name);
            }
            String id = (String) parameter.get("id");
            if (StrUtil.isNotEmpty(id)) {
                listNetworksCmd.withIdFilter(id);
            }
            List<Network> networks = listNetworksCmd.exec();
            networks = ObjectUtil.defaultIfNull(networks, new ArrayList<>());
            return networks.stream().map(container -> (JSONObject) JSONObject.toJSON(container)).collect(Collectors.toList());
        } finally {
            IoUtil.close(dockerClient);
        }
    }

    public void pullImageCmd(Map<String, Object> parameter) throws InterruptedException {
        DockerClient dockerClient = DockerUtil.build(parameter);
        try {
            Consumer<String> logConsumer = (Consumer<String>) parameter.get("logConsumer");
            String repository = (String) parameter.get("repository");
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(repository);
            pullImageCmd.exec(new InvocationBuilder.AsyncResultCallback<PullResponseItem>() {
                @Override
                public void onNext(PullResponseItem object) {
                    String responseItem = DockerUtil.parseResponseItem(object);
                    logConsumer.accept(responseItem);
                }

            }).awaitCompletion();
        } finally {
            IoUtil.close(dockerClient);
        }
    }


    private void createContainerCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.build(parameter);
        try {
            String imageId = (String) parameter.get("imageId");
            String name = (String) parameter.get("name");
            String exposedPorts = (String) parameter.get("exposedPorts");
            String volumes = (String) parameter.get("volumes");
            Object autorunStr = parameter.get("autorun");
            Map<String, String> env = (Map<String, String>) parameter.get("env");
            //
            CreateContainerCmd containerCmd = dockerClient.createContainerCmd(imageId);
            CreateContainerCmd createContainerCmd = containerCmd.withName(name);

            HostConfig hostConfig = HostConfig.newHostConfig();
            if (StrUtil.isNotEmpty(exposedPorts)) {
                List<PortBinding> portBindings = StrUtil.splitTrim(exposedPorts, StrUtil.COMMA)
                    .stream()
                    .map(PortBinding::parse)
                    .collect(Collectors.toList());
                hostConfig.withPortBindings(portBindings);
            }
            if (StrUtil.isNotEmpty(volumes)) {
                List<Bind> binds = StrUtil.splitTrim(volumes, StrUtil.COMMA)
                    .stream()
                    .map(Bind::parse)
                    .collect(Collectors.toList());
                hostConfig.withBinds(binds);
            }
            // 环境变量
            if (env != null) {
                List<String> envList = env.entrySet()
                    .stream()
                    .map(entry -> StrUtil.format("{}={}", entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());
                containerCmd.withEnv(envList);
            }
            // 命令
            List<String> commands = (List<String>) parameter.get("commands");
            if (CollUtil.isNotEmpty(commands)) {
                containerCmd.withCmd(commands);
            }
            createContainerCmd.withHostConfig(hostConfig);
            CreateContainerResponse containerResponse = containerCmd.exec();
            //
            boolean autorun = Convert.toBool(autorunStr, false);
            if (autorun) {
                //
                dockerClient.startContainerCmd(containerResponse.getId()).exec();
            }
        } finally {
            IoUtil.close(dockerClient);
        }
    }

    private JSONObject inspectImageCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.build(parameter);
        try {
            String imageId = (String) parameter.get("imageId");
            InspectImageCmd inspectImageCmd = dockerClient.inspectImageCmd(imageId);
            InspectImageResponse inspectImageResponse = inspectImageCmd.exec();
            return (JSONObject) JSONObject.toJSON(inspectImageResponse);
        } finally {
            IoUtil.close(dockerClient);
        }
    }

    private void buildImageCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.build(parameter);
        Consumer<String> logConsumer = (Consumer<String>) parameter.get("logConsumer");
        File dockerfile = (File) parameter.get("Dockerfile");
        File baseDirectory = (File) parameter.get("baseDirectory");
        String tags = (String) parameter.get("tags");

        try {
            BuildImageCmd buildImageCmd = dockerClient.buildImageCmd();
            buildImageCmd
                .withBaseDirectory(baseDirectory)
                .withDockerfile(dockerfile)
                .withTags(CollUtil.newHashSet(StrUtil.splitTrim(tags, StrUtil.COMMA)));
            buildImageCmd.exec(new InvocationBuilder.AsyncResultCallback<BuildResponseItem>() {
                @Override
                public void onNext(BuildResponseItem object) {
                    String responseItem = DockerUtil.parseResponseItem(object);
                    logConsumer.accept(responseItem);
                }
            }).awaitCompletion();
        } catch (InterruptedException e) {
            logConsumer.accept("容器 build 被中断:" + e);
        } finally {
            IoUtil.close(dockerClient);
        }
    }

    private void execCreateCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.build(parameter);
        Consumer<String> logConsumer = (Consumer<String>) parameter.get("logConsumer");
        Consumer<String> errorConsumer = (Consumer<String>) parameter.get("errorConsumer");
        try {
            String containerId = (String) parameter.get("containerId");
            Charset charset = (Charset) parameter.get("charset");
            InputStream stdin1 = (InputStream) parameter.get("stdin");
            //
            ExecCreateCmd execCreateCmd = dockerClient.execCreateCmd(containerId);
            execCreateCmd.withAttachStdout(true).withAttachStdin(true).withAttachStderr(true).withTty(true).withCmd("/bin/sh");
            ExecCreateCmdResponse exec = execCreateCmd.exec();
            //
            String execId = exec.getId();
            ExecStartCmd execStartCmd = dockerClient.execStartCmd(execId);
            execStartCmd.withDetach(false).withTty(true).withStdIn(stdin1);

            execStartCmd.exec(new InvocationBuilder.AsyncResultCallback<Frame>() {
                @Override
                public void onNext(Frame frame) {
                    String s = new String(frame.getPayload(), charset);
                    logConsumer.accept(s);
                }
            }).awaitCompletion();
        } catch (InterruptedException e) {
            errorConsumer.accept("容器cli被中断:" + e);
        } finally {
            IoUtil.close(dockerClient);
            errorConsumer.accept("exit");
        }
    }

    private void logContainerCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.build(parameter);
        Consumer<String> consumer = (Consumer<String>) parameter.get("consumer");
        try {
            String containerId = (String) parameter.get("containerId");
            Charset charset = (Charset) parameter.get("charset");
            Integer tail = (Integer) parameter.get("tail");
            DockerClientUtil.pullLog(dockerClient, containerId, tail, charset, consumer);
        } catch (InterruptedException e) {
            consumer.accept("获取容器日志被中断:" + e);
        } finally {
            IoUtil.close(dockerClient);
        }
    }

    private List<JSONObject> listVolumesCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.build(parameter);
        try {
            ListVolumesCmd listVolumesCmd = dockerClient.listVolumesCmd();
            Boolean dangling = Convert.toBool(parameter.get("dangling"), false);
            if (dangling) {
                listVolumesCmd.withDanglingFilter(dangling);
            }
            String name = (String) parameter.get("name");
            if (StrUtil.isNotEmpty(name)) {
                listVolumesCmd.withFilter("name", CollUtil.newArrayList(name));
            }

            ListVolumesResponse exec = listVolumesCmd.exec();
            List<InspectVolumeResponse> volumes = exec.getVolumes();
            volumes = ObjectUtil.defaultIfNull(volumes, new ArrayList<>());
            return volumes.stream().map((Function<InspectVolumeResponse, Object>) inspectVolumeResponse -> {
                InspectVolumeCmd inspectVolumeCmd = dockerClient.inspectVolumeCmd(inspectVolumeResponse.getName());
                return inspectVolumeCmd.exec();
            }).map(container -> (JSONObject) JSONObject.toJSON(container)).collect(Collectors.toList());
        } finally {
            IoUtil.close(dockerClient);
        }
    }

    private void removeVolumeCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.build(parameter);
        try {
            String volumeName = (String) parameter.get("volumeName");
            dockerClient.removeVolumeCmd(volumeName).exec();
        } finally {
            IoUtil.close(dockerClient);
        }
    }


    private List<JSONObject> listImagesCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.build(parameter);
        try {
            ListImagesCmd listImagesCmd = dockerClient.listImagesCmd();
            listImagesCmd.withShowAll(Convert.toBool(parameter.get("showAll"), true));
            listImagesCmd.withDanglingFilter(Convert.toBool(parameter.get("dangling"), false));

            String name = (String) parameter.get("name");
            if (StrUtil.isNotEmpty(name)) {
                listImagesCmd.withImageNameFilter(name);
            }
            List<Image> exec = listImagesCmd.exec();
            exec = ObjectUtil.defaultIfNull(exec, new ArrayList<>());
            return exec.stream().map(container -> (JSONObject) JSONObject.toJSON(container)).collect(Collectors.toList());
        } finally {
            IoUtil.close(dockerClient);
        }
    }

    private void removeImageCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.build(parameter);
        try {
            String imageId = (String) parameter.get("imageId");
            dockerClient.removeImageCmd(imageId).withForce(true).exec();
        } finally {
            IoUtil.close(dockerClient);
        }
    }


    private List<JSONObject> listContainerCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.build(parameter);
        try {
            ListContainersCmd listContainersCmd = dockerClient.listContainersCmd();
            listContainersCmd.withShowAll(Convert.toBool(parameter.get("showAll"), true));
            String name = (String) parameter.get("name");
            if (StrUtil.isNotEmpty(name)) {
                listContainersCmd.withNameFilter(CollUtil.newArrayList(name));
            }
            String containerId = (String) parameter.get("containerId");
            if (StrUtil.isNotEmpty(containerId)) {
                listContainersCmd.withIdFilter(CollUtil.newArrayList(containerId));
            }
//		String imageId = (String) parameter.get("imageId");
//		if (StrUtil.isNotEmpty(imageId)) {
//			listContainersCmd.withFilter("image", CollUtil.newArrayList(imageId));
//		}
            List<Container> exec = listContainersCmd.exec();
            exec = ObjectUtil.defaultIfNull(exec, new ArrayList<>());
            return exec.stream().map(container -> (JSONObject) JSONObject.toJSON(container)).collect(Collectors.toList());
        } finally {
            IoUtil.close(dockerClient);
        }
    }

    private void restartContainerCmd(Map<String, Object> parameter) {
        String containerId = (String) parameter.get("containerId");
        DockerClient dockerClient = DockerUtil.build(parameter);
        try {
            dockerClient.restartContainerCmd(containerId).exec();
        } finally {
            IoUtil.close(dockerClient);
        }
    }

    private void startContainerCmd(Map<String, Object> parameter) {
        String containerId = (String) parameter.get("containerId");
        DockerClient dockerClient = DockerUtil.build(parameter);
        try {
            dockerClient.startContainerCmd(containerId).exec();
        } finally {
            IoUtil.close(dockerClient);
        }
    }

    private void stopContainerCmd(Map<String, Object> parameter) {
        String containerId = (String) parameter.get("containerId");
        DockerClient dockerClient = DockerUtil.build(parameter);
        try {
            dockerClient.stopContainerCmd(containerId).exec();
        } finally {
            IoUtil.close(dockerClient);
        }
    }

    /**
     * 删除容器
     *
     * @param parameter 参数
     */
    private void removeContainerCmd(Map<String, Object> parameter) {
        String containerId = (String) parameter.get("containerId");
        DockerClient dockerClient = DockerUtil.build(parameter);
        try {
            DockerClientUtil.removeContainerCmd(dockerClient, containerId);
        } finally {
            IoUtil.close(dockerClient);
        }
    }

}
