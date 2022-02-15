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
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.command.RemoveSwarmNodeCmdImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * https://blog.csdn.net/qq_36609501/article/details/93138036
 * <p>
 * https://www.cnblogs.com/vinsent/p/11691562.html
 * <p>
 * https://www.runoob.com/docker/docker-swarm.html
 * <p>
 * https://zhuanlan.zhihu.com/p/22918583
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

//	private DockerCmdExecFactory client2(String host) {
//		Map<String, Object> map = new HashMap<>(10);
//		map.put("dockerHost", "tcp://" + host + ":2375");
//
//		if (StrUtil.equals(host, nodeLt)) {
//			map.put("dockerCertPath", "/Users/user/fsdownload/docker-ca");
//		}
//		DockerCmdExecFactory factory = DockerUtil.buildJersey(map, 1);
//		PingCmd.Exec pingCmdExec = factory.createPingCmdExec();
//		PingCmdImpl command = new PingCmdImpl(pingCmdExec);
//		Void exec = pingCmdExec.exec(command);
//		return factory;
//	}

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
	public void update() {
		DockerClient client = this.client(node1);


		String nodeId = "rk2gxpql2449t0s1ymtivtyoy";
		List<SwarmNode> nodes = client.listSwarmNodesCmd().withIdFilter(CollUtil.newArrayList(nodeId)).exec();
		System.out.println(nodes);
		SwarmNode swarmNode = CollUtil.getFirst(nodes);
		UpdateSwarmNodeCmd swarmNodeCmd = client.updateSwarmNodeCmd();

		swarmNodeCmd.withSwarmNodeId(nodeId);
		SwarmNodeSpec swarmNodeSpec = new SwarmNodeSpec();
		swarmNodeSpec.withAvailability(SwarmNodeAvailability.PAUSE);
		swarmNodeSpec.withRole(SwarmNodeRole.WORKER);
		swarmNodeCmd.withSwarmNodeSpec(swarmNodeSpec);

		swarmNodeCmd.withVersion(swarmNode.getVersion().getIndex());

		swarmNodeCmd.exec();
	}

	@Test
	public void testJoin() {

		JoinSwarmCmd joinSwarmCmd = dockerClient.joinSwarmCmd()
				.withRemoteAddrs(CollUtil.newArrayList(node2))
				.withJoinToken("SWMTKN-1-1g25kun6dsy76akteqxwww87d5i0y7dnbn8sy38x7asv5fkpre-0btwn3t33sjzi6ofzuwei5c1f");
		joinSwarmCmd.exec();
	}

	@Test
	public void testService() {
		DockerClient client = this.client(node1);
		ListServicesCmd listServicesCmd = client.listServicesCmd();

		List<Service> exec = listServicesCmd.exec();
		JSONArray toJSON = (JSONArray) JSONObject.toJSON(exec);
		System.out.println(toJSON.toString());
	}

	@Test
	public void testServiceInspect() {
		DockerClient client = this.client(node1);
		InspectServiceCmd inspectServiceCmd = client.inspectServiceCmd("whyf2udreftogvwzwf3pnl32g");

		Service exec = inspectServiceCmd.exec();

		System.out.println(exec);
	}

	@Test
	public void testServiceLog() throws InterruptedException {
		DockerClient client = this.client(node1);
		LogSwarmObjectCmd swarmObjectCmd = client.logServiceCmd("esjx0f126tvvicfizymdxymxq");
		swarmObjectCmd.withDetails(true);
		swarmObjectCmd.withStderr(true);
		swarmObjectCmd.withStdout(true);
		swarmObjectCmd.exec(new ResultCallback.Adapter<Frame>() {
			@Override
			public void onNext(Frame object) {
				String s = new String(object.getPayload(), StandardCharsets.UTF_8);
				System.out.print(s);
			}
		}).awaitCompletion();
	}

	@Test
	public void tsetTask() {
		DockerClient client = this.client(node1);
		ListTasksCmd listTasksCmd = client.listTasksCmd();
		listTasksCmd.withServiceFilter("esjx0f126tvvicfizymdxymxq");
		List<Task> exec = listTasksCmd.exec();
		System.out.println(exec);
	}

	@Test
	public void updateServiceCmd() {
		DockerClient client = this.client(node1);
		ServiceSpec serviceSpec = new ServiceSpec();

		{
			ServiceModeConfig serviceModeConfig = new ServiceModeConfig();
			// 副本数
			ServiceReplicatedModeOptions serviceReplicatedModeOptions = new ServiceReplicatedModeOptions();
			serviceReplicatedModeOptions.withReplicas(5);
			serviceModeConfig.withReplicated(serviceReplicatedModeOptions);
			//
			//			serviceModeConfig.withGlobal(new ServiceGlobalModeOptions());
			serviceSpec.withMode(serviceModeConfig);
		}
		{
			EndpointSpec endpointSpec = new EndpointSpec();
			endpointSpec.withMode(EndpointResolutionMode.VIP);
			PortConfig config = new PortConfig();
			config.withName("s");
			config.withProtocol(PortConfigProtocol.TCP);
			config.withPublishedPort(80);
			config.withTargetPort(80);
			config.withPublishMode(PortConfig.PublishMode.host);
			endpointSpec.withPorts(CollUtil.newArrayList(config));
			serviceSpec.withEndpointSpec(endpointSpec);
		}
		{
			UpdateConfig updateConfig = new UpdateConfig();

			serviceSpec.withUpdateConfig(updateConfig);
		}
		TaskSpec taskSpec = new TaskSpec();
		ContainerSpec containerSpec = new ContainerSpec();
//		containerSpec.with
		taskSpec.withContainerSpec(containerSpec);
		serviceSpec.withTaskTemplate(taskSpec);
		//
//		serviceSpec.withUpdateConfig()
		//
		UpdateServiceCmd updateServiceCmd = client.updateServiceCmd("esjx0f126tvvicfizymdxymxq", serviceSpec);
		updateServiceCmd.exec();
	}

	@Test
	public void updateServiceCmd3() {
		DockerClient client = this.client(node1);
		RemoveServiceCmd removeServiceCmd = client.removeServiceCmd("esjx0f126tvvicfizymdxymxq");
		removeServiceCmd.exec();
	}

	@Test
	public void updateServiceCmd2() {
		DockerClient client = this.client(node1);
		ServiceSpec serviceSpec = new ServiceSpec();
		serviceSpec.withName("helloworld");
		{
			ServiceModeConfig serviceModeConfig = new ServiceModeConfig();
			ServiceReplicatedModeOptions serviceReplicatedModeOptions = new ServiceReplicatedModeOptions();
			serviceReplicatedModeOptions.withReplicas(1);
			serviceModeConfig.withReplicated(serviceReplicatedModeOptions);
			//
			//			serviceModeConfig.withGlobal(new ServiceGlobalModeOptions());
			serviceSpec.withMode(serviceModeConfig);
		}
		{
			UpdateConfig updateConfig = new UpdateConfig();

			serviceSpec.withUpdateConfig(updateConfig);
		}
		{
			TaskSpec taskSpec = new TaskSpec();
			ContainerSpec containerSpec = new ContainerSpec();
			containerSpec.withImage("alpine");
			containerSpec.withCommand(CollUtil.newArrayList("ping docker.com"));

			taskSpec.withContainerSpec(containerSpec);
			serviceSpec.withTaskTemplate(taskSpec);
		}
//		serviceSpec.withRollbackConfig()
		serviceSpec.withEndpointSpec(new EndpointSpec());
		String serviceId = "tf2r29awevz2fcprybv6cvlm9";
		InspectServiceCmd inspectServiceCmd = client.inspectServiceCmd(serviceId);
		Service service = inspectServiceCmd.exec();
		UpdateServiceCmd updateServiceCmd = client.updateServiceCmd(serviceId, serviceSpec);
		updateServiceCmd.withVersion(service.getVersion().getIndex());
		updateServiceCmd.exec();
	}

	@Test
	public void testNetwork() {
		DockerClient client = this.client(node1);
		ListNetworksCmd listNetworksCmd = client.listNetworksCmd();
		List<Network> exec = listNetworksCmd.exec();
		System.out.println(exec);
	}

	@Test
	public void testUpdate() {
//		String serviceId = "eo0l430mf2v524rlgn5550d7w";
//		eo0l430mf2v524rlgn5550d7w
		ServiceSpec serviceSpec = new ServiceSpec();
		DockerClient client = this.client("172.19.106.253");
//		TaskSpec taskSpec = new TaskSpec();
//		ContainerSpec containerSpec = new ContainerSpec();
//		containerSpec.withImage("jpom-test:1.0");
//		taskSpec.withContainerSpec(containerSpec);
//		serviceSpec.withTaskTemplate(taskSpec);
		String name = "jpom-test";
		serviceSpec.withName(name);
		//
		InspectServiceCmd inspectServiceCmd = client.inspectServiceCmd(name);
		Service service = inspectServiceCmd.exec();
		ServiceSpec spec = service.getSpec();
		TaskSpec taskTemplate = spec.getTaskTemplate();
		ContainerSpec templateContainerSpec = taskTemplate.getContainerSpec();
		templateContainerSpec.withImage("jpom-test");
		//
		//
		UpdateServiceCmd updateServiceCmd = client.updateServiceCmd(name, spec);
		updateServiceCmd.withVersion(service.getVersion().getIndex());
		updateServiceCmd.exec();
	}

	@Test
	public void testNetwork2() {
		DockerClient client = this.client("172.19.106.252");
		InspectNetworkCmd inspectNetworkCmd = client.inspectNetworkCmd()
				.withNetworkId("903gj9lnisp5dbf77zpzti35x");
		Network exec = inspectNetworkCmd.exec();
		System.out.println(exec);
	}
}
