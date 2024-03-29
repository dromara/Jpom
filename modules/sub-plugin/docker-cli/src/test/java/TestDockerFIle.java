/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

/**
 * @author bwcx_jzy
 * @since 2022/1/27
 */
public class TestDockerFIle {

	private DockerClient dockerClient;
	private String containerId;

	@Before
	public void before() {
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
		dockerClient.pingCmd().exec();

		//
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger logger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
		logger.setLevel(Level.INFO);
	}

	@Test
	public void tset() {
		BuildImageCmd buildImageCmd = dockerClient.buildImageCmd();
	}
}
