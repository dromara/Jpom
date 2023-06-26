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
        JSch.setLogger(JschLogger.LOGGER);

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
