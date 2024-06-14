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

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.plugins.PluginConfig;
import com.alibaba.fastjson2.JSONObject;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.PingCmd;
import com.github.dockerjava.api.exception.UnauthorizedException;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.AuthResponse;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.Version;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.RemoteApiVersion;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.plugin.IDefaultPlugin;
import org.springframework.util.Assert;

import javax.net.ssl.SSLHandshakeException;
import java.io.File;
import java.io.IOException;
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
    public Object execute(Object main, Map<String, Object> parameter) throws Exception {
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
            case "testAuth":
                return this.auth(parameter);
            case "hasDependPlugin":
                return DockerBuild.hasDependPlugin(parameter);
            default:
                break;
        }
        return null;
    }

//    private void deleteRuns(Map<String, Object> parameter) {
//        String dataPath = DockerUtil.FILE_PATHS[0];
//        String name = (String) parameter.get("name");
//        File dockerfile = FileUtil.file(dataPath, DockerUtil.RUNS_FOLDER, name);
//        if (!FileUtil.exist(dockerfile)) {
////            return JsonMessage.getString(400, "文件不存在");
//            return;
//        }
//        FileUtil.del(dockerfile);
//    }
//
//    private void updateRuns(Map<String, Object> parameter) {
//        String name = (String) parameter.get("name");
//        String content = (String) parameter.get("content");
//        String dataPath = DockerUtil.FILE_PATHS[0];
//        File dockerfile = FileUtil.file(dataPath, DockerUtil.RUNS_FOLDER, name, DockerUtil.DOCKER_FILE);
//        FileUtil.writeString(content, dockerfile, StandardCharsets.UTF_8);
//    }
//
//    private Map<String, URI> listRuns() throws URISyntaxException {
//        // // runs/%s/Dockerfile
//        Map<String, URI> runs = new HashMap<>(5);
//        for (String filePath : DockerUtil.FILE_PATHS) {
//            //
//            File dockerFileDir = FileUtil.file(filePath, DockerUtil.RUNS_FOLDER);
//            if (!FileUtil.exist(dockerFileDir) || FileUtil.isFile(dockerFileDir)) {
//                continue;
//            }
//            File[] files = dockerFileDir.listFiles();
//            if (files == null) {
//                continue;
//            }
//            for (File file : files) {
//                File dockerFile = FileUtil.file(file, DockerUtil.DOCKER_FILE);
//                if (FileUtil.isFile(dockerFile)) {
//                    // 不存在才添加
//                    runs.putIfAbsent(file.getName(), dockerFile.toURI());
//                }
//            }
//        }
//        if (Objects.isNull(runs.get(DockerUtil.DEFAULT_RUNS))) {
//            URL resourceObj = ResourceUtil.getResource(DockerUtil.RUNS_FOLDER + "/" + DockerUtil.DEFAULT_RUNS + "/" + DockerUtil.DOCKER_FILE);
//            runs.put(DockerUtil.DEFAULT_RUNS, resourceObj.toURI());
//        }
//        return runs;
//    }

    private JSONObject auth(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);
        AuthConfig authConfig = dockerClient.authConfig();
        AuthResponse exec = dockerClient.authCmd().withAuthConfig(authConfig).exec();
        return DockerUtil.toJSON(exec);
    }

    private String testLocal() throws IOException {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        URI dockerHost = config.getDockerHost();
        String host = dockerHost.toString();
        try (DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(dockerHost).connectionTimeout(Duration.ofSeconds(3)).build()) {
            try (PingCmd pingCmd = DockerClientImpl.getInstance(config, httpClient).pingCmd()) {
                pingCmd.exec();
                return host;
            }
        }
    }

    /**
     * 获取 docker info 信息
     *
     * @param parameter 参数
     * @return info
     */
    private JSONObject infoCmd(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);
        Info exec = dockerClient.infoCmd().exec();
        return DockerUtil.toJSON(exec);
    }

    /**
     * 获取版本信息
     *
     * @param parameter 参数
     * @return true 可以通讯
     */
    private Version pullVersion(Map<String, Object> parameter) {
        DockerClient dockerClient = DockerUtil.get(parameter);
        return dockerClient.versionCmd().exec();

    }

    /**
     * 检查 ping docker 超时时间 5 秒
     *
     * @param parameter 参数
     * @return true 可以通讯
     */
    private String checkPing(Map<String, Object> parameter) {
        try {
            DockerClient dockerClient = DockerUtil.get(parameter);
            dockerClient.pingCmd().exec();
            return null;
        } catch (UnauthorizedException unauthorizedException) {
            log.warn(I18nMessageUtil.get("i18n.docker_authorization_failed.8ede"), unauthorizedException.getMessage());
            return I18nMessageUtil.get("i18n.auth_failed.2765") + unauthorizedException.getMessage();
        } catch (Exception e) {
            log.warn(I18nMessageUtil.get("i18n.check_docker_url_exception.4302"), e.getMessage());
            if (ExceptionUtil.isCausedBy(e, SSLHandshakeException.class)) {
                return I18nMessageUtil.get("i18n.ssl_connection_failed.e26c") + e.getMessage();
            }
            log.warn(I18nMessageUtil.get("i18n.check_docker_url_exception.4302"), e.getMessage());
            return StrUtil.emptyToDefault(e.getMessage(), I18nMessageUtil.get("i18n.check_docker_exception.a6d1"));
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
        if (dockerHost == null) {
            return false;
        }
        switch (dockerHost.getScheme()) {
            case "tcp":
            case "unix":
            case "npipe":
            case "ssh":
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
