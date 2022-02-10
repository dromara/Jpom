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
