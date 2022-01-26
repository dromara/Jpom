import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.VersionCmd;
import com.github.dockerjava.api.model.Version;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.junit.Test;
import org.slf4j.LoggerFactory;

/**
 * @author bwcx_jzy
 * @since 2022/1/25
 */
public class TestLocal {
	private DockerClient dockerClient;

	@Test
	public void test() {
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
		dockerClient.pingCmd().exec();
		VersionCmd versionCmd = dockerClient.versionCmd();
		Version exec = versionCmd.exec();
		System.out.println(exec);

	}
}
