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
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import cn.hutool.core.io.unit.DataSizeUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.api.model.UpdateContainerResponse;
import com.github.dockerjava.api.model.Version;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.InvocationBuilder;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

/**
 * https://docs.docker.com/engine/api/v1.41/#operation/ContainerKill
 * <p>
 * https://docs.docker.com/engine/api/v1.41/#operation/ContainerUpdate
 *
 * @author bwcx_jzy
 * @since 2022/1/25
 */
public class TestLocal {
    private DockerClient dockerClient;

    @Before
    public void init() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.setLevel(Level.INFO);

        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
            // .withDockerHost("tcp://192.168.163.11:2376").build();
//				.withApiVersion()
            .withDockerHost("tcp://127.0.0.1:2375").build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .maxConnections(100)
//				.connectionTimeout(Duration.ofSeconds(30))
//				.responseTimeout(Duration.ofSeconds(45))
            .build();
        this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
    }

    @Test
    public void test() {

        dockerClient.pingCmd().exec();
        VersionCmd versionCmd = dockerClient.versionCmd();
        Version exec = versionCmd.exec();
        System.out.println(exec);
    }

    @Test
    public void tset2() throws InterruptedException {
        StatsCmd statsCmd = dockerClient.statsCmd("socat");
        Statistics statistics = statsCmd.exec(new InvocationBuilder.AsyncResultCallback<>()).awaitResult();
        System.out.println(statistics);
        System.out.println(JSONUtil.toJsonStr(statistics));
    }

    @Test
    public void test3() {
//        dockerClient.inspectContainerCmd("socat")
        UpdateContainerCmd containerCmd = dockerClient.updateContainerCmd("socat");
//        containerCmd.withCpusetCpus("1");
//        containerCmd.withCpusetMems("1");
//        containerCmd.withCpuPeriod(1);
//        containerCmd.withCpuQuota(1);
//        containerCmd.withCpuShares(1);
//        containerCmd.withBlkioWeight(1);
//        containerCmd.withMemoryReservation(DataSizeUtil.parse("10M"));
//        containerCmd.withKernelMemory(DataSizeUtil.parse("10M"));
//        containerCmd.withMemory(DataSizeUtil.parse("10M"));
//        containerCmd.withMemorySwap(DataSizeUtil.parse("10M"));


        UpdateContainerResponse containerResponse = containerCmd.exec();
        System.out.println(containerResponse);
    }

    @Test
    public void testSize(){
        System.out.println(DataSizeUtil.parse("-1"));
        System.out.println(DataSizeUtil.parse("0"));
    }

    @Test
    public void test4() {
        InspectContainerCmd socat = dockerClient.inspectContainerCmd("socat").withSize(true);
        InspectContainerResponse exec = socat.exec();
        System.out.println(JSONObject.toJSONString(exec.getHostConfig(), SerializerFeature.PrettyFormat));
    }
}
