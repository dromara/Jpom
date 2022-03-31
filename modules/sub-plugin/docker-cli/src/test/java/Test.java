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
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.InvocationBuilder;
import com.github.dockerjava.core.exec.ExecStartCmdExec;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.After;
import org.junit.Before;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author bwcx_jzy
 * @since 2022/1/25
 */
@Slf4j
public class Test {

	private DockerClient dockerClient;
	private String containerId;

	//	@Before
	public void beforeLocal() {
		//
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
	}

	@Before
	public void before1() {
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger logger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
		logger.setLevel(Level.INFO);

		DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
				// .withDockerHost("tcp://192.168.163.11:2376").build();
//				.withApiVersion()
				.withDockerTlsVerify(true)
				.withDockerCertPath("/Users/user/fsdownload/docker-ca")
				.withDockerHost("tcp://172.19.106.253:2375").build();

		DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
				.dockerHost(config.getDockerHost())
				.sslConfig(config.getSSLConfig())
				.maxConnections(100)
//				.connectionTimeout(Duration.ofSeconds(30))
//				.responseTimeout(Duration.ofSeconds(45))
				.build();
		this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
		dockerClient.pingCmd().exec();
	}

	@After
	public void after() {
		if (containerId == null) {
			return;
		}
		// 清除容器
//		this.dockerClient.removeContainerCmd(containerId)
//				.withRemoveVolumes(true)
//				.withForce(true)
//				.exec();
	}

	@org.junit.Test
	public void testInfo() {
		PingCmd pingCmd = dockerClient.pingCmd();
		pingCmd.exec();
		System.out.println(pingCmd);
	}

	@org.junit.Test
	public void tset() {
		ListImagesCmd listImagesCmd = dockerClient.listImagesCmd();
		List<Image> exec = listImagesCmd.exec();
		exec.forEach(image -> {
			System.out.println(image);
		});
	}

	@org.junit.Test
	public void createImage() {
		File file = FileUtil.file("");
		file = FileUtil.getParent(file, 5);
		String absolutePath = "/Users/user/IdeaProjects/Jpom-demo-case";
		System.out.println(absolutePath);
		String image = "maven:3.8.5-jdk-8";
		String workingDir = "/jpom/";
		CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
		String name = "jpom-test";
		containerCmd.withName(name);
		CreateContainerCmd createContainerCmd = containerCmd.withWorkingDir(workingDir);
		//
//		List<Mount> mounts = new ArrayList<>();
//		mounts.add(new Mount()
//				.withType(MountType.VOLUME)
//				.withSource(absolutePath)
//				.withTarget(workingDir));


		List<Bind> bindList = new ArrayList<>();
		bindList.add(new Bind(absolutePath, new Volume(workingDir)));
		//bindList.add(new Bind("/Users/user/.m2", new Volume("/root/.m2")));
		//
		//
		HostConfig hostConfig = HostConfig.newHostConfig().withBinds(bindList);
//		.withMounts(mounts);
		createContainerCmd.withHostConfig(hostConfig);

		String[] entrypoint = {"/bin/sh", "-c"};
		String[] cmd = {"mkdir -p /root/.m2/ && ln -s /root/settings.xml /root/.m2/settings.xml && mvn clean package"};
//		String[] cmd = {""};
		createContainerCmd.withEntrypoint(entrypoint);


		createContainerCmd.withCmd(cmd);

		//
		// 检查镜像是否存在本地
		boolean imagePull = false;
		try {
			this.dockerClient.inspectImageCmd(image).exec();
		} catch (NotFoundException e) {
			log.info("镜像不存在，需要下载");
			imagePull = true;
		}
		// 拉取镜像
		if (imagePull) {
			try {
				this.dockerClient.pullImageCmd(image).exec(new ResultCallback.Adapter<PullResponseItem>() {
					@Override
					public void onNext(PullResponseItem object) {
						log.info("镜像下载成功: {} status: {}", object.getId(), object.getStatus());
					}
				}).awaitCompletion();
			} catch (InterruptedException | RuntimeException e) {
				log.error("镜像下载失败:", e);
				//				this.publisher.publishEvent(TaskFailedEvent.builder()
				//						.triggerId(dockerTask.getTriggerId())
				//						.taskId(dockerTask.getTaskInstanceId())
				//						.errorMsg(e.getMessage())
				//						.build());
				Thread.currentThread().interrupt();
				return;
			}
		}
		//
		// 创建容器
		CreateContainerResponse containerResponse;
		try {
			containerResponse = createContainerCmd.exec();
		} catch (RuntimeException e) {
			log.error("无法创建容器", e);
			return;
		}
//		String containerId =;
		this.containerId = containerResponse.getId();

		List<String> split = StrUtil.split("/Users/user/.m2/settings.xml:/root/:false", StrUtil.COLON);
		dockerClient.copyArchiveToContainerCmd(containerId)
				.withHostResource(split.get(0))
				.withRemotePath(split.get(1))
				.withNoOverwriteDirNonDir(true)
				.withDirChildrenOnly(Convert.toBool(CollUtil.get(split, 2), true))
				.exec();

		// 启动容器
		try {
			this.dockerClient.startContainerCmd(containerId).exec();
		} catch (RuntimeException e) {
			log.error("容器启动失败:", e);
			return;
		}
		// 获取日志
		try {
			this.dockerClient.logContainerCmd(containerId)
					.withStdOut(true)
					.withStdErr(true)
					.withTailAll()
					.withFollowStream(true)
					.exec(new ResultCallback.Adapter<Frame>() {
						@Override
						public void onNext(Frame object) {
							String s = new String(object.getPayload(), StandardCharsets.UTF_8);
							System.out.print(s);
						}
					}).awaitCompletion();
		} catch (InterruptedException e) {
			log.error("获取容器日志操作被中断:", e);

			Thread.currentThread().interrupt();
		} catch (RuntimeException e) {
			log.error("获取容器日志失败", e);

			Thread.currentThread().interrupt();
		}
		// 等待容器执行结果
		try {
			this.dockerClient.waitContainerCmd(containerId).exec(new ResultCallback.Adapter<WaitResponse>() {
				@Override
				public void onNext(WaitResponse object) {
					log.info("dockerTask status code is: {}", object.getStatusCode());
				}
			}).awaitCompletion();
		} catch (InterruptedException e) {
			log.error("获取容器执行结果操作被中断:", e);
			Thread.currentThread().interrupt();
		} catch (RuntimeException e) {
			log.error("获取容器执行结果失败", e);

			Thread.currentThread().interrupt();
		}
		// 获取容器执行结果文件(JSON,非数组)，转换为任务输出参数
		String re = "springboot-test-jar/target/";
		String resultFile = FileUtil.file(workingDir, re).getAbsolutePath();
		try (
				InputStream stream = this.dockerClient.copyArchiveFromContainerCmd(containerId, resultFile).exec();
				TarArchiveInputStream tarStream = new TarArchiveInputStream(stream);
				//BufferedReader reader = new BufferedReader(new InputStreamReader(tarStream, StandardCharsets.UTF_8))
		) {
//			tarStream.getNextTarEntry()
			TarArchiveEntry tarArchiveEntry;
			File file1 = FileUtil.file(file, re);
			FileUtil.del(file1);

			while ((tarArchiveEntry = tarStream.getNextTarEntry()) != null) {
				if (!tarStream.canReadEntryData(tarArchiveEntry)) {
					log.info("不能读取tarArchiveEntry");
				}
				if (tarArchiveEntry.isDirectory()) {
					continue;
				}
				log.info("tarArchiveEntry's name: {}", tarArchiveEntry.getName());
				File currentFile = FileUtil.file(file, re, tarArchiveEntry.getName());
				FileUtil.mkParentDirs(currentFile);
				IoUtil.copy(tarStream, new FileOutputStream(currentFile));
//			resultFile = IOUtils.toString(reader);
				// 将文件写出到解压的目录
				//IOUtils.copy(fin, new FileOutputStream(curfile));
			}

//			CompressionFileUtil
//			if (!tarArchiveEntry.isFile()) {
//				log.info("执行结果文件必须是文件类型, 不支持目录或其他类型");
//			}


			//FileUtil.del(file1);
			//Extractor extractor = CompressUtil.createExtractor(StandardCharsets.UTF_8, tarStream);
			//extractor.extract(file1);
			//IoUtil.copy(reader, new FileWriter(file1));
//			resultFile = IOUtils.toString(reader);
			log.info("结果文件内容: {}", resultFile);
		} catch (Exception e) {
			log.warn("无法获取容器执行结果文件: {}", e.getMessage());
		}


	}


	@org.junit.Test
	public void test2() throws InterruptedException, IOException {
		this.exec("ls");
//		this.exec("cd /lib");
//		this.exec("ls");
	}

	/**
	 * https://blog.csdn.net/will0532/article/details/78335280
	 *
	 * @param cmd
	 * @throws InterruptedException
	 * @see ExecStartCmdExec
	 */
	private void exec(String cmd) throws InterruptedException, IOException {
		ExecCreateCmd execCreateCmd = dockerClient.execCreateCmd("5848fd613ea4");
		execCreateCmd.withAttachStdout(true).withAttachStdin(true).withAttachStderr(true).withTty(true).withCmd("/bin/sh");
		ExecCreateCmdResponse exec = execCreateCmd.exec();
		String execId = exec.getId();
		ExecStartCmd execStartCmd = dockerClient.execStartCmd(execId);
		execStartCmd.withDetach(false).withTty(true);

		PipedInputStream in = new PipedInputStream();
		PipedOutputStream out = new PipedOutputStream(in);
//		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(stringWriter);
		//IoUtil.toStream(stringWriter.toString(), CharsetUtil.CHARSET_UTF_8);
		execStartCmd.withStdIn(in);
		InputStream stdin = execStartCmd.getStdin();
		System.out.println(stdin);

//		IoUtil.readLines(stdin, Charset.defaultCharset(), new LineHandler() {
//			@Override
//			public void handle(String line) {
//				System.out.println(line);
//			}
//		});
		ThreadUtil.execute(() -> {
			while (true) {
				try {
					out.write(StrUtil.bytes("ls \n"));
				} catch (IOException e) {
					e.printStackTrace();
				}
				ThreadUtil.sleep(2, TimeUnit.SECONDS);
			}
		});
		InvocationBuilder.AsyncResultCallback<Frame> resultCallback = execStartCmd.exec(new InvocationBuilder.AsyncResultCallback<Frame>() {
			@Override
			public void onNext(Frame frame) {
				System.out.println(frame);
			}
		});
		resultCallback.awaitCompletion();
	}

	@org.junit.Test
	public void testDockerfile() throws InterruptedException {
		File dir = FileUtil.file("/Users/user/IdeaProjects/Jpom-demo-case/springboot-test-jar/");
		BuildImageCmd buildImageCmd = dockerClient.buildImageCmd();
		buildImageCmd
				.withBaseDirectory(FileUtil.file(dir, "target/classes"))
				.withDockerfile(FileUtil.file(dir, "Dockerfile"))
//				.withQuiet()
				.withTags(CollUtil.newHashSet("jpom-test2"));
		buildImageCmd.exec(new InvocationBuilder.AsyncResultCallback<BuildResponseItem>() {


			@Override
			public void onNext(BuildResponseItem object) {
				String stream = object.getStream();
				if (stream == null) {
					String status = object.getStatus();
					if (status == null) {
						return;
					}
					System.out.print(StrUtil.format("{} {} {}", status, object.getId(), object.getProgressDetail()));
				}
				System.out.print(stream);
			}
		}).awaitCompletion();
	}

	@org.junit.Test
	public void testContainerCmd() {
		CreateContainerCmd containerCmd = dockerClient.createContainerCmd("e6cf7db033e2");
		CreateContainerCmd createContainerCmd = containerCmd.withName("jpom-build");

		HostConfig hostConfig = HostConfig.newHostConfig();
		PortBinding portBinding = PortBinding.parse("8084:8084");
		hostConfig.withPortBindings(portBinding);
		createContainerCmd.withHostConfig(hostConfig);
		CreateContainerResponse exec = containerCmd.exec();
	}

	@org.junit.Test
	public void testin() {
		InspectImageCmd inspectImageCmd = dockerClient.inspectImageCmd("e6cf7db033e2");
		InspectImageResponse inspectImageResponse = inspectImageCmd.exec();
		System.out.println(inspectImageResponse);
	}

	@org.junit.Test
	public void testinpull() throws InterruptedException {
		//docker pull jpomdocker/jpom:latest
		PullImageCmd pullImageCmd = dockerClient.pullImageCmd("jpomdocker/jpom:2.8.6");
//		pullImageCmd.withTag();
		pullImageCmd.exec(new InvocationBuilder.AsyncResultCallback<PullResponseItem>() {

			@Override
			public void onNext(PullResponseItem object) {
				System.out.println(object);
			}

		}).awaitCompletion();
	}
}
