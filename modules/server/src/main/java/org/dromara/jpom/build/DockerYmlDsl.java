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
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import cn.hutool.setting.yaml.YamlUtil;
import cn.keepbx.jpom.model.BaseJsonModel;
import cn.keepbx.jpom.plugins.IPlugin;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.IDockerConfigPlugin;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.func.assets.server.MachineDockerServer;
import org.dromara.jpom.model.docker.DockerInfoModel;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.service.docker.DockerInfoService;
import org.dromara.jpom.util.StringUtil;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * docker 构建 配置
 * <p>
 * <a href="https://www.jianshu.com/p/54cfa5721d5f">https://www.jianshu.com/p/54cfa5721d5f</a>
 *
 * @author bwcx_jzy
 * @since 2022/1/25
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class DockerYmlDsl extends BaseJsonModel {

    /**
     * 基础镜像
     */
    private String runsOn;
    /**
     * 使用对应到 docker tag 构建
     */
    private String fromTag;
    /**
     * 构建步骤
     */
    private List<Map<String, Object>> steps;

    /**
     * 将本地文件复制到 容器
     * <p>
     * <host path>:<container path>:true
     * <p>
     * * If this flag is set to true, all children of the local directory will be copied to the remote without the root directory. For ex: if
     * * I have root/titi and root/tata and the remote path is /var/data. dirChildrenOnly = true will create /var/data/titi and /var/data/tata
     * * dirChildrenOnly = false will create /var/data/root/titi and /var/data/root/tata
     * *
     * * @param dirChildrenOnly
     * *            if root directory is ignored
     */
    private List<String> copy;
    /**
     * bind mounts 将宿主机上的任意位置的文件或者目录挂在到容器 （--mount type=bind,src=源目录,dst=目标目录）
     * /host:/container:ro
     */
    private List<String> binds;
    /**
     * 环境变量
     */
    private Map<String, String> env;
    /**
     * <a href="https://docs.docker.com/engine/api/v1.43/#tag/Container/operation/ContainerCreate">https://docs.docker.com/engine/api/v1.43/#tag/Container/operation/ContainerCreate</a>
     * <p>
     * cpuCount
     * <p>
     * cpuPercent
     * <p>
     * memoryReservation
     * <p>
     * cpusetCpus 允许执行的CPU（例如，0-3, 0,1）。
     * <p>
     * cpuShares
     */
    private Map<String, String> hostConfig;

    /**
     * 验证信息是否正确
     *
     * @param dockerInfoService   容器server
     * @param machineDockerServer 机器server
     * @param workspaceId         工作空间id
     * @param plugin              插件
     */
    public void check(DockerInfoService dockerInfoService,
                      MachineDockerServer machineDockerServer,
                      String workspaceId,
                      IDockerConfigPlugin plugin) {
        Assert.hasText(runsOn, I18nMessageUtil.get("i18n.please_fill_in_runs_on.c2ff"));
        Validator.validateMatchRegex(StringUtil.GENERAL_STR, runsOn, I18nMessageUtil.get("i18n.invalid_runs_on_image_name.4b96"));
        Assert.state(CollUtil.isNotEmpty(steps), I18nMessageUtil.get("i18n.please_fill_in_steps.229d"));
        this.stepsCheck(dockerInfoService, machineDockerServer, workspaceId, plugin);
    }

    /**
     * 检查 steps
     */
    private void stepsCheck(DockerInfoService dockerInfoService, MachineDockerServer machineDockerServer,
                            String workspaceId,
                            IDockerConfigPlugin plugin) {
        Set<String> usesSet = new HashSet<>();
        boolean containsRun = false;
        for (Map<String, Object> step : steps) {
            if (!containsRun && step.containsKey("run")) {
                containsRun = true;
            }
            if (step.containsKey("env")) {
                Object env1 = step.get("env");
                Assert.isInstanceOf(Map.class, env1, I18nMessageUtil.get("i18n.env_must_be_map_type.f8ad"));
            }
            if (step.containsKey("uses")) {
                Object uses1 = step.get("uses");
                Assert.isInstanceOf(String.class, uses1, I18nMessageUtil.get("i18n.uses_only_supports_string_type.ac54"));
                String uses = (String) step.get("uses");
                if ("node".equals(uses)) {
                    nodePluginCheck(step);
                } else if ("java".equals(uses)) {
                    javaPluginCheck(step);
                } else if ("gradle".equals(uses)) {
                    gradlePluginCheck(step);
                } else if ("maven".equals(uses)) {
                    mavenPluginCheck(step, dockerInfoService, machineDockerServer, workspaceId);
                } else if ("cache".equals(uses)) {
                    cachePluginCheck(step);
                } else if ("go".equals(uses)) {
                    goPluginCheck(step);
                } else if ("python3".equals(uses)) {
                    python3PluginCheck(step);
                } else {
                    // 其他自定义插件
                    File tmpDir = FileUtil.file(FileUtil.getTmpDir(), "check-users");
                    File pluginInstallResource = null;
                    try {
                        pluginInstallResource = plugin.getResourceToFile("uses/" + uses + "/install.sh", tmpDir);
                        Assert.notNull(pluginInstallResource, StrUtil.format(I18nMessageUtil.get("i18n.unsupported_plugin_message.2889"), uses));
                    } finally {
                        FileUtil.del(pluginInstallResource);
                    }
                }
                usesSet.add(uses);
            }
        }
        if (usesSet.contains("maven") && !usesSet.contains("java")) {
            throw new IllegalArgumentException(I18nMessageUtil.get("i18n.maven_plugin_depends_on_java.23f8"));
        }
        if (usesSet.contains("gradle") && !usesSet.contains("java")) {
            throw new IllegalArgumentException(I18nMessageUtil.get("i18n.gradle_plugin_depends_on_java.2bb3"));
        }
        Assert.isTrue(containsRun, I18nMessageUtil.get("i18n.no_run_found_in_steps.a141"));
    }

    private void cachePluginCheck(Map<String, Object> step) {
        Object path = step.get("path");
        Assert.notNull(path, I18nMessageUtil.get("i18n.cache_plugin_path_required.2093"));
    }

    /**
     * 检查 maven 插件
     *
     * @param step 参数
     */
    private void mavenPluginCheck(Map<String, Object> step, DockerInfoService dockerInfoService, MachineDockerServer machineDockerServer, String workspaceId) {
        Object version1 = step.get("version");
        Assert.notNull(version1, I18nMessageUtil.get("i18n.maven_plugin_version_required.71f1"));
        String version = String.valueOf(version1);
        String link = String.format("https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/%s/binaries/apache-maven-%s-bin.tar.gz", version, version);
        HttpRequest request = HttpUtil.createRequest(Method.HEAD, link);
        try (HttpResponse httpResponse = request.execute()) {
            boolean success = httpResponse.isOk()
                || httpResponse.getStatus() == HttpStatus.HTTP_MOVED_TEMP
                || httpResponse.getStatus() == HttpStatus.HTTP_BAD_METHOD;
            if (success) {
                return;
            }
        }
        // 判断容器中是否存在
        try {
            // 根据 tag 查询
            List<DockerInfoModel> dockerInfoModels =
                dockerInfoService
                    .queryByTag(workspaceId, fromTag);
            Map<String, Object> map = machineDockerServer.dockerParameter(dockerInfoModels);
            if (map != null) {
                map.put("pluginName", "maven");
                map.put("version", version);
                IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
                boolean exists = Convert.toBool(plugin.execute("hasDependPlugin", map), false);
                if (exists) {
                    return;
                }
            }
        } catch (Exception e) {
            log.warn(I18nMessageUtil.get("i18n.check_docker_dependency_error.60f7"), e.getMessage());
        }
        // 提示远程版本
        Collection<String> pluginVersion = this.listMavenPluginVersion();
        throw new IllegalArgumentException(I18nMessageUtil.get("i18n.please_fill_in_correct_maven_version.468c") + CollUtil.join(pluginVersion, StrUtil.COMMA));
    }


    private Collection<String> listMavenPluginVersion() {
        String html = HttpUtil.get("https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/");
        //使用正则获取所有可用版本
        List<String> versions = ReUtil.findAll("<a\\s+href=\"3.*?/\">(.*?)</a>", html, 1);
        Set<String> set = versions.stream()
            .map(s -> StrUtil.removeSuffix(s, StrUtil.SLASH))
            .filter(StrUtil::isNotEmpty)
            .collect(Collectors.toSet());
        Assert.notEmpty(set, I18nMessageUtil.get("i18n.no_available_maven_versions.dffe"));
        return set;
    }

    /**
     * 检查 go 插件
     *
     * @param step 参数
     */
    private void javaPluginCheck(Map<String, Object> step) {
        Object version1 = step.get("version");
        Assert.notNull(version1, I18nMessageUtil.get("i18n.java_plugin_version_required.de39"));
        Integer version = Integer.valueOf(String.valueOf(version1));
        List<Integer> supportedVersions = ListUtil.of(8, 11, 17, 18);
        Assert.isTrue(supportedVersions.contains(version), String.format(I18nMessageUtil.get("i18n.supported_java_plugin_versions.bd70"), supportedVersions));
    }


    /**
     * 检查 gradle 插件
     *
     * @param step 参数
     */
    private void gradlePluginCheck(Map<String, Object> step) {
        Object version1 = step.get("version");
        Assert.notNull(version1, I18nMessageUtil.get("i18n.gradle_plugin_version_required.b983"));
        String version = String.valueOf(version1);
        String link = String.format("https://downloads.gradle-dn.com/distributions/gradle-%s-bin.zip", version);
        HttpUtil.createRequest(Method.HEAD, link).thenFunction(httpResponse -> {
            Assert.isTrue(httpResponse.isOk() ||
                httpResponse.getStatus() == HttpStatus.HTTP_MOVED_TEMP ||
                httpResponse.getStatus() == HttpStatus.HTTP_SEE_OTHER, I18nMessageUtil.get("i18n.please_fill_in_correct_gradle_version.6e19"));
            return null;
        });
    }

    /**
     * 检查 node 插件
     *
     * @param step 参数
     */
    private void nodePluginCheck(Map<String, Object> step) {
        Object version1 = step.get("version");
        Assert.notNull(version1, I18nMessageUtil.get("i18n.node_plugin_version_required.2318"));
        String version = String.valueOf(version1);
        String link = String.format("https://registry.npmmirror.com/-/binary/node/v%s/node-v%s-linux-x64.tar.gz", version, version);
        HttpResponse httpResponse = HttpUtil.createRequest(Method.HEAD, link).execute();
        Assert.isTrue(httpResponse.isOk() || httpResponse.getStatus() == HttpStatus.HTTP_MOVED_TEMP, I18nMessageUtil.get("i18n.please_fill_in_correct_node_version.8483"));
    }

    /**
     * 检查 go 插件
     *
     * @param step 参数
     */
    private void goPluginCheck(Map<String, Object> step) {
        Object version1 = step.get("version");
        Assert.notNull(version1, I18nMessageUtil.get("i18n.go_plugin_version_required.ccf6"));
        String version = String.valueOf(version1);
        String link = String.format("https://studygolang.com/dl/golang/go%s.linux-amd64.tar.gz", version);
        HttpUtil.createRequest(Method.HEAD, link).thenFunction(new Function<HttpResponse, Object>() {
            @Override
            public Object apply(HttpResponse httpResponse) {
                Assert.isTrue(httpResponse.isOk() ||
                    httpResponse.getStatus() == HttpStatus.HTTP_MOVED_TEMP ||
                    httpResponse.getStatus() == HttpStatus.HTTP_SEE_OTHER, I18nMessageUtil.get("i18n.please_fill_in_correct_go_version.44ed"));
                return null;
            }
        });
    }

    /**
     * 检查 python3 插件
     *
     * @param step 参数
     */
    private void python3PluginCheck(Map<String, Object> step) {
        Object version1 = step.get("version");
        Assert.notNull(version1, I18nMessageUtil.get("i18n.python3_plugin_version_required.a0ce"));
        String version = String.valueOf(version1);
        Assert.state(StrUtil.startWith(version, "3."), () -> {
            //
            return I18nMessageUtil.get("i18n.please_fill_in_correct_python3_version.abb1");
        });
        String link = String.format("https://repo.huaweicloud.com/python/%s/Python-%s.tar.xz", version, version);
        HttpUtil.createRequest(Method.HEAD, link).thenFunction(new Function<HttpResponse, Object>() {
            @Override
            public Object apply(HttpResponse httpResponse) {
                Assert.isTrue(httpResponse.isOk() ||
                    httpResponse.getStatus() == HttpStatus.HTTP_MOVED_TEMP, I18nMessageUtil.get("i18n.please_fill_in_correct_python3_version.abb1"));
                return null;
            }
        });

    }

    /**
     * 构建对象
     *
     * @param yml yml 内容
     * @return DockerYmlDsl
     */
    public static DockerYmlDsl build(String yml) {
        yml = StrUtil.replace(yml, StrUtil.TAB, StrUtil.SPACE + StrUtil.SPACE);
        InputStream inputStream = new ByteArrayInputStream(yml.getBytes());
        return YamlUtil.load(inputStream, DockerYmlDsl.class);
    }
}
