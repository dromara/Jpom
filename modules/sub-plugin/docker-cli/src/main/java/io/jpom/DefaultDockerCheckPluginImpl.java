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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.Version;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.RemoteApiVersion;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import io.jpom.plugin.IDefaultPlugin;
import io.jpom.plugin.PluginConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * docker 验证 实现
 *
 * @author bwcx_jzy
 * @since 2022/1/26
 */
@PluginConfig(name = "docker-cli:check")
@Slf4j
public class DefaultDockerCheckPluginImpl implements IDefaultPlugin {

    @Override
    public Object execute(Object main, Map<String, Object> parameter) {
        String type = main.toString();
        switch (type) {
            case "certPath":
                return this.checkCertPath(parameter);
            case "apiVersions":
                return getApiVersions();
            case "host":
                return this.checkUrl(parameter);
            case "ping":
                return this.checkPing(parameter);
            case "info":
                return this.infoCmd(parameter);
            case "testLocal":
                return this.testLocal();
            default:
                break;
        }
        return null;
    }

    private String testLocal() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        URI dockerHost = config.getDockerHost();
        String host = dockerHost.toString();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(dockerHost).connectionTimeout(Duration.ofSeconds(3)).build();
        DockerClientImpl.getInstance(config, httpClient).pingCmd().exec();
        return host;
    }

    /**
     * 获取 docker info 信息
     *
     * @param parameter 参数
     * @return info
     */
    private JSONObject infoCmd(Map<String, Object> parameter) {
        parameter.putIfAbsent("timeout", 5);
        DockerClient dockerClient = DockerUtil.build(parameter, 1);
        try {
            Info exec = dockerClient.infoCmd().exec();
            return (JSONObject) JSONObject.toJSON(exec);
        } finally {
            IoUtil.close(dockerClient);
        }
    }

    /**
     * 获取版本信息
     *
     * @param parameter 参数
     * @return true 可以通讯
     */
    private Version pullVersion(Map<String, Object> parameter) {
        parameter.putIfAbsent("timeout", 5);
        DockerClient dockerClient = DockerUtil.build(parameter, 1);
        try {
            return dockerClient.versionCmd().exec();
        } finally {
            IoUtil.close(dockerClient);
        }
    }

    /**
     * 检查 ping docker 超时时间 5 秒
     *
     * @param parameter 参数
     * @return true 可以通讯
     */
    private boolean checkPing(Map<String, Object> parameter) {
        DockerClient dockerClient = null;
        try {
            parameter.putIfAbsent("timeout", 5);
            dockerClient = DockerUtil.build(parameter, 1);
            dockerClient.pingCmd().exec();
            return true;
        } catch (Exception e) {
            log.warn("检查 docker url 异常 {}", e.getMessage());
            return false;
        } finally {
            IoUtil.close(dockerClient);
        }
    }

    /**
     * 检查 docker url 是否可用
     *
     * @param parameter 参数
     * @return true 可用
     */
    private boolean checkUrl(Map<String, Object> parameter) {
        String url = (String) parameter.get("host");
        URI dockerHost;
        try {
            dockerHost = URI.create(url);
        } catch (Exception e) {
            return false;
        }
        switch (dockerHost.getScheme()) {
            case "tcp":
            case "unix":
            case "npipe":
                return true;
            default:
                return false;
        }
    }

    /**
     * 获取支持到所有 api 版本
     *
     * @return list
     */
    private List<JSONObject> getApiVersions() {
        Field[] fields = ReflectUtil.getFields(RemoteApiVersion.class);
        return Arrays.stream(fields)
                .map(field -> {
                    boolean aFinal = Modifier.isFinal(field.getModifiers());
                    boolean aStatic = Modifier.isStatic(field.getModifiers());
                    boolean aPublic = Modifier.isPublic(field.getModifiers());
                    if (!aFinal || !aStatic || !aPublic) {
                        return null;
                    }
                    Object fieldValue = ReflectUtil.getFieldValue(null, field);
                    if (fieldValue instanceof RemoteApiVersion) {
                        return (RemoteApiVersion) fieldValue;
                    }
                    return null;
                })
                .filter(apiVersion -> apiVersion != null && apiVersion != RemoteApiVersion.UNKNOWN_VERSION)
                .sorted((o1, o2) -> StrUtil.compareVersion(o2.getVersion(), o1.getVersion())).map(apiVersion -> {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("webVersion", apiVersion.asWebPathPart());
                    jsonObject.put("version", apiVersion.getVersion());
                    return jsonObject;
                })
                .collect(Collectors.toList());
    }

    /**
     * 验证 证书是否满足条件
     *
     * @param parameter 参数
     * @return 都是文件满足条件
     */
    private boolean checkCertPath(Map<String, Object> parameter) {
        String certPath = (String) parameter.get("certPath");
        Assert.hasText(certPath, "certPath is empty");
        File caPemPath = FileUtil.file(certPath, "ca.pem");
        File keyPemPath = FileUtil.file(certPath, "key.pem");
        File certPemPath = FileUtil.file(certPath, "cert.pem");
        return FileUtil.isFile(caPemPath) && FileUtil.isFile(keyPemPath) && FileUtil.isFile(certPemPath);
    }
}
