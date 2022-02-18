import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectNetworkCmd;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.AuthResponse;
import com.github.dockerjava.api.model.Network;
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
 * @since 2022/2/18
 */
public class TestAuth {

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

    @Test
    public void testNetwork2() {
        DockerClient client = this.client("172.19.106.252");
        AuthConfig authConfig = client.authConfig();
        System.out.println(authConfig);

        AuthResponse authResponse = client.authCmd().withAuthConfig(authConfig).exec();
        System.out.println(authResponse);
    }
}
