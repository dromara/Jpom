import cn.hutool.core.io.FileUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Before;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2022/1/25
 */
@Slf4j
public class Test {

	private DockerClient dockerClient;

	@Before
	public void before() {
		DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
				// .withDockerHost("tcp://192.168.163.11:2376").build();
				.withDockerHost("tcp://127.0.0.1:2375").build();
		DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
				.dockerHost(config.getDockerHost())
				.sslConfig(config.getSSLConfig())
				.maxConnections(100)
				.connectionTimeout(Duration.ofSeconds(30))
				.responseTimeout(Duration.ofSeconds(45))
				.build();
		this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
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
		String absolutePath = FileUtil.getAbsolutePath(file);
		System.out.println(absolutePath);
		String image = "maven:3.8.4-jdk-8";
		String workingDir = "jpom";
		CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
		String name = "jpom-test";
		containerCmd.withName(name);
		CreateContainerCmd createContainerCmd = containerCmd.withWorkingDir(workingDir);
		//
		List<Mount> mounts = new ArrayList<>();
		mounts.add(new Mount()
				.withType(MountType.VOLUME)
				.withSource(absolutePath)
				.withTarget(workingDir));
		//
		HostConfig hostConfig = HostConfig.newHostConfig().withMounts(mounts);
		createContainerCmd.withHostConfig(hostConfig);
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
		// 启动容器
		try {
			this.dockerClient.startContainerCmd(containerResponse.getId()).exec();
		} catch (RuntimeException e) {
			log.error("容器启动失败:", e);
			return;
		}
		// 获取日志
		try {
			this.dockerClient.logContainerCmd(containerResponse.getId())
					.withStdOut(true)
					.withStdErr(true)
					.withTailAll()
					.withFollowStream(true)
					.exec(new ResultCallback.Adapter<Frame>() {
						@Override
						public void onNext(Frame object) {
							String s = new String(object.getPayload(), StandardCharsets.UTF_8);
							System.out.println(s);
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
			this.dockerClient.waitContainerCmd(containerResponse.getId()).exec(new ResultCallback.Adapter<WaitResponse>() {
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
		String resultFile = null;
		String re = "modules/server/target/server-2.8.7-release.zip";

		try (
				InputStream stream = this.dockerClient.copyArchiveFromContainerCmd(containerResponse.getId(), re).exec();
				TarArchiveInputStream tarStream = new TarArchiveInputStream(stream);
				BufferedReader reader = new BufferedReader(new InputStreamReader(tarStream, StandardCharsets.UTF_8))
		) {
			TarArchiveEntry tarArchiveEntry = tarStream.getNextTarEntry();
			if (!tarStream.canReadEntryData(tarArchiveEntry)) {
				log.info("不能读取tarArchiveEntry");
			}
			if (!tarArchiveEntry.isFile()) {
				log.info("执行结果文件必须是文件类型, 不支持目录或其他类型");
			}
			log.info("tarArchiveEntry's name: {}", tarArchiveEntry.getName());
			resultFile = IOUtils.toString(reader);
			log.info("结果文件内容: {}", resultFile);
		} catch (Exception e) {
			log.warn("无法获取容器执行结果文件: {}", e.getMessage());
		}

		// 清除容器
		this.dockerClient.removeContainerCmd(containerResponse.getId())
				.withRemoveVolumes(true)
				.withForce(true)
				.exec();
	}
}
