import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.util.FS;

import java.io.File;

/**
 * @author bwcx_jzy
 * @date 2019/7/10
 **/
public class TestSshGit {
    public static void main(String[] args) throws GitAPIException {
        Git.cloneRepository()
                .setURI("git@gitee.com:jiangzeyin/test.git")
                .setDirectory(new File("D:\\test\\gitssh"))
                .setTransportConfigCallback(new TransportConfigCallback() {
                    @Override
                    public void configure(Transport transport) {
                        SshTransport sshTransport = (SshTransport) transport;
                        sshTransport.setSshSessionFactory(new JschConfigSessionFactory() {
                            @Override
                            protected void configure(OpenSshConfig.Host hc, Session session) {
                                session.setConfig("StrictHostKeyChecking", "no");
                                session.setPassword("");
                            }

                            @Override
                            protected JSch createDefaultJSch(FS fs) throws JSchException {
                                JSch defaultJSch = super.createDefaultJSch(fs);

                                defaultJSch.addIdentity("C:\\Users\\Colorful\\.ssh\\id_rsa.pub");
                                return defaultJSch;
                            }

                        });
                    }
                })
                .call();
    }
}
