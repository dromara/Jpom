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
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.Swarm;
import com.github.dockerjava.api.model.SwarmNode;
import com.github.dockerjava.api.model.SwarmSpec;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.command.PingCmdImpl;
import com.github.dockerjava.core.command.RemoveSwarmNodeCmdImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import io.jpom.DockerUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * https://blog.csdn.net/qq_36609501/article/details/93138036
 * <p>
 * https://www.cnblogs.com/vinsent/p/11691562.html
 *
 * @author bwcx_jzy
 * @since 2022/2/13
 */
@Slf4j
public class TestSwarm {

	private DockerClient dockerClient;
	private String containerId;

	private String node1 = "192.168.105.13";
	private String node2 = "192.168.105.177";
	private String node3 = "192.168.105.182";

	private String nodeLt = "172.19.106.253";

	@Before
	public void beforeLocal() {
		//
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger logger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
		logger.setLevel(Level.INFO);

//		this.dockerClient = this.client(node1);
//		dockerClient.pingCmd().exec();
	}

	private DockerClient client(String host) {
		DefaultDockerClientConfig.Builder builder = DefaultDockerClientConfig.createDefaultConfigBuilder()
				.withDockerHost("tcp://" + host + ":2375");
		if (StrUtil.equals(host, nodeLt)) {
			builder.withDockerTlsVerify(true)
					.withDockerCertPath("/Users/user/fsdownload/docker-ca");
		}
		DockerClientConfig config = builder.build();

		DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
				.dockerHost(config.getDockerHost())
				.sslConfig(config.getSSLConfig())
				.maxConnections(100)
//				.connectionTimeout(Duration.ofSeconds(30))
//				.responseTimeout(Duration.ofSeconds(45))
				.build();
		DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);
		dockerClient.pingCmd().exec();
		return dockerClient;
	}

	private DockerCmdExecFactory client2(String host) {
		Map<String, Object> map = new HashMap<>(10);
		map.put("dockerHost", "tcp://" + host + ":2375");

		if (StrUtil.equals(host, nodeLt)) {
			map.put("dockerCertPath", "/Users/user/fsdownload/docker-ca");
		}
		DockerCmdExecFactory factory = DockerUtil.buildJersey(map, 1);
		PingCmd.Exec pingCmdExec = factory.createPingCmdExec();
		PingCmdImpl command = new PingCmdImpl(pingCmdExec);
		Void exec = pingCmdExec.exec(command);
		return factory;
	}

	@After
	public void after() {
		if (containerId == null) {
			return;
		}
		// 清除容器
//		this.dockerClient.removeContainerCmd(containerId)
//				.withRemoveVolumes(true)
//				.withForce(true)
//				.exec();
	}

	@Test
	public void testInfo() {
		SwarmSpec swarmSpec = new SwarmSpec();
//		swarmSpec.withDispatcher()
		swarmSpec.withName("default");
		dockerClient.initializeSwarmCmd(swarmSpec).exec();
	}

	@Test
	public void testInSpectSwarmCmd() {
		this.dockerClient = this.client(node2);
		Swarm exec = dockerClient.inspectSwarmCmd().exec();
		JSONObject toJSON = (JSONObject) JSONObject.toJSON(exec);
		System.out.println(toJSON.toString(SerializerFeature.PrettyFormat));
	}

	@Test
	public void testListNodeSwarmCmd() {
		this.dockerClient = this.client(nodeLt);
		ListSwarmNodesCmd listSwarmNodesCmd = dockerClient.listSwarmNodesCmd();
		List<SwarmNode> exec = listSwarmNodesCmd.exec();
		JSONArray toJSON = (JSONArray) JSONObject.toJSON(exec);
		System.out.println(toJSON.toString());
	}

	@Test
	public void tsetInfo() {
		this.dockerClient = this.client(node1);
		InfoCmd infoCmd = dockerClient.infoCmd();
		Info exec = infoCmd.exec();
		JSONObject toJSON = (JSONObject) JSONObject.toJSON(exec);
		System.out.println(toJSON.toString(SerializerFeature.PrettyFormat));
	}

	@Test
	public void tsetRemove() {
		DockerClient client = this.client(nodeLt);
		DockerCmdExecFactory dockerCmdExecFactory = (DockerCmdExecFactory) ReflectUtil.getFieldValue(client, "dockerCmdExecFactory");


		RemoveSwarmNodeCmdImpl removeSwarmNodeCmd = new RemoveSwarmNodeCmdImpl(dockerCmdExecFactory.removeSwarmNodeCmdExec(), "hvtr4gy520x67m97h2ds8wcnu");

		removeSwarmNodeCmd.exec();
	}

	@Test
	public void testJoin() {

		JoinSwarmCmd joinSwarmCmd = dockerClient.joinSwarmCmd()
				.withRemoteAddrs(CollUtil.newArrayList(node2))
				.withJoinToken("SWMTKN-1-1g25kun6dsy76akteqxwww87d5i0y7dnbn8sy38x7asv5fkpre-0btwn3t33sjzi6ofzuwei5c1f");
		joinSwarmCmd.exec();
	}

}
