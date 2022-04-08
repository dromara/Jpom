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
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import cn.hutool.setting.yaml.YamlUtil;
import io.jpom.model.BaseJsonModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * docker 构建 配置
 * <p>
 * https://www.jianshu.com/p/54cfa5721d5f
 *
 * @author bwcx_jzy
 * @since 2022/1/25
 */
@EqualsAndHashCode(callSuper = true)
@Data
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

    public void check() {
        Assert.hasText(runsOn, "请填写runsOn。目前仅支持 ubuntu-latest.更多支持敬请期待!");
        Assert.state(CollUtil.isNotEmpty(steps), "请填写 steps");
        stepsCheck();
    }

    /**
     * 检查 steps
     */
    public void stepsCheck() {
        Set<String> usesSet = new HashSet<>();
        boolean containsRun = false;
        for (Map<String, Object> step : steps) {
            if (!containsRun && step.containsKey("run")) {
                containsRun = true;
            }
            if (step.containsKey("env")) {
                Assert.isInstanceOf(Map.class, step.get("env"), "env 必须是 map 类型");
            }
            if (step.containsKey("uses")) {
                List<String> supportedPlugins = ListUtil.of("node", "java", "maven", "cache", "go", "python3");
                Assert.isInstanceOf(String.class, step.get("uses"), "uses 只支持 String 类型");
                String uses = (String) step.get("uses");
                Assert.isTrue(supportedPlugins.contains(uses), String.format("目前仅支持的插件: %s", supportedPlugins));
                if ("node".equals(uses)) {
                    nodePluginCheck(step);
                } else if ("java".equals(uses)) {
                    javaPluginCheck(step);
                } else if ("maven".equals(uses)) {
                    mavenPluginCheck(step);
                } else if ("cache".equals(uses)) {
                    cachePluginCheck(step);
                } else if ("go".equals(uses)) {
                    goPluginCheck(step);
                } else if ("python3".equals(uses)) {
                    python3PluginCheck(step);
                }
                usesSet.add(uses);
            }
        }
        if (usesSet.contains("maven") && !usesSet.contains("java")) {
            throw new IllegalArgumentException("maven 插件依赖 java , 使用 maven 插件必须优先引入 java 插件");
        }
        Assert.isTrue(containsRun, "steps 中没有发现任何 run , run 用于执行命令");
    }

    private void cachePluginCheck(Map<String, Object> step) {
        Assert.notNull(step.get("path"), "cache 插件 path 不能为空");
    }

    /**
     * 检查 maven 插件
     *
     * @param step 参数
     */
    private void mavenPluginCheck(Map<String, Object> step) {
        Assert.notNull(step.get("version"), "maven 插件 version 不能为空");
        String version = String.valueOf(step.get("version"));
        String link = String.format("https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/%s/binaries/apache-maven-%s-bin.tar.gz", version, version);
        HttpRequest request = HttpUtil.createRequest(Method.HEAD, link);
        HttpResponse httpResponse = request.execute();
        Assert.isTrue(httpResponse.isOk()
            || httpResponse.getStatus() == HttpStatus.HTTP_MOVED_TEMP
            || httpResponse.getStatus() == HttpStatus.HTTP_BAD_METHOD, "请填入正确的 maven 版本号");
    }

    /**
     * 检查 go 插件
     *
     * @param step 参数
     */
    private void javaPluginCheck(Map<String, Object> step) {
        Assert.notNull(step.get("version"), "java 插件 version 不能为空");
        Integer version = Integer.valueOf(String.valueOf(step.get("version")));
        List<Integer> supportedVersions = ListUtil.of(8, 9, 10, 11, 12, 13, 14, 15, 16, 17);
        Assert.isTrue(supportedVersions.contains(version), String.format("目前java 插件支持的版本: %s", supportedVersions));
    }

    /**
     * 检查 node 插件
     *
     * @param step 参数
     */
    private void nodePluginCheck(Map<String, Object> step) {
        Assert.notNull(step.get("version"), "node 插件 version 不能为空");
        String version = String.valueOf(step.get("version"));
        String link = String.format("https://registry.npmmirror.com/-/binary/node/v%s/node-v%s-linux-x64.tar.gz", version, version);
        HttpResponse httpResponse = HttpUtil.createRequest(Method.HEAD, link).execute();
        Assert.isTrue(httpResponse.isOk() || httpResponse.getStatus() == HttpStatus.HTTP_MOVED_TEMP, "请填入正确的 node 版本号");
    }

    /**
     * 检查 go 插件
     *
     * @param step 参数
     */
    private void goPluginCheck(Map<String, Object> step) {
        Assert.notNull(step.get("version"), "go 插件 version 不能为空");
        String version = String.valueOf(step.get("version"));
        String link = String.format("https://studygolang.com/dl/golang/go%s.linux-amd64.tar.gz", version);
        HttpResponse httpResponse = HttpUtil.createRequest(Method.HEAD, link).execute();
        Assert.isTrue(httpResponse.isOk() ||
            httpResponse.getStatus() == HttpStatus.HTTP_MOVED_TEMP ||
            httpResponse.getStatus() == HttpStatus.HTTP_SEE_OTHER, "请填入正确的 go 版本号");
    }

    /**
     * 检查 python3 插件
     *
     * @param step 参数
     */
    private void python3PluginCheck(Map<String, Object> step) {
        Assert.notNull(step.get("version"), "python3 插件 version 不能为空");
        String version = String.valueOf(step.get("version"));
        Assert.state(StrUtil.startWith(version, "3."), "请填入正确的 python3 版本号");
        String link = String.format("https://repo.huaweicloud.com/python/%s/Python-%s.tar.xz", version, version);
        HttpResponse httpResponse = HttpUtil.createRequest(Method.HEAD, link).execute();
        Assert.isTrue(httpResponse.isOk() ||
            httpResponse.getStatus() == HttpStatus.HTTP_MOVED_TEMP, "请填入正确的 python3 版本号");
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
