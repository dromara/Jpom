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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.system.SystemUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import org.dromara.jpom.util.LogRecorder;
import org.dromara.jpom.util.StringUtil;
import org.springframework.util.Assert;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 容器构建
 *
 * @author bwcx_jzy
 * @since 2022/2/7
 */
public class DockerBuild implements AutoCloseable {

    private final Map<String, Object> parameter;
    private final DockerClient dockerClient;
    private final Map<String, String> env;
    private final IDockerConfigPlugin plugin;

    public DockerBuild(Map<String, Object> parameter, IDockerConfigPlugin plugin) {
        this.parameter = parameter;
        this.dockerClient = DockerUtil.get(parameter);
        this.env = (Map<String, String>) parameter.get("env");
        this.plugin = plugin;
    }

    public int build() {

        LogRecorder logRecorder = (LogRecorder) parameter.get("logRecorder");
        File tempDir = (File) parameter.get("tempDir");
        // 生成临时目录
        tempDir = FileUtil.file(tempDir, "docker-temp", IdUtil.fastSimpleUUID());
        List<String> copy = (List<String>) parameter.get("copy");
        String resultFile = (String) parameter.get("resultFile");
        String resultFileOut = (String) parameter.get("resultFileOut");
        List<Map<String, Object>> steps = (List<Map<String, Object>>) parameter.get("steps");

        String buildId = env.get("JPOM_BUILD_ID");
        String runsOn = (String) parameter.get("runsOn");

        String image = String.format("jpomdocker/runs_%s", runsOn);
        parameter.put("image", image);
        String containerId = null;
        try {
            this.buildRunOn(dockerClient, runsOn, image, tempDir, logRecorder);
            List<Mount> mounts = new ArrayList<>();
            // 缓存目录
            List<Mount> cacheMount = this.cachePluginCheck(dockerClient, steps, buildId);
            mounts.addAll(cacheMount);
            // 添加插件到 mount
            mounts.addAll(this.checkDependPlugin(dockerClient, image, steps, buildId, tempDir, logRecorder));
            containerId = this.buildNewContainer(dockerClient, parameter, mounts);

            String buildShell = this.generateBuildShell(steps, buildId);
            File tempFile = DockerUtil.createTemp("build.sh", tempDir);
            FileUtil.writeUtf8String(buildShell, tempFile);
            dockerClient.copyArchiveToContainerCmd(containerId)
                .withHostResource(tempFile.getAbsolutePath())
                .withRemotePath("/tmp/")
                .exec();
            //
            copy = this.replaceEnv(copy);
            this.copyArchiveToContainerCmd(dockerClient, containerId, copy, logRecorder);
            // 启动容器
            try {
                dockerClient.startContainerCmd(containerId).exec();
            } catch (RuntimeException e) {
                logRecorder.error("容器启动失败:", e);
                return -101;
            }
            // 获取日志
            this.pullLog(dockerClient, containerId, logRecorder);
            // 等待容器执行结果
            int statusCode = this.waitContainerCmd(dockerClient, containerId, logRecorder);
            // 获取容器执行结果文件
            DockerClientUtil.copyArchiveFromContainerCmd(dockerClient, containerId, logRecorder, resultFile, resultFileOut);
            return statusCode;
        } finally {
            DockerClientUtil.removeContainerCmd(dockerClient, containerId);
            // 删除临时目录
            FileUtil.del(tempDir);
        }
    }

    private List<String> replaceEnv(List<String> list) {
        if (env == null) {
            return list;
        }
        // 处理变量
        return list.stream().map(this::replaceEnv).collect(Collectors.toList());
    }

    private String replaceEnv(String value) {
        if (env == null) {
            return value;
        }
        // 处理变量
        for (Map.Entry<?, ?> envEntry : env.entrySet()) {
            String envValue = StrUtil.utf8Str(envEntry.getValue());
            if (null == envValue) {
                continue;
            }
            value = StrUtil.replace(value, "${" + envEntry.getKey() + "}", envValue);
        }
        return value;
    }


    /**
     * 构建一个执行 镜像
     *
     * @param dockerClient docker 客户端连接
     * @param parameter    参数
     * @param mounts       挂载目录
     * @return 容器ID
     */
    private String buildNewContainer(DockerClient dockerClient, Map<String, Object> parameter, List<Mount> mounts) {
        String image = (String) parameter.get("image");
        String workingDir = (String) parameter.get("workingDir");
        String dockerName = (String) parameter.get("dockerName");
        List<String> binds = (List<String>) parameter.get("binds");

        Map<String, String> env = (Map<String, String>) parameter.get("env");
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        containerCmd.withName(dockerName).withWorkingDir(workingDir);
        //
        List<Bind> bindList = new ArrayList<>();
        if (CollUtil.isNotEmpty(binds)) {
            bindList = this.replaceEnv(binds).stream()
                .map(Bind::parse)
                .collect(Collectors.toList());
        }

        HostConfig hostConfig = HostConfig.newHostConfig()
            .withMounts(mounts).withBinds(bindList);
        // 一定不能使用 auto remove 否则无法下载容器构建产物
        // hostConfig.withAutoRemove(true);
        containerCmd.withHostConfig(hostConfig);
        // 环境变量
        if (env != null) {
            List<String> envList = env.entrySet()
                .stream()
                .map(entry -> StrUtil.format("{}={}", entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
            containerCmd.withEnv(envList);
        }
        // 如果容器已经存在则先删除
        try {
            InspectContainerResponse exec = dockerClient.inspectContainerCmd(dockerName).exec();
            DockerClientUtil.removeContainerCmd(dockerClient, exec.getId());
        } catch (NotFoundException ignored) {
        }
        // 创建容器
        CreateContainerResponse containerResponse;
        containerResponse = containerCmd.exec();
        return containerResponse.getId();
    }

    /**
     * 将本地 文件 上传到 容器
     *
     * @param dockerClient docker 连接
     * @param containerId  容器ID
     * @param copy         需要 上传到文件信息
     */
    private void copyArchiveToContainerCmd(DockerClient dockerClient, String containerId, List<String> copy, LogRecorder logRecorder) {
        if (copy == null || dockerClient == null) {
            return;
        }
        for (String s : copy) {
            // C:\Users\bwcx_\jpom\server\data\build\5c631117d4834dd4833c04dc1e6e635c\source:/home/jpom/:true
            String resource;
            String remotePath;
            boolean dirChildrenOnly;
            List<String> split = StrUtil.split(s, StrUtil.COLON);
            if (SystemUtil.getOsInfo().isWindows() && StrUtil.length(split.get(0)) == 1) {
                // 第一位是盘符
                resource = split.get(0) + StrUtil.COLON + split.get(1);
                remotePath = split.get(2);
                dirChildrenOnly = Convert.toBool(CollUtil.get(split, 3), true);
            } else {
                resource = split.get(0);
                remotePath = split.get(1);
                dirChildrenOnly = Convert.toBool(CollUtil.get(split, 2), true);
            }
            logRecorder.system("send file from : {} to : {}", resource, remotePath);
            dockerClient.copyArchiveToContainerCmd(containerId)
                .withHostResource(resource)
                .withRemotePath(remotePath)
                .withDirChildrenOnly(dirChildrenOnly)
                .exec();
        }
    }

    /**
     * 生成执行构建的命令
     *
     * @param steps   执行步骤
     * @param buildId 构建ID
     * @return sh
     */
    private String generateBuildShell(List<Map<String, Object>> steps, String buildId) {
        StringBuilder stepsScript = new StringBuilder("#!/bin/bash\n");
        stepsScript.append("echo \"\n<<<<<<< Build Start >>>>>>>\"\n");
        //
        List<String> afterScriptList = new ArrayList<>();
        for (Map<String, Object> step : steps) {
            if (step.containsKey("env")) {
                stepsScript.append("# env\n");
                Map<String, String> env = (Map<String, String>) step.get("env");
                for (String key : env.keySet()) {
                    stepsScript.append(String.format("echo \"export %s=%s\" >> /etc/profile\n", key, env.get(key)));
                }
            }
            stepsScript.append("source /etc/profile \n");
            if (step.containsKey("uses")) {
                String uses = (String) step.get("uses");
                if ("node".equals(uses)) {
                    stepsScript.append(nodeScript(step));
                } else if ("java".equals(uses)) {
                    stepsScript.append(javaScript(step));
                } else if ("maven".equals(uses)) {
                    stepsScript.append(mavenScript(step));
                } else if ("cache".equals(uses)) {
                    // 缓存插件
                    String[] cacheScript = cacheScript(step, buildId);
                    stepsScript.append(cacheScript[0]);
                    afterScriptList.add(cacheScript[1]);
                } else if ("go".equals(uses)) {
                    stepsScript.append(goScript(step));
                } else if ("gradle".equals(uses)) {
                    stepsScript.append(gradleScript(step));
                } else if ("python3".equals(uses)) {
                    stepsScript.append(python3Script(step));
                } else {
                    // 其他自定义插件
                    stepsScript.append(otherScript(step, uses));
                }
            }
            if (step.containsKey("run")) {
                stepsScript.append("# run\n");
                String run = (String) step.get("run");
                stepsScript.append(run).append(" \n");
            }
        }
        // copy
        afterScriptList.forEach(stepsScript::append);
        stepsScript.append("echo \"<<<<<<< Build End >>>>>>>\"\n");
        return stepsScript.toString();
    }

    /**
     * 格式化其他插件端 脚本
     *
     * @param step 步骤
     * @param name 插件名
     * @return 结果
     */
    private String otherScript(Map<String, Object> step, String name) {
        String path = String.format("/opt/jpom_%s", name);
        StringBuilder script = new StringBuilder("# " + name + "Script\n");
        Map<String, String> map = new HashMap<>();
        map.put("JPOM_PLUGIN_PATH", path);
        for (Map.Entry<String, Object> entry : step.entrySet()) {
            String key = entry.getKey();
            String value = ObjectUtil.toString(entry.getValue());
            value = StringUtil.formatStrByMap(value, map);
            script.append(String.format("echo \"export %s=%s\" >> /etc/profile\n", key, value));
        }
        script.append(String.format("echo \"export PATH=%s/bin:$PATH\" >> /etc/profile\n", path));
        script.append("source /etc/profile \n");
        return script.toString();
    }


    private String mavenScript(Map<String, Object> step) {
        String version = String.valueOf(step.get("version"));
        String path = String.format("/opt/jpom_maven_%s", version);
        String script = "# mavenScript\n";
        script += String.format("echo \"export MAVEN_HOME=%s\" >> /etc/profile\n", path);
        script += String.format("echo \"export PATH=%s/bin:$PATH\" >> /etc/profile\n", path);
        script += "source /etc/profile \n";
        return script;
    }

    private String goScript(Map<String, Object> step) {
        String version = String.valueOf(step.get("version"));
        String path = String.format("/opt/jpom_go_%s", version);
        String script = "# goScript\n";
        script += String.format("echo \"export GOROOT=%s\" >> /etc/profile\n", path);
        script += String.format("echo \"export PATH=$PATH:%s/bin\" >> /etc/profile\n", path);
        script += "source /etc/profile \n";
        return script;
    }


    private String gradleScript(Map<String, Object> step) {
        String version = String.valueOf(step.get("version"));
        String path = String.format("/opt/jpom_gradle_%s", version);
        String script = "# gradleScript\n";
        script += String.format("echo \"export GRADLE_HOME=%s\" >> /etc/profile\n", path);
        script += String.format("echo \"export PATH=$PATH:%s/bin\" >> /etc/profile\n", path);
        script += "source /etc/profile \n";
        return script;
    }

    private String javaScript(Map<String, Object> step) {
        String version = String.valueOf(step.get("version"));
        String path = String.format("/opt/jpom_java_%s", version);
        String script = "# javaScript\n";
        script += String.format("echo \"export JAVA_HOME=%s\" >> /etc/profile\n", path);
        script += String.format("echo \"export PATH=%s/bin:$PATH\" >> /etc/profile\n", path);
        script += "source /etc/profile \n";
        return script;
    }

    private String python3Script(Map<String, Object> step) {
        String version = String.valueOf(step.get("version"));
        String path = String.format("/opt/jpom_python3_%s", version);
        String script = "# python3Script\n";
        script += String.format("echo \"export PYTHONPATH=%s\" >> /etc/profile\n", path);
        script += String.format("echo \"export PATH=%s/bin:$PATH\" >> /etc/profile\n", path);
        // pip3 `bad interpreter: no such file or directory:`
        script += String.format("sed -i \"1c#!%s/bin/python3\" %s/bin/pip3\n", path, path);
        script += "source /etc/profile \n";
        return script;
    }

    private String nodeScript(Map<String, Object> step) {
        String version = String.valueOf(step.get("version"));
        String path = String.format("/opt/jpom_node_%s", version);
        String script = "# nodeScript\n";
        script += String.format("echo \"export NODE_HOME=%s\" >> /etc/profile\n", path);
        script += String.format("echo \"export PATH=%s/bin:$PATH\" >> /etc/profile\n", path);
        script += "source /etc/profile \n";
        return script;
    }

    /**
     * 插件 runsOn 镜像
     *
     * @param dockerClient docker 连接
     * @param runsOn       基于容器镜像
     * @param image        镜像
     * @param tempDir      临时路径
     * @param logRecorder  日志记录
     */
    private void buildRunOn(DockerClient dockerClient, String runsOn, String image, File tempDir, LogRecorder logRecorder) {
        try {
            dockerClient.inspectImageCmd(image).exec();
        } catch (NotFoundException e) {
            HashSet<String> tags = new HashSet<>();
            tags.add(image);
            try {
                File file = plugin.getResourceToFile(String.format("runs/%s/" + DockerUtil.DOCKER_FILE, runsOn), tempDir);
                Assert.notNull(file, "当前还不支持：" + runsOn);
                dockerClient.buildImageCmd(file)
                    .withTags(tags)
                    .exec(new ResultCallback.Adapter<BuildResponseItem>() {
                        @Override
                        public void onNext(BuildResponseItem object) {
                            String responseItem = DockerUtil.parseResponseItem(object);
                            logRecorder.append(responseItem);
                        }
                    }).awaitCompletion();
            } catch (InterruptedException ex) {
                logRecorder.error("构建 runsOn 镜像被中断", ex);
            }
        }
    }

    private String[] cacheScript(Map<String, Object> step, String buildId) {
        String mode = String.valueOf(step.get("mode"));
        String path = String.format("/opt/%s", this.buildCacheName(step, buildId));
        String beforeScript = "# cacheScript\n";
        String afterScript = "";
        String cachePath = String.valueOf(step.get("path"));
        // 可能存在变量，替换为完整的值
        cachePath = this.replaceEnv(cachePath);
        if (StrUtil.equalsIgnoreCase(mode, "copy")) {
            // npm WARN reify Removing non-directory
            // https://github.com/npm/cli/issues/3669
            beforeScript += String.format("echo \"upload cache %s\"\n", cachePath);
            beforeScript += String.format("mkdir -p %s\n", cachePath);
            beforeScript += String.format("cp -rf %s/* %s \n", path, cachePath);
            // 执行构建完成后的命令，将缓存目录 copy 到卷中
            afterScript += "# cacheScript after\n";
            afterScript += String.format("echo \"download cache %s\"\n", cachePath);
            afterScript += String.format("cp -rf %s/* %s \n", cachePath, path);
        } else {
            beforeScript += String.format("ln -s %s %s \n", path, cachePath);
        }
        return new String[]{beforeScript, afterScript};
    }

    /**
     * 构建缓存插件名称
     *
     * @param step    缓存配置信息
     * @param buildId 构建id
     * @return 名称
     */
    private String buildCacheName(Map<String, Object> step, String buildId) {
        String path = (String) step.get("path");
        String type = StrUtil.toString(step.get("type"));
        // 全局模式
        boolean global = StrUtil.equalsIgnoreCase(type, "global");
        String md5 = SecureUtil.md5(path);
        return global ? String.format("jpom_cache_%s", md5) : String.format("jpom_cache_%s_%s", buildId, md5);
    }

    private List<Mount> cachePluginCheck(DockerClient dockerClient, List<Map<String, Object>> steps, String buildId) {
        return steps.stream()
            .filter(map -> {
                String uses = (String) map.get("uses");
                return StrUtil.equals("cache", uses);
            })
            .map(objMap -> {
                String name = this.buildCacheName(objMap, buildId);
                try {
                    dockerClient.inspectVolumeCmd(name).exec();
                } catch (NotFoundException e) {
                    HashMap<String, String> labels = MapUtil.of("jpom_build_" + buildId, buildId);
                    String path = (String) objMap.get("path");
                    String type = StrUtil.toString(objMap.get("type"));
                    labels.put("jpom_build_path", path);
                    labels.put("jpom_build_cache_type", type);
                    dockerClient.createVolumeCmd()
                        .withName(name)
                        .withLabels(labels)
                        .exec();
                }
                // 最终使用 ls 来绑定
                Mount mount = new Mount();
                mount.withType(MountType.VOLUME).withSource(name).withTarget("/opt/" + name);
                return mount;
            }).collect(Collectors.toList());
    }

    /**
     * 依赖插件检查
     *
     * @param dockerClient docker 客户端连接
     * @param image        执行镜像名称
     * @param steps        相关参数
     * @param tempDir      临时文件目录
     * @param logRecorder  日志记录器
     * @param buildId      构建ID
     * @return list mount
     */
    private List<Mount> checkDependPlugin(DockerClient dockerClient, String image, List<Map<String, Object>> steps, String buildId, File tempDir, LogRecorder logRecorder) {
        return steps.stream()
            .filter(map -> {
                String uses = (String) map.get("uses");
                return StrUtil.isNotEmpty(uses);
            })
            .filter(map -> {
                // 缓存插件不检查挂载
                String uses = (String) map.get("uses");
                return !StrUtil.equals("cache", uses);
            })
            .map(map -> this.dependPluginCheck(dockerClient, image, map, buildId, tempDir, logRecorder))
            .collect(Collectors.toList());
    }

    /**
     * 依赖插件检查，判断是否存在对应的 依赖插件
     *
     * @param parameter 参数
     * @return mount
     */
    public static boolean hasDependPlugin(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);
        String pluginName = (String) parameter.get("pluginName");
        String version = Optional.ofNullable(parameter.get("version"))
            .map(StrUtil::toStringOrNull)
            .map(s -> "_" + s)
            .orElse(StrUtil.EMPTY);
        String name = String.format("jpom_%s%s", pluginName, version);
        return hasDependPlugin(dockerClient, name);
    }

    /**
     * 依赖插件检查，判断是否存在对应的 依赖插件
     *
     * @param dockerClient docker 客户端连接
     * @param pluginName   插件端名称
     * @return mount
     */
    public static boolean hasDependPlugin(DockerClient dockerClient, String pluginName) {
        try {
            dockerClient.inspectVolumeCmd(pluginName).exec();
            return true;
        } catch (NotFoundException e) {
            return false;
        }
    }

    /**
     * 依赖插件检查
     *
     * @param dockerClient docker 客户端连接
     * @param image        执行镜像名称
     * @param usesMap      插件相关参数
     * @param tempDir      临时文件目录
     * @param logRecorder  日志记录器
     * @param buildId      构建ID
     * @return mount
     */
    private Mount dependPluginCheck(DockerClient dockerClient, String image, Map<String, Object> usesMap, String buildId, File tempDir, LogRecorder logRecorder) {
        String pluginName = (String) usesMap.get("uses");
        // 兼容没有版本号
        String version = Optional.ofNullable(usesMap.get("version"))
            .map(StrUtil::toStringOrNull)
            .orElse(StrUtil.EMPTY);
        // 拼接 _
        String version2 = Opt.ofBlankAble(version).map(s -> "_" + s).get();
        String name = String.format("jpom_%s%s", pluginName, version2);
        if (!DockerBuild.hasDependPlugin(dockerClient, name)) {
            HashMap<String, String> labels = MapUtil.of("jpom_build_" + buildId, buildId);
            labels.put("jpom_build_cache", "true");
            //
            dockerClient.createVolumeCmd()
                .withName(name)
                .withLabels(labels)
                .exec();

            Mount mount = new Mount().withType(MountType.VOLUME)
                .withSource(name)
                .withTarget("/opt/" + pluginName);

            HostConfig hostConfig = new HostConfig()
                .withAutoRemove(true).withMounts(CollUtil.newArrayList(mount));
            CreateContainerResponse createContainerResponse = dockerClient.createContainerCmd(image)
                .withHostConfig(hostConfig)
                .withName(name)
                .withEnv(pluginName.toUpperCase() + "_VERSION=" + version)
                .withLabels(labels)
                .withEntrypoint("/bin/bash", "/tmp/install.sh")
                .exec();
            String containerId = createContainerResponse.getId();
            // 将脚本 复制到容器
            File pluginInstallResource = plugin.getResourceToFile("uses/" + pluginName + "/install.sh", tempDir);
            Assert.notNull(pluginInstallResource, "当前不支持：" + pluginName);
            dockerClient.copyArchiveToContainerCmd(containerId)
                .withHostResource(pluginInstallResource.getAbsolutePath())
                .withRemotePath("/tmp/")
                .exec();
            //
            dockerClient.startContainerCmd(containerId).exec();
            // 获取日志
            this.pullLog(dockerClient, containerId, logRecorder);
        }
        return new Mount().withType(MountType.VOLUME).withSource(name).withTarget("/opt/" + name);
    }


    private void pullLog(DockerClient dockerClient, String containerId, LogRecorder logRecorder) {
        // 获取日志
        try {
            DockerClientUtil.pullLog(dockerClient, containerId, false, null, StandardCharsets.UTF_8, logRecorder::append, null);
        } catch (InterruptedException e) {
            logRecorder.error("获取容器日志操作被中断:", e);
        } catch (RuntimeException e) {
            logRecorder.error("获取容器日志失败", e);
        }
    }


    private int waitContainerCmd(DockerClient dockerClient, String containerId, LogRecorder logRecorder) {
        final Integer[] statusCode = {-100};
        try {
            dockerClient.waitContainerCmd(containerId).exec(new ResultCallback.Adapter<WaitResponse>() {
                @Override
                public void onNext(WaitResponse object) {
                    statusCode[0] = object.getStatusCode();
                    logRecorder.system("dockerTask status code is: {}", statusCode[0]);
                }
            }).awaitCompletion();

        } catch (InterruptedException e) {
            logRecorder.error("获取容器执行结果操作被中断:", e);
        } catch (RuntimeException e) {
            logRecorder.error("获取容器执行结果失败", e);
        }
        return statusCode[0];
    }

    @Override
    public void close() throws Exception {

    }
}
