import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.VersionCmd;
import com.github.dockerjava.api.model.Version;
import io.jpom.DockerUtil;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2022/1/26
 */
public class TestTsl {

	@Test
	public void test() {

		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger logger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
		logger.setLevel(Level.INFO);

		String dockerHost = "tcp://172.19.106.253:2375";
		String dockerCertPath = "/Users/user/fsdownload/docker-ca";
		Map<String, Object> map = new HashMap<>();
		map.put("dockerHost", dockerHost);
		map.put("dockerCertPath", dockerCertPath);
		DockerClient dockerClient = DockerUtil.build(map, 1, 5);

		dockerClient.pingCmd().exec();
		VersionCmd versionCmd = dockerClient.versionCmd();
		Version exec = versionCmd.exec();
		System.out.println(exec);

	}
}
