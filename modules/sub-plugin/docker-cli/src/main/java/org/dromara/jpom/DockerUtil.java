/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.map.SafeConcurrentHashMap;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.AuthResponse;
import com.github.dockerjava.api.model.ResponseItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.ssh.JschDockerHttpClient;

import java.io.File;
import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author bwcx_jzy
 * @since 2022/1/26
 */
@Slf4j
public class DockerUtil {

    static {
        // 禁用 jackson
        JSONFactory.setUseJacksonAnnotation(false);
        // 枚举对象使用 枚举名称
        JSON.config(JSONWriter.Feature.WriteEnumsUsingName);
    }

    /**
     * Docker Hub 的统一标识
     */
    private static final String DOCKER_HUB_REGISTRY = "docker.io";
    /**
     * Docker Hub 的等价域名
     */
    private static final String[] DOCKER_HUB_HOST_ALIASES = {"docker.io", "index.docker.io", "registry-1.docker.io", "registry.hub.docker.com"};

    private static final Map<String, DockerClient> DOCKER_CLIENT_MAP = new SafeConcurrentHashMap<>();
    private static final Map<String, AutoCloseable> CACHE_CLOSET = new SafeConcurrentHashMap<>();

    /**
     * dockerfile 文件名称
     */
    public static final String DOCKER_FILE = "Dockerfile";

    /**
     * 获取 docker client ，会使用缓存
     * <p>
     * 如果参数包含 closeBefore 则重新创建
     *
     * @param parameter 参数
     * @return DockerClient
     */
    public static DockerClient get(Map<String, Object> parameter) {
        String host = (String) parameter.get("dockerHost");
        String dockerCertPath = (String) parameter.get("dockerCertPath");
        String key = StrUtil.format("{}-{}", host, StrUtil.emptyToDefault(dockerCertPath, StrUtil.EMPTY));
        if (parameter.containsKey("closeBefore")) {
            //  关闭之前的连接
            DockerClient dockerClient = DOCKER_CLIENT_MAP.remove(key);
            IoUtil.close(dockerClient);
        }
        return DOCKER_CLIENT_MAP.computeIfAbsent(key, s -> create(parameter));
    }

    /**
     * 构建 docker client 对象
     *
     * @param parameter 参数
     * @return DockerClient
     */
    private static DockerClient create(Map<String, Object> parameter) {
        String host = (String) parameter.get("dockerHost");
        String apiVersion = (String) parameter.get("apiVersion");
        String dockerCertPath = (String) parameter.get("dockerCertPath");
        String registryUsername = (String) parameter.get("registryUsername");
        String registryPassword = (String) parameter.get("registryPassword");
        String registryEmail = (String) parameter.get("registryEmail");
        String registryUrl = (String) parameter.get("registryUrl");
        Boolean sshUseSudo = (Boolean) parameter.get("sshUseSudo");
        Supplier<Session> sessionSupplier = (Supplier<Session>) parameter.get("session");
        //
        DefaultDockerClientConfig.Builder defaultConfigBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();
        defaultConfigBuilder
            .withDockerTlsVerify(StrUtil.isNotEmpty(dockerCertPath))
            .withApiVersion(apiVersion)
            .withDockerCertPath(dockerCertPath)
            .withDockerHost(host);
        //
        Opt.ofBlankAble(registryUrl).ifPresent(s -> defaultConfigBuilder.withRegistryUrl(registryUrl));
        Opt.ofBlankAble(registryEmail).ifPresent(s -> defaultConfigBuilder.withRegistryEmail(registryEmail));
        Opt.ofBlankAble(registryUsername).ifPresent(s -> defaultConfigBuilder.withRegistryUsername(registryUsername));
        Opt.ofBlankAble(registryPassword).ifPresent(s -> defaultConfigBuilder.withRegistryPassword(registryPassword));

        DockerClient dockerClient;
        DockerClientConfig config = defaultConfigBuilder.build();
        if (sessionSupplier != null) {
            // 通过SSH连接Docker
            JschDockerHttpClient httpClient = new JschDockerHttpClient(config.getDockerHost(), sshUseSudo != null && sshUseSudo, sessionSupplier);
            dockerClient = DockerClientImpl.getInstance(config, httpClient);
        } else {
            ApacheDockerHttpClient.Builder builder = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100);
            int timeout = Convert.toInt(parameter.get("timeout"), 0);
            if (timeout > 0) {
                builder.connectionTimeout(Duration.ofSeconds(timeout));
                builder.responseTimeout(Duration.ofSeconds(timeout));
            }
            ApacheDockerHttpClient httpClient = builder.build();
            dockerClient = DockerClientImpl.getInstance(config, httpClient);
        }
        if (StrUtil.isNotEmpty(registryUrl)) {
            AuthConfig authConfig = dockerClient.authConfig();
            AuthResponse authResponse = dockerClient.authCmd().withAuthConfig(authConfig).exec();
            log.debug("auth cmd:{}", JSONObject.toJSONString(authResponse));
        }
        return dockerClient;
    }

    /**
     * 判断镜像名是否指向资产配置的镜像仓库
     * <p>
     * 用于决定拉取镜像时是否附加资产里保存的仓库账号密码：只有镜像来自资产配置的同一个仓库时才附带，
     * 否则调用者指定的第三方仓库会收到该资产的仓库凭据（2026-11-19 外部安全报告）。
     * 资产没有配置仓库地址时一律不附带凭据（按匿名拉取处理）。
     *
     * @param repository  镜像名（可能带仓库前缀）
     * @param registryUrl 资产配置的仓库地址
     * @return true 表示同一个仓库，可以使用资产配置的仓库凭据
     */
    public static boolean registryMatches(String repository, String registryUrl) {
        String repositoryHost = repositoryRegistryHost(repository);
        String configuredHost = normalizeRegistryHost(registryUrl);
        if (StrUtil.isEmpty(repositoryHost) || StrUtil.isEmpty(configuredHost)) {
            return false;
        }
        return StrUtil.equalsIgnoreCase(repositoryHost, configuredHost);
    }

    /**
     * 解析镜像名里的仓库域名
     * <p>
     * 规则与 docker 一致：第一个 `/` 之前包含 `.` 或 `:`（或为 localhost）才认为是仓库域名，
     * 否则是 Docker Hub 上的命名空间（如 `library/nginx`）。
     *
     * @param repository 镜像名
     * @return 仓库域名（小写），无法解析时返回空字符串
     */
    static String repositoryRegistryHost(String repository) {
        if (StrUtil.isBlank(repository)) {
            return StrUtil.EMPTY;
        }
        // 去掉 digest 与可能的协议前缀（用户可能直接粘贴完整地址）
        String repo = StrUtil.subBefore(repository.trim(), "@", false);
        repo = removeScheme(repo);
        int slash = repo.indexOf('/');
        if (slash < 0) {
            // `nginx`、`nginx:1.25` 都属于 Docker Hub 官方镜像
            return DOCKER_HUB_REGISTRY;
        }
        String first = repo.substring(0, slash);
        if (StrUtil.containsAny(first, ".", ":") || StrUtil.equalsIgnoreCase(first, "localhost")) {
            return normalizeDockerHub(first.toLowerCase());
        }
        return DOCKER_HUB_REGISTRY;
    }

    /**
     * 归一化资产配置的仓库地址，取出仓库域名
     *
     * @param registryUrl 仓库地址
     * @return 仓库域名（小写），无法解析时返回空字符串
     */
    static String normalizeRegistryHost(String registryUrl) {
        if (StrUtil.isBlank(registryUrl)) {
            return StrUtil.EMPTY;
        }
        String url = removeScheme(registryUrl.trim());
        int slash = url.indexOf('/');
        if (slash >= 0) {
            url = url.substring(0, slash);
        }
        url = StrUtil.removeSuffix(url, "/");
        if (StrUtil.isEmpty(url)) {
            return StrUtil.EMPTY;
        }
        return normalizeDockerHub(url.toLowerCase());
    }

    private static String removeScheme(String url) {
        String result = StrUtil.removePrefixIgnoreCase(url, "https://");
        result = StrUtil.removePrefixIgnoreCase(result, "http://");
        result = StrUtil.removePrefixIgnoreCase(result, "//");
        return result;
    }

    private static String normalizeDockerHub(String host) {
        for (String alias : DOCKER_HUB_HOST_ALIASES) {
            if (StrUtil.equalsIgnoreCase(alias, host)) {
                return DOCKER_HUB_REGISTRY;
            }
        }
        return host;
    }

    /**
     * 临时文件目录
     *
     * @param name    文件名
     * @param tempDir 临时文件目录
     * @return temp
     */
    public static File createTemp(String name, File tempDir) {
        return FileUtil.file(tempDir, name);
    }

    /**
     * 获取进度信息
     *
     * @param responseItem 响应结果
     * @return 转化为 字符串
     */
    public static String parseResponseItem(ResponseItem responseItem) {
        String stream = responseItem.getStream();
        if (stream == null) {
            String status = responseItem.getStatus();
            if (status == null) {
                Map<String, Object> rawValues = responseItem.getRawValues();
                return MapUtil.join(rawValues, ",", "=") + StrUtil.LF;
            }
            String progress = responseItem.getProgress();
            progress = StrUtil.emptyToDefault(progress, StrUtil.EMPTY);
            String id = responseItem.getId();
            id = StrUtil.emptyToDefault(id, StrUtil.EMPTY);
            return StrUtil.format("{} {} {}", status, id, progress);
        }
        return stream;
    }

    /**
     * 深度转换为 json object
     *
     * @param object 对象
     * @return 转换后的
     */
    public static JSONObject toJSON(Object object) {
        String jsonString = JSONObject.toJSONString(object);
        return (JSONObject) JSON.parse(jsonString);
    }

    public static void putClose(String id, AutoCloseable autoCloseable) {
        AutoCloseable closeable = CACHE_CLOSET.put(id, autoCloseable);
        // 关闭上一次资源
        IoUtil.close(closeable);
    }

    public static void close(String id) {
        IoUtil.close(CACHE_CLOSET.remove(id));
    }
}
