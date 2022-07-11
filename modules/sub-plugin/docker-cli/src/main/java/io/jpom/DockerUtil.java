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

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.Resource;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.AuthResponse;
import com.github.dockerjava.api.model.ResponseItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author bwcx_jzy
 * @since 2022/1/26
 */
@Slf4j
public class DockerUtil {

    private static final Map<String, DockerClient> DOCKER_CLIENT_MAP = new ConcurrentHashMap<>();

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
        //
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerTlsVerify(StrUtil.isNotEmpty(dockerCertPath))
            .withApiVersion(apiVersion)
            .withDockerCertPath(dockerCertPath)
            .withDockerHost(host)
            .withRegistryUrl(registryUrl).withRegistryEmail(registryEmail)
            .withRegistryUsername(registryUsername).withRegistryPassword(StrUtil.emptyToDefault(registryPassword, StrUtil.EMPTY))
            .build();
        //
        ApacheDockerHttpClient.Builder builder = new ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .maxConnections(100);
        //
        int timeout = Convert.toInt(parameter.get("timeout"), 0);
        if (timeout > 0) {
            builder.connectionTimeout(Duration.ofSeconds(timeout));
            builder.responseTimeout(Duration.ofSeconds(timeout));
        }
        ApacheDockerHttpClient httpClient = builder.build();
        DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);
        if (StrUtil.isNotEmpty(registryUrl)) {
            AuthConfig authConfig = dockerClient.authConfig();
            AuthResponse authResponse = dockerClient.authCmd().withAuthConfig(authConfig).exec();
            log.debug("auth cmd:{}", JSONObject.toJSONString(authResponse));
        }
        return dockerClient;
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
     * 转化文件
     *
     * @param name    资源名称
     * @param tempDir 临时路径
     * @return file 无法读取或者写入返回 null
     */
    public static File getResourceToFile(String name, File tempDir) {
        try {
            Resource resourceObj = ResourceUtil.getResourceObj(name);
            InputStream stream = resourceObj.getStream();
            File tempFile = DockerUtil.createTemp(name, tempDir);
            FileUtil.writeFromStream(stream, tempFile);
            return tempFile;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 转化文件
     *
     * @param name    资源名称
     * @param tempDir 临时路径
     * @return path 无法读取或者写入返回 null
     */
    public static String getResourceToFilePath(String name, File tempDir) {
        File resourceToFile = getResourceToFile(name, tempDir);
        if (resourceToFile == null) {
            return null;
        }
        return resourceToFile.getAbsolutePath();
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
}
