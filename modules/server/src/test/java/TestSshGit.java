import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.util.FS;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * https://qa.1r1g.com/sf/ask/875171671/
 *
 * https://www.jianshu.com/p/036072b45a2d
 *
 * @author bwcx_jzy
 * @date 2019/7/10
 **/
public class TestSshGit {
    public static void main(String[] args) throws GitAPIException, IOException {
        Git.cloneRepository()
                .setURI("git@gitee.com:jiangzeyin/test.git").setProgressMonitor(new TextProgressMonitor(new PrintWriter(System.out)))
                .setDirectory(new File("D:\\test\\gitssh"))
                .setTransportConfigCallback(new TransportConfigCallback() {
                    @Override
                    public void configure(Transport transport) {
                        System.out.println("ssssssssssssssssssssssss");
                        SshTransport sshTransport = (SshTransport) transport;
                        sshTransport.setSshSessionFactory(new JschConfigSessionFactory() {
                            @Override
                            protected void configure(OpenSshConfig.Host hc, Session session) {
                                session.setConfig("StrictHostKeyChecking", "no");
                            }

                            @Override
                            protected JSch createDefaultJSch(FS fs) throws JSchException {
                                JSch jSch = super.createDefaultJSch(fs);
                                //添加私钥文件
                                jSch.addIdentity("ss");

                                return jSch;
                            }
                        });
                    }
                })
                .call();
    }

    @Test
    public void test2() {
//        SshSessionFactory sshSessionFactory = new SshSessionFactory() {
//            @Override
//            public RemoteSession getSession(URIish uri, CredentialsProvider credentialsProvider, FS fs, int tms) throws TransportException {
//
//                return null;
//            }
//
//            @Override
//            public String getType() {
//                return null;
//            }
//        }
//        JschConfigSessionFactory sessionFactory = new JschConfigSessionFactory() {
//            @Override
//            protected void configure(OpenSshConfig.Host hc, Session session) {
//                CredentialsProvider provider = new CredentialsProvider() {
//                    @Override
//                    public boolean isInteractive() {
//                        return false;
//                    }
//
//                    @Override
//                    public boolean supports(CredentialItem... items) {
//                        return true;
//                    }
//
//                    @Override
//                    public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
//                        for (CredentialItem item : items) {
//                            ((CredentialItem.StringType) item).setValue("yourpassphrase");
//                        }
//                        return true;
//                    }
//                };
//                UserInfo userInfo = new CredentialsProviderUserInfo(session, provider);
//                session.setUserInfo(userInfo);
//            }
//        };
    }
}
