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
package org.dromara.jpom;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.exceptions.InvocationTargetRuntimeException;
import cn.hutool.core.io.unit.DataSizeUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.core.util.*;
import cn.keepbx.jpom.plugins.PluginConfig;
import com.alibaba.fastjson2.JSONObject;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.InvocationBuilder;
import com.github.dockerjava.core.NameParser;
import lombok.Lombok;
import lombok.SneakyThrows;
import org.dromara.jpom.util.StringUtil;
import org.springframework.util.Assert;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.*;
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
public class DefaultDockerPluginImpl implements IDockerConfigPlugin {


    @Override
    public Object execute(Object main, Map<String, Object> parameter) throws Exception {
        String type = main.toString();
        if ("build".equals(type)) {
            try (DockerBuild dockerBuild = new DockerBuild(parameter, this)) {
                return dockerBuild.build();
            }
        }
        Method method = ReflectUtil.getMethodByName(this.getClass(), type + "Cmd");
        Assert.notNull(method, "不支持的类型:" + type);
        try {
            return ReflectUtil.invoke(this, method, parameter);
        } catch (InvocationTargetRuntimeException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof InvocationTargetException) {
                InvocationTargetException invocationTargetException = (InvocationTargetException) cause;
                throw Lombok.sneakyThrow(invocationTargetException.getTargetException());
            }
            throw Lombok.sneakyThrow(cause);
        }
    }

    /**
     * 裁剪
     * <a href="https://blog.csdn.net/zhanremo3062/article/details/120860327">https://blog.csdn.net/zhanremo3062/article/details/120860327</a>
     *
     * @param parameter 参数
     * @return 回收空间
     */
    private Long pruneCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);
        String pruneTypeStr = (String) parameter.get("pruneType");

        PruneType pruneType = EnumUtil.fromString(PruneType.class, pruneTypeStr, null);
        Assert.notNull(pruneType, "pruneType 未知");
        String until = (String) parameter.get("until");
        String labels = (String) parameter.get("labels");
        String dangling = (String) parameter.get("dangling");
        PruneCmd pruneCmd = dockerClient.pruneCmd(pruneType);
        Opt.ofBlankAble(dangling).map(s -> Convert.toBool(s, true)).ifPresent(pruneCmd::withDangling);
        Opt.ofBlankAble(until).ifPresent(pruneCmd::withUntilFilter);
        Opt.ofBlankAble(labels).map(s -> StrUtil.splitToArray(s, StrUtil.COMMA)).ifPresent(pruneCmd::withLabelFilter);
        PruneResponse pruneResponse = pruneCmd.exec();
        return pruneResponse.getSpaceReclaimed();
    }

    private Map<String, JSONObject> statsCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);
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
            return new Tuple(s, DockerUtil.toJSON(statistics));
        }).collect(Collectors.toMap(tuple -> tuple.get(0), tuple -> tuple.get(1)));
    }

    private JSONObject updateContainerCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);
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
        return DockerUtil.toJSON(updateContainerResponse);
    }

    private JSONObject inspectContainerCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);
        String containerId = (String) parameter.get("containerId");
        InspectContainerResponse containerResponse = dockerClient.inspectContainerCmd(containerId).withSize(true).exec();
        return DockerUtil.toJSON(containerResponse);
    }

    private List<JSONObject> listNetworksCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);

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
        return networks.stream().map(DockerUtil::toJSON).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public void pullImageCmd(Map<String, Object> parameter) throws InterruptedException {
        DockerClient dockerClient = DockerUtil.get(parameter);

        Consumer<String> logConsumer = (Consumer<String>) parameter.get("logConsumer");
        String repositoryStr = (String) parameter.get("repository");
        Assert.hasText(repositoryStr, "请填写镜名称");
        NameParser.ReposTag reposTag = NameParser.parseRepositoryTag(repositoryStr);
        // 解析 tag
        String tag = reposTag.tag;
        tag = StrUtil.emptyToDefault(tag, "latest");
        logConsumer.accept(StrUtil.format("start pull {}:{}", reposTag.repos, tag));
        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(reposTag.repos)
            .withTag(tag)
            .withAuthConfig(dockerClient.authConfig());
        pullImageCmd.exec(new InvocationBuilder.AsyncResultCallback<PullResponseItem>() {
            @Override
            public void onNext(PullResponseItem object) {
                String responseItem = DockerUtil.parseResponseItem(object);
                logConsumer.accept(responseItem);
            }

        }).awaitCompletion();
    }


    @SuppressWarnings(value = {"unchecked"})
    private void createContainerCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);

        String imageId = (String) parameter.get("imageId");
        String name = (String) parameter.get("name");
        String exposedPorts = (String) parameter.get("exposedPorts");
        String volumes = (String) parameter.get("volumes");
        String networkMode = (String) parameter.get("networkMode");
        Object autorunStr = parameter.get("autorun");
        Object privileged = parameter.get("privileged");
        String restartPolicy = (String) parameter.get("restartPolicy");
        Map<String, String> env = (Map<String, String>) parameter.get("env");
        Map<String, String> storageOpt = (Map<String, String>) parameter.get("storageOpt");
        String labels = (String) parameter.get("labels");
        String runtime = (String) parameter.get("runtime");
        //
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(imageId);
        containerCmd.withName(name);
        Opt.ofBlankAble(labels)
            .map(s -> UrlQuery.of(s, CharsetUtil.CHARSET_UTF_8))
            .map(UrlQuery::getQueryMap)
            .map((Function<Map<CharSequence, CharSequence>, Map<String, String>>) map -> {
                HashMap<String, String> labelMap = MapUtil.newHashMap();
                for (Map.Entry<CharSequence, CharSequence> entry : map.entrySet()) {
                    labelMap.put(StrUtil.toString(entry.getKey()), StrUtil.toString(entry.getValue()));
                }
                return labelMap;
            })
            .ifPresent(containerCmd::withLabels);
        String hostname = (String) parameter.get("hostname");
        Opt.ofBlankAble(hostname).ifPresent(containerCmd::withHostName);
        HostConfig hostConfig = HostConfig.newHostConfig();
        Opt.ofBlankAble(runtime).ifPresent(hostConfig::withRuntime);
        List<ExposedPort> exposedPortList = new ArrayList<>();
        if (StrUtil.isNotEmpty(exposedPorts)) {
            List<PortBinding> portBindings = StrUtil.splitTrim(exposedPorts, StrUtil.COMMA)
                .stream()
                .map(PortBinding::parse)
                .peek(portBinding -> exposedPortList.add(portBinding.getExposedPort()))
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
        Opt.ofBlankAble(networkMode).ifPresent(hostConfig::withNetworkMode);
        Optional.ofNullable(privileged).map(o -> Convert.toBool(o, false)).ifPresent(hostConfig::withPrivileged);
        Opt.ofBlankAble(restartPolicy).map(RestartPolicy::parse).ifPresent(hostConfig::withRestartPolicy);
        // 环境变量
        if (env != null) {
            List<String> envList = env.entrySet()
                .stream()
                .map(entry -> StrUtil.format("{}={}", entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
            containerCmd.withEnv(envList);
        }
        Optional.ofNullable(storageOpt).map(map -> {
            if (MapUtil.isEmpty(map)) {
                // 空参数不能传入，避免低版本不支持
                return null;
            }
            return map;
        }).ifPresent(hostConfig::withStorageOpt);

        // 命令
        List<String> commands = (List<String>) parameter.get("commands");
        Optional.ofNullable(commands).ifPresent(strings -> {
            List<String> list = strings.stream()
                .filter(StrUtil::isNotEmpty)
                .collect(Collectors.toList());
            if (CollUtil.isNotEmpty(list)) {
                containerCmd.withCmd(list);
            }
        });

        containerCmd.withHostConfig(hostConfig).withExposedPorts(exposedPortList);
        CreateContainerResponse containerResponse = containerCmd.exec();
        //
        boolean autorun = Convert.toBool(autorunStr, false);
        if (autorun) {
            //
            dockerClient.startContainerCmd(containerResponse.getId()).exec();
        }

    }

    private JSONObject inspectImageCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);

        String imageId = (String) parameter.get("imageId");
        InspectImageCmd inspectImageCmd = dockerClient.inspectImageCmd(imageId);
        InspectImageResponse inspectImageResponse = inspectImageCmd.exec();
        return DockerUtil.toJSON(inspectImageResponse);
    }

    @SuppressWarnings("unchecked")
    private void pushImageCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);
        Consumer<String> logConsumer = (Consumer<String>) parameter.get("logConsumer");
        String repository = (String) parameter.get("repository");
        try {
            dockerClient.pushImageCmd(repository).exec(new InvocationBuilder.AsyncResultCallback<PushResponseItem>() {
                @Override
                public void onNext(PushResponseItem object) {
                    String responseItem = DockerUtil.parseResponseItem(object);
                    logConsumer.accept(responseItem);
                }
            }).awaitCompletion();
        } catch (InterruptedException e) {
            logConsumer.accept("push image 被中断:" + e);
        }
    }

    /**
     * <a href="http://edu.jb51.net/docker/docker-command-manual-build.html">http://edu.jb51.net/docker/docker-command-manual-build.html</a>
     * 构建镜像
     *
     * @param parameter 参数
     */
    @SuppressWarnings("unchecked")
    private void buildImageCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);
        Consumer<String> logConsumer = (Consumer<String>) parameter.get("logConsumer");
        File dockerfile = (File) parameter.get("Dockerfile");
        File baseDirectory = (File) parameter.get("baseDirectory");
        String tags = (String) parameter.get("tags");
        String buildArgs = (String) parameter.get("buildArgs");
        Object pull = parameter.get("pull");
        Object noCache = parameter.get("noCache");
        String labels = (String) parameter.get("labels");
        Map<String, String> env = (Map<String, String>) parameter.get("env");

        try {
            AuthConfigurations authConfigurations = new AuthConfigurations();
            authConfigurations.addConfig(dockerClient.authConfig());

            BuildImageCmd buildImageCmd = dockerClient.buildImageCmd();
            buildImageCmd
                .withBaseDirectory(baseDirectory)
                .withDockerfile(dockerfile)
                .withBuildAuthConfigs(authConfigurations)
                .withTags(CollUtil.newHashSet(StrUtil.splitTrim(tags, StrUtil.COMMA)));
            // 添加构建参数
            UrlQuery query = UrlQuery.of(buildArgs, CharsetUtil.CHARSET_UTF_8);
            query.getQueryMap()
                .forEach((key, value) -> {
                    String valueStr = StrUtil.toString(value);
                    valueStr = StringUtil.formatStrByMap(valueStr, env);
                    buildImageCmd.withBuildArg(StrUtil.toString(key), valueStr);
                });
            // 标签
            UrlQuery labelsQuery = UrlQuery.of(labels, CharsetUtil.CHARSET_UTF_8);
            HashMap<String, String> labelMap = MapUtil.newHashMap();
            labelsQuery.getQueryMap().forEach((key, value) -> {
                String valueStr = StrUtil.toString(value);
                valueStr = StringUtil.formatStrByMap(valueStr, env);
                labelMap.put(StrUtil.toString(key), valueStr);
            });
            buildImageCmd.withLabels(labelMap);
            //
            Optional.ofNullable(pull).map(Convert::toBool).ifPresent(buildImageCmd::withPull);
            Optional.ofNullable(noCache).map(Convert::toBool).ifPresent(buildImageCmd::withNoCache);

            buildImageCmd.exec(new InvocationBuilder.AsyncResultCallback<BuildResponseItem>() {
                @Override
                public void onNext(BuildResponseItem object) {
                    String responseItem = DockerUtil.parseResponseItem(object);
                    logConsumer.accept(responseItem);
                }
            }).awaitCompletion();
        } catch (InterruptedException e) {
            logConsumer.accept("容器 build 被中断:" + e);
        }
    }

    @SuppressWarnings("unchecked")
    private void execCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);
        Consumer<String> logConsumer = (Consumer<String>) parameter.get("logConsumer");
        Consumer<String> errorConsumer = (Consumer<String>) parameter.get("errorConsumer");
        try {
            String containerId = (String) parameter.get("containerId");
            Charset charset = (Charset) parameter.get("charset");
            InputStream stdin1 = (InputStream) parameter.get("stdin");
            //
            ExecCreateCmd execCreateCmd = dockerClient.execCreateCmd(containerId);
            execCreateCmd.withAttachStdout(true)
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withTty(true)
                .withCmd("/bin/bash");
            ExecCreateCmdResponse exec = execCreateCmd.exec();
            //
            String execId = exec.getId();
            ExecStartCmd execStartCmd = dockerClient.execStartCmd(execId);
            execStartCmd.withDetach(false).withTty(true).withStdIn(stdin1);
            logConsumer.accept(StrUtil.format("CALLBACK_EXECID:{}", execId));
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
            errorConsumer.accept("exit");
        }
    }

    /**
     * 检查 终端
     *
     * @param parameter 参数
     */
    private void inspectExecCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);
        String execId = (String) parameter.get("execId");
        //
        InspectExecCmd inspectExecCmd = dockerClient.inspectExecCmd(execId);
        inspectExecCmd.exec().isRunning();
    }

    /**
     * 中断 终端
     *
     * @param parameter 参数
     */
    private void resizeExecCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);
        String execId = (String) parameter.get("execId");
        //
        ResizeExecCmd resizeExecCmd = dockerClient.resizeExecCmd(execId);
        Integer sizeHeight = (Integer) parameter.get("sizeHeight");
        Integer sizeWidth = (Integer) parameter.get("sizeWidth");
        resizeExecCmd.withSize(sizeHeight, sizeWidth).exec();
    }

    @SuppressWarnings("unchecked")
    private void logContainerCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);
        String uuid = (String) parameter.get("uuid");
        Consumer<String> consumer = (Consumer<String>) parameter.get("consumer");
        try {
            String containerId = (String) parameter.get("containerId");
            Charset charset = (Charset) parameter.get("charset");
            Integer tail = (Integer) parameter.get("tail");
            Boolean timestamps = Convert.toBool(parameter.get("timestamps"));
            DockerClientUtil.pullLog(dockerClient, containerId, timestamps, tail, charset, consumer, autoCloseable -> DockerUtil.putClose(uuid, autoCloseable));
        } catch (InterruptedException e) {
            consumer.accept("获取容器日志被中断:" + e);
        } finally {
            DockerUtil.close(uuid);
        }
    }

    private List<JSONObject> listVolumesCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);

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
        }).map(DockerUtil::toJSON).collect(Collectors.toList());

    }

    private void removeVolumeCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);

        String volumeName = (String) parameter.get("volumeName");
        dockerClient.removeVolumeCmd(volumeName).exec();

    }


    private List<JSONObject> listImagesCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);

        ListImagesCmd listImagesCmd = dockerClient.listImagesCmd();
        listImagesCmd.withShowAll(Convert.toBool(parameter.get("showAll"), true));
        listImagesCmd.withDanglingFilter(Convert.toBool(parameter.get("dangling"), false));

        String name = (String) parameter.get("name");
        if (StrUtil.isNotEmpty(name)) {
            listImagesCmd.withImageNameFilter(name);
        }
        List<Image> exec = listImagesCmd.exec();
        exec = ObjectUtil.defaultIfNull(exec, new ArrayList<>());
        return exec.stream().map(DockerUtil::toJSON).collect(Collectors.toList());
    }

    private void removeImageCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);

        String imageId = (String) parameter.get("imageId");
        dockerClient.removeImageCmd(imageId).withForce(true).exec();
    }

    /**
     * 不包含 docker compose
     *
     * @param parameter 参数
     * @return list
     */
    private List<JSONObject> listContainerCmd(Map<String, Object> parameter) {
        List<JSONObject> list = this.listContainerByLabelCmd(parameter, null);
        String composeLabel = "com.docker.compose.project";
        return list.stream()
            .filter(jsonObject -> {
                JSONObject labels = jsonObject.getJSONObject("labels");
                String project = MapUtil.get(labels, composeLabel, String.class);
                return project == null;
            })
            .collect(Collectors.toList());
    }

    private List<JSONObject> listContainerByLabelCmd(Map<String, Object> parameter, String label) {
        DockerClient dockerClient = DockerUtil.get(parameter);

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

        Opt.ofBlankAble(label).ifPresent(s -> {
            // 只筛选 docker compose
            listContainersCmd.withLabelFilter(CollUtil.newArrayList(s));
        });
        String imageId = (String) parameter.get("imageId");
        List<Container> exec = listContainersCmd.exec();
        exec = ObjectUtil.defaultIfNull(exec, new ArrayList<>());
        return exec.stream()
            .map(DockerUtil::toJSON)
            .filter(jsonObject -> {
                if (StrUtil.isEmpty(imageId)) {
                    return true;
                }
                String imageId1 = jsonObject.getString("imageId");
                return StrUtil.contains(imageId1, imageId);
            })
            .collect(Collectors.toList());
    }

    /**
     * 不包含 docker compose
     *
     * @param parameter 参数
     * @return list
     */
    private List<JSONObject> listComposeContainerCmd(Map<String, Object> parameter) {
        String composeLabel = "com.docker.compose.project";
        List<JSONObject> list = this.listContainerByLabelCmd(parameter, composeLabel);
        //
        Map<String, List<JSONObject>> map = CollStreamUtil.groupKeyValue(list, jsonObject -> {
            JSONObject labels = jsonObject.getJSONObject("labels");
            return Optional.ofNullable(labels)
                .map(jsonObject1 -> jsonObject1.getString(composeLabel))
                .orElse("null");
        }, jsonObject -> jsonObject);
        //
        return map.entrySet().stream()
            .map(stringListEntry -> {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", stringListEntry.getKey());
                jsonObject.put("child", stringListEntry.getValue());
                return jsonObject;
            })
            .collect(Collectors.toList());
    }

    private void restartContainerCmd(Map<String, Object> parameter) {
        String containerId = (String) parameter.get("containerId");
        DockerClient dockerClient = DockerUtil.get(parameter);

        dockerClient.restartContainerCmd(containerId).exec();
    }

    private void startContainerCmd(Map<String, Object> parameter) {
        String containerId = (String) parameter.get("containerId");
        DockerClient dockerClient = DockerUtil.get(parameter);

        dockerClient.startContainerCmd(containerId).exec();
    }

    private void stopContainerCmd(Map<String, Object> parameter) {
        String containerId = (String) parameter.get("containerId");
        DockerClient dockerClient = DockerUtil.get(parameter);

        dockerClient.stopContainerCmd(containerId).exec();
    }

    /**
     * 删除容器
     *
     * @param parameter 参数
     */
    private void removeContainerCmd(Map<String, Object> parameter) {
        String containerId = (String) parameter.get("containerId");
        DockerClient dockerClient = DockerUtil.get(parameter);

        DockerClientUtil.removeContainerCmd(dockerClient, containerId);

    }

    /**
     * 关闭异步资源
     *
     * @param parameter 参数
     */
    private void closeAsyncResourceCmd(Map<String, Object> parameter) {
        String uuid = (String) parameter.get("uuid");
        DockerUtil.close(uuid);
    }
}
