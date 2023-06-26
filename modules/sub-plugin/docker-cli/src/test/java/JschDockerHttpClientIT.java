import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.transport.DockerHttpClient;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.dromara.jpom.plugins.JschLogger;
import org.dromara.jpom.ssh.JschDockerHttpClient;
import org.junit.Test;

import java.io.IOException;


public class JschDockerHttpClientIT {

    @Test
    public void pingViaDialer() throws IOException, JSchException {

        final JSch jSch = new JSch();
        JSch.setLogger(new JschLogger());

//        final String configFile = System.getProperty("user.home") + File.separator + ".ssh" + File.separator + "config";
//        final File file = new File(configFile);
//        if (file.exists()) {
//            final OpenSSHConfig openSSHConfig = OpenSSHConfig.parseFile(file.getAbsolutePath());
//            jSch.setConfigRepository(openSSHConfig);
//        }


        final Session newSession = jSch.getSession("root", "192.168.127.156", 22);
        newSession.setPassword("123456+");
        newSession.setConfig("StrictHostKeyChecking", "no");
        newSession.connect();

        final DefaultDockerClientConfig dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost("ssh://root@192.168.127.156")

            .build();


        try (final DockerHttpClient dockerHttpClient = new JschDockerHttpClient(dockerClientConfig.getDockerHost(), () -> newSession)) {


            final DockerClient dockerClient = DockerClientImpl.getInstance(dockerClientConfig, dockerHttpClient);


            Info exec = dockerClient.infoCmd().exec();
            System.out.println(exec);
            //
            exec = dockerClient.infoCmd().exec();
            System.out.println(exec);


            dockerClient.close();
        }
    }


}
