/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package git;

import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.PemUtil;
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

import java.io.*;
import java.security.PrivateKey;

/**
 * https://qa.1r1g.com/sf/ask/875171671/
 * <p>
 * https://www.jianshu.com/p/036072b45a2d
 *
 * @author bwcx_jzy
 * @since 2019/7/10
 **/
public class TestSshGit {
	public static void main(String[] args) throws GitAPIException, IOException {
		File directory = new File("～/test/gitssh");
		FileUtil.del(directory);
		Git.cloneRepository()
				.setURI("git@gitee.com:keepbx/Jpom-demo-case.git").setProgressMonitor(new TextProgressMonitor(new PrintWriter(System.out)))
				.setDirectory(directory)
				.setTransportConfigCallback(new TransportConfigCallback() {
					@Override
					public void configure(Transport transport) {
						// System.out.println("ssssssssssssssssssssssss");
						SshTransport sshTransport = (SshTransport) transport;
						sshTransport.setSshSessionFactory(new JschConfigSessionFactory() {
							@Override
							protected void configure(OpenSshConfig.Host hc, Session session) {
								session.setConfig("StrictHostKeyChecking", "no");
							}

							@Override
							protected JSch createDefaultJSch(FS fs) throws JSchException {
								JSch jSch = super.createDefaultJSch(fs);
								//JSch.setConfig();
								//添加私钥文件
								//jSch.addIdentity("~/.ssh/test/id_rsa", "~/.ssh/test/id_rsa.pub", null);

								//jSch.addIdentity("~/.ssh/id_rsa");
								return jSch;
							}
						});
					}
				})
				.call();
	}

    @Test
	public void test1() throws GitAPIException {
//    	String url = "https://gitee.com/dromara/Jpom.git";
//    	String url = "git@gitee.com:dromara/Jpom.git";
//    	String rsaFile = "/Users/zhangxin/.ssh/jpom_key/id_rsa";
//    	String passphrase = "12345";
//    	Git.cloneRepository()
//				.setURI(url)
//				.setDirectory(new File("/Users/zhangxin/Documents/jpom/server/test-jpom"))
//				.setTransportConfigCallback(transport -> {
//					SshTransport sshTransport = (SshTransport) transport;
//					sshTransport.setSshSessionFactory( new JschConfigSessionFactory() {
//
//						@Override
//						protected JSch createDefaultJSch(FS fs) throws JSchException {
//							JSch jSch = super.createDefaultJSch(fs);
//							//添加私钥文件
//							jSch.addIdentity(rsaFile, passphrase);
//							return jSch;
//						}
//					});
//				})
//				.call();
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

	@Test
	public void test() throws FileNotFoundException {
		File keyFile = new File("/Users/user/.ssh/id_rsa");
		PrivateKey privateKey = PemUtil.readPemPrivateKey(new FileInputStream(keyFile));

//		PublicKey rsaPublicKey = KeyUtil.("~/.ssh/id_rsa");
	}
}
